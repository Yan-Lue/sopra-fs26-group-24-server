package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class SessionCleanupService {

    private final Logger log = LoggerFactory.getLogger(SessionCleanupService.class);
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final GuestUserRepository guestUserRepository;

    public SessionCleanupService(SessionRepository sessionRepository,
                                 UserRepository userRepository,
                                 GuestUserRepository guestUserRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.guestUserRepository = guestUserRepository;
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupSessions() {
        List<Session> expired = sessionRepository.findByStatusAndExpiresAtBefore(SessionStatus.OFFLINE, Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        List<Long> ids = expired.stream()
                .map(Session::getSessionId)
                .toList();

        int usersUnlinked = userRepository.unlinkUsersFromSessions(ids);
        int guestUnlinked = guestUserRepository.unlinkGuestUsersFromSessions(ids);

        log.debug("Unlinked {} Users and Guest Users from sessions", usersUnlinked + guestUnlinked);

        sessionRepository.deleteAll(expired);
        log.debug("Cleaned up {} expired sessions", ids.size());
    }
}
