package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User addUser(User user);

    User updateUser(User user);

    User getUserById(int id);

    // Добавьте этот метод для совместимости с тестами
    Optional<User> findUserById(int id);

    List<User> getAllUsers();

    void addFriend(int userId, int friendId);

    void removeFriend(int userId, int friendId);
}