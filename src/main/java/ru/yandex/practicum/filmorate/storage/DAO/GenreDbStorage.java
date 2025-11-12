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

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Genre> getAllGenres() {
        String sql = "SELECT * FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToGenre);
    }

    public Optional<Genre> getGenreById(int id) {
        String sql = "SELECT * FROM genres WHERE id = ?";
        List<Genre> result = jdbcTemplate.query(sql, this::mapRowToGenre, id);
        return result.stream().findFirst();
    }

    public void validateGenreIdsExist(Set<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return;
        }
        List<Genre> foundGenres = getGenresByIds(genreIds);
        Set<Integer> foundIds = foundGenres.stream().map(Genre::getId).collect(Collectors.toSet());
        Set<Integer> nonExistentIds = genreIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toSet());

        if (!nonExistentIds.isEmpty()) {
            throw new RuntimeException("Жанры с id " + nonExistentIds + " не найдены.");
        }
    }

    public List<Genre> getGenresByIds(Set<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return List.of();
        }
        String inSql = genreIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT * FROM genres WHERE id IN (" + inSql + ") ORDER BY id";
        return jdbcTemplate.query(sql, genreIds.toArray(), this::mapRowToGenre);
    }

    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM genres WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}