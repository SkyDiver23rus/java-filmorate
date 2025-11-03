package ru.yandex.practicum.filmorate.storage.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private Map<Integer, Genre> genreCache;

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Genre> getAllGenres() {
        String sql = "SELECT * FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToGenre);
    }

    public Map<Integer, Genre> getAllGenresMap() {
        if (genreCache == null) {
            genreCache = getAllGenres().stream()
                    .collect(Collectors.toMap(Genre::getId, genre -> genre));
        }
        return genreCache;
    }

    public Optional<Genre> getGenreById(int id) {
        if (genreCache != null && genreCache.containsKey(id)) {
            return Optional.of(genreCache.get(id));
        }

        String sql = "SELECT * FROM genres WHERE id = ?";
        List<Genre> result = jdbcTemplate.query(sql, this::mapRowToGenre, id);

        if (!result.isEmpty() && genreCache != null) {
            genreCache.put(id, result.get(0));
        }

        return result.stream().findFirst();
    }

    public void validateGenreIdsExist(Set<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return;
        }

        Map<Integer, Genre> allGenres = getAllGenresMap();

        Set<Integer> nonExistentIds = genreIds.stream()
                .filter(id -> !allGenres.containsKey(id))
                .collect(Collectors.toSet());

        if (!nonExistentIds.isEmpty()) {
            throw new RuntimeException("Жанры с id " + nonExistentIds + " не найдены.");
        }
    }

    public List<Genre> getGenresByIds(Set<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, Genre> allGenres = getAllGenresMap();
        return genreIds.stream()
                .filter(allGenres::containsKey)
                .map(allGenres::get)
                .sorted(Comparator.comparingInt(Genre::getId))
                .collect(Collectors.toList());
    }

    public void refreshCache() {
        genreCache = null;
        getAllGenresMap();
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}