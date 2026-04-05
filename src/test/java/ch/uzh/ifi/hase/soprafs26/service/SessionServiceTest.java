package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import ch.uzh.ifi.hase.soprafs26.service.model.MovieFilters;
import ch.uzh.ifi.hase.soprafs26.service.model.SimilarMovie;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestUserRepository guestUserRepository;

    @Mock
    private TmdbService tmdbService;

    @InjectMocks
    private SessionService sessionService;

    private Session testSession;

    private String token;

    private User testUser;
    private GuestUser testGuest;

    @BeforeEach
    void setup() {
        testSession = new Session();
        testSession.setSessionName("testSession");
        testSession.setMaxPlayers(5);
        testSession.setHostId(1L);

        testUser = new User();
        testUser.setId(1L);

        testGuest = new GuestUser();
        testGuest.setId(1L);

        token = "randomToken";
    }

    @Test
    void createSession_withoutRoundLimit_assignsDefaults() {
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

        Session createdSession = sessionService.createSession(testSession, token);

        Mockito.verifyNoInteractions(tmdbService);
        Mockito.verify(sessionRepository).save(testSession);
        Mockito.verify(sessionRepository).flush();

        assertEquals(15, createdSession.getRoundLimit());
        assertEquals(0, createdSession.getCurrentMovieIndex());
        assertEquals(List.of(), createdSession.getSessionMovieIds());
        assertEquals(SessionStatus.ONLINE, createdSession.getStatus());
        assertNotNull(createdSession.getCreationDate());
        assertNotNull(createdSession.getSessionCode());
        assertEquals(5, createdSession.getSessionCode().length());
    }

    @Test
    void createSession_missingRequiredFields_throwsBadRequest() {
        Session invalidSession = new Session();
        invalidSession.setHostId(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.createSession(invalidSession, token));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Failed to create Session", exception.getReason());
        Mockito.verifyNoInteractions(tmdbService);
    }

    @Test
    void getNextMovie_validSession_returnsMovieAndAdvancesIndex() {
        Session storedSession = new Session();
        storedSession.setSessionId(1L);
        storedSession.setCurrentMovieIndex(0);
        storedSession.setSessionMovieIds(List.of(55L, 66L));

        Movie movie = new Movie(
                55L,
                "Fight Club",
                "desc",
                "/poster.jpg",
                8.8,
                "1999-10-15",
                List.of("Drama"),
                List.of(new SimilarMovie(66L, "Se7en", "/poster2.jpg", 8.3, "1995-09-22")));

        Mockito.when(sessionRepository.findSessionBySessionCode("1")).thenReturn(storedSession);
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(movie);

        Movie result = sessionService.getNextMovie("1");

        assertEquals(movie, result);
        assertEquals(1, storedSession.getCurrentMovieIndex());
        Mockito.verify(tmdbService, Mockito.times(1)).getMovieDetails(55L);
        Mockito.verify(sessionRepository, Mockito.times(1)).save(storedSession);
        Mockito.verify(sessionRepository, Mockito.times(1)).flush();
    }

    @Test
    void getNextMovie_unknownSession_throwsNotFound() {
        Mockito.when(sessionRepository.findSessionBySessionCode("999")).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.getNextMovie("999"));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Session could not be found.", exception.getReason());
    }

    @Test
    void getNextMovie_withoutAssignedMovies_throwsConflict() {
        Session storedSession = new Session();
        storedSession.setSessionId(1L);
        storedSession.setCurrentMovieIndex(0);
        storedSession.setSessionMovieIds(List.of());

        Mockito.when(sessionRepository.findSessionBySessionCode("1")).thenReturn(storedSession);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.getNextMovie("1"));

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("Session has no movies assigned", exception.getReason());
        Mockito.verifyNoInteractions(tmdbService);
    }

    @Test
    void updateSessionFilters_validInput_fetchesMovieIdsAndStoresThem() {
        Session storedSession = new Session();
        storedSession.setSessionId(1L);
        storedSession.setSessionCode("ABCDE");
        storedSession.setCurrentMovieIndex(4);

        SessionFilterPutDTO dto = new SessionFilterPutDTO();
        dto.setRoundLimit(3);
        dto.setGenres(List.of("Action", "Romance"));
        dto.setMinRating(7.5);
        dto.setReleaseYear(2024);

        List<Long> movieIds = List.of(101L, 102L, 103L);

        Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(tmdbService.discoverMovieIds(Mockito.eq(3), Mockito.any(MovieFilters.class)))
                .thenReturn(movieIds);

        Session updatedSession = sessionService.updateSessionFilters("ABCDE", dto);

        Mockito.verify(tmdbService).discoverMovieIds(Mockito.eq(3), Mockito.any(MovieFilters.class));
        Mockito.verify(sessionRepository).save(storedSession);
        Mockito.verify(sessionRepository).flush();

        assertEquals(3, updatedSession.getRoundLimit());
        assertEquals(0, updatedSession.getCurrentMovieIndex());
        assertEquals(movieIds, updatedSession.getSessionMovieIds());
    }

    @Test
    void updateSessionFilters_withoutRoundLimit_usesDefaultLimit() {
        Session storedSession = new Session();
        storedSession.setSessionId(1L);
        storedSession.setSessionCode("ABCDE");

        SessionFilterPutDTO dto = new SessionFilterPutDTO();
        dto.setGenres(List.of("Action"));

        List<Long> movieIds = java.util.stream.LongStream.rangeClosed(1, 15).boxed().toList();

        Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(tmdbService.discoverMovieIds(Mockito.eq(15), Mockito.any(MovieFilters.class)))
                .thenReturn(movieIds);

        Session updatedSession = sessionService.updateSessionFilters("ABCDE", dto);

        Mockito.verify(tmdbService).discoverMovieIds(Mockito.eq(15), Mockito.any(MovieFilters.class));
        assertEquals(15, updatedSession.getRoundLimit());
        assertEquals(movieIds, updatedSession.getSessionMovieIds());
    }

    @Test
    void updateSessionFilters_unknownSession_throwsNotFound() {
        SessionFilterPutDTO dto = new SessionFilterPutDTO();
        dto.setGenres(List.of("Action"));

        Mockito.when(sessionRepository.findSessionBySessionCode("MISSING")).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.updateSessionFilters("MISSING", dto)
        );

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Session could not be found.", exception.getReason());
    }

    @Test
    void updateSessionFilters_withoutAnyFilters_fetchesDefaultMoviePool() {
        Session storedSession = new Session();
        storedSession.setSessionId(1L);
        storedSession.setSessionCode("ABCDE");

        SessionFilterPutDTO dto = new SessionFilterPutDTO();
        dto.setRoundLimit(3);

        List<Long> movieIds = List.of(11L, 12L, 13L);

        Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(tmdbService.discoverMovieIds(Mockito.eq(3), Mockito.any(MovieFilters.class)))
                .thenReturn(movieIds);

        Session updatedSession = sessionService.updateSessionFilters("ABCDE", dto);

        assertEquals(3, updatedSession.getRoundLimit());
        assertEquals(List.of(11L, 12L, 13L), updatedSession.getSessionMovieIds());
        assertEquals(0, updatedSession.getCurrentMovieIndex());
    }
}
