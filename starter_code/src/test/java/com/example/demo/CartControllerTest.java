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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void authenticatedUserCanAddItemToOwnCart()
            throws Exception {

        createUser(
                "cartowner",
                "Password123"
        );

        String token = loginAndGetToken(
                "cartowner",
                "Password123"
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
                                .content("""
                                        {
                                          "username": "cartowner",
                                          "itemId": 1,
                                          "quantity": 1
                                        }
                                        """)
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

        User user =
                userRepository.findByUsername("cartowner");

        assertNotNull(user);
        assertNotNull(user.getCart());

        assertEquals(
                1,
                user.getCart().getItems().size()
        );
    }

    @Test
    public void authenticatedUserCanRemoveItemFromOwnCart()
            throws Exception {

        createUser(
                "cartremoveuser",
                "Password123"
        );

        String token = loginAndGetToken(
                "cartremoveuser",
                "Password123"
        );

        addItemToCart(
                "cartremoveuser",
                token,
                1,
                1
        );

        mockMvc.perform(
                        post("/api/cart/removeFromCart")
                                .header(
                                        "Authorization",
                                        token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "cartremoveuser",
                                          "itemId": 1,
                                          "quantity": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.items")
                                .isEmpty()
                );
    }

    @Test
    public void userCannotModifyAnotherUsersCart()
            throws Exception {

        createUser(
                "cartuserone",
                "Password123"
        );

        createUser(
                "cartusertwo",
                "Password123"
        );

        String firstUserToken = loginAndGetToken(
                "cartuserone",
                "Password123"
        );

        mockMvc.perform(
                        post("/api/cart/addToCart")
                                .header(
                                        "Authorization",
                                        firstUserToken
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "cartusertwo",
                                          "itemId": 1,
                                          "quantity": 1
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void cartCannotBeModifiedWithoutAuthentication()
            throws Exception {

        createUser(
                "unauthenticatedcartuser",
                "Password123"
        );

        mockMvc.perform(
                        post("/api/cart/addToCart")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "username": "unauthenticatedcartuser",
                                          "itemId": 1,
                                          "quantity": 1
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void addingUnknownItemReturnsNotFound()
            throws Exception {

        createUser(
                "invaliditemuser",
                "Password123"
        );

        String token = loginAndGetToken(
                "invaliditemuser",
                "Password123"
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
                                .content("""
                                        {
                                          "username": "invaliditemuser",
                                          "itemId": 999999,
                                          "quantity": 1
                                        }
                                        """)
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