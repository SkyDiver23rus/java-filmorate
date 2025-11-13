package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
public class DirectorController {
    private final DirectorService directorService;

    @GetMapping
    public ResponseEntity<Collection<Director>> getAll() {
        log.info("GET /directors — запрошен список всех режиссёров");
        return ResponseEntity.ok(directorService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Director> getById(@PathVariable int id) {
        log.info("GET /directors/{} — получение режиссёра", id);
        Director director = directorService.getById(id);
        return ResponseEntity.ok(director);
    }

    @PostMapping
    public ResponseEntity<Director> create(@Valid @RequestBody Director d) {
        log.info("POST /directors — создание режиссёра: {}", d);
        return ResponseEntity.status(HttpStatus.CREATED).body(directorService.create(d));
    }

    @PutMapping
    public ResponseEntity<Director> update(@Valid @RequestBody Director d) {
        log.info("PUT /directors — обновление режиссёра: {}", d);
        return ResponseEntity.ok(directorService.update(d));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        log.info("DELETE /directors/{} — удаление режиссёра", id);
        directorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
