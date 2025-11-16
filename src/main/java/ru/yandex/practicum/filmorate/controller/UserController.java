package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final FilmService filmService;

    public UserController(UserService userService, FilmService filmService) {
        this.userService = userService;
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user) {
        User created = userService.addUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable int id, @PathVariable int friendId) {
        userService.addFriend(id, friendId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable int id, @PathVariable int friendId) {
        userService.removeFriend(id, friendId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<User>> getFriends(@PathVariable int id) {
        return ResponseEntity.ok(userService.getFriends(id));
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public ResponseEntity<List<User>> getCommonFriends(@PathVariable int id, @PathVariable int otherId) {
        return ResponseEntity.ok(userService.getCommonFriends(id, otherId));
    }

    //по задаче Рекомендации
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<Film>> getRecommendations(@PathVariable int id) {
        List<Film> recommended = filmService.getRecommendedFilms(id);
        return ResponseEntity.ok(recommended);
    }

    //по задаче удаление
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // Новый эндпоинт для ленты событий
    @GetMapping("/{id}/feed")
    public ResponseEntity<List<Event>> getUserFeed(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUserFeed(id));
    }

}
