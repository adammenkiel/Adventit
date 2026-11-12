package org.adventit.adapter.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import org.adventit.application.port.out.AuthenticationTokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationTokenProvider authenticationTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtCookieAuthenticationFilter(
            AuthenticationTokenProvider authenticationTokenProvider,
            UserDetailsService userDetailsService) {
        this.authenticationTokenProvider = authenticationTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<String> token = getToken(request);
            if (token.isPresent()) {
                Optional<String> subject = authenticationTokenProvider.getSubject(token.get());
                if (subject.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication token is incorrect!");
                    return;
                }
                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(subject.get());
                } catch (UsernameNotFoundException exception) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Authentication token is incorrect!");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails.getUsername(),
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
