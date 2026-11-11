package org.adventit.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.adventit.application.port.out.UserRepository;
import org.adventit.domain.user.EmailAlreadyRegisteredException;
import org.adventit.domain.user.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SqliteUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public SqliteUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT id, email, password_hash, created_at FROM users WHERE email = ?",
                (resultSet, rowNumber) -> mapUser(resultSet),
                email).stream().findFirst();
    }

    @Override
    public User save(User user) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO users (email, password_hash, created_at) VALUES (?, ?, ?)",
                    user.email(),
                    user.passwordHash(),
                    user.createdAt().toString());
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
        return findByEmail(user.email()).orElseThrow(
                () -> new IllegalStateException("The saved user could not be loaded"));
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                Instant.parse(resultSet.getString("created_at")));
    }
}
