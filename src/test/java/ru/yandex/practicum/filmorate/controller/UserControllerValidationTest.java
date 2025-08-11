package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerValidationTest {

    @Test
    void shouldSetLoginAsNameIfNameIsBlank() {
        UserController controller = new UserController();
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("testuser");
        user.setName("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User result = controller.addUser(user);

        assertEquals("testuser", result.getName(), "Имя должно быть равно логину, если оно пустое");
    }
}