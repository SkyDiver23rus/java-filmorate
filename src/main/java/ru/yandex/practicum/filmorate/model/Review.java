package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Review {
    private Integer reviewId;
    private Integer userId;
    private Integer filmId;
    private String content;
    private Boolean isPositive;
    private Integer useful; // рейтинг полезности (лайк или дизлайк)
}
