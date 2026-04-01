package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("sessionRepository")
public interface SessionRepository extends JpaRepository<Session, Long> {

    Session findSessionBySessionId(Long sessionId);

    Session findSessionBySessionCode(String sessionCode);

}
