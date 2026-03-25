package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
class UserServiceIntegrationTest {

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
	}

	@Test
	void createUser_validInputs_success() {
		// given
		assertNull(userRepository.findByUsername("testUsername"));

		User testUser = new User();
		testUser.setName("testName");
		testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setBio("testBio");
        testUser.setEmail("test@test.com");

		// when
		User createdUser = userService.createUser(testUser);

		// then
		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getName(), createdUser.getName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
        assertEquals(testUser.getEmail(), createdUser.getEmail());
	}

	@Test
	void createUser_duplicateUsername_throwsException() {
		assertNull(userRepository.findByUsername("testUsername"));

		User testUser = new User();
		testUser.setName("testName");
		testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setBio("testBio");
        testUser.setEmail("test@test.com");
		userService.createUser(testUser);

		// attempt to create second user with same username
		User testUser2 = new User();

		// change the name but forget about the username
		testUser2.setName("testName2");
		testUser2.setUsername("testUsername");
        testUser2.setPassword("testPassword2");
        testUser2.setBio("testBio2");
        testUser2.setEmail("test@test2.com");

		// check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
	}

	@Test
	void createUser_passwordIsHashed_notStoredInPlaintext() {
		User user = new User();
		user.setName("testName");
		user.setUsername("testUsername");
		user.setPassword("plainPassword123");
		user.setBio("testBio");
        user.setEmail("test@test.com");

		User createdUser = userService.createUser(user);
		User persistedUser = userRepository.findByUsername("testUsername");

		assertNotNull(persistedUser);
		assertNotEquals("plainPassword123", persistedUser.getPassword());
		assertTrue(new BCryptPasswordEncoder().matches("plainPassword123", persistedUser.getPassword()));
		assertEquals(createdUser.getId(), persistedUser.getId());
	}

    @Test
    void createUser_duplicateEmail_throwsException() {
        assertNull(userRepository.findByUsername("testUsername"));

        User testUser = new User();
        testUser.setName("testName");
        testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setBio("testBio");
        testUser.setEmail("test@test.com");
        userService.createUser(testUser);

        User testUser2 = new User();

        testUser2.setName("testName2");
        testUser2.setUsername("testUsername2");
        testUser2.setPassword("testPassword2");
        testUser2.setBio("testBio2");
        testUser2.setEmail("test@test.com");

        assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
    }

	@Test
	void loginUser_unknownUsername_throwsUnauthorized() {
		ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userService.loginUser("doesNotExist", "anyPassword")
    );

    assertEquals(401, exception.getStatusCode().value());
    assertTrue(exception.getReason().contains("Invalid username or password"));
	}

	@Test
	void loginUser_wrongPassword_throwsUnauthorized() {
		User user = new User();
		user.setName("testName");
		user.setUsername("testUsername");
		user.setPassword("correctPassword");
		user.setBio("testBio");
        user.setEmail("test@test.com");
		userService.createUser(user);

		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> userService.loginUser("testUsername", "wrongPassword")
		);

		assertEquals(401, exception.getStatusCode().value());
		assertTrue(exception.getReason().contains("Invalid username or password"));
	}

	@Test
	void loginUser_validCredentials_successAndTokenRotates() {
		User user = new User();
		user.setName("testName");
		user.setUsername("testUsername");
		user.setPassword("testPassword");
		user.setBio("testBio");
        user.setEmail("test@test.com");

		User createdUser = userService.createUser(user);
		String tokenBeforeLogin = createdUser.getToken();

		User loggedInUser = userService.loginUser("testUsername", "testPassword");

		assertEquals(createdUser.getId(), loggedInUser.getId());
		assertEquals("testUsername", loggedInUser.getUsername());
		assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
		assertNotNull(loggedInUser.getToken());
		assertNotEquals(tokenBeforeLogin, loggedInUser.getToken());
	}
}
