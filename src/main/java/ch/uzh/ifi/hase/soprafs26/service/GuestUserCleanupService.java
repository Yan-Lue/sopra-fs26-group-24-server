package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Transactional
public class GuestUserCleanupService {

    private final Logger log = LoggerFactory.getLogger(GuestUserCleanupService.class);
    private final GuestUserRepository guestUserRepository;

    public GuestUserCleanupService(GuestUserRepository guestUserRepository) {
        this.guestUserRepository = guestUserRepository;
    }

    @Scheduled(fixedRate = 300000)
    public void deleteExpiredGuests() {
        long deleted = guestUserRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.debug("Deleted {} expired guest users", deleted);
        }
    }
}
