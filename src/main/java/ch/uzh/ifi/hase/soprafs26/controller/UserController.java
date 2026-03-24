package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers() {
		// fetch all users in the internal representation
		List<User> users = userService.getUsers();
		List<UserGetDTO> userGetDTOs = new ArrayList<>();

		// convert each user to the API representation
		for (User user : users) {
			userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
		}
		return userGetDTOs;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO createUser(@RequestBody(required = false) UserPostDTO userPostDTO) {
		// differentiate between guest User or "regular User"
		if (userPostDTO == null || userPostDTO.getUsername() == null) {

			GuestUser guestUser = userService.createGuestUser();

			return DTOMapper.INSTANCE.convertGuestEntityToUserGetDTO(guestUser);
		} else {
			// convert API user to internal representation
			User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

			// create user
			User createdUser = userService.createUser(userInput);
			// convert internal representation of user back to API
			return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
			// add extra comment for deployment test
		}

	}

	@PostMapping("/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		User loggedInUser = userService.loginUser(userInput.getUsername(), userInput.getPassword());
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(loggedInUser);
	}
}
