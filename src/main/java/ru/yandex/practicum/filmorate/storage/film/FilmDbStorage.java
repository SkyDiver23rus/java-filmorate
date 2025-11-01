package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film addFilm(Film film) {
        try {
            String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, film.getName());
                stmt.setString(2, film.getDescription());
                stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
                stmt.setInt(4, film.getDuration());
                stmt.setInt(5, film.getMpa() != null ? film.getMpa().getId() : 1);
                return stmt;
            }, keyHolder);

            Integer generatedId = keyHolder.getKey() != null ? keyHolder.getKey().intValue() : null;
            if (generatedId == null) {
                throw new RuntimeException("Failed to generate film ID");
            }
            film.setId(generatedId);

            // Сохраняем жанры
            if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                updateFilmGenres(film);
            }

            return getFilmById(film.getId());
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while adding film", e);
        }
    }

    @Override
    public Film updateFilm(Film film) {
        try {
            String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";

            int updated = jdbcTemplate.update(sql,
                    film.getName(),
                    film.getDescription(),
                    film.getReleaseDate(),
                    film.getDuration(),
                    film.getMpa() != null ? film.getMpa().getId() : 1,
                    film.getId());

            if (updated == 0) {
                return null;
            }

            // Обновляем жанры
            updateFilmGenres(film);

            return getFilmById(film.getId());
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while updating film", e);
        }
    }

    @Override
    public Film getFilmById(int id) {
        try {
            String sql = "SELECT f.*, m.name as mpa_name FROM films f " +
                    "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                    "WHERE f.id = ?";

            List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
            return films.isEmpty() ? null : films.get(0);
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while getting film by id: " + id, e);
        }
    }

    @Override
    public Optional<Film> findFilmById(int id) {
        try {
            String sql = "SELECT f.*, m.name as mpa_name FROM films f " +
                    "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                    "WHERE f.id = ?";

            List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
            return films.isEmpty() ? Optional.empty() : Optional.of(films.get(0));
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while finding film by id: " + id, e);
        }
    }

    @Override
    public List<Film> getAllFilms() {
        try {
            String sql = "SELECT f.*, m.name as mpa_name FROM films f " +
                    "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id";

            return jdbcTemplate.query(sql, this::mapRowToFilm);
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while getting all films", e);
        }
    }

    @Override
    public void addLike(int filmId, int userId) {
        try {
            String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataAccessException e) {
            // Игнорируем, если лайк уже существует
        }
    }

    @Override
    public void removeLike(int filmId, int userId) {
        try {
            String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataAccessException e) {
            // Игнорируем, если лайка нет
        }
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        try {
            Film film = new Film();
            film.setId(rs.getInt("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));

            Date releaseDate = rs.getDate("release_date");
            if (releaseDate != null) {
                film.setReleaseDate(releaseDate.toLocalDate());
            }

            film.setDuration(rs.getInt("duration"));

            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("mpa_rating_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);

            // Загружаем жанры
            film.setGenres(getFilmGenres(film.getId()));

            // Загружаем лайки
            film.setLikes(new HashSet<>(getFilmLikes(film.getId())));

            return film;
        } catch (SQLException e) {
            throw new RuntimeException("Error mapping film row", e);
        }
    }

    private List<Genre> getFilmGenres(int filmId) {
        try {
            String sql = "SELECT g.* FROM film_genres fg " +
                    "JOIN genres g ON fg.genre_id = g.id " +
                    "WHERE fg.film_id = ? ORDER BY g.id";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                return genre;
            }, filmId);
        } catch (DataAccessException e) {
            return new ArrayList<>();
        }
    }

    private List<Integer> getFilmLikes(int filmId) {
        try {
            String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
            return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("user_id"), filmId);
        } catch (DataAccessException e) {
            return new ArrayList<>();
        }
    }

    private void updateFilmGenres(Film film) {
        try {
            // Удаляем старые жанры
            jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());

            // Добавляем новые жанры (уникальные)
            if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                Set<Integer> uniqueGenreIds = film.getGenres().stream()
                        .map(Genre::getId)
                        .collect(Collectors.toSet());

                String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
                for (Integer genreId : uniqueGenreIds) {
                    jdbcTemplate.update(sql, film.getId(), genreId);
                }
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while updating film genres", e);
        }
    }
}