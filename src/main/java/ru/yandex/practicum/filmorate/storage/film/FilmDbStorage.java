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

        updateFilmGenres(film);

        return getFilmById(film.getId());
    }

    @Override
    public Film updateFilm(Film film) {
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
        updateFilmGenres(film);

        return getFilmById(film.getId());
    }

    @Override
    public Film getFilmById(int id) {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id WHERE f.id = ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);

        if (films.isEmpty()) {
            return null;
        }

        Film film = films.get(0);

        film.setGenres(getGenresForFilms(Collections.singletonList(film.getId())).getOrDefault(film.getId(), new ArrayList<>()));
        film.setLikes(new HashSet<>(getLikesForFilms(Collections.singletonList(film.getId())).getOrDefault(film.getId(), new ArrayList<>())));

        return film;
    }

    @Override
    public Optional<Film> findFilmById(int id) {
        Film film = getFilmById(id);
        return Optional.ofNullable(film);
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());

        Map<Integer, List<Genre>> genresMap = getGenresForFilms(filmIds);
        Map<Integer, List<Integer>> likesMap = getLikesForFilms(filmIds);

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new ArrayList<>()));
            film.setLikes(new HashSet<>(likesMap.getOrDefault(film.getId(), new ArrayList<>())));
        }

        return films;
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
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, COUNT(fl.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id, m.name " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());
        Map<Integer, List<Genre>> genresMap = getGenresForFilms(filmIds);
        Map<Integer, List<Integer>> likesMap = getLikesForFilms(filmIds);

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new ArrayList<>()));
            film.setLikes(new HashSet<>(likesMap.getOrDefault(film.getId(), new ArrayList<>())));
        }

        return films;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
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

        return film;
    }

    private Map<Integer, List<Genre>> getGenresForFilms(List<Integer> filmIds) {
        if (filmIds.isEmpty()) return Collections.emptyMap();
        String inSql = filmIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT fg.film_id, g.id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + inSql + ") " +
                "ORDER BY fg.film_id, g.id";
        Map<Integer, List<Genre>> result = new HashMap<>();
        Object[] params = filmIds.toArray();
        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        }, params);
        return result;
    }

    private Map<Integer, List<Integer>> getLikesForFilms(List<Integer> filmIds) {
        if (filmIds.isEmpty()) return Collections.emptyMap();
        String inSql = filmIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + inSql + ")";
        Map<Integer, List<Integer>> result = new HashMap<>();
        Object[] params = filmIds.toArray();
        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("film_id");
            int userId = rs.getInt("user_id");
            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(userId);
        }, params);
        return result;
    }

    private void updateFilmGenres(Film film) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Integer> uniqueGenreIds = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> params = uniqueGenreIds.stream().map(gid -> new Object[]{film.getId(), gid}).collect(Collectors.toList());
            jdbcTemplate.batchUpdate(sql, params);
        }
    }
}