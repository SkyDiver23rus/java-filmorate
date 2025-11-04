package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User addUser(User user) {
        validateUser(user);
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        validateUserExists(user.getId());
        validateUser(user);
        return userStorage.updateUser(user);
    }

    public User getUser(int id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id " + id + " не найден.");
        }
        return user;
    }

    public List<User> getAllUsers() {

        return userStorage.getAllUsers();
    }

    public void addFriend(int id, int friendId) {
        validateUserExists(id);
        validateUserExists(friendId);
        userStorage.addFriend(id, friendId);
    }

    public void removeFriend(int id, int friendId) {
        validateUserExists(id);
        validateUserExists(friendId);
        userStorage.removeFriend(id, friendId);
    }

    public List<User> getFriends(int id) {
        User user = validateUserExists(id);
        User userWithFriends = userStorage.getUserById(id);
        return userWithFriends.getFriends().stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(int id, int otherId) {
        validateUserExists(id);
        validateUserExists(otherId);
        return userStorage.getCommonFriends(id, otherId);
    }


    private User getUserWithFriends(int id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id " + id + " не найден.");
        }
        return user;
    }

    public User validateUserExists(int userId) {
        User user = userStorage.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        return user;
    }

    public void validateUser(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым.");
        }
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым.");
        }
        if (!user.getEmail().contains("@")) {
            throw new ValidationException("Email должен содержать символ @.");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}