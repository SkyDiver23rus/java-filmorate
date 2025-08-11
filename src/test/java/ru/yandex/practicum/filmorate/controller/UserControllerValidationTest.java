package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController()).build();

    @Test
    void shouldSetLoginAsNameIfNameIsBlank() throws Exception {
        String json = """
            {
                "email": "user@example.com",
                "login": "testuser",
                "name": "",
                "birthday": "2000-01-01"
            }
        """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testuser"));
    }
}