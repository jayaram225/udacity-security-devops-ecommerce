package com.example.demo.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.springframework.security.core.Authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/user")
public class UserController {

	private static final Logger logger =
			LoggerFactory.getLogger(UserController.class);


	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@GetMapping("/id/{id}")
	public ResponseEntity<User> findById(@PathVariable Long id) {
		return ResponseEntity.of(userRepository.findById(id));
	}

	@GetMapping("/{username}")
	public ResponseEntity<User> findByUserName(
			@PathVariable String username,
			Authentication authentication) {

		if (!authentication.getName().equals(username)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		User user = userRepository.findByUsername(username);

		return user == null
				? ResponseEntity.notFound().build()
				: ResponseEntity.ok(user);
	}

	@PostMapping("/create")
	public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {

		if (createUserRequest.getPassword() == null ||
				createUserRequest.getPassword().length() < 7) {

			logger.warn(
					"CreateUser failure username={} reason=invalid_password_length",
					createUserRequest.getUsername()
			);

			return ResponseEntity.badRequest().build();
		}

		if (!createUserRequest.getPassword()
				.equals(createUserRequest.getConfirmPassword())) {

			logger.warn(
					"CreateUser failure username={} reason=password_mismatch",
					createUserRequest.getUsername()
			);

			return ResponseEntity.badRequest().build();
		}

		User user = new User();
		user.setUsername(createUserRequest.getUsername());

		user.setPassword(
				passwordEncoder.encode(createUserRequest.getPassword())
		);

		Cart cart = new Cart();

		user.setCart(cart);
		cart.setUser(user);

		userRepository.save(user);

		logger.info(
				"CreateUser success username={}",
				user.getUsername()
		);

		return ResponseEntity.ok(user);
	}
	
}
