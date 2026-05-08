package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Session;

import java.time.Instant;
import java.util.List;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {
	User findByName(String name);

	User findByUsername(String username);

	User findByEmail(String email);

	User findByToken(String token);

	List<User> findAllByCurrentSession(Session currentSession);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE User u
        SET u.token = NULL,
            u.status = :offlineStatus,
            u.expiresAt = NULL
        WHERE u.expiresAt IS NOT NULL
            AND u.expiresAt < :now
            AND u.status <> :offlineStatus
    """)
    int expireUsers(
            @Param("now") Instant now,
            @Param("offlineStatus") UserStatus offlineStatus
    );

    @Modifying
    @Query("""
        UPDATE User u
        SET u.currentSession = NULL
        WHERE u.currentSession.sessionId IN: sessionIds
    """)
    int unlinkUsersFromSessions(@Param("sessionIds") List<Long> sessionIds);
}
