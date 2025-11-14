CREATE TABLE IF NOT EXISTS mpa_ratings (
                                           id INTEGER PRIMARY KEY,
                                           name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS genres (
                                      id INTEGER PRIMARY KEY,
                                      name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS directors (
                                         id INTEGER AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS films (
                                     id INTEGER AUTO_INCREMENT PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
                                     description VARCHAR(200),
                                     release_date DATE,
                                     duration INTEGER,
                                     mpa_rating_id INTEGER,
                                     CONSTRAINT fk_films_mpa_rating FOREIGN KEY (mpa_rating_id) REFERENCES mpa_ratings(id)
);

CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER AUTO_INCREMENT PRIMARY KEY,
                                     email VARCHAR(255) NOT NULL UNIQUE,
                                     login VARCHAR(255) NOT NULL UNIQUE,
                                     name VARCHAR(255),
                                     birthday DATE
);

CREATE TABLE IF NOT EXISTS film_genres (
                                           film_id INTEGER,
                                           genre_id INTEGER,
                                           PRIMARY KEY (film_id, genre_id),
                                           CONSTRAINT fk_film_genres_film FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
                                           CONSTRAINT fk_film_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS film_likes (
                                          film_id INTEGER,
                                          user_id INTEGER,
                                          PRIMARY KEY (film_id, user_id),
                                          CONSTRAINT fk_film_likes_film FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
                                          CONSTRAINT fk_film_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS film_directors (
                                              film_id INTEGER NOT NULL,
                                              director_id INTEGER NOT NULL,
                                              PRIMARY KEY (film_id, director_id),
                                              CONSTRAINT fk_film_directors_film
                                                  FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
                                              CONSTRAINT fk_film_directors_director
                                                  FOREIGN KEY (director_id) REFERENCES directors(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friendships (
                                           user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           friend_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           PRIMARY KEY (user_id, friend_id)
);