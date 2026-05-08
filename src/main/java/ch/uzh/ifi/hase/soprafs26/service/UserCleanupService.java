package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Transactional
public class UserCleanupService {

    private final Logger log = LoggerFactory.getLogger(UserCleanupService.class);
    private final UserRepository userRepository;

    public UserCleanupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 300000)
    public void setExpiredUsers() {
        long updated = userRepository.setByExpiresAtBefore(Instant.now());
        if (updated > 0) {
            log.debug("Updated {} expired users", updated);
        }
    }
}
