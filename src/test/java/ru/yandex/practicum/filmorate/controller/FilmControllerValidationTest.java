package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.DAO.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.DAO.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerValidationTest {

    private FilmController controller;
    private MpaDbStorage mpaDbStorage;
    private GenreDbStorage genreDbStorage;

    @BeforeEach
    void setUp() {
        mpaDbStorage = Mockito.mock(MpaDbStorage.class);
        genreDbStorage = Mockito.mock(GenreDbStorage.class);

        // по умолчанию любой MPA "найден"
        Mockito.when(mpaDbStorage.getMpaById(Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    int id = invocation.getArgument(0);
                    Mpa mpa = new Mpa();
                    mpa.setId(id);
                    mpa.setName("Test");
                    return Optional.of(mpa);
                });

        FilmService filmService = new FilmService(
                new InMemoryFilmStorage(),
                new InMemoryUserStorage(),
                genreDbStorage,
                mpaDbStorage
        );
        controller = new FilmController(filmService);
    }

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

        // MPA по умолчанию будет найден через мок
        assertDoesNotThrow(() -> controller.addFilm(film));
    }
}