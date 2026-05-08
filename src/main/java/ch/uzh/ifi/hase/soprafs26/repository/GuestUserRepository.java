package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.GuestUser; // Correct the case if needed
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import ch.uzh.ifi.hase.soprafs26.entity.Session;

@Repository("guestUserRepository")
public interface GuestUserRepository extends JpaRepository<GuestUser, Long> {

    GuestUser findByToken(String token);

    GuestUser findByUsername(String name);

    long deleteByExpiresAtBefore(Instant now);
    
    List<GuestUser> findAllByCurrentSession(Session currentSession);

    @Modifying
    @Query("""
    UPDATE GuestUser g
    SET g.currentSession = NULL
    WHERE g.currentSession.sessionId IN :sessionIds
    """)
    int unlinkGuestUsersFromSessions(@Param("sessionIds") List<Long> sessionIds);

}