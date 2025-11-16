package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private Long eventId;
    private Long timestamp;
    private Integer userId;
    private String eventType; // LIKE, REVIEW, FRIEND
    private String operation; // REMOVE, ADD, UPDATE
    private Integer entityId;
}