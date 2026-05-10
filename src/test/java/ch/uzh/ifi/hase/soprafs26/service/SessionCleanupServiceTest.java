package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class SessionCleanupServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestUserRepository guestUserRepository;

    private SessionCleanupService sessionCleanupService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        sessionCleanupService = new SessionCleanupService(
                sessionRepository,
                userRepository,
                guestUserRepository
        );
    }

    @Test
    void cleanupSessions_whenNoExpiredSessions_doesNothing() {
        Mockito.when(sessionRepository.findByStatusAndExpiresAtBefore(eq(SessionStatus.OFFLINE), any(Instant.class)))
                .thenReturn(List.of());

        sessionCleanupService.cleanupSessions();

        Mockito.verify(userRepository, never()).unlinkUsersFromSessions(any());
        Mockito.verify(guestUserRepository, never()).unlinkGuestUsersFromSessions(any());
        Mockito.verify(sessionRepository, never()).deleteAll(any());
    }

    @Test
    void cleanupSessions_whenExpiredSessionsExist_unlinksUsersAndDeletesSessions() {
        Session session1 = new Session();
        session1.setSessionId(1L);

        Session session2 = new Session();
        session2.setSessionId(2L);

        List<Session> expiredSessions = List.of(session1, session2);
        List<Long> sessionIds = List.of(1L, 2L);

        Mockito.when(sessionRepository.findByStatusAndExpiresAtBefore(eq(SessionStatus.OFFLINE), any(Instant.class)))
                .thenReturn(expiredSessions);
        Mockito.when(userRepository.unlinkUsersFromSessions(sessionIds)).thenReturn(2);
        Mockito.when(guestUserRepository.unlinkGuestUsersFromSessions(sessionIds)).thenReturn(1);

        sessionCleanupService.cleanupSessions();

        Mockito.verify(userRepository, times(1)).unlinkUsersFromSessions(sessionIds);
        Mockito.verify(guestUserRepository, times(1)).unlinkGuestUsersFromSessions(sessionIds);
        Mockito.verify(sessionRepository, times(1)).deleteAll(expiredSessions);
    }
}