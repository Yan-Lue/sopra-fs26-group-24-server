package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "password", target = "password")
	@Mapping(source = "email", target = "email")
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	UserGetDTO convertGuestEntityToUserGetDTO(GuestUser guestUser);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "bio", target = "bio")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "sessionName", target = "sessionName")
	@Mapping(source = "maxPlayers", target = "maxPlayers")
    @Mapping(source = "roundLimit", target = "roundLimit")
	@Mapping(source = "hostId", target = "hostId")
	Session convertSessionPostDTOtoEntity(SessionPostDTO sessionPostDTO);

	@Mapping(source = "sessionCode", target = "sessionCode")
	@Mapping(source = "sessionToken", target = "sessionToken")
    @Mapping(source = "sessionId", target = "sessionId")
	SessionGetDTO convertEntitytoSessionGetDTO(Session createdSession);

    @Mapping(source = "id", target = "movieId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "overview", target = "description")
    @Mapping(source = "posterPath", target = "posterPath")
    @Mapping(source = "rating", target = "rating")
    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "genres", target = "genres")
    MovieGetDTO convertMovieGetDTOtoEntity(Movie movie);
}
