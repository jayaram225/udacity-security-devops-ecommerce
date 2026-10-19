package com.example.demo;

import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void authenticatedUserCanSubmitOwnOrder()
            throws Exception {

        createUser(
                "orderowner",
                "Password123"
        );

        String token = loginAndGetToken(
                "orderowner",
                "Password123"
        );

        addItemToCart(
                "orderowner",
                token,
                1,
                1
        );

        mockMvc.perform(
                        post("/api/order/submit/orderowner")
                                .header(
                                        "Authorization",
                                        token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.total")
                                .value(2.99)
                );
    }

    @Test
    public void authenticatedUserCannotSubmitAnotherUsersOrder()
            throws Exception {

        createUser(
                "orderuserone",
                "Password123"
        );

        createUser(
                "orderusertwo",
                "Password123"
        );

        String firstUserToken = loginAndGetToken(
                "orderuserone",
                "Password123"
        );

        mockMvc.perform(
                        post("/api/order/submit/orderusertwo")
                                .header(
                                        "Authorization",
                                        firstUserToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void authenticatedUserCanViewOwnOrderHistory()
            throws Exception {

        createUser(
                "historyowner",
                "Password123"
        );

        String token = loginAndGetToken(
                "historyowner",
                "Password123"
        );

        addItemToCart(
                "historyowner",
                token,
                1,
                1
        );

        mockMvc.perform(
                        post("/api/order/submit/historyowner")
                                .header(
                                        "Authorization",
                                        token
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/order/history/historyowner")
                                .header(
                                        "Authorization",
                                        token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].items[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].total")
                                .value(2.99)
                );
    }

    @Test
    public void authenticatedUserCannotViewAnotherUsersOrderHistory()
            throws Exception {

        createUser(
                "historyuserone",
                "Password123"
        );

        createUser(
                "historyusertwo",
                "Password123"
        );

        String firstUserToken = loginAndGetToken(
                "historyuserone",
                "Password123"
        );

        mockMvc.perform(
                        get("/api/order/history/historyusertwo")
                                .header(
                                        "Authorization",
                                        firstUserToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void orderCannotBeSubmittedWithoutAuthentication()
            throws Exception {

        createUser(
                "unauthenticatedorderuser",
                "Password123"
        );

        mockMvc.perform(
                        post(
                                "/api/order/submit/"
                                        + "unauthenticatedorderuser"
                        )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void unknownUserReturnsNotFound()
            throws Exception {

        createUser(
                "deletedorderuser",
                "Password123"
        );

        String token = loginAndGetToken(
                "deletedorderuser",
                "Password123"
        );

        User user =
                userRepository.findByUsername(
                        "deletedorderuser"
                );

        assertNotNull(user);

        userRepository.delete(user);
        userRepository.flush();

        mockMvc.perform(
                        post("/api/order/submit/deletedorderuser")
                                .header(
                                        "Authorization",
                                        token
                                )
                )
                .andExpect(status().isNotFound());
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

    private void addItemToCart(
            String username,
            String token,
            long itemId,
            int quantity)
            throws Exception {

        String requestBody = """
                {
                  "username": "%s",
                  "itemId": %d,
                  "quantity": %d
                }
                """.formatted(
                username,
                itemId,
                quantity
        );

        mockMvc.perform(
                        post("/api/cart/addToCart")
                                .header(
                                        "Authorization",
                                        token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }
}