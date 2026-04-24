package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.VotePutDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import ch.uzh.ifi.hase.soprafs26.service.model.MovieFilters;
import ch.uzh.ifi.hase.soprafs26.service.model.SimilarMovie;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MovieResultDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

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

        @Mock
        private SimpMessagingTemplate messagingTemplate;

        @Mock
        private VoteRepository voteRepository;

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
                testUser.setToken(token);

                testGuest = new GuestUser();
                testGuest.setId(1L);

                token = "randomToken";
        }

        @Test
        void createSession_withoutRoundLimit_assignsDefaults() {
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Mockito.when(userRepository.findByToken(token)).thenReturn(testUser);

                Session createdSession = sessionService.createSession(testSession, token);

                Mockito.verifyNoInteractions(tmdbService);
                verify(sessionRepository).save(testSession);
                verify(sessionRepository).flush();

                assertEquals(15, createdSession.getRoundLimit());
                assertEquals(15, createdSession.getTimePerRound());
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
        void createSession_userTokenButUserNotFound_throwsNotFound() {
            Mockito.when(userRepository.findByToken("token")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.createSession(testSession, "token"));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("User not found", ex.getReason());
        }

        @Test
        void createSession_guestTokenButGuestNotFound_throwsNotFound() {
            Mockito.when(guestUserRepository.findByToken("Guest123")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.createSession(testSession, "Guest123"));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("Guest User not found", ex.getReason());
        }

        @Test
        void createSession_hostIdNotFoundAnywhere_throwsNotFound() {
            testSession.setHostId(99L);

            Mockito.when(guestUserRepository.findById(99L)).thenReturn(Optional.empty());
            Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.createSession(testSession, null));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("Host User not found", ex.getReason());
        }

        @Test
        void createSession_guestHost_setsCurrentSession() {
            Mockito.when(guestUserRepository.findByToken("Guest123")).thenReturn(testGuest);
            Mockito.when(sessionRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

            sessionService.createSession(testSession, "Guest123");

            assertEquals(testSession, testGuest.getCurrentSession());
            verify(guestUserRepository).save(testGuest);
            verify(guestUserRepository).flush();
        }

        @Test
        void createSession_invalidInput_throwsBadRequest() {
            Session testSession2 = new Session();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.createSession(testSession2, null));

            assertEquals(400, ex.getStatusCode().value());
        }

        @Test
        void createSession_noTokenAndNoHostId_throws() {
            Session testSession2 = new Session();
            testSession2.setSessionName("Test");
            testSession2.setMaxPlayers(5);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.createSession(testSession2, null));

            assertEquals(400, ex.getStatusCode().value());
            assertEquals("Please try logging in again", ex.getReason());
        }

        @Test
        void getNextMovie_validSession_resetsVoteProgress() {
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
                Mockito.doNothing().when(messagingTemplate).convertAndSend(Mockito.anyString(), Mockito.<Object>any());

                Movie result = sessionService.getNextMovie("1");

                assertEquals(movie, result);
                assertEquals(1, storedSession.getCurrentMovieIndex());
                verify(tmdbService, Mockito.times(1)).getMovieDetails(55L);
                verify(sessionRepository, Mockito.times(1)).save(storedSession);
                verify(sessionRepository, Mockito.times(1)).flush();
                verify(messagingTemplate).convertAndSend(
                        Mockito.eq("/topic/session/1/vote-progress"),
                        Mockito.any(Object.class));
        }

        @Test
        void getNextMovie_validSession_broadcastsNextMovie() {
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
                Mockito.doNothing().when(messagingTemplate).convertAndSend(Mockito.anyString(), Mockito.<Object>any());

                Movie result = sessionService.getNextMovie("1");

                assertEquals(movie, result);
                assertEquals(1, storedSession.getCurrentMovieIndex());
                verify(tmdbService, Mockito.times(1)).getMovieDetails(55L);
                verify(sessionRepository, Mockito.times(1)).save(storedSession);
                verify(sessionRepository, Mockito.times(1)).flush();
                verify(messagingTemplate).convertAndSend(
                        Mockito.eq("/topic/session/1/next"),
                        Mockito.any(Object.class));
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

                verify(tmdbService).discoverMovieIds(Mockito.eq(3), Mockito.any(MovieFilters.class));
                verify(sessionRepository).save(storedSession);
                verify(sessionRepository).flush();

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

                verify(tmdbService).discoverMovieIds(Mockito.eq(15), Mockito.any(MovieFilters.class));
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
                                () -> sessionService.updateSessionFilters("MISSING", dto));

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

        @Test
        void calculateFullLeaderboard_validSession_returnsDetailedSortedResultsAndBroadcasts() {
                Session session = new Session();
                session.setSessionCode("test1234");
                session.setSessionMovieIds(List.of(11L, 22L));

                Movie movie1 = new Movie(
                        11L,
                        "Movie A",
                        "Desc A",
                        "https://img/a.jpg",
                        7.5,
                        "2020-01-01",
                        List.of("Drama"),
                        List.of(new SimilarMovie(111L, "Sim A", "https://img/simA.jpg", 6.8, "2019-01-01"))
                );

                Movie movie2 = new Movie(
                        22L,
                        "Movie B",
                        "Desc B",
                        "https://img/b.jpg",
                        8.4,
                        "2021-01-01",
                        List.of("Action"),
                        List.of()
                );

                Mockito.when(sessionRepository.findSessionBySessionCode("test1234")).thenReturn(session);
                Mockito.when(tmdbService.getMovieDetails(11L)).thenReturn(movie1);
                Mockito.when(tmdbService.getMovieDetails(22L)).thenReturn(movie2);

                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 11L, 1)).thenReturn(3L);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 11L, -1)).thenReturn(1L);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 11L, 0)).thenReturn(2L);
                Mockito.when(voteRepository.getSumOfScores("test1234", 11L)).thenReturn(2);

                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 22L, 1)).thenReturn(5L);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 22L, -1)).thenReturn(0L);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 22L, 0)).thenReturn(1L);
                Mockito.when(voteRepository.getSumOfScores("test1234", 22L)).thenReturn(5);

                List<MovieResultDTO> results = sessionService.calculateFullLeaderboard("test1234");

                assertEquals(2, results.size());

                assertEquals(22L, results.get(0).getMovieId());
                assertEquals("Movie B", results.get(0).getTitle());
                assertEquals(5, results.get(0).getScore());
                assertEquals(5, results.get(0).getLikes());
                assertEquals(0, results.get(0).getDislikes());
                assertEquals(1, results.get(0).getNeutrals());

                assertEquals(11L, results.get(1).getMovieId());
                assertEquals("Movie A", results.get(1).getTitle());
                assertEquals("Desc A", results.get(1).getDescription());
                assertEquals(2, results.get(1).getScore());
                assertEquals(3, results.get(1).getLikes());
                assertEquals(1, results.get(1).getDislikes());
                assertEquals(2, results.get(1).getNeutrals());
                assertEquals(1, results.get(1).getSimilarMovies().size());

                verify(messagingTemplate).convertAndSend("/topic/session/test1234/results", results);
                verify(tmdbService, Mockito.never()).getMovieResults(Mockito.anyMap());
        }

        @Test
        void calculateFullLeaderboard_invalidSession_throwsNotFound() {

                Mockito.when(sessionRepository.findSessionBySessionCode("random")).thenReturn(null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> sessionService.calculateFullLeaderboard("random"));

                assertEquals(404, exception.getStatusCode().value());
                assertEquals("Session could not be found.", exception.getReason());
        }

        @Test
        void calculateFullLeaderboard_whenSumIsNull_usesZeroScore() {
                Session session = new Session();
                session.setSessionCode("test1234");
                session.setSessionMovieIds(List.of(11L));

                Movie movie = new Movie(
                        11L, "Movie A", "Desc A", "https://img/a.jpg", 7.5, "2020-01-01", List.of("Drama"), List.of()
                );

                Mockito.when(sessionRepository.findSessionBySessionCode("test1234")).thenReturn(session);
                Mockito.when(tmdbService.getMovieDetails(11L)).thenReturn(movie);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 11L, 1)).thenReturn(0L);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 11L, -1)).thenReturn(0L);
                Mockito.when(voteRepository.countBySessionCodeAndMovieIdAndScore("test1234", 11L, 0)).thenReturn(0L);
                Mockito.when(voteRepository.getSumOfScores("test1234", 11L)).thenReturn(null);

                List<MovieResultDTO> results = sessionService.calculateFullLeaderboard("test1234");

                assertEquals(1, results.size());
                assertEquals(0, results.get(0).getScore());
        }

        @Test
        void getSessionTIming_validSessionCode_returnsTimePerRound() {
            Session storedSession = new Session();
            storedSession.setSessionCode("ABCDE");
            storedSession.setTimePerRound(15);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);

            Integer result = sessionService.getSessionTiming("ABCDE");

            assertEquals(15, result);
        }

        @Test
        void getNextMovie_whenNoMoreMovies_setsOffline_broadcastsEndAndThrowsConflict() {
                Session storedSession = new Session();
                storedSession.setSessionId(1L);
                storedSession.setSessionCode("ABCDE");
                storedSession.setCurrentMovieIndex(2);
                storedSession.setSessionMovieIds(List.of(55L, 66L));
                storedSession.setStatus(SessionStatus.ONLINE);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

                ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.getNextMovie("ABCDE"));

                assertEquals(409, exception.getStatusCode().value());
                assertEquals("No more movies available in this session", exception.getReason());
                assertEquals(SessionStatus.OFFLINE, storedSession.getStatus());

                verify(sessionRepository).save(storedSession);
                verify(sessionRepository).flush();
                verify(messagingTemplate).convertAndSend("/topic/session/ABCDE/end", "ABCDE");
                Mockito.verifyNoInteractions(tmdbService);
        }

        @Test
        void getCurrentMovie_startedSession_returnsCurrentMovie() {
                Session storedSession = new Session();
                storedSession.setSessionId(1L);
                storedSession.setSessionCode("ABCDE");
                storedSession.setSessionMovieIds(List.of(55L, 66L));
                storedSession.setCurrentMovieIndex(1);
                storedSession.setStatus(SessionStatus.ONLINE);

                Movie movie = new Movie(
                55L,
                "Fight Club",
                "desc",
                "/poster.jpg",
                8.8,
                "1999-10-15",
                List.of("Drama"),
                List.of());

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);
                Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(movie);

                Movie result = sessionService.getCurrentMovie("ABCDE");

                assertEquals(movie, result);
                verify(tmdbService).getMovieDetails(55L);
        }

        @Test
        void getCurrentMovie_notStarted_throwsConflict() {
                Session storedSession = new Session();
                storedSession.setSessionId(1L);
                storedSession.setSessionCode("ABCDE");
                storedSession.setSessionMovieIds(List.of(55L, 66L));
                storedSession.setCurrentMovieIndex(0);
                storedSession.setStatus(SessionStatus.ONLINE);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);

                ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.getCurrentMovie("ABCDE"));

                assertEquals(409, exception.getStatusCode().value());
                assertEquals("Session has not started yet", exception.getReason());
                Mockito.verifyNoInteractions(tmdbService);
        }

        @Test
        void getCurrentMovie_offlineSession_throwsConflict() {
                Session storedSession = new Session();
                storedSession.setSessionId(1L);
                storedSession.setSessionCode("ABCDE");
                storedSession.setSessionMovieIds(List.of(55L, 66L));
                storedSession.setCurrentMovieIndex(1);
                storedSession.setStatus(SessionStatus.OFFLINE);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);

                ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.getCurrentMovie("ABCDE"));

                assertEquals(409, exception.getStatusCode().value());
                assertEquals("Session has ended", exception.getReason());
                Mockito.verifyNoInteractions(tmdbService);
        }

        @Test
        void getCurrentMovie_unknownSession_throwsNotFound() {
                Mockito.when(sessionRepository.findSessionBySessionCode("MISSING")).thenReturn(null);

                ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sessionService.getCurrentMovie("MISSING"));

                assertEquals(404, exception.getStatusCode().value());
                assertEquals("Session could not be found.", exception.getReason());
        }

        @Test
        void leaveSession_unknownSession_throwsNotFound() {
                Mockito.when(sessionRepository.findSessionBySessionCode("MISSING")).thenReturn(null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> sessionService.leaveSession("MISSING", "someToken"));

                assertEquals(404, exception.getStatusCode().value());
                assertEquals("Session could not be found.", exception.getReason());
        }

        @Test
        void leaveSession_unknownGuestUser_throwsNotFound() {
                Session session = new Session();
                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
                Mockito.when(guestUserRepository.findByToken("GuestToken")).thenReturn(null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> sessionService.leaveSession("ABCDE", "GuestToken"));

                assertEquals(404, exception.getStatusCode().value());
                assertEquals("Guest user not found", exception.getReason());
        }

        @Test
        void leaveSession_hostLeaves_setsOfflineAndBroadcastsEnd() {
                Session session = new Session();
                session.setSessionId(1L);
                session.setSessionCode("ABCDE");
                session.setHostId(10L);
                session.setStatus(SessionStatus.ONLINE);

                User user = new User();
                user.setId(10L);
                user.setCurrentSession(session);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
                Mockito.when(userRepository.findByToken("HostToken")).thenReturn(user);

                sessionService.leaveSession("ABCDE", "HostToken");

                assertEquals(SessionStatus.OFFLINE, session.getStatus());
                assertNull(user.getCurrentSession());
                verify(sessionRepository).save(session);
                verify(userRepository).save(user);
                verify(messagingTemplate).convertAndSend("/topic/session/ABCDE/end", "ABCDE");
        }

        @Test
        void leaveSession_guestParticipantLeaves_decrementsCountAndBroadcastsLobbyUpdate() {
                Session session = new Session();
                session.setSessionId(1L);
                session.setSessionCode("ABCDE");
                session.setHostId(10L);
                session.setJoinedUsers(3);
                session.setMaxPlayers(10);

                GuestUser guest = new GuestUser();
                guest.setId(22L);
                guest.setCurrentSession(session);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
                Mockito.when(guestUserRepository.findByToken("GuestToken")).thenReturn(guest);
                Mockito.when(userRepository.findAllByCurrentSession(session)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(session)).thenReturn(List.of());

                sessionService.leaveSession("ABCDE", "GuestToken");

                assertEquals(2, session.getJoinedUsers());
                assertNull(guest.getCurrentSession());
                verify(sessionRepository).save(session);
                verify(guestUserRepository).save(guest);
                verify(messagingTemplate).convertAndSend(Mockito.eq("/topic/session/ABCDE/lobby"), Mockito.<Object>any());
        }

        @Test
        void getSessionById_valid_returnsSession() {
            testSession.setSessionId(5L);
            Mockito.when(sessionRepository.findSessionBySessionId(5L)).thenReturn(testSession);

            Session result = sessionService.getSessionById(5L);

            assertEquals(5L, result.getSessionId());
        }

        @Test
        void getSessionById_invalidId_throwsNotFound() {
            Mockito.when(sessionRepository.findSessionBySessionId(1L)).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.getSessionById(1L));

            assertEquals(404, ex.getStatusCode().value());
        }

        @Test
        void joinSession_whenFull_throwsConflict() {
            testSession.setJoinedUsers(5);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);

            SessionPutDTO dto = new SessionPutDTO();
            dto.setToken(token);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.joinSession("ABCDE", dto));

            assertEquals(409, ex.getStatusCode().value());
            assertEquals("Session is already full", ex.getReason());
        }

        @Test
        void joinSession_guest_success_incrementAndBroadcast() {
            testSession.setJoinedUsers(1);

            SessionPutDTO dto = new SessionPutDTO();
            dto.setToken("Guest123");

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(guestUserRepository.findByToken("Guest123")).thenReturn(testGuest);

            sessionService.joinSession("ABCDE", dto);

            assertEquals(2, testSession.getJoinedUsers());
            verify(sessionRepository).save(testSession);
            verify(messagingTemplate).convertAndSend(
                    Mockito.eq("/topic/session/ABCDE/lobby"),
                    Mockito.any(Object.class));
        }

        @Test
        void joinSession_nonExistingId_throwsNotFound() {
            testSession.setJoinedUsers(1);

            SessionPutDTO dto = new SessionPutDTO();
            dto.setToken("token");

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.joinSession("ABCDE", dto));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("User not found", ex.getReason());
        }

        @Test
        void vote_guestNotInSession_throwsForbidden() {
            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("Guest123");

            testGuest.setCurrentSession(null);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(guestUserRepository.findByToken("Guest123")).thenReturn(testGuest);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(403, ex.getStatusCode().value());
            assertEquals("Guest user is not part of the session", ex.getReason());
        }

        @Test
        void vote_existingVote_updatesInsteadOfCreating() {
            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setMovieId(10L);
            dto.setUserId(1L);
            dto.setScore(1);

            Session testSession2 = new Session();
            testSession2.setSessionCode("ABCDE");

            User testUser2 = new User();
            testUser2.setCurrentSession(testSession2);

            Vote existing = new Vote();

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession2);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser2);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 10L))
                    .thenReturn(existing);

            sessionService.setVote(dto);

            assertEquals(1, existing.getScore());
            verify(voteRepository).save(existing);
        }

        /**
         * Commented out for the time being, until the related method is fixed
        @Test
        void vote_allUsersVoted_resetsVoteCount() {
            Session session = new Session();
            session.setSessionId(1L);
            session.setSessionCode("ABCDE");
            session.setSessionMovieIds(List.of(55L, 66L));
            session.setCurrentMovieIndex(0);
            session.setJoinedUsers(2);
            session.setVotesReceivedThisRound(1);

            User user = new User();
            user.setId(1L);
            user.setCurrentSession(session);

            Movie movie = new Movie(
                    55L,
                    "Fight Club",
                    "desc",
                    "/poster.jpg",
                    8.8,
                    "1999-10-15",
                    List.of("Drama"),
                    List.of());

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setMovieId(55L);
            dto.setUserId(1L);
            dto.setScore(1);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
            Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            Mockito.when(userRepository.findByToken("token")).thenReturn(user);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 55L))
                    .thenReturn(null);
            Mockito.when(voteRepository.countBySessionCodeAndMovieId("ABCDE", 55L)).thenReturn(2L);
            Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(movie);

            sessionService.setVote(dto);

            assertEquals(0, session.getVotesReceivedThisRound());
            verify(messagingTemplate, Mockito.atLeastOnce()).convertAndSend(
                    Mockito.eq("/topic/session/ABCDE/vote-progress"),
                    Mockito.any(Object.class));
        }

        @Test
        void vote_allUsersVoted_advancesToNextMovie() {
            Session session = new Session();
            session.setSessionId(1L);
            session.setSessionCode("ABCDE");
            session.setSessionMovieIds(List.of(55L, 66L));
            session.setCurrentMovieIndex(0);
            session.setJoinedUsers(2);
            session.setVotesReceivedThisRound(0);

            User user = new User();
            user.setId(1L);
            user.setCurrentSession(session);

            Movie movie = new Movie(
                    55L,
                    "Fight Club",
                    "desc",
                    "/poster.jpg",
                    8.8,
                    "1999-10-15",
                    List.of("Drama"),
                    List.of());

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setMovieId(55L);
            dto.setUserId(1L);
            dto.setScore(1);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
            Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            Mockito.when(userRepository.findByToken("token")).thenReturn(user);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 55L))
                    .thenReturn(null);
            Mockito.when(voteRepository.countBySessionCodeAndMovieId("ABCDE", 55L)).thenReturn(2L);
            Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(movie);

            sessionService.setVote(dto);

            assertEquals(0, session.getCurrentMovieIndex());

            verify(messagingTemplate).convertAndSend(
                    Mockito.eq("/topic/session/ABCDE/next"),
                    Mockito.any(Object.class));
            verify(tmdbService).getMovieDetails(55L);
        }

         **/

        @Test
        void vote_allUsersVoted_invalidMovieIndex_throwsConflict() {
            Session session = new Session();
            session.setSessionId(1L);
            session.setSessionCode("ABCDE");
            session.setSessionMovieIds(List.of(55L, 66L));
            session.setCurrentMovieIndex(-1);
            session.setJoinedUsers(2);
            session.setVotesReceivedThisRound(0);

            User user = new User();
            user.setId(1L);
            user.setCurrentSession(session);

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setMovieId(55L);
            dto.setUserId(1L);
            dto.setScore(1);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
            Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            Mockito.when(userRepository.findByToken("token")).thenReturn(user);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 55L))
                    .thenReturn(null);
            Mockito.when(voteRepository.countBySessionCodeAndMovieId("ABCDE", 55L)).thenReturn(2L);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(409, exception.getStatusCode().value());
            assertEquals("Invalid movie index", exception.getReason());
            verify(messagingTemplate, Mockito.times(2)).convertAndSend(
                    Mockito.eq("/topic/session/ABCDE/vote-progress"),
                    Mockito.any(Object.class));
        }

        @Test
        void vote_allUsersVoted_noMoviesAssigned_throwsConflict() {
            Session session = new Session();
            session.setSessionId(1L);
            session.setSessionCode("ABCDE");
            session.setSessionMovieIds(List.of());
            session.setCurrentMovieIndex(0);
            session.setJoinedUsers(1);
            session.setVotesReceivedThisRound(0);

            User user = new User();
            user.setId(1L);
            user.setCurrentSession(session);

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setMovieId(55L);
            dto.setUserId(1L);
            dto.setScore(1);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
            Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            Mockito.when(userRepository.findByToken("token")).thenReturn(user);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 55L))
                    .thenReturn(null);
            Mockito.when(voteRepository.countBySessionCodeAndMovieId("ABCDE", 55L)).thenReturn(1L);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(409, exception.getStatusCode().value());
            assertEquals("Session has no movies assigned", exception.getReason());
            verify(messagingTemplate, Mockito.times(2)).convertAndSend(
                    Mockito.eq("/topic/session/ABCDE/vote-progress"),
                    Mockito.any(Object.class));
        }
}
