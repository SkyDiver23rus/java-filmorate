package ru.yandex.practicum.filmorate.storage.event;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addEvent(int userId, EventType eventType, Operation operation, int entityId) {
        String sql = "INSERT INTO events (user_id, event_type, operation, entity_id, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";
        long timestamp = System.currentTimeMillis();
        jdbcTemplate.update(sql,
                userId,
                eventType.name(),
                operation.name(),
                entityId,
                timestamp);
    }

    @Override
    public List<Event> getUserFeed(int userId) {
        // ASC - от старых к новым (как ожидает тест)
        String sql = "SELECT * FROM events WHERE user_id = ? ORDER BY timestamp ASC";
        return jdbcTemplate.query(sql, this::mapRowToEvent, userId);
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getLong("event_id"));
        event.setTimestamp(rs.getLong("timestamp"));
        event.setUserId(rs.getInt("user_id"));
        event.setEventType(EventType.valueOf(rs.getString("event_type")));
        event.setOperation(Operation.valueOf(rs.getString("operation")));
        event.setEntityId(rs.getInt("entity_id"));
        return event;
    }
}