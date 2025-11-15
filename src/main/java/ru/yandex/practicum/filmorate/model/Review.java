package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Review {
    private Long reviewId;
    private Long userId;
    private Long filmId;
    private String content;
    private Boolean isPositive;
    private Integer useful = 0; // рейтинг полезности (лайк или дизлайк)
}
