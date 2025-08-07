package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerValidationTest {

    private final UserController controller = new UserController();

    @Test
    void validateUserThrowIfEmailIsEmpty() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setName("name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Электронная почта"));
    }

    @Test
    void validateUserThrowIfEmailWithout() {
        User user = new User();
        user.setEmail("mailmail.com");
        user.setLogin("login");
        user.setName("name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Электронная почта"));
    }

    @Test
    void validateUserLoginEmpty() {
        User user = new User();
        user.setEmail("mail@mail.com");
        user.setLogin("");
        user.setName("name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Логин"));
    }

    @Test
    void validateUserLoginWithSpace() {
        User user = new User();
        user.setEmail("mail@mail.com");
        user.setLogin("lo gin");
        user.setName("name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Логин"));
    }

    @Test
    void validateUserBirthdayInFuture() {
        User user = new User();
        user.setEmail("mail@mail.com");
        user.setLogin("login");
        user.setName("name");
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addUser(user));
        assertTrue(exception.getMessage().contains("Дата рождения"));
    }

    @Test
    void validateUserPassIfAllOk() {
        User user = new User();
        user.setEmail("mail@mail.com");
        user.setLogin("login");
        user.setName("name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertDoesNotThrow(() -> controller.addUser(user));
    }
}