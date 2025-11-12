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

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
            String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) "
                    + "VALUES (?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, film.getName());
                stmt.setString(2, film.getDescription());
                stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
                stmt.setInt(4, film.getDuration());
                int mpaId = (film.getMpa() != null && film.getMpa().getId() > 0) ? film.getMpa().getId() : 1;
                stmt.setInt(5, mpaId);
                return stmt;
            }, keyHolder);

            Integer generatedId = keyHolder.getKey() != null ? keyHolder.getKey().intValue() : null;
            if (generatedId == null) {
                throw new RuntimeException("Failed to generate film ID");
            }
            film.setId(generatedId);

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
            String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, "
                    + "mpa_rating_id = ? WHERE id = ?";

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
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while updating film", e);
        }
    }

    @Override
    public Film getFilmById(int id) {
        try {
            String sql = "SELECT f.*, m.name as mpa_name FROM films f "
                    + "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id "
                    + "WHERE f.id = ?";

            List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Film film = mapRowToFilm(rs);
                film.setGenres(getGenresForFilmIds(Set.of(film.getId())).getOrDefault(film.getId(), new ArrayList<>()));
                film.setLikes(new HashSet<>(getLikesForFilmIds(Set.of(film.getId())).getOrDefault(film.getId(), Collections.emptyList())));
                return film;
            }, id);
            return films.isEmpty() ? null : films.get(0);
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while getting film by id: " + id, e);
        }
    }

    @Override
    public Optional<Film> findFilmById(int id) {
        Film film = getFilmById(id);
        return film == null ? Optional.empty() : Optional.of(film);
    }

    @Override
    public List<Film> getAllFilms() {
        try {
            String sql = "SELECT f.*, m.name as mpa_name FROM films f "
                    + "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id";

            List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToFilm(rs));

            if (films.isEmpty()) return films;

            Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

            Map<Integer, List<Genre>> genresByFilmId = getGenresForFilmIds(filmIds);
            Map<Integer, List<Integer>> likesByFilmId = getLikesForFilmIds(filmIds);

            for (Film film : films) {
                film.setGenres(genresByFilmId.getOrDefault(film.getId(), new ArrayList<>()));
                film.setLikes(new HashSet<>(likesByFilmId.getOrDefault(film.getId(), Collections.emptyList())));
            }
            return films;
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
//игнорим
        }
    }

    @Override
    public void removeLike(int filmId, int userId) {
        try {
            String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataAccessException e) {
//игнорим
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        try {
            String sql = "SELECT f.*, m.name as mpa_name, "
                    + "COUNT(fl.user_id) as likes_count "
                    + "FROM films f "
                    + "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id "
                    + "LEFT JOIN film_likes fl ON f.id = fl.film_id "
                    + "GROUP BY f.id, m.name "
                    + "ORDER BY likes_count DESC "
                    + "LIMIT ?";

            List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToFilm(rs), count);

            if (films.isEmpty()) return films;

            Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

            Map<Integer, List<Genre>> genresByFilmId = getGenresForFilmIds(filmIds);
            Map<Integer, List<Integer>> likesByFilmId = getLikesForFilmIds(filmIds);

            for (Film film : films) {
                film.setGenres(genresByFilmId.getOrDefault(film.getId(), new ArrayList<>()));
                film.setLikes(new HashSet<>(likesByFilmId.getOrDefault(film.getId(), Collections.emptyList())));
            }
            return films;
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while getting popular films", e);
        }
    }


    //жанры за 1 запрос!(вроде:))
    public Map<Integer, List<Genre>> getGenresForFilmIds(Set<Integer> filmIds) {
        if (filmIds == null || filmIds.isEmpty())
            return new HashMap<>();

        String inSql = filmIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT fg.film_id, g.id, g.name FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + inSql + ") ORDER BY fg.film_id, g.id";

        Map<Integer, List<Genre>> result = new HashMap<>();
        jdbcTemplate.query(sql, filmIds.toArray(), (rs) -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        });
        return result;
    }

    // то же с лайками
    public Map<Integer, List<Integer>> getLikesForFilmIds(Set<Integer> filmIds) {
        if (filmIds == null || filmIds.isEmpty())
            return new HashMap<>();

        String inSql = filmIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + inSql + ")";

        Map<Integer, List<Integer>> result = new HashMap<>();
        jdbcTemplate.query(sql, filmIds.toArray(), (rs) -> {
            int filmId = rs.getInt("film_id");
            int userId = rs.getInt("user_id");
            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(userId);
        });
        return result;
    }

    private Film mapRowToFilm(ResultSet rs) throws SQLException {
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

    private void updateFilmGenres(Film film) {
        try {
            jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());

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

    // по задаче рекомендации
    @Override
    public List<Film> getRecommendedFilms(int userId) {
        try {
            String getSimilarUserSql =
                    "SELECT fl2.user_id AS similar_user, COUNT(*) as common_likes " +
                            "FROM film_likes fl1 " +
                            "JOIN film_likes fl2 ON fl1.film_id = fl2.film_id " +
                            "WHERE fl1.user_id = ? AND fl2.user_id != ? " +
                            "GROUP BY fl2.user_id " +
                            "ORDER BY common_likes DESC " +
                            "LIMIT 1";
            List<Integer> similarUserIds = jdbcTemplate.query(getSimilarUserSql, (rs, rowNum) ->
                    rs.getInt("similar_user"), userId, userId);

            if (similarUserIds.isEmpty()) {
                return List.of();
            }
            int similarUserId = similarUserIds.get(0);

            String recommendationsSql =
                    "SELECT f.*, m.name AS mpa_name " +
                            "FROM films f " +
                            "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                            "JOIN film_likes fl ON f.id = fl.film_id " +
                            "WHERE fl.user_id = ? AND f.id NOT IN (" +
                            "SELECT film_id FROM film_likes WHERE user_id = ?" +
                            ")";
            List<Film> films = jdbcTemplate.query(recommendationsSql,
                    (rs, rowNum) -> mapRowToFilm(rs), similarUserId, userId);

            if (films.isEmpty()) return List.of();

            Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());
            Map<Integer, List<Genre>> genresByFilmId = getGenresForFilmIds(filmIds);
            Map<Integer, List<Integer>> likesByFilmId = getLikesForFilmIds(filmIds);
            for (Film film : films) {
                film.setGenres(genresByFilmId.getOrDefault(film.getId(), new ArrayList<>()));
                film.setLikes(new HashSet<>(likesByFilmId.getOrDefault(film.getId(), Collections.emptyList())));
            }
            return films;
        } catch (Exception e) {
            //  возвращаем пустой список
            return List.of();
        }
    }
}