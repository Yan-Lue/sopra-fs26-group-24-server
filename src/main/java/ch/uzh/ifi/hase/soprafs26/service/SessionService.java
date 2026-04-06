package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionPutDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.SessionFilterPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.model.MovieFilters;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SessionService {

    private static final int DEFAULT_ROUND_LIMIT = 15;

    private final SessionRepository sessionRepository;
    private final TmdbService tmdbService;
    private final GuestUserRepository guestUserRepository;
    private final UserRepository userRepository;

    public SessionService(SessionRepository sessionRepository, TmdbService tmdbService,
            GuestUserRepository guestUserRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.tmdbService = tmdbService;
        this.guestUserRepository = guestUserRepository;
        this.userRepository = userRepository;
    }

    public Session createSession(Session newSession, String userToken) {

        checkValidSessionCreation(newSession);

        newSession.setRoundLimit(DEFAULT_ROUND_LIMIT);
        newSession.setCurrentMovieIndex(0);
        newSession.setSessionMovieIds(List.of());
        newSession.setCreationDate(new java.util.Date());
        newSession.setSessionCode(UUID.randomUUID().toString().substring(0, 5));
        newSession.setStatus(SessionStatus.ONLINE);
        newSession.setSessionToken(UUID.randomUUID().toString());
        newSession.setJoinedUsers(1); // Initialize joined users to 1 since the host is joining

        newSession = sessionRepository.save(newSession);
        sessionRepository.flush();
        // the host user most also have the session linked in the profile
        Long id = newSession.getHostId();

        if (userToken != null && userToken.startsWith("Guest")) {
            GuestUser guestUser = guestUserRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Guest user with ID " + id + " not found in the database"));
            guestUser.setCurrentSession(newSession);
            guestUserRepository.save(guestUser);
            guestUserRepository.flush();
        } else {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "User with ID " + id + " not found in the database"));

            user.setCurrentSession(newSession);
            userRepository.save(user);
            userRepository.flush();

        }

        return newSession;
    }

    public Session getSessionById(Long sessionId) {
        Session session = sessionRepository.findSessionBySessionId(sessionId);

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        return session;
    }

    public Session getSessionByCode(String sessionCode) {
        Session session = sessionRepository.findSessionBySessionCode(sessionCode);

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        return session;
    }

    public Session joinSession(String sessionCode, SessionPutDTO sessionPutDTO) {
        Session session = getSessionByCode(sessionCode);

        // first check if the session is already full
        if (session.getJoinedUsers() >= session.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already full");
        }

        // this should now link the session to the user
        if (sessionPutDTO.getToken().startsWith("Guest")) {
            GuestUser guestUser = guestUserRepository.findByToken(sessionPutDTO.getToken());
            if (guestUser == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest user not found");
            }
            guestUser.setCurrentSession(session);
            guestUserRepository.save(guestUser);
            guestUserRepository.flush();
        } else {
            User user = userRepository.findByToken(sessionPutDTO.getToken());
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            user.setCurrentSession(session);
            userRepository.save(user);
            userRepository.flush();
        }

        return session;
    }

    private void checkValidSessionCreation(Session newSession) {
        String sessionName = newSession.getSessionName();
        Integer maxPlayers = newSession.getMaxPlayers();
        Long hostId = newSession.getHostId();

        if (sessionName == null || maxPlayers == null || hostId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create Session");
        }
    }

    public Movie getNextMovie(String sessionCode) {
        Session session = sessionRepository.findSessionBySessionCode(sessionCode);

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        List<Long> movieIds = session.getSessionMovieIds();
        Integer currentMovieIndex = session.getCurrentMovieIndex();

        if (movieIds == null || movieIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session has no movies assigned");
        }

        if (currentMovieIndex == null || currentMovieIndex < 0 || currentMovieIndex >= movieIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No more movies available in this session");
        }

        Long movieId = movieIds.get(currentMovieIndex);
        Movie movie = tmdbService.getMovieDetails(movieId);

        session.setCurrentMovieIndex(currentMovieIndex + 1);
        sessionRepository.save(session);
        sessionRepository.flush();

        return movie;
    }

    public Session updateSessionFilters(String sessionCode, SessionFilterPutDTO dto) {
        Session session = getSessionByCode(sessionCode);

        int roundLimit = dto.getRoundLimit() == null ? DEFAULT_ROUND_LIMIT : dto.getRoundLimit();

        MovieFilters filters = MovieFilters.fromDTO(dto);
        List<Long> sessionMovieIds = tmdbService.discoverMovieIds(roundLimit, filters);

        session.setRoundLimit(roundLimit);
        session.setCurrentMovieIndex(0);
        session.setSessionMovieIds(sessionMovieIds);

        session = sessionRepository.save(session);
        sessionRepository.flush();

        return session;
    }
}
