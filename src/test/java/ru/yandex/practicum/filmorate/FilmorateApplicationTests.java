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
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmorateApplicationTests {

    private final UserDbStorage userStorage;

    @Test
    public void testFindUserById() {

        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User createdUser = userStorage.addUser(user);
        int userId = createdUser.getId();

        Optional<User> userOptional = userStorage.findUserById(userId);

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(foundUser ->
                        assertThat(foundUser).hasFieldOrPropertyWithValue("id", userId)
                );
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