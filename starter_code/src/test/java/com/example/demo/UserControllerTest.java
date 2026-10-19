package com.example.demo;

import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void createUserSuccessfully() throws Exception {

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "validuser",
                                  "password": "Password123",
                                  "confirmPassword": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("validuser"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User savedUser =
                userRepository.findByUsername("validuser");

        assertNotNull(savedUser);
        assertNotNull(savedUser.getPassword());

        assertNotEquals(
                "Password123",
                savedUser.getPassword()
        );

        assertTrue(
                passwordEncoder.matches(
                        "Password123",
                        savedUser.getPassword()
                )
        );
    }

    @Test
    public void createUserFailsWhenPasswordIsTooShort()
            throws Exception {

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "shortpassworduser",
                                  "password": "12345",
                                  "confirmPassword": "12345"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertNull(
                userRepository.findByUsername(
                        "shortpassworduser"
                )
        );
    }

    @Test
    public void createUserFailsWhenPasswordsDoNotMatch()
            throws Exception {

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "mismatchuser",
                                  "password": "Password123",
                                  "confirmPassword": "Password456"
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertNull(
                userRepository.findByUsername(
                        "mismatchuser"
                )
        );
    }

    @Test
    public void profileCannotBeAccessedWithoutAuthentication()
            throws Exception {

        createUser(
                "protecteduser",
                "Password123"
        );

        mockMvc.perform(
                        get("/api/user/protecteduser")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void authenticatedUserCanAccessOwnProfile()
            throws Exception {

        createUser(
                "profileowner",
                "Password123"
        );

        String token = loginAndGetToken(
                "profileowner",
                "Password123"
        );

        mockMvc.perform(
                        get("/api/user/profileowner")
                                .header(
                                        "Authorization",
                                        token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.username")
                                .value("profileowner")
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                );
    }

    @Test
    public void authenticatedUserCannotAccessAnotherUsersProfile()
            throws Exception {

        createUser(
                "profileuserone",
                "Password123"
        );

        createUser(
                "profileusertwo",
                "Password123"
        );

        String token = loginAndGetToken(
                "profileuserone",
                "Password123"
        );

        mockMvc.perform(
                        get("/api/user/profileusertwo")
                                .header(
                                        "Authorization",
                                        token
                                )
                )
                .andExpect(status().isForbidden());
    }

    private void createUser(
            String username,
            String password)
            throws Exception {

        String requestBody = """
                {
                  "username": "%s",
                  "password": "%s",
                  "confirmPassword": "%s"
                }
                """.formatted(
                username,
                password,
                password
        );

        mockMvc.perform(
                        post("/api/user/create")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }

    private String loginAndGetToken(
            String username,
            String password)
            throws Exception {

        String requestBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(
                username,
                password
        );

        MvcResult result = mockMvc.perform(
                        post("/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().exists("Authorization")
                )
                .andReturn();

        String token = result
                .getResponse()
                .getHeader("Authorization");

        assertNotNull(token);
        assertTrue(token.startsWith("Bearer "));

        return token;
    }
}