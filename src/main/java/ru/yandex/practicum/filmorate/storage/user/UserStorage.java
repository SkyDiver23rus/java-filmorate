package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserStorage {
    User addUser(User user);

    User updateUser(User user);

    User getUserById(int id);

    Optional<User> findUserById(int id);

    List<User> getAllUsers();

    void addFriend(int userId, int friendId);

    void removeFriend(int userId, int friendId);

    Set<Integer> getUserFriends(int userId);

    List<User> getCommonFriends(int userId1, int userId2);

    void deleteUser(int id); //по задаче удаления

}