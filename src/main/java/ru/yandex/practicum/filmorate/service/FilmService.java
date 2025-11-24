package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.DAO.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.DAO.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaDbStorage mpaDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final DirectorService directorService;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final EventStorage eventStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaDbStorage mpaDbStorage,
                       GenreDbStorage genreDbStorage,
                       DirectorService directorService,
                       EventStorage eventStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaDbStorage = mpaDbStorage;
        this.genreDbStorage = genreDbStorage;
        this.directorService = directorService;
        this.eventStorage = eventStorage;
    }

    public Film addFilm(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);

        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        Film existingFilm = filmStorage.getFilmById(film.getId());
        if (existingFilm == null) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден.");
        }
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);

        return filmStorage.updateFilm(film);
    }

    public Film getFilm(int id) {
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id " + id + " не найден.");
        }
        return film;
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void addLike(int filmId, int userId) {
        Film film = filmStorage.getFilmById(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден.");
        }
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        filmStorage.addLike(filmId, userId);
        // Логируем событие добавления лайка
        eventStorage.addEvent(userId, EventType.LIKE, Operation.ADD, filmId);
    }

    public void removeLike(int filmId, int userId) {
        Film film = filmStorage.getFilmById(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден.");
        }
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        filmStorage.removeLike(filmId, userId);
        // Логируем событие удаления лайка
        eventStorage.addEvent(userId, EventType.LIKE, Operation.REMOVE, filmId);
    }

    public List<Film> getPopularFilms(int count, Integer genreId, Integer year) {
        if (count <= 0) {
            throw new ValidationException("Количество фильмов должно быть положительным числом.");
        }
        if (genreId != null) {
            validateGenre(genreId);
        }
        if (year != null && year < CINEMA_BIRTHDAY.getYear()) {
            throw new ValidationException("Год не может быть меньше 1895");
        }
        return filmStorage.getPopularFilms(count, genreId, year);
    }

    public List<Film> getFilmsByDirectorSorted(int directorId, String sortBy) {
        if (!"likes".equalsIgnoreCase(sortBy) && !"year".equalsIgnoreCase(sortBy)) {
            throw new ValidationException("Параметр sortBy должен быть 'likes' или 'year'.");
        }
        directorService.checkExists(directorId);

        return ((FilmDbStorage) filmStorage).getFilmsByDirectorSorted(directorId, sortBy);
    }

    private void validateMpa(Film film) {
        if (film.getMpa() == null) {
            Mpa defaultMpa = new Mpa();
            defaultMpa.setId(1);
            defaultMpa.setName("G");
            film.setMpa(defaultMpa);
            return;
        }

        if (!mpaDbStorage.existsById(film.getMpa().getId())) {
            throw new NotFoundException("MPA с id " + film.getMpa().getId() + " не найден.");
        }
    }

    private void validateGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        for (Genre genre : film.getGenres()) {
            if (!genreDbStorage.getGenreById(genre.getId()).isPresent()) {
                throw new NotFoundException("Жанр с id " + genre.getId() + " не найден.");
            }
        }
    }

    public void validateFilm(Film film) {
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

    public List<Film> getFilmsByFilter(String query, List<String> by) {
        Set<String> allowedParametersForSearch = Set.of("director", "title");

        if ((query != null && by.isEmpty()) || (query == null && !by.isEmpty())) {
            throw new ValidationException("Не полный список парметров запроса.");
        }
        if (!allowedParametersForSearch.containsAll(by)) {
            throw new ValidationException("Неверные параметры запроса.");
        }

        return filmStorage.getFilmsByFilter(query, by);
    }

    private void validateGenre(int genreId) {
        if (genreDbStorage.getGenreById(genreId).isEmpty()) {
            throw new NotFoundException("Жанр с id " + genreId + " не найден.");
        }
    }

    //По задаче рекомендации
    public List<Film> getRecommendedFilms(int userId) {
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        try {
            return filmStorage.getRecommendedFilms(userId);
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя с id {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Ошбка при получении рекомендаций", e);
        }
    }

    //по задаче удаление
    public void deleteFilm(int id) {
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id " + id + " не найден.");
        }
        filmStorage.deleteFilm(id);
    }

    // по "Общим фильмам"
    public List<Film> getCommonFilms(int userId, int friendId) {
        return filmStorage.getCommonFilms(userId, friendId);
    }
}