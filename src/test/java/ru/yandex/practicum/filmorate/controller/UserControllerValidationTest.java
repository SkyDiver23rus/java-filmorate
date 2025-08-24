package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerValidationTest {

    @Test
    void shouldSetLoginAsNameIfNameIsBlank() {
        InMemoryUserStorage storage = new InMemoryUserStorage();
        UserService service = new UserService(storage);
        UserController controller = new UserController(service, storage);

        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("testuser");
        user.setName("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User result = controller.addUser(user);

        assertEquals("testuser", result.getName(), "Имя должно быть равно логину, если оно пустое");
    }
}