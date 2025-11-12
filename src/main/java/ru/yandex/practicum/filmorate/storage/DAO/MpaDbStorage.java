package ru.yandex.practicum.filmorate.storage.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToMpa);
    }

    //исправлено
    public Optional<Mpa> getMpaById(int id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        List<Mpa> result = jdbcTemplate.query(sql, this::mapRowToMpa, id);
        return result.stream().findFirst();
    }

    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    public List<Mpa> getMpaByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String inClause = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = String.format("SELECT * FROM mpa_ratings WHERE id IN (%s) ORDER BY id", inClause);

        return jdbcTemplate.query(sql, ids.toArray(), this::mapRowToMpa);
    }

    private Mpa mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}