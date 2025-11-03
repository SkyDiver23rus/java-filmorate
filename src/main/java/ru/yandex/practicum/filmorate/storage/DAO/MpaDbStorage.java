package ru.yandex.practicum.filmorate.storage.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private Map<Integer, Mpa> mpaCache;

    @Autowired
    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Mpa> getAllMpa() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToMpa);
    }

    public Map<Integer, Mpa> getAllMpaMap() {
        if (mpaCache == null) {
            mpaCache = getAllMpa().stream()
                    .collect(Collectors.toMap(Mpa::getId, Function.identity()));
        }
        return mpaCache;
    }

    public Optional<Mpa> getMpaById(int id) {
        if (mpaCache != null && mpaCache.containsKey(id)) {
            return Optional.of(mpaCache.get(id));
        }

        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        List<Mpa> result = jdbcTemplate.query(sql, this::mapRowToMpa, id);

        if (!result.isEmpty() && mpaCache != null) {
            mpaCache.put(id, result.get(0));
        }

        return result.stream().findFirst();
    }

    public void refreshCache() {
        mpaCache = null;
        getAllMpaMap();
    }

    public boolean existsById(int id) {
        if (mpaCache != null && mpaCache.containsKey(id)) {
            return true;
        }

        String sql = "SELECT 1 FROM mpa_ratings WHERE id = ? LIMIT 1";
        List<Integer> result = jdbcTemplate.query(sql,
                (rs, rowNum) -> 1, id);
        return !result.isEmpty();
    }

    public List<Mpa> getMpaByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        if (mpaCache != null) {
            return ids.stream()
                    .filter(mpaCache::containsKey)
                    .map(mpaCache::get)
                    .collect(Collectors.toList());
        }

        String inClause = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String sql = String.format("SELECT * FROM mpa_ratings WHERE id IN (%s) ORDER BY id", inClause);

        return jdbcTemplate.query(sql, this::mapRowToMpa);
    }

    private Mpa mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {
        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}