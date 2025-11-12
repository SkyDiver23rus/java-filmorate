package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.DAO.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.DAO.MpaDbStorage;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        FilmDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmorateApplicationTests {

    @Autowired
    private UserDbStorage userStorage;

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private GenreDbStorage genreStorage;

    @Autowired
    private MpaDbStorage mpaStorage;

    // UserDbStorage
    @Test
    public void testUserStorageCreateAndFind() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User created = userStorage.addUser(user);

        Optional<User> found = userStorage.findUserById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@mail.ru");
    }

    // FilmDbStorage тесты
    @Test
    public void testFilmStorageCreateAndFind() {
        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("Epic sci-fi");
        film.setReleaseDate(LocalDate.of(2009, 12, 18));
        film.setDuration(162);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        film.setMpa(mpa);

        Genre genre = new Genre();
        genre.setId(1);
        genre.setName("Боевик");
        film.setGenres(Collections.singletonList(genre));

        Film created = filmStorage.addFilm(film);
        Optional<Film> found = filmStorage.findFilmById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Avatar");
    }

    //  GenreDbStorage тест
    @Test
    public void testGetAllGenres() {
        assertThat(genreStorage.getAllGenres()).isNotEmpty();
    }

    //  MpaDbStorage тест
    @Test
    public void testGetAllMpa() {
        assertThat(mpaStorage.getAllMpa()).isNotEmpty();
    }

    //  тесты для методов валидации
    @Test
    public void testValidateFilm_valid() {
        FilmService filmService = new FilmService(filmStorage, userStorage, mpaStorage, genreStorage);

        Film validFilm = new Film();
        validFilm.setName("Titanic");
        validFilm.setDescription("Ship romance");
        validFilm.setReleaseDate(LocalDate.of(1997, 12, 19));
        validFilm.setDuration(195);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        validFilm.setMpa(mpa);

        filmService.validateFilm(validFilm);
    }

    @Test
    public void testValidateFilm_invalid() {
        FilmService filmService = new FilmService(filmStorage, userStorage, mpaStorage, genreStorage);

        Film invalidFilm = new Film();
        invalidFilm.setName(""); // Пустое имя
        invalidFilm.setDescription(" ");
        invalidFilm.setReleaseDate(LocalDate.of(1800, 1, 1)); // слишком ранняя дата
        invalidFilm.setDuration(-100);
        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        invalidFilm.setMpa(mpa);

        assertThatThrownBy(() -> filmService.validateFilm(invalidFilm))
                .isInstanceOfAny(ru.yandex.practicum.filmorate.exception.ValidationException.class);
    }

    @Test
    public void testValidateUser_valid() {
        UserService userService = new UserService(userStorage);

        User validUser = new User();
        validUser.setEmail("ok@mail.ru");
        validUser.setLogin("validLogin");
        validUser.setName("Valid User");
        validUser.setBirthday(LocalDate.of(2000, 1, 1));
        userService.validateUser(validUser);
    }

    @Test
    public void testValidateUser_invalid() {
        UserService userService = new UserService(userStorage);

        User invalidUser = new User();
        invalidUser.setEmail(""); // Некорректный email
        invalidUser.setLogin(""); // Некорректный login
        invalidUser.setBirthday(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> userService.validateUser(invalidUser))
                .isInstanceOfAny(ru.yandex.practicum.filmorate.exception.ValidationException.class);
    }
}