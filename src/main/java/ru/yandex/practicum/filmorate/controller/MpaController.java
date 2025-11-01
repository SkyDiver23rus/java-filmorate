package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.DAO.MpaDbStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    private final MpaDbStorage mpaStorage;

    @GetMapping
    public List<Mpa> getAllMpa() {
        return mpaStorage.getAllMpa();
    }

    @GetMapping("/{id}")
    public Mpa getMpa(@PathVariable int id) {
        return mpaStorage.getMpaById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id " + id + " не найден"));
    }
}