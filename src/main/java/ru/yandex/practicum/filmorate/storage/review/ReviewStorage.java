package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;
import java.util.List;

public interface ReviewStorage {
    Review addReview(Review review);
    Review updateReview(Review review);
    void deleteReview(long reviewId);
    Review getReviewById(long id);
    List<Review> getReviews(Long filmId, int count);

    void addLike(long reviewId, long userId);
    void addDislike(long reviewId, long userId);
    void removeLike(long reviewId, long userId);
    void removeDislike(long reviewId, long userId);
}
