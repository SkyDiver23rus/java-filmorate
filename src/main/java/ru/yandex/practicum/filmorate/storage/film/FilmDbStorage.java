package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
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

            if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
                saveFilmDirectors(film.getId(), film.getDirectors());
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

            saveFilmDirectors(film.getId(), film.getDirectors());

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
                film.setDirectors(loadDirectors(film.getId()));
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
                film.setDirectors(loadDirectors(film.getId()));
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
            // игнорим
        }
    }

    @Override
    public void removeLike(int filmId, int userId) {
        try {
            String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataAccessException e) {
            // игнорим
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        try {
            String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, m.name as mpa_name, "
                    + "COUNT(fl.user_id) as likes_count "
                    + "FROM films f "
                    + "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id "
                    + "LEFT JOIN film_likes fl ON f.id = fl.film_id "
                    + "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, m.name "
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
                film.setDirectors(loadDirectors(film.getId()));
            }
            return films;
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error while getting popular films", e);
        }
    }

    public Map<Integer, List<Genre>> getGenresForFilmIds(Set<Integer> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return new HashMap<>();
        }

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

    public Map<Integer, List<Integer>> getLikesForFilmIds(Set<Integer> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return new HashMap<>();
        }

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
        film.setName(Optional.ofNullable(rs.getString("name")).orElse(""));
        film.setDescription(Optional.ofNullable(rs.getString("description")).orElse(""));
        Date releaseDate = rs.getDate("release_date");
        film.setReleaseDate(releaseDate != null ? releaseDate.toLocalDate() : LocalDate.MIN);
        film.setDuration(rs.getInt("duration"));

        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("mpa_rating_id"));
        String mpaName = rs.getString("mpa_name");
        mpa.setName(mpaName != null ? mpaName : "G");
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

    //по задаче рекомендации
    @Override
    public List<Film> getRecommendedFilms(int userId) {
        try {
            // Проверка, есть ли вообще лайки у пользователя
            String userHasLikesSql = "SELECT COUNT(*) FROM film_likes WHERE user_id = ?";
            Integer userLikesCount = jdbcTemplate.queryForObject(userHasLikesSql, Integer.class, userId);

            if (userLikesCount == null || userLikesCount == 0) {
                return List.of();
            }

            //пользователи с максимальным количеством совпадающих лайков
            String findSimilarUserSql =
                    "SELECT fl2.user_id AS similar_user, COUNT(*) as common_likes " +
                            "FROM film_likes fl1 " +
                            "JOIN film_likes fl2 ON fl1.film_id = fl2.film_id " +
                            "WHERE fl1.user_id = ? AND fl2.user_id != ? " +
                            "GROUP BY fl2.user_id " +
                            "ORDER BY common_likes DESC ";

            List<Integer> similarUserIds = jdbcTemplate.query(findSimilarUserSql, (rs, rowNum) ->
                    rs.getInt("similar_user"), userId, userId);

            if (similarUserIds == null || similarUserIds.isEmpty()) {
                return List.of();
            }

            int similarUserId = similarUserIds.get(0);

            // фильмы, которые лайкнул похожий пользователь, но не лайкнул текущий
            String getRecommendationsSql =
                    "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, m.name AS mpa_name " +
                            "FROM films f " +
                            "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                            "WHERE f.id IN (" +
                            "SELECT film_id FROM film_likes WHERE user_id = ?" +
                            ") " +
                            "AND f.id NOT IN (" +
                            "SELECT film_id FROM film_likes WHERE user_id = ?" +
                            ")";

            List<Film> films = jdbcTemplate.query(getRecommendationsSql, (rs, rowNum) -> mapRowToFilm(rs),
                    similarUserId, userId);

            if (films == null || films.isEmpty()) {
                return List.of();
            }

            // Загружаем жанры и лайки для всех рекомендованных фильмов
            Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());
            Map<Integer, List<Genre>> genresByFilmId = getGenresForFilmIds(filmIds);
            Map<Integer, List<Integer>> likesByFilmId = getLikesForFilmIds(filmIds);

            for (Film film : films) {
                film.setGenres(genresByFilmId.getOrDefault(film.getId(), new ArrayList<>()));
                film.setLikes(new HashSet<>(likesByFilmId.getOrDefault(film.getId(), Collections.emptyList())));
            }

            return films;

        } catch (Exception e) {
            System.err.println("Error in getRecommendedFilms: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    // сортировка фильмов одного режиссера по лайкам или году
    public List<Film> getFilmsByDirectorSorted(int directorId, String sortBy) {
        String base = "SELECT f.*, m.name as mpa_name " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.id " +
                "JOIN film_directors fd ON fd.film_id = f.id " +
                "WHERE fd.director_id = ?";

        String order;
        if ("likes".equalsIgnoreCase(sortBy)) {
            order = " ORDER BY (SELECT COUNT(*) FROM film_likes fl WHERE fl.film_id = f.id) DESC, f.id";
        } else if ("year".equalsIgnoreCase(sortBy)) {
            order = " ORDER BY f.release_date, f.id";
        } else {
            throw new IllegalArgumentException("sortBy must be 'likes' or 'year'");
        }

        List<Film> films = jdbcTemplate.query(base + order, (rs, rn) -> mapRowToFilm(rs), directorId);
        if (films.isEmpty()) {
            return films;
        }

        Set<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

        Map<Integer, List<Genre>> genresByFilmId = getGenresForFilmIds(filmIds);
        Map<Integer, List<Integer>> likesByFilmId = getLikesForFilmIds(filmIds);

        for (Film film : films) {
            film.setGenres(genresByFilmId.getOrDefault(film.getId(), new ArrayList<>()));
            film.setLikes(new HashSet<>(likesByFilmId.getOrDefault(film.getId(), Collections.emptyList())));
            film.setDirectors(loadDirectors(film.getId()));
        }
        return films;
    }

    // поддержка сохранения режиссёров в таблицу film_directors
    private void saveFilmDirectors(int filmId, Set<Director> directors) {
        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", filmId);
        if (directors == null || directors.isEmpty()) return;

        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        for (Director d : directors) {
            jdbcTemplate.update(sql, filmId, d.getId());
        }
    }

    private Set<Director> loadDirectors(int filmId) {
        String sql = "SELECT d.id, d.name " +
                "FROM film_directors fd " +
                "JOIN directors d ON d.id = fd.director_id " +
                "WHERE fd.film_id = ? " +
                "ORDER BY d.name";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rn) -> {
            Director director = new Director();
            director.setId(rs.getInt("id"));
            director.setName(rs.getString("name"));
            return director;
        }, filmId));
    }
}