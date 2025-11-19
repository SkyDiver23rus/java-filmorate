package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DAO.DirectorDbStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorDbStorage directorDbStorage;

    public Director create(Director director) {
        validateName(director.getName());
        return directorDbStorage.create(director);
    }

    public Director update(Director director) {
        validateName(director.getName());
        checkExists(director.getId());
        return directorDbStorage.update(director);
    }

    public Director getById(int id) {
        return directorDbStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id " + id + " не найден."));
    }

    public Collection<Director> getAll() {
        return directorDbStorage.getAllDirectors();
    }

    public void delete(int id) {
        checkExists(id);
        directorDbStorage.delete(id);
    }

    public void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Имя режиссёра не может быть пустым.");
        }
    }

    // проверка существования режиссера по id
    public void checkExists(int id) {
        if (!directorDbStorage.existsById(id)) {
            throw new NotFoundException("Режиссёр с id " + id + " не найден.");
        }
    }
}
