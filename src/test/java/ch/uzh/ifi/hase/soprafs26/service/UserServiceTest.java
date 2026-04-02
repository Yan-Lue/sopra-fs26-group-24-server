package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;

class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private GuestUserRepository guestUserRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;
	private GuestUser guestTestUser;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(1L);
		testUser.setName("testName");
		testUser.setUsername("testUsername");

		// given Guest User
		guestTestUser = new GuestUser();
		guestTestUser.setId(2L);
		guestTestUser.setUsername("Guest-Random");
		guestTestUser.setToken("Guest-Token");
		guestTestUser.setStatus(UserStatus.ONLINE);

		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);

		// if anything is saved into the guest User Database --> return this created
		// simple Guest User
		Mockito.when(guestUserRepository.save(Mockito.any())).thenReturn(guestTestUser);

	}

	// Test of saving a new user

	@Test
	void createUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy
		// testUser
		User createdUser = userService.createUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getName(), createdUser.getName());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.ONLINE, createdUser.getStatus());
	}

	// Test of saving a new guest user
	@Test
	void createGuestUser_validInputs_success() {

		GuestUser createdGuestUser = userService.createGuestUser();

		Mockito.verify(guestUserRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(guestTestUser.getId(), createdGuestUser.getId());
		assertEquals(guestTestUser.getUsername(), createdGuestUser.getUsername());
		assertEquals("Guest-Token", createdGuestUser.getToken());
		assertEquals(UserStatus.ONLINE, createdGuestUser.getStatus());
	}

	@Test
	void createUser_duplicateInputs_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
		Mockito.when(userRepository.findByEmail(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error
		// is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	void deleteUser_regularUser_success() {
		Mockito.when(userRepository.existsById(1L)).thenReturn(true);

		userService.deleteUser(1L);

		Mockito.verify(userRepository, Mockito.times(1)).deleteById(1L);
		Mockito.verify(userRepository, Mockito.times(1)).flush();
		Mockito.verify(guestUserRepository, Mockito.never()).deleteById(Mockito.anyLong());
	}

	@Test
	void deleteUser_guestUser_success() {
		Mockito.when(userRepository.existsById(2L)).thenReturn(false);
		Mockito.when(guestUserRepository.existsById(2L)).thenReturn(true);

		userService.deleteUser(2L);

		Mockito.verify(guestUserRepository, Mockito.times(1)).deleteById(2L);
		Mockito.verify(guestUserRepository, Mockito.times(1)).flush();
		Mockito.verify(userRepository, Mockito.never()).deleteById(Mockito.anyLong());
	}

	@Test
	void deleteUser_notFound_throwsNotFound() {
		Mockito.when(userRepository.existsById(999L)).thenReturn(false);
		Mockito.when(guestUserRepository.existsById(999L)).thenReturn(false);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> userService.deleteUser(999L));

		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void updateUser_validInput_success() {
		User existingUser = new User();
		existingUser.setId(1L);
		existingUser.setName("Old Name");
		existingUser.setUsername("oldUsername");
		existingUser.setBio("old bio");
		existingUser.setEmail("old@test.com");
		existingUser.setStatus(UserStatus.ONLINE);
		existingUser.setToken("token");
		existingUser.setPassword(new BCryptPasswordEncoder(12).encode("oldPass"));

		User updateInput = new User();
		updateInput.setName("New Name");
		updateInput.setUsername("newUsername");
		updateInput.setBio("new bio");
		updateInput.setEmail("new@test.com");

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);
		Mockito.when(userRepository.findByEmail("new@test.com")).thenReturn(null);
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User updated = userService.updateUser(1L, updateInput, "oldPass", "newPass", "OFFLINE");

		assertEquals("New Name", updated.getName());
		assertEquals("newUsername", updated.getUsername());
		assertEquals("new bio", updated.getBio());
		assertEquals("new@test.com", updated.getEmail());
		assertEquals(UserStatus.OFFLINE, updated.getStatus());
		assertEquals(null, updated.getToken());
		assertTrue(new BCryptPasswordEncoder(12).matches("newPass", updated.getPassword()));
	}

	@Test
	void updateUser_wrongOldPassword_throwsBadRequest() {
		User existingUser = new User();
		existingUser.setId(1L);
		existingUser.setPassword(new BCryptPasswordEncoder(12).encode("correctOldPassword"));
		existingUser.setEmail("old@test.com");
		existingUser.setUsername("oldUsername");
		existingUser.setStatus(UserStatus.ONLINE);

		User updateInput = new User();
		updateInput.setEmail("new@test.com");

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		Mockito.when(userRepository.findByEmail("new@test.com")).thenReturn(null);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> userService.updateUser(1L, updateInput, "wrongOldPassword", "newPass", null));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void updateUser_invalidStatus_throwsBadRequest() {
		User existingUser = new User();
		existingUser.setId(1L);
		existingUser.setPassword(new BCryptPasswordEncoder(12).encode("oldPass"));
		existingUser.setEmail("old@test.com");
		existingUser.setUsername("oldUsername");
		existingUser.setStatus(UserStatus.ONLINE);

		User updateInput = new User();

		Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> userService.updateUser(1L, updateInput, null, null, "NOT_A_STATUS"));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void updateUser_notFound_throwsNotFound() {
		Mockito.when(userRepository.findById(123L)).thenReturn(Optional.empty());

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> userService.updateUser(123L, new User(), null, null, null));

		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

}
