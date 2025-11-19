package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    private Review mapRow(ResultSet rs, int rowNum) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getInt("review_id"));
        r.setContent(rs.getString("content"));
        r.setIsPositive(rs.getBoolean("is_positive"));
        r.setUserId(rs.getInt("user_id"));
        r.setFilmId(rs.getInt("film_id"));
        r.setUseful(rs.getInt("useful"));
        return r;
    }

    @Override
    public Review addReview(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Содержимое отзыва не может быть пустым");
        }
        if (review.getUserId() == null) {
            throw new ValidationException("ID пользователя не может быть null");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("ID фильма не может быть null");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("Поле isPositive не может быть null");
        }

        String checkUserSql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer userCount = jdbcTemplate.queryForObject(checkUserSql, Integer.class, review.getUserId());
        if (userCount == null || userCount == 0) {
            throw new NotFoundException("Пользователь с id " + review.getUserId() + " не найден");
        }

        String checkFilmSql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer filmCount = jdbcTemplate.queryForObject(checkFilmSql, Integer.class, review.getFilmId());
        if (filmCount == null || filmCount == 0) {
            throw new NotFoundException("Фильм с id " + review.getFilmId() + " не найден");
        }

        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            return ps;
        }, keyHolder);

        long reviewId = keyHolder.getKey().longValue();
        return getReviewById(reviewId);
    }

    @Override
    public Review updateReview(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }

        try {
            getReviewById(review.getReviewId());
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден");
        }

        jdbcTemplate.update("UPDATE reviews SET content=?, is_positive=? WHERE review_id=?",
                review.getContent(), review.getIsPositive(), review.getReviewId());

        return getReviewById(review.getReviewId());
    }

    @Override
    public void deleteReview(long reviewId) {
        String checkSql = "SELECT COUNT(*) FROM reviews WHERE review_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, reviewId);
        if (count == null || count == 0) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден");
        }
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id=?", reviewId);
    }

    @Override
    public Review getReviewById(long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT * FROM reviews WHERE review_id=?", this::mapRow, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Отзыв с id " + id + " не найден");
        }
    }

    @Override
    public List<Review> getReviews(Long filmId, int count) {
        if (filmId == null) {
            return jdbcTemplate.query(
                    "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?",
                    this::mapRow, count);
        }
        return jdbcTemplate.query(
                "SELECT * FROM reviews WHERE film_id=? ORDER BY useful DESC LIMIT ?",
                this::mapRow, filmId, count);
    }

    @Override
    public void addLike(long reviewId, long userId) {
        try {
            // Проверяем есть ли дизлайк
            String checkSql = "SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = FALSE";
            Integer dislikeCount = jdbcTemplate.queryForObject(checkSql, Integer.class, reviewId, userId);

            if (dislikeCount != null && dislikeCount > 0) {
                // Удаляем дизлайк
                jdbcTemplate.update("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?", reviewId, userId);
                // Компенсируем: было -1 (от дизлайка), станет +1 (от лайка) = +2
                jdbcTemplate.update("UPDATE reviews SET useful = useful + 2 WHERE review_id = ?", reviewId);
                // Добавляем лайк
                jdbcTemplate.update(
                        "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, TRUE)",
                        reviewId, userId);
            } else {
                // Просто добавляем лайк
                jdbcTemplate.update(
                        "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, TRUE)",
                        reviewId, userId);
                jdbcTemplate.update("UPDATE reviews SET useful = useful + 1 WHERE review_id = ?", reviewId);
            }
        } catch (Exception e) {
            // Игнорируем дубликаты
        }
    }

    @Override
    public void addDislike(long reviewId, long userId) {
        try {
            // Проверяем есть ли лайк
            String checkSql = "SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = TRUE";
            Integer likeCount = jdbcTemplate.queryForObject(checkSql, Integer.class, reviewId, userId);

            if (likeCount != null && likeCount > 0) {
                // Удаляем лайк
                jdbcTemplate.update("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?", reviewId, userId);
                // Компенсируем: было +1 (от лайка), станет -1 (от дизлайка) = -2
                jdbcTemplate.update("UPDATE reviews SET useful = useful - 2 WHERE review_id = ?", reviewId);
                // Добавляем дизлайк
                jdbcTemplate.update(
                        "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, FALSE)",
                        reviewId, userId);
            } else {
                // Просто добавляем дизлайк
                jdbcTemplate.update(
                        "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, FALSE)",
                        reviewId, userId);
                jdbcTemplate.update("UPDATE reviews SET useful = useful - 1 WHERE review_id = ?", reviewId);
            }
        } catch (Exception e) {
            // Игнорируем дубликаты
        }
    }

    @Override
    public void removeLike(long reviewId, long userId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM review_likes WHERE review_id=? AND user_id=? AND is_like=TRUE",
                reviewId, userId);
        if (deleted > 0) {
            jdbcTemplate.update("UPDATE reviews SET useful = useful - 1 WHERE review_id=?", reviewId);
        }
    }

    @Override
    public void removeDislike(long reviewId, long userId) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM review_likes WHERE review_id=? AND user_id=? AND is_like=FALSE",
                reviewId, userId);
        if (deleted > 0) {
            jdbcTemplate.update("UPDATE reviews SET useful = useful + 1 WHERE review_id=?", reviewId);
        }
    }
}