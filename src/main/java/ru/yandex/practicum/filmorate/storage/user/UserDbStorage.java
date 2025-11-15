package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;

import java.sql.Date;
import java.sql.*;
import java.util.*;

@Repository
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final EventDbStorage eventDbStorage;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate, EventDbStorage eventDbStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventDbStorage = eventDbStorage;
    }

    @Override
    public User addUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        return user;
    }

    @Override
    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);
        if (users.isEmpty()) {
            return null;
        }
        User user = users.get(0);
        user.setFriends(getUserFriends(user.getId()));
        return user;
    }

    @Override
    public Optional<User> findUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);
        Map<Integer, Set<Integer>> allFriends = getAllFriends();
        for (User user : users) {
            user.setFriends(allFriends.getOrDefault(user.getId(), new HashSet<>()));
        }

        return users;
    }

    @Override
    public void addFriend(int userId, int friendId) {
        String sql = "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, userId, friendId);
            // Логирование события добавления друга
            eventDbStorage.addEvent(userId, EventType.FRIEND, Operation.ADD, friendId);
        } catch (Exception e) {
            // игнорируем
        }
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId, friendId);

        // Логируем событие только если удаление произошло
        if (rowsAffected > 0) {
            eventDbStorage.addEvent(userId, EventType.FRIEND, Operation.REMOVE, friendId);
        }
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        user.setFriends(new HashSet<>());
        return user;
    }

    @Override
    public Set<Integer> getUserFriends(int userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        List<Integer> friends = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("friend_id"), userId);
        return new HashSet<>(friends);
    }

    @Override
    public List<User> getCommonFriends(int userId1, int userId2) {
        // Получаем id общих друзей через
        String sql = "SELECT f1.friend_id " +
                "FROM friendships f1 " +
                "INNER JOIN friendships f2 ON f1.friend_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        List<Integer> commonFriendIds = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("friend_id"), userId1, userId2);

        if (commonFriendIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Групповая загрузка юхеров
        String inSql = String.join(",", Collections.nCopies(commonFriendIds.size(), "?"));
        String userSql = "SELECT * FROM users WHERE id IN (" + inSql + ")";
        List<User> users = jdbcTemplate.query(userSql, this::mapRowToUser, commonFriendIds.toArray());
        return users;
    }

    private Map<Integer, Set<Integer>> getAllFriends() {
        String sql = "SELECT user_id, friend_id FROM friendships";
        Map<Integer, Set<Integer>> allFriends = new HashMap<>();

        jdbcTemplate.query(sql, (rs, rowNum) -> {
            int userId = rs.getInt("user_id");
            int friendId = rs.getInt("friend_id");
            allFriends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
            return null;
        });

        return allFriends;
    }

    //по задаче удаление
    @Override
    public void deleteUser(int id) {
        try {
            // Удаляем дружбу (входящие и исходящие)
            String deleteOutgoingFriendshipsSql = "DELETE FROM friendships WHERE user_id = ?";
            jdbcTemplate.update(deleteOutgoingFriendshipsSql, id);

            String deleteIncomingFriendshipsSql = "DELETE FROM friendships WHERE friend_id = ?";
            jdbcTemplate.update(deleteIncomingFriendshipsSql, id);

            // Удаляем лайки пользователя
            String deleteLikesSql = "DELETE FROM film_likes WHERE user_id = ?";
            jdbcTemplate.update(deleteLikesSql, id);

            // Удаляем пользователя
            String deleteUserSql = "DELETE FROM users WHERE id = ?";
            jdbcTemplate.update(deleteUserSql, id);
        } catch (Exception e) {
            throw new RuntimeException("Database error while deleting user", e);
        }
    }
}