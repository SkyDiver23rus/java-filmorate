package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @Override
    public Film addFilm(Film film) {
        film.setId(nextId++);
        // Устанавливаем пустые значения по умолчанию
        if (film.getMpa() == null) {
            Mpa mpa = new Mpa();
            mpa.setId(1);
            mpa.setName("G");
            film.setMpa(mpa);
        }
        if (film.getGenres() == null) {
            film.setGenres(new ArrayList<>());
        }
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        // Сохраняем лайки при обновлении
        Film existingFilm = films.get(film.getId());
        if (existingFilm != null) {
            if (existingFilm.getLikes() != null) {
                film.setLikes(existingFilm.getLikes());
            }
            // Сохраняем MPA, если в обновленном фильме его нет
            if (film.getMpa() == null && existingFilm.getMpa() != null) {
                film.setMpa(existingFilm.getMpa());
            }
        }

        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        if (film.getMpa() == null) {
            Mpa mpa = new Mpa();
            mpa.setId(1);
            mpa.setName("G");
            film.setMpa(mpa);
        }
        if (film.getGenres() == null) {
            film.setGenres(new ArrayList<>());
        }

        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film getFilmById(int id) {
        return films.get(id);
    }

    @Override
    public Optional<Film> findFilmById(int id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> getAllFilms() {
        return new ArrayList<>(films.values());
    }

    @Override
    public void addLike(int filmId, int userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().add(userId);
        }
    }

    @Override
    public void removeLike(int filmId, int userId) {
        Film film = films.get(filmId);
        if (film != null) {
            film.getLikes().remove(userId);
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        // Сортируем фильмы по количеству лайков (по убыванию) и берем первые count
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getRecommendedFilms(int userId) {
        // список лайкнутых фильмов
        Map<Integer, Set<Integer>> likesByUser = new HashMap<>();
        for (Film film : films.values()) {
            for (Integer likerId : film.getLikes()) {
                likesByUser.computeIfAbsent(likerId, k -> new HashSet<>()).add(film.getId());
            }
        }

        Set<Integer> userLikes = likesByUser.getOrDefault(userId, Collections.emptySet());

        // Поиск самого похожего по лайкам
        int maxCommon = 0;
        int similarUserId = -1;
        for (Map.Entry<Integer, Set<Integer>> entry : likesByUser.entrySet()) {
            int otherId = entry.getKey();
            if (otherId == userId) continue;
            Set<Integer> otherLikes = entry.getValue();
            Set<Integer> intersection = new HashSet<>(userLikes);
            intersection.retainAll(otherLikes);
            if (intersection.size() > maxCommon) {
                maxCommon = intersection.size();
                similarUserId = otherId;
            }
        }

        if (similarUserId == -1) {
            return new ArrayList<>();
        }

        Set<Integer> similarUserLikes = likesByUser.getOrDefault(similarUserId, Collections.emptySet());
        // Рекомендуем те, которые похожий лайкнул, а userId нет
        Set<Integer> recommendedIds = new HashSet<>(similarUserLikes);
        recommendedIds.removeAll(userLikes);

        return recommendedIds.stream()
                .map(films::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}