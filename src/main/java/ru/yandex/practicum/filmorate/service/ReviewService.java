package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final EventStorage eventStorage;

    @Autowired
    public ReviewService(ReviewStorage reviewStorage, EventStorage eventStorage) {
        this.reviewStorage = reviewStorage;
        this.eventStorage = eventStorage;
    }

    public Review addReview(Review review) {
        Review created = reviewStorage.addReview(review);
        // Логируем событие добавления отзыва
        eventStorage.addEvent(review.getUserId().intValue(), "REVIEW", "ADD",
                created.getReviewId().intValue());
        return created;
    }

    public Review updateReview(Review review) {
        Review updated = reviewStorage.updateReview(review);
        // Логируем событие обновления отзыва
        eventStorage.addEvent(updated.getUserId().intValue(), "REVIEW", "UPDATE",
                updated.getReviewId().intValue());
        return updated;
    }

    public void deleteReview(long id) {
        Review review = reviewStorage.getReviewById(id);
        reviewStorage.deleteReview(id);
        // Логируем событие удаления отзыва
        eventStorage.addEvent(review.getUserId().intValue(), "REVIEW", "REMOVE",
                review.getReviewId().intValue());
    }

    public Review getReviewById(long id) {
        return reviewStorage.getReviewById(id);
    }

    public List<Review> getReviews(Long filmId, int count) {
        return reviewStorage.getReviews(filmId, count);
    }

    public void like(long reviewId, long userId) {
        reviewStorage.addLike(reviewId, userId);
    }

    public void dislike(long reviewId, long userId) {
        reviewStorage.addDislike(reviewId, userId);
    }

    public void removeLike(long reviewId, long userId) {
        reviewStorage.removeLike(reviewId, userId);
    }

    public void removeDislike(long reviewId, long userId) {
        reviewStorage.removeDislike(reviewId, userId);
    }
}