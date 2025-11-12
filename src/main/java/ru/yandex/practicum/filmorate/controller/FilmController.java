package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<Film> addFilm(@RequestBody Film film) {
        Film created = filmService.addFilm(film);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@RequestBody Film film) {
        Film updated = filmService.updateFilm(film);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilm(@PathVariable int id) {
        return ResponseEntity.ok(filmService.getFilm(id));
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        return ResponseEntity.ok(filmService.getAllFilms());
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable int id, @PathVariable int userId) {
        filmService.addLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable int id, @PathVariable int userId) {
        filmService.removeLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Film>> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(filmService.getPopularFilms(count));
    }

    // получение всех фильмов режиссера
    @GetMapping("/director/{directorId}")
    public ResponseEntity<List<Film>> filmByDirector(@PathVariable int directorId, @RequestParam String sortBy) {
        return ResponseEntity.ok(filmService.getFilmsByDirectorSorted(directorId, sortBy));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Film>> getFilmsByFilter(@RequestParam(required = false) String query, @RequestParam(required = false) List<String> by) {
        return ResponseEntity.ok(filmService.getFilmsByFilter(query, by));
    }
}