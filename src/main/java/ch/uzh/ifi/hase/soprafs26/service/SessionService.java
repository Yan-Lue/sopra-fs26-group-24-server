package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import jakarta.transaction.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Session createSession(Session newSession) {

        checkValidSessionCreation(newSession);

        newSession.setCreationDate(new java.util.Date());
        newSession.setSessionCode(UUID.randomUUID().toString().substring(0, 5));
        newSession.setStatus(SessionStatus.ONLINE);
        newSession.setSessionToken(UUID.randomUUID().toString());

        newSession = sessionRepository.save(newSession);
        sessionRepository.flush();

        return newSession;
    }

    private void checkValidSessionCreation(Session newSession) {
        String sessionName = newSession.getSessionName();
        Integer maxPlayers = newSession.getMaxPlayers();

        System.out.println("Session Name: " + sessionName);
        System.out.println("Max Players: " + maxPlayers);

        if (sessionName == null || maxPlayers == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create Session");
        }
    }

    public Movie getNextMovie(Long sessionId) {
        Session session = sessionRepository.findSessionById(sessionId);
        Integer roundLimit = session.getRoundLimit();
        //DB Request from collection session_session_movie_id, parse through them (with global index maybe?)
        //return the current movie with all data to display in frontend
        Movie movie = new Movie();
        return movie;
    }
}
