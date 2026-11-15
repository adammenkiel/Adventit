package org.adventit.adapter.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private static final String TOKEN_COOKIE_NAME = "token";
    private static final String INVALID_TOKEN_MESSAGE = "Authentication token is incorrect!";

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
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> token = getToken(request);
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> subject = authenticationTokenProvider.getSubject(token.get());
        if (subject.isEmpty()) {
            rejectInvalidToken(response);
            return;
        }

        UserDetails userDetails = loadUserDetails(subject.get(), response);
        if (userDetails == null) {
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(createAuthentication(request, userDetails));
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private UserDetails loadUserDetails(String email, HttpServletResponse response) throws IOException {
        try {
            return userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException exception) {
            rejectInvalidToken(response);
            return null;
        }
    }

    private UsernamePasswordAuthenticationToken createAuthentication(
            HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails.getUsername(),
                        null,
                        userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private void rejectInvalidToken(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, INVALID_TOKEN_MESSAGE);
    }

    private Optional<String> getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
