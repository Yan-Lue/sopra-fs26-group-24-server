package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import java.util.Optional;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
		// given
		User user = new User();
		user.setName("Firstname Lastname");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);

		List<User> allUsers = Collections.singletonList(user);

		// this mocks the UserService -> we define above what the userService should
		// return when getUsers() is called
		given(userService.getUsers()).willReturn(allUsers);

		// when
		MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is(user.getName())))
				.andExpect(jsonPath("$[0].username", is(user.getUsername())))
				.andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
	}

	@Test
	void createUser_validInput_userCreated() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);
		user.setEmail("test@test.com");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("Test User");
		userPostDTO.setUsername("testUsername");

		given(userService.createUser(Mockito.any())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.name", is(user.getName())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	// POST Mapping /register for guest user successful 201
	@Test
	void createGuestUser_validInput_guestUserCreated() throws Exception {
		GuestUser guestUser = new GuestUser();
		guestUser.setId(1L);
		guestUser.setToken("Guest-randomToken");
		guestUser.setUsername("Guest-12345");
		guestUser.setStatus(UserStatus.ONLINE);

		given(userService.createGuestUser()).willReturn(guestUser);

		MockHttpServletRequestBuilder postRequest = post("/register")
				.contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(guestUser.getId().intValue())))
				.andExpect(jsonPath("$.username", is(guestUser.getUsername())))
				.andExpect(jsonPath("$.token", is(guestUser.getToken())))
				.andExpect(jsonPath("$.status", is(guestUser.getStatus().toString())));
	}

	// POST Mapping /login successful 200
	@Test
	void loginUser_validInput_userLoggedIn() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testUsername");
		userPostDTO.setPassword("testPassword");

		given(userService.loginUser("testUsername", "testPassword")).willReturn(user);

		MockHttpServletRequestBuilder postRequest = post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.name", is(user.getName())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.token", is(user.getToken())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));

	}

	// POST Mapping /login with wrong password 401
	@Test
	void loginUser_invalidInput_userNotLoggedIn() throws Exception {
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testUsername");
		userPostDTO.setPassword("invalidPassword");

		given(userService.loginUser("testUsername", "invalidPassword"))
				.willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

		MockHttpServletRequestBuilder postRequest = post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isUnauthorized());

	}

	// POST Mapping /register with invalid email
	@Test
	void createUser_invalidEmail_throwsException() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);
		user.setEmail("obvious invalid email");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("Test User");
		userPostDTO.setUsername("testUsername");
		userPostDTO.setEmail("obvious invalid email");

		given(userService.createUser(Mockito.any())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isBadRequest());
	}

	// GET Mapping /users/{id} successful 200
	@Test
	public void givenUserId_whenGetUserById_thenReturnJson() throws Exception {
		User user = new User();
		user.setId(1L); // mock user with id 1
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.OFFLINE);

		given(userService.getUserById(1L)).willReturn(user);

		MockHttpServletRequestBuilder getRequest = get("/users/1").contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(getRequest)
				.andExpect(status().isOk()) // 200
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.name", is(user.getName())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	// GET Mapping /users/{id} with wrong id 404
	@Test
	public void givenUserId_invalidUserId_whenGetUserById_thenReturnIdNotFound() throws Exception {
		// no user needed as error will be thrown anyway
		given(userService.getUserById(444L))
				.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "user with userId 444 was not found"));

		MockHttpServletRequestBuilder getRequest = get("/users/444").contentType(MediaType.APPLICATION_JSON);

		mockMvc.perform(getRequest)
				.andExpect(status().isNotFound());
	}

	@Test
    void updateUser_validInput_userUpdated() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Name");
        updatedUser.setUsername("updatedUsername");
        updatedUser.setBio("Updated bio");
        updatedUser.setStatus(UserStatus.ONLINE);

        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setName("Updated Name");
        userPutDTO.setUsername("updatedUsername");
        userPutDTO.setBio("Updated bio");
        userPutDTO.setEmail("updated@test.com");
        userPutDTO.setOldPassword("oldPassword");
        userPutDTO.setNewPassword("newPassword");
        userPutDTO.setStatus("ONLINE");

        given(userService.updateUser(
                Mockito.eq(1L),
                Mockito.any(User.class),
                Mockito.eq("oldPassword"),
                Mockito.eq("newPassword"),
                Mockito.eq("ONLINE")
        )).willReturn(updatedUser);

        MockHttpServletRequestBuilder putRequest = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPutDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.username", is("updatedUsername")))
                .andExpect(jsonPath("$.bio", is("Updated bio")))
                .andExpect(jsonPath("$.status", is("ONLINE")));
    }

    @Test
    void updateUser_invalidEmail_returnsBadRequest() throws Exception {
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setName("Updated Name");
        userPutDTO.setUsername("updatedUsername");
        userPutDTO.setEmail("invalid-email");
        userPutDTO.setStatus("ONLINE");

        MockHttpServletRequestBuilder putRequest = put("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPutDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_validId_noContent() throws Exception {
        MockHttpServletRequestBuilder deleteRequest = delete("/users/1")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());

        Mockito.verify(userService, Mockito.times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_invalidId_notFound() throws Exception {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User with userid 999 not found"))
                .when(userService).deleteUser(999L);

        MockHttpServletRequestBuilder deleteRequest = delete("/users/999")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }

	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}