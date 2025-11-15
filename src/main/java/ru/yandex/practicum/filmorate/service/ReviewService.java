package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;

    public Review addReview(Review review) {
        // Валидация пользователя и фильма
        if (!userDbStorage.findUserById(Math.toIntExact(review.getUserId())).isPresent()) {
            throw new NotFoundException("Пользователь с id " + review.getUserId() + " не найден.");
        }
        if (!filmDbStorage.findFilmById(Math.toIntExact(review.getFilmId())).isPresent()) {
            throw new NotFoundException("Фильм с id " + review.getFilmId() + " не найден.");
        }
        return reviewStorage.addReview(review);
    }

    public Review updateReview(Review review) {
        if (!reviewStorage.getReviewById(review.getReviewId()).isPresent()) {
            throw new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден.");
        }
        return reviewStorage.updateReview(review);
    }

    public void deleteReview(long id) {
        if (!reviewStorage.getReviewById(id).isPresent()) {
            throw new NotFoundException("Отзыв с id " + id + " не найден.");
        }
        reviewStorage.deleteReview(id);
    }

    public Review getReviewById(long id) {
        return reviewStorage.getReviewById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + id + " не найден."));
    }

    public List<Review> getReviews(Long filmId, int count) {
        return reviewStorage.getReviews(filmId, count);
    }

    public void like(long reviewId, long userId) {
        if (!reviewStorage.getReviewById(reviewId).isPresent()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден.");
        }
        if (!userDbStorage.findUserById(Math.toIntExact(userId)).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        reviewStorage.addLike(reviewId, userId);
    }

    public void dislike(long reviewId, long userId) {
        if (!reviewStorage.getReviewById(reviewId).isPresent()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден.");
        }
        if (!userDbStorage.findUserById(Math.toIntExact(userId)).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        reviewStorage.addDislike(reviewId, userId);
    }

    public void removeLike(long reviewId, long userId) {
        if (!reviewStorage.getReviewById(reviewId).isPresent()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден.");
        }
        if (!userDbStorage.findUserById(Math.toIntExact(userId)).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        reviewStorage.removeLike(reviewId, userId);
    }

    public void removeDislike(long reviewId, long userId) {
        if (!reviewStorage.getReviewById(reviewId).isPresent()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден.");
        }
        if (!userDbStorage.findUserById(Math.toIntExact(userId)).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        reviewStorage.removeDislike(reviewId, userId);
    }
}