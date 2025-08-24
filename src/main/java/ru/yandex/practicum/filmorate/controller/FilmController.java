package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;
    private final FilmStorage filmStorage;

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public FilmController(FilmService filmService, FilmStorage filmStorage) {
        this.filmService = filmService;
        this.filmStorage = filmStorage;
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        validateFilm(film);
        Film created = filmStorage.addFilm(film);
        log.info("Добавлен фильм: {}", created);
        return created;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        if (filmStorage.getFilmById(film.getId()) == null) {
            throw new ResourceNotFoundException("Фильм с таким id не найден.");
        }
        validateFilm(film);
        Film updated = filmStorage.updateFilm(film);
        log.info("Обновлён фильм: {}", updated);
        return updated;
    }

    @GetMapping("/{id}")
    public Film getFilm(@PathVariable int id) {
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new ResourceNotFoundException("Фильм с таким id не найден.");
        }
        return film;
    }

    @GetMapping
    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable int id, @PathVariable int userId) {
        // Проверка существования фильма и пользователя (реализуйте в сервисе или тут)
        if (filmStorage.getFilmById(id) == null) {
            throw new ResourceNotFoundException("Фильм с таким id не найден.");
        }
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable int id, @PathVariable int userId) {
        if (filmStorage.getFilmById(id) == null) {
            throw new ResourceNotFoundException("Фильм с таким id не найден.");
        }
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        return filmService.getPopularFilms(count);
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации: пустое название фильма");
            throw new ValidationException("Название фильма не может быть пустым.");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Ошибка валидации: слишком длинное описание");
            throw new ValidationException("Максимальная длина описания — 200 символов.");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Ошибка валидации: некорректная дата релиза");
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года.");
        }
        if (film.getDuration() <= 0) {
            log.warn("Ошибка валидации: отрицательная или нулевая продолжительность");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }
    }
}