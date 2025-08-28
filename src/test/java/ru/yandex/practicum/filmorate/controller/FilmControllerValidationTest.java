package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerValidationTest {

    private final FilmController controller = new FilmController(
            new FilmService(new InMemoryFilmStorage()),
            new InMemoryFilmStorage(),
            new InMemoryUserStorage()

    );

    @Test
    void validateFilmNameIsEmpty() {
        Film film = new Film();
        film.setName("");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(10);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            controller.addFilm(film);
        });
        assertTrue(exception.getMessage().contains("Название фильма"));
    }

    @Test
    void validateFilmDescriptionTooLong() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(10);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addFilm(film));
        assertTrue(exception.getMessage().contains("Максимальная длина описания"));
    }

    @Test
    void validateFilmIfReleaseDateTooEarly() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(10);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addFilm(film));
        assertTrue(exception.getMessage().contains("Дата релиза"));
    }

    @Test
    void validateFilmDurationZeroOrNegative() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.addFilm(film));
        assertTrue(exception.getMessage().contains("Продолжительность"));
    }

    @Test
    void validateFilmPassIfAllOk() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(10);

        assertDoesNotThrow(() -> controller.addFilm(film));
    }
}