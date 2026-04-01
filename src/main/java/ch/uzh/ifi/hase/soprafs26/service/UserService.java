package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String GUEST_USERNAME_PREFIX = "guest_";
    private static final List<String> GUEST_NAMES = Arrays.asList(
            "otter", "panda", "falcon", "lynx", "badger", "koala", "tiger", "rabbit", "beaver", "fox", "walrus", "gecko",
            "ferret", "owl", "bison", "lemur", "grootcod", "moose", "orca", "raven", "seal", "wombat", "zebra", "alpaca", "buffalo",
            "cougar", "dolphin", "lion"
    );

	private final UserRepository userRepository;
	private final GuestUserRepository guestUserRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(@Qualifier("userRepository") UserRepository userRepository,
			GuestUserRepository guestUserRepository) {
		this.userRepository = userRepository;
		this.guestUserRepository = guestUserRepository;
		this.passwordEncoder = new BCryptPasswordEncoder(12);
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		checkIfUserExists(newUser);

		String hashedPassword = passwordEncoder.encode(newUser.getPassword());
		newUser.setPassword(hashedPassword);

		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.ONLINE);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	public GuestUser createGuestUser() {

		GuestUser newGuestUser = new GuestUser();
		String randomUsername = generateGuestUsername();

		newGuestUser.setUsername(randomUsername);
		newGuestUser.setToken("Guest-" + UUID.randomUUID());
		newGuestUser.setStatus(UserStatus.ONLINE);

		newGuestUser = guestUserRepository.save(newGuestUser);
		guestUserRepository.flush();

		log.debug("Created Information for Guest User: {}", newGuestUser);
		return newGuestUser;
	}

    public String generateGuestUsername() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String genName = GUEST_NAMES.get(ThreadLocalRandom.current().nextInt(GUEST_NAMES.size()));
            String username = GUEST_USERNAME_PREFIX + genName;

            if (isGuestUsernameAvailable(username)) {
                return username;
            }
        }

        String fallbackUsername = GUEST_USERNAME_PREFIX
                + GUEST_NAMES.get(ThreadLocalRandom.current().nextInt(GUEST_NAMES.size()))
                + UUID.randomUUID().toString().substring(0, 4);

        while (!isGuestUsernameAvailable(fallbackUsername)) {
            fallbackUsername = GUEST_USERNAME_PREFIX
                    + GUEST_NAMES.get(ThreadLocalRandom.current().nextInt(GUEST_NAMES.size()))
                    + UUID.randomUUID().toString().substring(0, 4);
        }

        return fallbackUsername;
    }

    private boolean isGuestUsernameAvailable(String username) {
        return userRepository.findByUsername(username) == null
                && guestUserRepository.findByUsername(username) == null;
    }

	public User loginUser(String username, String password) {
		User user = userRepository.findByUsername(username);

		if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}

		user.setStatus(UserStatus.ONLINE);
		user.setToken(UUID.randomUUID().toString());
		userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}



	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
        User userByEmail = userRepository.findByEmail(userToBeCreated.getEmail());

		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
		}

        if (userByEmail != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
	}

	public User getUserById(Long userid) {
		return userRepository.findById(userid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with userid " + userid + " not found"));
	}
}
