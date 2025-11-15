package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private long eventId;          // Идентификатор события
    private long timestamp;        // Временная метка в миллисекундах
    private int userId;            // Идентификатор пользователя, совершившего действие
    private EventType eventType;   // Тип события (LIKE, REVIEW, FRIEND)
    private Operation operation;   // Тип операции (REMOVE, ADD, UPDATE)
    private int entityId;          // Идентификатор сущности (filmId, userId друга, reviewId)
}