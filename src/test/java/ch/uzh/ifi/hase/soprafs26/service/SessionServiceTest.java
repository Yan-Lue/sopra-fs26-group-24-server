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
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionStateGetDTO;
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

import java.time.Instant;
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
        private Movie testMovie;
        private String sessionCode;

        @BeforeEach
        void setup() {
                token = "randomToken";
                testSession = new Session();
                testSession.setSessionName("testSession");
                testSession.setMaxPlayers(5);
                testSession.setHostId(1L);

                testUser = new User();
                testUser.setId(1L);
                testUser.setToken(token);

                testGuest = new GuestUser();
                testGuest.setId(1L);

                sessionCode = "ABCDE";

                testMovie = new Movie(
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
                                                "1995-09-22")),
                                List.of("Netflix", "Amazon Prime"));
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


                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("1")).thenReturn(storedSession);
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(testMovie);
                Mockito.doNothing().when(messagingTemplate).convertAndSend(Mockito.anyString(), Mockito.<Object>any());

                Movie result = sessionService.getNextMovie("1");

                assertEquals(testMovie, result);
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


                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("1")).thenReturn(storedSession);
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(testMovie);
                Mockito.doNothing().when(messagingTemplate).convertAndSend(Mockito.anyString(), Mockito.<Object>any());

                Movie result = sessionService.getNextMovie("1");

                assertEquals(testMovie, result);
                assertEquals(1, storedSession.getCurrentMovieIndex());
                verify(tmdbService, Mockito.times(1)).getMovieDetails(55L);
                verify(sessionRepository, Mockito.times(1)).save(storedSession);
                verify(sessionRepository, Mockito.times(1)).flush();
                verify(messagingTemplate).convertAndSend(
                                Mockito.eq("/topic/session/1/next"),
                                Mockito.any(Object.class));
        }

        @Test
        void getNextMovie_validSession_setsRoundStartedAtAndBroadcastsState() {
                Session storedSession = new Session();
                storedSession.setSessionId(1L);
                storedSession.setSessionCode("1");
                storedSession.setStatus(SessionStatus.ONLINE);
                storedSession.setCurrentMovieIndex(0);
                storedSession.setSessionMovieIds(List.of(55L, 66L));
                storedSession.setTimePerRound(15);
                storedSession.setRoundLimit(2);
                storedSession.setJoinedUsers(3);
                storedSession.setVotesReceivedThisRound(2);

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("1")).thenReturn(storedSession);
                Mockito.when(sessionRepository.findSessionBySessionCode("1")).thenReturn(storedSession);
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(testMovie);
                Mockito.when(userRepository.findAllByCurrentSession(storedSession)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(storedSession)).thenReturn(List.of());
                Mockito.doNothing().when(messagingTemplate).convertAndSend(Mockito.anyString(), Mockito.<Object>any());

                Movie result = sessionService.getNextMovie("1");

                assertEquals(testMovie, result);
                assertEquals(1, storedSession.getCurrentMovieIndex());
                assertEquals(0, storedSession.getVotesReceivedThisRound());
                assertNotNull(storedSession.getRoundStartedAt());
                verify(messagingTemplate).convertAndSend(
                                Mockito.eq("/topic/session/1/state"),
                                Mockito.any(SessionStateGetDTO.class));
        }

        @Test
        void getNextMovie_unknownSession_throwsNotFound() {
                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("999")).thenReturn(null);

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

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("1")).thenReturn(storedSession);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> sessionService.getNextMovie("1"));

                assertEquals(409, exception.getStatusCode().value());
                assertEquals("Session has no movies assigned", exception.getReason());
                Mockito.verifyNoInteractions(tmdbService);
        }

        @Test
        void getSessionState_waitingSession_returnsWaitingWithoutMovie() {
                testSession.setSessionCode("ABCDE");
                testSession.setStatus(SessionStatus.ONLINE);
                testSession.setCurrentMovieIndex(0);
                testSession.setSessionMovieIds(List.of(55L, 66L));
                testSession.setTimePerRound(15);
                testSession.setRoundLimit(2);
                testSession.setJoinedUsers(3);
                testSession.setVotesReceivedThisRound(0);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());

                SessionStateGetDTO state = sessionService.getSessionState("ABCDE");

                assertEquals("ABCDE", state.getSessionCode());
                assertEquals("WAITING", state.getStatus());
                assertEquals(0, state.getCurrentMovieIndex());
                assertNull(state.getCurrentMovie());
                assertEquals(15, state.getTimePerRound());
                assertEquals(2, state.getTotalRounds());
                assertEquals(3, state.getJoinedUsers());
                assertEquals(0, state.getVotesReceived());
                Mockito.verifyNoInteractions(tmdbService);
        }

        @Test
        void getSessionState_playingSession_returnsCurrentMovieAndRoundFields() {
                Instant roundStartedAt = Instant.parse("2026-05-13T12:00:00Z");
                testSession.setSessionCode("ABCDE");
                testSession.setStatus(SessionStatus.ONLINE);
                testSession.setCurrentMovieIndex(2);
                testSession.setSessionMovieIds(List.of(111L, 550L, 333L));
                testSession.setRoundStartedAt(roundStartedAt);
                testSession.setTimePerRound(20);
                testSession.setRoundLimit(3);
                testSession.setJoinedUsers(2);
                testSession.setVotesReceivedThisRound(1);

                User joinedUser = new User();
                joinedUser.setUsername("regularUser");
                GuestUser joinedGuest = new GuestUser();
                joinedGuest.setUsername("guestUser");

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
                Mockito.when(tmdbService.getMovieDetails(550L)).thenReturn(testMovie);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(joinedUser));
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(joinedGuest));

                SessionStateGetDTO state = sessionService.getSessionState("ABCDE");

                assertEquals("PLAYING", state.getStatus());
                assertEquals(2, state.getCurrentMovieIndex());
                assertEquals(roundStartedAt, state.getRoundStartedAt());
                assertEquals(20, state.getTimePerRound());
                assertEquals(3, state.getTotalRounds());
                assertEquals(2, state.getJoinedUsers());
                assertEquals(1, state.getVotesReceived());
                assertEquals(List.of("regularUser", "guestUser"), state.getUsernames());
                assertNotNull(state.getCurrentMovie());
                assertEquals(550L, state.getCurrentMovie().getMovieId());
                assertEquals("Fight Club", state.getCurrentMovie().getTitle());
        }

        @Test
        void getSessionState_offlineBeforeFirstMovie_returnsCanceled() {
                testSession.setSessionCode("ABCDE");
                testSession.setStatus(SessionStatus.OFFLINE);
                testSession.setCurrentMovieIndex(0);
                testSession.setSessionMovieIds(List.of(550L));

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());

                SessionStateGetDTO state = sessionService.getSessionState("ABCDE");

                assertEquals("CANCELED", state.getStatus());
                assertNull(state.getCurrentMovie());
                Mockito.verifyNoInteractions(tmdbService);
        }

        @Test
        void getSessionState_offlineAfterFirstMovie_returnsEnded() {
                testSession.setSessionCode("ABCDE");
                testSession.setStatus(SessionStatus.OFFLINE);
                testSession.setCurrentMovieIndex(1);
                testSession.setSessionMovieIds(List.of(550L));

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());

                SessionStateGetDTO state = sessionService.getSessionState("ABCDE");

                assertEquals("ENDED", state.getStatus());
                assertNull(state.getCurrentMovie());
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
                dto.setMinReleaseYear(2024);
                dto.setMaxReleaseYear(2026);
                dto.setProviders(List.of("Netflix", "Amazon Prime"));

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
                                List.of(new SimilarMovie(111L, "Sim A", "https://img/simA.jpg", 6.8, "2019-01-01")),
                                List.of("Streaming A", "Streaming B")
                        );

                Movie movie2 = new Movie(
                                22L,
                                "Movie B",
                                "Desc B",
                                "https://img/b.jpg",
                                8.4,
                                "2021-01-01",
                                List.of("Action"),
                                List.of(),
                                List.of("Streaming C", "Streaming D")
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
                                11L, "Movie A", "Desc A", "https://img/a.jpg", 7.5, "2020-01-01", List.of("Drama"),
                                List.of(), List.of("Initial Provider"));

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

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(storedSession);
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


                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(storedSession);
                Mockito.when(tmdbService.getMovieDetails(55L)).thenReturn(testMovie);

                Movie result = sessionService.getCurrentMovie("ABCDE");

                assertEquals(testMovie, result);
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
                Mockito.when(userRepository.countByCurrentSession(session)).thenReturn(1L);
                Mockito.when(guestUserRepository.countByCurrentSession(session)).thenReturn(1L);

                sessionService.leaveSession("ABCDE", "GuestToken");

                assertEquals(2, session.getJoinedUsers());
                assertNull(guest.getCurrentSession());
                verify(sessionRepository).save(session);
                verify(guestUserRepository).save(guest);
                verify(messagingTemplate).convertAndSend(Mockito.eq("/topic/session/ABCDE/lobby"),
                                Mockito.<Object>any());
        }

        @Test
        void leaveSession_unknownUser_throwsNotFound() {
                Session session = new Session();
                session.setSessionId(1L);
                session.setSessionCode("ABCDE");

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
                Mockito.when(userRepository.findByToken("UserToken")).thenReturn(null);

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> sessionService.leaveSession("ABCDE", "UserToken"));

                assertEquals(404, exception.getStatusCode().value());
                assertEquals("User not found", exception.getReason());
        }

        @Test
        void leaveSession_userParticipantLeaves_decrementsCountAndBroadcastsLobbyUpdate() {
                Session session = new Session();
                session.setSessionId(1L);
                session.setSessionCode("ABCDE");
                session.setHostId(10L);
                session.setJoinedUsers(3);
                session.setMaxPlayers(10);

                User user = new User();
                user.setId(22L);
                user.setCurrentSession(session);

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
                Mockito.when(userRepository.findByToken("UserToken")).thenReturn(user);
                Mockito.when(userRepository.findAllByCurrentSession(session)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(session)).thenReturn(List.of());
                Mockito.when(userRepository.countByCurrentSession(session)).thenReturn(1L);
                Mockito.when(guestUserRepository.countByCurrentSession(session)).thenReturn(1L);

                sessionService.leaveSession("ABCDE", "UserToken");

                assertEquals(2, session.getJoinedUsers());
                assertNull(user.getCurrentSession());
                verify(sessionRepository).save(session);
                verify(userRepository).save(user);
                verify(messagingTemplate).convertAndSend(Mockito.eq("/topic/session/ABCDE/lobby"),
                                Mockito.<Object>any());
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
                testSession.setMaxPlayers(5);
                testUser.setCurrentSession(null);

                SessionPutDTO dto = new SessionPutDTO();
                dto.setToken(token);

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findByToken(token)).thenReturn(testUser);
                Mockito.when(userRepository.countByCurrentSession(testSession)).thenReturn(5L);
                Mockito.when(guestUserRepository.countByCurrentSession(testSession)).thenReturn(0L);

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

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
                Mockito.when(guestUserRepository.findByToken("Guest123")).thenReturn(testGuest);
                Mockito.when(userRepository.countByCurrentSession(testSession)).thenReturn(1L);
                Mockito.when(guestUserRepository.countByCurrentSession(testSession)).thenReturn(0L, 1L);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(testGuest));

                sessionService.joinSession("ABCDE", dto);

                assertEquals(2, testSession.getJoinedUsers());
                verify(sessionRepository, Mockito.atLeastOnce()).save(testSession);
                verify(messagingTemplate).convertAndSend(
                                Mockito.eq("/topic/session/ABCDE/lobby"),
                                Mockito.any(Object.class));
        }

        @Test
        void joinSession_nonExistingId_throwsNotFound() {
                testSession.setJoinedUsers(1);

                SessionPutDTO dto = new SessionPutDTO();
                dto.setToken("token");

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findByToken("token")).thenReturn(null);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> sessionService.joinSession("ABCDE", dto));

                assertEquals(404, ex.getStatusCode().value());
                assertEquals("User not found", ex.getReason());
        }

        @Test
        void joinSession_unknownSession_throwsNotFound() {
                SessionPutDTO dto = new SessionPutDTO();
                dto.setToken(token);

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("MISSING")).thenReturn(null);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> sessionService.joinSession("MISSING", dto));

                assertEquals(404, ex.getStatusCode().value());
                assertEquals("Session could not be found.", ex.getReason());
        }

        @Test
        void joinSession_userAlreadyInSession_doesNotSaveUserAgain() {
                testSession.setSessionId(1L);
                testSession.setSessionCode("ABCDE");
                testSession.setJoinedUsers(1);
                testSession.setMaxPlayers(5);
                testSession.setCurrentMovieIndex(0);

                testUser.setCurrentSession(testSession);

                SessionPutDTO dto = new SessionPutDTO();
                dto.setToken(token);

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findByToken(token)).thenReturn(testUser);
                Mockito.when(userRepository.countByCurrentSession(testSession)).thenReturn(1L);
                Mockito.when(guestUserRepository.countByCurrentSession(testSession)).thenReturn(0L);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(testUser));
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());

                Session result = sessionService.joinSession("ABCDE", dto);

                assertEquals(testSession, result);
                assertEquals(1, testSession.getJoinedUsers());
                Mockito.verify(userRepository, Mockito.never()).save(testUser);
                verify(messagingTemplate).convertAndSend(
                                Mockito.eq("/topic/session/ABCDE/lobby"),
                                Mockito.any(Object.class));
        }

        @Test
        void joinSession_lateJoinCurrentMovieSendFails_stillReturnsSession() {
                testSession.setSessionId(1L);
                testSession.setSessionCode("ABCDE");
                testSession.setJoinedUsers(1);
                testSession.setMaxPlayers(5);
                testSession.setCurrentMovieIndex(1);
                testSession.setSessionMovieIds(List.of(550L));

                testUser.setCurrentSession(null);

                SessionPutDTO dto = new SessionPutDTO();
                dto.setId(1L);
                dto.setToken(token);

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
                Mockito.when(userRepository.findByToken(token)).thenReturn(testUser);
                Mockito.when(userRepository.countByCurrentSession(testSession)).thenReturn(1L, 2L);
                Mockito.when(guestUserRepository.countByCurrentSession(testSession)).thenReturn(0L);
                Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(testUser));
                Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
                Mockito.when(tmdbService.getMovieDetails(550L)).thenThrow(new RuntimeException("tmdb failed"));

                Session result = sessionService.joinSession("ABCDE", dto);

                assertEquals(testSession, result);
                assertEquals(testSession, testUser.getCurrentSession());
                assertEquals(2, testSession.getJoinedUsers());
                verify(userRepository).save(testUser);
                verify(messagingTemplate, Mockito.never()).convertAndSendToUser(
                                Mockito.anyString(),
                                Mockito.anyString(),
                                Mockito.any());
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
                testSession2.setStatus(SessionStatus.ONLINE);
                testSession2.setCurrentMovieIndex(1);
                testSession2.setSessionMovieIds(List.of(10L));

                User testUser2 = new User();
                testUser2.setCurrentSession(testSession2);

                Vote existingVote = new Vote();

                Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession2);
                Mockito.when(userRepository.findByToken("token")).thenReturn(testUser2);
                Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId(
                        "ABCDE",
                                1L,
                                10L))
                        .thenReturn(existingVote);

                sessionService.setVote(dto);
                assertEquals(1, existingVote.getScore());
                verify(voteRepository).save(existingVote);
        }

        @Test
        void forceNextMovie_validHost_returnsNextMovie() {
                testSession.setSessionMovieIds(List.of(550L));
                testSession.setCurrentMovieIndex(0);
                testSession.setVotesReceivedThisRound(0);
                testSession.setJoinedUsers(null);

                testUser.setId(1L);

                Mockito.when(sessionRepository.findSessionBySessionCode(sessionCode))
                                .thenReturn(testSession);
                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate(sessionCode))
                                .thenReturn(testSession);
                Mockito.when(userRepository.findByToken(token))
                                .thenReturn(testUser);
                Mockito.when(tmdbService.getMovieDetails(550L))
                                .thenReturn(testMovie);
                Mockito.doNothing().when(messagingTemplate)
                                .convertAndSend(Mockito.anyString(), Mockito.<Object>any());
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(i -> i.getArgument(0));

                Movie result = sessionService.forceNextMovie(sessionCode, token);

                assertEquals(testMovie, result);
        }

        @Test
        void advanceToNextMovie_validHost_returnsNextMovieFromLockedSession() {
                testSession.setSessionCode(sessionCode);
                testSession.setStatus(SessionStatus.ONLINE);
                testSession.setSessionMovieIds(List.of(550L));
                testSession.setCurrentMovieIndex(0);
                testSession.setVotesReceivedThisRound(1);
                testSession.setJoinedUsers(2);

                testUser.setId(1L);

                Mockito.when(sessionRepository.findSessionBySessionCode(sessionCode))
                                .thenReturn(testSession);
                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate(sessionCode))
                                .thenReturn(testSession);
                Mockito.when(userRepository.findByToken(token))
                                .thenReturn(testUser);
                Mockito.when(tmdbService.getMovieDetails(550L))
                                .thenReturn(testMovie);
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(i -> i.getArgument(0));
                Mockito.doNothing().when(messagingTemplate)
                                .convertAndSend(Mockito.anyString(), Mockito.<Object>any());

                Movie result = sessionService.advanceToNextMovie(sessionCode, token);

                assertEquals(testMovie, result);
                assertEquals(1, testSession.getCurrentMovieIndex());
                assertEquals(0, testSession.getVotesReceivedThisRound());
                assertNotNull(testSession.getRoundStartedAt());
                verify(sessionRepository).findSessionBySessionCodeForUpdate(sessionCode);
        }

        @Test
        void advanceToNextMovie_nonHost_throwsForbidden() {
                testSession.setHostId(99L);
                testUser.setId(1L);

                Mockito.when(sessionRepository.findSessionBySessionCode(sessionCode)).thenReturn(testSession);
                Mockito.when(userRepository.findByToken(token)).thenReturn(testUser);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> sessionService.advanceToNextMovie(sessionCode, token));

                assertEquals(403, ex.getStatusCode().value());
                assertEquals("Only the host can advance the session", ex.getReason());
                Mockito.verify(sessionRepository, Mockito.never()).findSessionBySessionCodeForUpdate(Mockito.anyString());
        }

        @Test
        void getNextMovie_invalidMovieIndex_throwsConflict() {
                Session session = new Session();
                session.setSessionId(1L);
                session.setSessionCode("ABCDE");
                session.setSessionMovieIds(List.of(55L, 66L));
                session.setCurrentMovieIndex(-1);
                session.setJoinedUsers(2);

                Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(session);
                Mockito.when(sessionRepository.save(Mockito.any(Session.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResponseStatusException exception = assertThrows(
                                ResponseStatusException.class,
                                () -> sessionService.getNextMovie("ABCDE"));

                assertEquals(409, exception.getStatusCode().value());
                assertEquals("No more movies available in this session", exception.getReason());
                verify(messagingTemplate, Mockito.times(1)).convertAndSend(
                                Mockito.eq("/topic/session/ABCDE/end"),
                                Mockito.any(Object.class));
        }

        @Test
        void joinSession_registeredUserDuringStartedSession_savesUserAndSendsCurrentMovie() {
            testSession.setSessionCode("ABCDE");
            testSession.setJoinedUsers(1);
            testSession.setMaxPlayers(5);
            testSession.setCurrentMovieIndex(1);
            testSession.setSessionMovieIds(List.of(550L));

            SessionPutDTO dto = new SessionPutDTO();
            dto.setId(1L);
            dto.setToken("token");

            Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser);
            Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
            Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of());
            Mockito.when(tmdbService.getMovieDetails(550L)).thenReturn(testMovie);
            Mockito.when(userRepository.countByCurrentSession(testSession)).thenReturn(1L, 2L);
            Mockito.when(guestUserRepository.countByCurrentSession(testSession)).thenReturn(0L);

            sessionService.joinSession("ABCDE", dto);

            assertEquals(testSession, testUser.getCurrentSession());
            assertEquals(2, testSession.getJoinedUsers());
            verify(userRepository).save(testUser);
            verify(userRepository).flush();
            verify(messagingTemplate).convertAndSendToUser(
                    Mockito.eq("1"),
                    Mockito.eq("/queue/current-movie"),
                    Mockito.any());
        }

        @Test
        void joinSession_guestUserNotFound_throwsNotFound() {
            testSession.setJoinedUsers(1);
            testSession.setMaxPlayers(5);

            SessionPutDTO dto = new SessionPutDTO();
            dto.setToken("GuestMissing");

            Mockito.when(sessionRepository.findSessionBySessionCodeForUpdate("ABCDE")).thenReturn(testSession);
            Mockito.when(guestUserRepository.findByToken("GuestMissing")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.joinSession("ABCDE", dto));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("Guest user not found", ex.getReason());
        }

        @Test
        void setVote_unknownSession_throwsNotFound() {
            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("MISSING");

            Mockito.when(sessionRepository.findSessionBySessionCode("MISSING")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("Session could not be found.", ex.getReason());
        }

        @Test
        void setVote_registeredUserNotInSession_throwsForbidden() {
            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");

            User user = new User();
            user.setCurrentSession(null);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(user);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(403, ex.getStatusCode().value());
            assertEquals("User is not part of the session", ex.getReason());
        }

        @Test
        void setVote_newVote_createsVoteAndUpdatesVoteProgress() {
            testSession.setSessionCode("ABCDE");
            testSession.setJoinedUsers(2);
            testSession.setStatus(SessionStatus.ONLINE);
            testSession.setCurrentMovieIndex(1);
            testSession.setSessionMovieIds(List.of(10L));

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setUserId(1L);
            dto.setMovieId(10L);
            dto.setScore(1);

            testUser.setCurrentSession(testSession);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 10L)).thenReturn(null);
            Mockito.when(voteRepository.countBySessionCodeAndMovieId("ABCDE", 10L)).thenReturn(1L);

            sessionService.setVote(dto);

            assertEquals(1, testSession.getVotesReceivedThisRound());
            verify(voteRepository).save(Mockito.any(Vote.class));
            verify(messagingTemplate).convertAndSend(
                    Mockito.eq("/topic/session/ABCDE/vote-progress"),
                    Mockito.any(Object.class));
        }

        @Test
        void setVote_staleMovieForPreviousRound_throwsConflictWithoutSavingVote() {
            testSession.setSessionCode("ABCDE");
            testSession.setStatus(SessionStatus.ONLINE);
            testSession.setCurrentMovieIndex(2);
            testSession.setSessionMovieIds(List.of(10L, 20L));

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setUserId(1L);
            dto.setMovieId(10L);
            dto.setScore(1);

            testUser.setCurrentSession(testSession);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(409, ex.getStatusCode().value());
            assertEquals("Vote is stale for the current round", ex.getReason());
            Mockito.verifyNoInteractions(voteRepository);
            Mockito.verify(sessionRepository, Mockito.never()).save(Mockito.any(Session.class));
        }

        @Test
        void setVote_beforeSessionStarted_throwsConflictWithoutSavingVote() {
            testSession.setSessionCode("ABCDE");
            testSession.setStatus(SessionStatus.ONLINE);
            testSession.setCurrentMovieIndex(0);
            testSession.setSessionMovieIds(List.of(10L));

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setUserId(1L);
            dto.setMovieId(10L);
            dto.setScore(1);

            testUser.setCurrentSession(testSession);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(409, ex.getStatusCode().value());
            assertEquals("Session has not started yet", ex.getReason());
            Mockito.verifyNoInteractions(voteRepository);
        }

        @Test
        void setVote_offlineSession_throwsConflictWithoutSavingVote() {
            testSession.setSessionCode("ABCDE");
            testSession.setStatus(SessionStatus.OFFLINE);
            testSession.setCurrentMovieIndex(1);
            testSession.setSessionMovieIds(List.of(10L));

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setUserId(1L);
            dto.setMovieId(10L);
            dto.setScore(1);

            testUser.setCurrentSession(testSession);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.setVote(dto));

            assertEquals(409, ex.getStatusCode().value());
            assertEquals("Session has ended", ex.getReason());
            Mockito.verifyNoInteractions(voteRepository);
        }

        @Test
        void setVote_lastVoteAfterFinalMovie_setsSessionOfflineAndBroadcastsEnd() {
            testSession.setSessionCode("ABCDE");
            testSession.setJoinedUsers(1);
            testSession.setCurrentMovieIndex(1);
            testSession.setSessionMovieIds(List.of(10L));
            testSession.setStatus(SessionStatus.ONLINE);

            VotePutDTO dto = new VotePutDTO();
            dto.setSessionCode("ABCDE");
            dto.setToken("token");
            dto.setUserId(1L);
            dto.setMovieId(10L);
            dto.setScore(1);

            testUser.setCurrentSession(testSession);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken("token")).thenReturn(testUser);
            Mockito.when(voteRepository.findBySessionCodeAndUserIdAndMovieId("ABCDE", 1L, 10L)).thenReturn(null);
            Mockito.when(voteRepository.countBySessionCodeAndMovieId("ABCDE", 10L)).thenReturn(1L);

            sessionService.setVote(dto);

            assertEquals(SessionStatus.OFFLINE, testSession.getStatus());
            verify(messagingTemplate).convertAndSend("/topic/session/ABCDE/end", "ABCDE");
        }

        @Test
        void forceNextMovie_unknownSession_throwsNotFound() {
            Mockito.when(sessionRepository.findSessionBySessionCode("MISSING")).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.forceNextMovie("MISSING", token));

            assertEquals(404, ex.getStatusCode().value());
            assertEquals("Session not found", ex.getReason());
        }

        @Test
        void forceNextMovie_nonHost_throwsForbidden() {
            testSession.setHostId(99L);
            testUser.setId(1L);

            Mockito.when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(testSession);
            Mockito.when(userRepository.findByToken(token)).thenReturn(testUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> sessionService.forceNextMovie("ABCDE", token));

            assertEquals(403, ex.getStatusCode().value());
            assertEquals("Only the host can force the next movie", ex.getReason());
        }

        @Test
        void getJoinedUsernames_withUsersAndGuests_returnsAllNames() {
            User user = new User();
            user.setUsername("regularUser");

            GuestUser guest = new GuestUser();
            guest.setUsername("guestUser");

            Mockito.when(userRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(user));
            Mockito.when(guestUserRepository.findAllByCurrentSession(testSession)).thenReturn(List.of(guest));

            List<String> usernames = sessionService.getJoinedUsernames(testSession);

            assertEquals(List.of("regularUser", "guestUser"), usernames);
        }
}
