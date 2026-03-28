package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SessionService {

    private static final int DEFAULT_ROUND_LIMIT = 15;

    private final SessionRepository sessionRepository;
    private final TmdbService tmdbService;

    public SessionService(SessionRepository sessionRepository, TmdbService tmdbService) {
        this.sessionRepository = sessionRepository;
        this.tmdbService = tmdbService;
    }

    public Session createSession(Session newSession) {

        checkValidSessionCreation(newSession);

        int roundLimit = newSession.getRoundLimit() == null ? DEFAULT_ROUND_LIMIT : newSession.getRoundLimit();

        List<Long> sessionMovieIds = tmdbService.discoverMovieIds(roundLimit);

        newSession.setRoundLimit(roundLimit);
        newSession.setCurrentMovieIndex(0);
        newSession.setSessionMovieIds(sessionMovieIds);
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

        if (sessionName == null || maxPlayers == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create Session");
        }
    }

    public Movie getNextMovie(Long sessionId) {
        Session session = sessionRepository.findSessionBysessionId(sessionId);

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        List<Long> movieIds = session.getSessionMovieIds();
        Integer currentMovieIndex = session.getCurrentMovieIndex();

        if (movieIds == null || movieIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session has no movies assigned");
        }

        Long movieId = movieIds.get(currentMovieIndex);
        Movie movie = tmdbService.getMovieDetails(movieId);

        session.setCurrentMovieIndex(currentMovieIndex + 1);
        sessionRepository.save(session);
        sessionRepository.flush();

        return movie;
    }
}
