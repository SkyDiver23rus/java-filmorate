package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserStorage userStorage;

    public UserController(UserService userService, UserStorage userStorage) {
        this.userService = userService;
        this.userStorage = userStorage;
    }

    @PostMapping
    public User addUser(@Valid @RequestBody User user) {
        validateUser(user);
        User created = userStorage.addUser(user);
        log.info("Добавлен пользователь: {}", created);
        return created;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        User stored = userStorage.getUserById(user.getId());
        if (stored == null) {
            throw new ResourceNotFoundException("Пользователь с таким id не найден.");
        }
        validateUser(user);
        User updated = userStorage.updateUser(user);
        log.info("Обновлён пользователь: {}", updated);
        return updated;
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable int id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new ResourceNotFoundException("Пользователь с таким id не найден.");
        }
        return user;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable int id, @PathVariable int friendId) {
        if (userStorage.getUserById(id) == null) {
            throw new ResourceNotFoundException("Пользователь с таким id не найден.");
        }
        if (userStorage.getUserById(friendId) == null) {
            throw new ResourceNotFoundException("Друг с таким id не найден.");
        }
        userService.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable int id, @PathVariable int friendId) {
        if (userStorage.getUserById(id) == null) {
            throw new ResourceNotFoundException("Пользователь с таким id не найден.");
        }
        if (userStorage.getUserById(friendId) == null) {
            throw new ResourceNotFoundException("Друг с таким id не найден.");
        }
        userService.removeFriend(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable int id) {
        if (userStorage.getUserById(id) == null) {
            throw new ResourceNotFoundException("Пользователь с таким id не найден.");
        }
        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable int id, @PathVariable int otherId) {
        if (userStorage.getUserById(id) == null || userStorage.getUserById(otherId) == null) {
            throw new ResourceNotFoundException("Пользователь с таким id не найден.");
        }
        return userService.getCommonFriends(id, otherId);
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