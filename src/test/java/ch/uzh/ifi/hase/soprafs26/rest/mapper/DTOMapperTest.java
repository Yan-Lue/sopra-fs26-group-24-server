package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.entity.HistoryMovieEntry;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import ch.uzh.ifi.hase.soprafs26.service.model.SimilarMovie;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
class DTOMapperTest {
	@Test
    void testCreateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("name");
		userPostDTO.setUsername("username");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getName(), user.getName());
		assertEquals(userPostDTO.getUsername(), user.getUsername());
	}

	@Test
	void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
		user.setName("Firstname Lastname");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check content
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getName(), userGetDTO.getName());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
	}

	@Test
    void testUpdateUser_fromUserPutDTO_toUser_success() {
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setName("updatedName");
        userPutDTO.setUsername("updatedUsername");
        userPutDTO.setBio("updatedBio");
        userPutDTO.setEmail("updated@test.com");
        userPutDTO.setOldPassword("oldPassword");
        userPutDTO.setNewPassword("newPassword");
        userPutDTO.setStatus("ONLINE");

        User user = DTOMapper.INSTANCE.convertUserPutDTOtoEntity(userPutDTO);

        assertEquals(userPutDTO.getName(), user.getName());
        assertEquals(userPutDTO.getUsername(), user.getUsername());
        assertEquals(userPutDTO.getBio(), user.getBio());
        assertEquals(userPutDTO.getEmail(), user.getEmail());
        assertNull(user.getPassword());
        assertNull(user.getStatus());
    }

    @Test
    void testGetMovie_fromMovie_toMovieGetDTO_success() {
        Movie movie = new Movie(
                550L,
                "Fight Club",
                "Insomnia and soap.",
                "https://image.tmdb.org/t/p/w500/fight-club.jpg",
                8.4,
                "1999-10-15",
                List.of("Drama", "Thriller"),
                List.of(new SimilarMovie(
                        551L,
                        "Se7en",
                        "https://image.tmdb.org/t/p/w500/se7en.jpg",
                        8.3,
                        "1995-09-22"))
        );

        MovieGetDTO movieGetDTO = DTOMapper.INSTANCE.convertMovieGetDTOtoEntity(movie);

        assertEquals(movie.getId(), movieGetDTO.getMovieId());
        assertEquals(movie.getTitle(), movieGetDTO.getTitle());
        assertEquals(movie.getOverview(), movieGetDTO.getDescription());
        assertEquals(movie.getPosterPath(), movieGetDTO.getPosterPath());
        assertEquals(movie.getRating(), movieGetDTO.getRating());
        assertEquals(movie.getReleaseDate(), movieGetDTO.getReleaseDate());
        assertEquals(movie.getGenres(), movieGetDTO.getGenres());
        assertEquals(1, movieGetDTO.getSimilarMovies().size());
        SimilarMovieGetDTO similarMovieGetDTO = movieGetDTO.getSimilarMovies().get(0);
        assertEquals(551L, similarMovieGetDTO.getMovieId());
        assertEquals("Se7en", similarMovieGetDTO.getTitle());
        assertEquals("https://image.tmdb.org/t/p/w500/se7en.jpg", similarMovieGetDTO.getPosterPath());
        assertEquals(8.3, similarMovieGetDTO.getRating());
        assertEquals("1995-09-22", similarMovieGetDTO.getReleaseDate());
    }

    @Test
    void convertEntityToHistoryGetDTO_validInput_mapsCorrectly() {

        History history = new History();
        history.setHistoryId(1L);
        history.setSessionName("Test Round");
        history.setSessionCode("ABCDE");
        history.setJoinedUsers(3);
        history.setCreationDate(new Date());
        history.setUserId(7L);
        history.setMovies(List.of());

        HistoryGetDTO dto = DTOMapper.INSTANCE.convertEntityToHistoryGetDTO(history);

        assertEquals(1L, dto.getHistoryId());
        assertEquals("Test Round", dto.getSessionName());
        assertEquals("ABCDE", dto.getSessionCode());
        assertEquals(3, dto.getJoinedUsers());
        assertEquals(7L, dto.getUserId());
        assertEquals(history.getCreationDate(), dto.getCreationDate());
        assertNotNull(dto.getMovies());
        assertTrue(dto.getMovies().isEmpty());
    }

    @Test
    void convertEntityToHistoryGetDTO_withMovies_mapsMoviesCorrectly() {

        HistoryMovieEntry entry = new HistoryMovieEntry();
        entry.setMovieId(10L);
        entry.setScore(5);

        History history = new History();
        history.setHistoryId(1L);
        history.setMovies(List.of(entry));

        HistoryGetDTO dto = DTOMapper.INSTANCE.convertEntityToHistoryGetDTO(history);

        assertNotNull(dto.getMovies());
        assertEquals(1, dto.getMovies().size());
        assertEquals(10L, dto.getMovies().get(0).getMovieId());
        assertEquals(5, dto.getMovies().get(0).getScore());
    }
}
