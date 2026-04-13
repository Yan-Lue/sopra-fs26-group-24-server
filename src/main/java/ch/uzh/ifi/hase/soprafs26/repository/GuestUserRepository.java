package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.GuestUser; // Correct the case if needed
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository("guestUserRepository")
public interface GuestUserRepository extends JpaRepository<GuestUser, Long> {

    GuestUser findByToken(String token);

    GuestUser findByUsername(String name);

    long deleteByExpiresAtBefore(Instant now);

}