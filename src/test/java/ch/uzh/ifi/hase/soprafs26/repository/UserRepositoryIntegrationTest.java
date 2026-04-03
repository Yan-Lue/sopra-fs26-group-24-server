package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;

@DataJpaTest
class UserRepositoryIntegrationTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	private User user;

	@BeforeEach
	void setup() {
		user = new User();
		user.setName("Firstname Lastname");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");
		user.setPassword("password");
		user.setEmail("firstname@lastname");

		entityManager.persist(user);
		entityManager.flush();
	}

	@Test
	void findByName_success() {

		// when
		User found = userRepository.findByName(user.getName());

		// then
		assertNotNull(found.getId());
		assertEquals(found.getName(), user.getName());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
		assertEquals(found.getEmail(), user.getEmail());
	}

	@Test
	void findByToken_success() {
		User found = userRepository.findByToken(user.getToken());

		assertNotNull(found.getId());
		assertEquals(found.getName(), user.getName());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
		assertEquals(found.getEmail(), user.getEmail());
	}

	@Test
	void findByEmail_success() {
		User found = userRepository.findByEmail(user.getEmail());

		assertNotNull(found.getId());
		assertEquals(found.getName(), user.getName());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
		assertEquals(found.getEmail(), user.getEmail());
	}

	@Test
	void findByUsername_success() {
		User found = userRepository.findByUsername(user.getUsername());

		assertNotNull(found.getId());
		assertEquals(found.getName(), user.getName());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
		assertEquals(found.getEmail(), user.getEmail());
	}
}
