package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbc;

    @Override
    public Review addReview(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, useful) " +
                "VALUES (?, ?, ?, ?, 0)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey().longValue();
        review.setReviewId(id);
        return review;
    }

    @Override
    public Review updateReview(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";
        int rows = jdbc.update(sql, review.getContent(), review.getIsPositive(), review.getReviewId());
        if (rows == 0) {
            throw new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден.");
        }
        return review;
    }

    @Override
    public void deleteReview(long reviewId) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        jdbc.update(sql, reviewId);
    }

    @Override
    public Optional<Review> getReviewById(long id) {
        String sql = "SELECT * FROM reviews WHERE review_id = ?";
        try {
            Review review = jdbc.queryForObject(sql, this::mapRowToReview, id);
            return Optional.ofNullable(review);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Review> getReviews(Long filmId, int count) {
        String sql;
        Object[] params;
        if (filmId == null) {
            sql = "SELECT * FROM reviews ORDER BY useful DESC, review_id DESC LIMIT ?";
            params = new Object[]{count};
        } else {
            sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC, review_id DESC LIMIT ?";
            params = new Object[]{filmId, count};
        }
        return jdbc.query(sql, this::mapRowToReview, params);
    }

    @Override
    public void addLike(long reviewId, long userId) {
        handleLikeDislike(reviewId, userId, true);
    }

    @Override
    public void addDislike(long reviewId, long userId) {
        handleLikeDislike(reviewId, userId, false);
    }

    @Override
    public void removeLike(long reviewId, long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = true";
        jdbc.update(sql, reviewId, userId);
        updateUseful(reviewId);
    }

    @Override
    public void removeDislike(long reviewId, long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ? AND is_like = false";
        jdbc.update(sql, reviewId, userId);
        updateUseful(reviewId);
    }

    // Универсальный обработчик лайка/дизлайка
    private void handleLikeDislike(long reviewId, long userId, boolean isLike) {
        String checkSql = "SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbc.query(checkSql, (rs, row) -> rs.getBoolean("is_like"), reviewId, userId);

        if (!existing.isEmpty()) {
            Boolean current = existing.get(0);
            if (current == isLike) {
                return; // Уже стоит такой же
            } else {
                // Меняем лайк на дизлайк или наоборот
                String sql = "UPDATE review_likes SET is_like = ? WHERE review_id = ? AND user_id = ?";
                jdbc.update(sql, isLike, reviewId, userId);
            }
        } else {
            // Новый лайк/дизлайк
            String sql = "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, ?)";
            jdbc.update(sql, reviewId, userId, isLike);
        }
        updateUseful(reviewId);
    }

    // Обновление поля useful
    private void updateUseful(long reviewId) {
        String sql = """
                UPDATE reviews
                SET useful = (
                    COALESCE((SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND is_like = true), 0) -
                    COALESCE((SELECT COUNT(*) FROM review_likes WHERE review_id = ? AND is_like = false), 0)
                )
                WHERE review_id = ?
                """;
        jdbc.update(sql, reviewId, reviewId, reviewId);
    }

    private Review mapRowToReview(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Review review = new Review();
        review.setReviewId(rs.getLong("review_id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getLong("user_id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUseful(rs.getInt("useful"));
        return review;
    }
}
