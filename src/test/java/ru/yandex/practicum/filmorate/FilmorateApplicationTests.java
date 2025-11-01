package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
class FilmorateApplicationTests {

    private final UserDbStorage userStorage;

    @Test
    public void testFindUserById() {
        // Сначала создаем пользователя
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testLogin");
        user.setName("Test Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userStorage.addUser(user);

        // Теперь ищем его
        Optional<User> userOptional = userStorage.findUserById(createdUser.getId());

        // Простая проверка без лямбды
        assertThat(userOptional).isPresent();
        assertThat(userOptional.get().getId()).isEqualTo(createdUser.getId());
    }

    @Test
    public void testFindUserById_NotFound() {
        Optional<User> userOptional = userStorage.findUserById(999);
        assertThat(userOptional).isEmpty();
    }

    @Test
    public void testCreateAndGetUser() {
        User user = new User();
        user.setEmail("newuser@mail.ru");
        user.setLogin("newuser");
        user.setName("New User");
        user.setBirthday(LocalDate.of(1995, 5, 15));

        User createdUser = userStorage.addUser(user);

        assertThat(createdUser.getId()).isPositive();
        assertThat(createdUser.getEmail()).isEqualTo("newuser@mail.ru");
        assertThat(createdUser.getLogin()).isEqualTo("newuser");
    }
}