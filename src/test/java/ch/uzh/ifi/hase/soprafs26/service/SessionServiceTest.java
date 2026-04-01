package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
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
    private TmdbService tmdbService;

    @InjectMocks
    private SessionService sessionService;

    private Session testSession;

    @BeforeEach
    void setup() {
        testSession = new Session();
        testSession.setSessionName("testSession");
        testSession.setMaxPlayers(5);
        testSession.setHostId(1L);
    }

    @Test
    void createSession_withoutRoundLimit_aassignsDefaultsandMovieIds() {
        List<Long> movieIds = List.of(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L);
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(tmdbService.discoverMovieIds(15)).thenReturn(movieIds);

        Session createdSession = sessionService.createSession(testSession);

        Mockito.verify(tmdbService, Mockito.times(1)).discoverMovieIds(15);
        Mockito.verify(sessionRepository, Mockito.times(1)).save(testSession);
        Mockito.verify(sessionRepository, Mockito.times(1)).flush();

        assertEquals(15, createdSession.getRoundLimit());
        assertEquals(0, createdSession.getCurrentMovieIndex());
        assertEquals(movieIds, createdSession.getSessionMovieIds());
        assertEquals(SessionStatus.ONLINE, createdSession.getStatus());
        assertNotNull(createdSession.getCreationDate());
        assertNotNull(createdSession.getSessionCode());
        assertEquals(5, createdSession.getSessionCode().length());
    }

    @Test
    void createSession_withRoundLimit_usesProvidedLimit() {
        testSession.setRoundLimit(3);
        List<Long> movieIds = List.of(101L, 102L, 103L);
        Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(tmdbService.discoverMovieIds(3)).thenReturn(movieIds);

        Session createdSession = sessionService.createSession(testSession);

        Mockito.verify(tmdbService, Mockito.times(1)).discoverMovieIds(3);
        assertEquals(3, createdSession.getRoundLimit());
        assertEquals(movieIds, createdSession.getSessionMovieIds());
    }

    @Test
    void createSession_missingRequiredFields_throwsBadRequest() {
        Session invalidSession = new Session();
        invalidSession.setHostId(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.createSession(invalidSession)
        );

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
                List.of("Drama")
        );

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
                () -> sessionService.getNextMovie("999")
        );

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
                () -> sessionService.getNextMovie("1")
        );

        assertEquals(409, exception.getStatusCode().value());
        assertEquals("Session has no movies assigned", exception.getReason());
        Mockito.verifyNoInteractions(tmdbService);
    }
}
