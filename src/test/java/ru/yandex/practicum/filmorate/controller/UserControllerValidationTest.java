package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerValidationTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        UserService userService = new UserService(new InMemoryUserStorage());
        controller = new UserController(userService);
    }

    @Test
    void validateUserLoginIsEmpty() {
        User user = new User();
        user.setLogin("");
        user.setEmail("test@mail.com");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            controller.addUser(user);
        });
        assertTrue(exception.getMessage().contains("Логин"));
    }

    @Test
    void validateUserLoginContainsSpace() {
        User user = new User();
        user.setLogin("bad login");
        user.setEmail("test@mail.com");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Логин"));
    }

    @Test
    void validateUserEmailIsEmpty() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Email"));
    }

    @Test
    void validateUserEmailNoAt() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("badmail.com");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Email"));
    }

    @Test
    void validateUserBirthdayInFuture() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("test@mail.com");
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Дата рождения"));
    }

    @Test
    void validateUserPassIfAllOk() {
        User user = new User();
        user.setLogin("login");
        user.setEmail("test@mail.com");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertDoesNotThrow(() -> controller.addUser(user));
    }
}