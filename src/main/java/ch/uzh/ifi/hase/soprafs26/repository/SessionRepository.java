package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository("sessionRepository")
public interface SessionRepository extends JpaRepository<Session, Long> {

    Session findSessionBySessionId(Long sessionId);

    Session findSessionBySessionCode(String sessionCode);

    List<Session> findByExpiresAtBefore(Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Session s
        WHERE s.sessionCode = :sessionCode
        """)
    Session findSessionBySessionCodeForUpdate(@Param("sessionCode") String sessionCode);
}
