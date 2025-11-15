package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    private Review mapRow(ResultSet rs, int rowNum) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getLong("review_id"));
        r.setContent(rs.getString("content"));
        r.setIsPositive(rs.getBoolean("is_positive"));
        r.setUserId(rs.getLong("user_id"));
        r.setFilmId(rs.getLong("film_id"));
        r.setUseful(rs.getInt("useful"));
        return r;
    }

    @Override
    public Review addReview(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) " +
                     "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, review.getContent(), review.getIsPositive(),
                review.getUserId(), review.getFilmId());

        return jdbcTemplate.queryForObject(
                "SELECT * FROM reviews ORDER BY review_id DESC LIMIT 1",
                this::mapRow);
    }

    @Override
    public Review updateReview(Review review) {
        jdbcTemplate.update("UPDATE reviews SET content=?, is_positive=? WHERE review_id=?",
                review.getContent(), review.getIsPositive(), review.getReviewId());

        return getReviewById(review.getReviewId());
    }

    @Override
    public void deleteReview(long reviewId) {
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id=?", reviewId);
    }

    @Override
    public Review getReviewById(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM reviews WHERE review_id=?", this::mapRow, id);
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
        jdbcTemplate.update(
                "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, TRUE)",
                reviewId, userId);
        jdbcTemplate.update("UPDATE reviews SET useful = useful + 1 WHERE review_id=?", reviewId);
    }

    @Override
    public void addDislike(long reviewId, long userId) {
        jdbcTemplate.update(
                "INSERT INTO review_likes (review_id, user_id, is_like) VALUES (?, ?, FALSE)",
                reviewId, userId);
        jdbcTemplate.update("UPDATE reviews SET useful = useful - 1 WHERE review_id=?", reviewId);
    }

    @Override
    public void removeLike(long reviewId, long userId) {
        jdbcTemplate.update(
                "DELETE FROM review_likes WHERE review_id=? AND user_id=? AND is_like=TRUE",
                reviewId, userId);
        jdbcTemplate.update("UPDATE reviews SET useful = useful - 1 WHERE review_id=?", reviewId);
    }

    @Override
    public void removeDislike(long reviewId, long userId) {
        jdbcTemplate.update(
                "DELETE FROM review_likes WHERE review_id=? AND user_id=? AND is_like=FALSE",
                reviewId, userId);
        jdbcTemplate.update("UPDATE reviews SET useful = useful + 1 WHERE review_id=?", reviewId);
    }
}
