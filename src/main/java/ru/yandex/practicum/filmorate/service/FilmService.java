package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    // Список допустимых жанров
    private final Set<Integer> validGenreIds = Set.of(1, 2, 3, 4, 5, 6);
    // Список допустимых MPA рейтингов
    private final Set<Integer> validMpaIds = Set.of(1, 2, 3, 4, 5);

    public FilmService(@Qualifier("inMemoryFilmStorage") FilmStorage filmStorage,
                       @Qualifier("inMemoryUserStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film addFilm(Film film) {
        log.info("Adding film: {}", film);
        validateFilm(film);
        validateMpaAndGenres(film);
        setDefaultMpa(film);
        Film savedFilm = filmStorage.addFilm(film);
        log.info("Film added successfully: {}", savedFilm);
        return savedFilm;
    }

    public Film updateFilm(Film film) {
        log.info("Updating film: {}", film);
        validateFilmExists(film.getId());
        validateFilm(film);
        validateMpaAndGenres(film);
        setDefaultMpa(film);
        Film updatedFilm = filmStorage.updateFilm(film);
        if (updatedFilm == null) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден.");
        }
        log.info("Film updated successfully: {}", updatedFilm);
        return updatedFilm;
    }

    public Film getFilm(int id) {
        log.info("Getting film with id: {}", id);
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id " + id + " не найден.");
        }
        return film;
    }

    public List<Film> getAllFilms() {
        log.info("Getting all films");
        return filmStorage.getAllFilms();
    }

    public void addLike(int filmId, int userId) {
        log.info("Adding like from user {} to film {}", userId, filmId);
        validateFilmExists(filmId);
        validateUserExists(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        log.info("Removing like from user {} to film {}", userId, filmId);
        validateFilmExists(filmId);
        validateUserExists(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        log.info("Getting {} popular films", count);
        return filmStorage.getAllFilms().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    // Вспомогательные методы
    private void validateFilmExists(int filmId) {
        if (filmStorage.getFilmById(filmId) == null) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден.");
        }
    }

    private void validateUserExists(int userId) {
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
    }

    private void setDefaultMpa(Film film) {
        if (film.getMpa() == null) {
            Mpa defaultMpa = new Mpa();
            defaultMpa.setId(1);
            defaultMpa.setName("G");
            film.setMpa(defaultMpa);
        }
    }

    private void validateMpaAndGenres(Film film) {
        // Валидация MPA
        if (film.getMpa() != null && !validMpaIds.contains(film.getMpa().getId())) {
            throw new NotFoundException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден.");
        }

        // Валидация жанров
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (!validGenreIds.contains(genre.getId())) {
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не найден.");
                }
            }
        }
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым.");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        }
        if (film.getReleaseDate() == null) {
            throw new ValidationException("Дата релиза не может быть пустой.");
        }
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года.");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }
    }
}