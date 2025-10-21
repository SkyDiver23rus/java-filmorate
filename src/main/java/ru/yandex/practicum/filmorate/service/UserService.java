package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User addUser(User user) {
        validateUser(user);
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        User stored = userStorage.getUserById(user.getId());
        if (stored == null) {
            throw new NotFoundException("Пользователь с таким id не найден.");
        }
        validateUser(user);
        return userStorage.updateUser(user);
    }

    public User getUser(int id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с таким id не найден.");
        }
        return user;
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public void addFriend(int id, int friendId) {
        User user = userStorage.getUserById(id);
        User friend = userStorage.getUserById(friendId);
        if (user == null || friend == null) {
            throw new NotFoundException("Пользователь с таким id не найден.");
        }
        user.getFriends().add(friendId);
        friend.getFriends().add(id);
    }

    public void removeFriend(int id, int friendId) {
        User user = userStorage.getUserById(id);
        User friend = userStorage.getUserById(friendId);
        if (user == null || friend == null) {
            throw new NotFoundException("Пользователь с таким id не найден.");
        }
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
    }

    public List<User> getFriends(int id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с таким id не найден.");
        }
        Set<Integer> friendsIds = user.getFriends();
        return friendsIds.stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(int id, int otherId) {
        User user1 = userStorage.getUserById(id);
        User user2 = userStorage.getUserById(otherId);
        if (user1 == null || user2 == null) {
            throw new NotFoundException("Пользователь с таким id не найден.");
        }
        Set<Integer> commonIds = user1.getFriends().stream()
                .filter(user2.getFriends()::contains)
                .collect(Collectors.toSet());
        return commonIds.stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    private void validateUser(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Email должен быть корректным и содержать символ @.");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}