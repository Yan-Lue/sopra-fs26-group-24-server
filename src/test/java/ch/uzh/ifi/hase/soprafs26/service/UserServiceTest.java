package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

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

}
