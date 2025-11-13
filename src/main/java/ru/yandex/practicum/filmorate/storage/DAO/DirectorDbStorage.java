package ru.yandex.practicum.filmorate.storage.DAO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

@Repository
public class DirectorDbStorage {
    private final JdbcTemplate jdbc;

    @Autowired
    public DirectorDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // добавить нового режиссера
    public Director create(Director director) {
        final String sql = "INSERT INTO directors(name) VALUES (?)";
        KeyHolder kh = new GeneratedKeyHolder();

        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, director.getName().trim());
            return ps;
        }, kh);

        if (kh.getKey() != null) {
            director.setId(kh.getKey().intValue());
        }
        return director;
    }

    // обновить данные режиссера по id
    public Director update(Director director) {
        final String sql = "UPDATE directors SET name = ? WHERE id = ?";
        jdbc.update(sql,
                director.getName(),
                director.getId());
        return director;
    }

    // получить режиссера по id
    public Optional<Director> getById(int id) {
        final String sql = "SELECT id, name FROM directors WHERE id = ?";
        return jdbc.query(sql, this::mapRowToDirector, id)
                .stream()
                .findFirst();
    }

    // получить всех режиссеров
    public Collection<Director> getAllDirectors() {
        final String sql = "SELECT id, name FROM directors ORDER BY name";
        return jdbc.query(sql, this::mapRowToDirector);
    }

    // удалить режиссера по id
    public void delete(int id) {
        final String sql = "DELETE FROM directors WHERE id = ?";
        jdbc.update(sql, id);
    }

    public boolean existsById(int id) {
        final String sql = "SELECT COUNT(*) FROM directors WHERE id = ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, id);
        return count > 0;
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}
