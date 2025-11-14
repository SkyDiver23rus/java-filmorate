package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @Override
    public User addUser(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User getUserById(int id) {
        return users.get(id);
    }

    @Override
    public Optional<User> findUserById(int id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void addFriend(int userId, int friendId) {
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().add(friendId);
        }
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().remove(friendId);
        }
    }

    @Override
    public Set<Integer> getUserFriends(int userId) {
        return Set.of();
    }

    @Override
    public List<User> getCommonFriends(int userId1, int userId2) {
        User user1 = users.get(userId1);
        User user2 = users.get(userId2);

        if (user1 == null || user2 == null) {
            return new ArrayList<>();
        }

        // Находим общих друзей
        Set<Integer> commonFriendIds = new HashSet<>(user1.getFriends());
        commonFriendIds.retainAll(user2.getFriends());

        // Преобразуем ID друзей в объекты User
        return commonFriendIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(int id) {
        users.remove(id);
    }
}