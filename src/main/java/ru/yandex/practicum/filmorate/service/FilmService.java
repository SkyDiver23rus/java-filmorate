package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.*;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;

    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public void addLike(int filmId, int userId) {
        Film film = getFilmOrThrow(filmId);
        film.getLikes().add(userId);
        log.info("Пользователь {} лайкнул фильм {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        Film film = getFilmOrThrow(filmId);
        film.getLikes().remove(userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        List<Film> films = filmStorage.getAllFilms();
        films.sort((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()));
        return films.subList(0, Math.min(count, films.size()));
    }

    private Film getFilmOrThrow(int id) {
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id " + id + " не найден.");
        }
        return film;
    }
}