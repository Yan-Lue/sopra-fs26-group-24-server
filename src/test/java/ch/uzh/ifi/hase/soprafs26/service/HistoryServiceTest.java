package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.HistoryRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryPostDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private HistoryRepository historyRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private HistoryService historyService;

    private Session session;
    private User user;
    private HistoryPostDTO dto;

    @BeforeEach
    void setup() {
        session = new Session();
        session.setSessionCode("ABCDE");
        session.setSessionName("Test Round");
        session.setJoinedUsers(3);
        session.setCreationDate(new Date());
        session.setSessionMovieIds(List.of(10L, 20L));

        user = new User();
        user.setId(7L);
        user.setToken("test-token");
        user.setCurrentSession(session);

        dto = new HistoryPostDTO();
        dto.setSessionCode("ABCDE");
        dto.setToken("test-token");
    }

    @Test
    void saveHistory_validRegisteredUser_savesHistoryWithMovieScores() {
        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
        when(userRepository.findByToken("test-token")).thenReturn(user);
        when(historyRepository.findBySessionCodeAndUserId("ABCDE", 7L)).thenReturn(null);
        when(voteRepository.getSumOfScores("ABCDE", 10L)).thenReturn(4);
        when(voteRepository.getSumOfScores("ABCDE", 20L)).thenReturn(null);

        historyService.saveHistory(dto);

        ArgumentCaptor<History> captor = ArgumentCaptor.forClass(History.class);
        verify(historyRepository).save(captor.capture());
        verify(historyRepository).flush();

        History saved = captor.getValue();
        assertEquals("Test Round", saved.getSessionName());
        assertEquals("ABCDE", saved.getSessionCode());
        assertEquals(3, saved.getJoinedUsers());
        assertEquals(7L, saved.getUserId());
        assertEquals(2, saved.getMovies().size());
        assertEquals(10L, saved.getMovies().get(0).getMovieId());
        assertEquals(4, saved.getMovies().get(0).getScore());
        assertEquals(20L, saved.getMovies().get(1).getMovieId());
        assertEquals(0, saved.getMovies().get(1).getScore());
    }

    @Test
    void saveHistory_unknownSession_throwsNotFound() {
        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historyService.saveHistory(dto));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Session could not be found.", ex.getReason());
        verifyNoInteractions(userRepository, voteRepository, historyRepository);
    }

    @Test
    void saveHistory_missingToken_throwsBadRequest() {
        dto.setToken(null);
        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historyService.saveHistory(dto));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Token is required.", ex.getReason());
    }

    @Test
    void saveHistory_guestToken_throwsForbidden() {
        dto.setToken("Guest-test");
        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historyService.saveHistory(dto));

        assertEquals(403, ex.getStatusCode().value());
        assertEquals("Please register as User to save history.", ex.getReason());
    }

    @Test
    void saveHistory_unknownUser_throwsNotFound() {
        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
        when(userRepository.findByToken("test-token")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historyService.saveHistory(dto));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("User not found.", ex.getReason());
    }

    @Test
    void saveHistory_userNotInSession_throwsForbidden() {
        Session otherSession = new Session();
        otherSession.setSessionCode("OTHER");
        user.setCurrentSession(otherSession);

        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
        when(userRepository.findByToken("test-token")).thenReturn(user);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historyService.saveHistory(dto));

        assertEquals(403, ex.getStatusCode().value());
        assertEquals("User is not part of this session.", ex.getReason());
    }

    @Test
    void saveHistory_duplicateForSameUserAndSession_throwsConflict() {
        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
        when(userRepository.findByToken("test-token")).thenReturn(user);
        when(historyRepository.findBySessionCodeAndUserId("ABCDE", 7L)).thenReturn(new History());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historyService.saveHistory(dto));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("History already saved.", ex.getReason());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void saveHistory_sameSessionDifferentUser_isAllowed() {
        User otherUser = new User();
        otherUser.setId(8L);
        otherUser.setToken("test-token2");
        otherUser.setCurrentSession(session);

        dto.setToken("test-token2");

        when(sessionRepository.findSessionBySessionCode("ABCDE")).thenReturn(session);
        when(userRepository.findByToken("test-token2")).thenReturn(otherUser);
        when(historyRepository.findBySessionCodeAndUserId("ABCDE", 8L)).thenReturn(null);
        when(voteRepository.getSumOfScores("ABCDE", 10L)).thenReturn(4);
        when(voteRepository.getSumOfScores("ABCDE", 20L)).thenReturn(null);

        historyService.saveHistory(dto);

        verify(historyRepository).save(any(History.class));
        verify(historyRepository).flush();
    }

    @Test
    void getHistoryByHistoryId_validId_returnsHistory() {

        History history = new History();
        history.setSessionCode("ABCDE");
        history.setHistoryId(1L);

        when(historyRepository.findByHistoryId(1L)).thenReturn(history);

        History result = historyService.getHistoryByHistoryId(1L);

        assertEquals(1L, result.getHistoryId());
        assertEquals("ABCDE", result.getSessionCode());

        verify(historyRepository, times(1)).findByHistoryId(1L);
    }

    @Test
    void getHistoryByHistoryId_invalidId_throwsNotFound() {
        when(historyRepository.findByHistoryId(1L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> historyService.getHistoryByHistoryId(1L));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("History not found.", ex.getReason());

        verify(historyRepository, times(1)).findByHistoryId(1L);
    }
}
