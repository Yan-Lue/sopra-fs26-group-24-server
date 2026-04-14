package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.service.model.Movie;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GuestUser;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.SessionRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import ch.uzh.ifi.hase.soprafs26.service.model.MovieFilters;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Service
@Transactional
public class SessionService {

    private static final int DEFAULT_ROUND_LIMIT = 15;
    private static final int DEFAULT_TIME_PER_ROUND = 15;

    private final SessionRepository sessionRepository;
    private final TmdbService tmdbService;
    private final GuestUserRepository guestUserRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SessionService(SessionRepository sessionRepository, TmdbService tmdbService,
            GuestUserRepository guestUserRepository, UserRepository userRepository, VoteRepository voteRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.tmdbService = tmdbService;
        this.guestUserRepository = guestUserRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Session createSession(Session newSession, String userToken) {

        checkValidSessionCreation(newSession, userToken);

        GuestUser guestHost = null;
        User userHost = null;

        if (userToken != null && !userToken.isBlank()) {
            if (userToken.startsWith("Guest")) {
                guestHost = guestUserRepository.findByToken(userToken);
                if (guestHost == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest user not found");
                }
                newSession.setHostId(guestHost.getId());
            } else {
                userHost = userRepository.findByToken(userToken);
                if (userHost == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
                }
                newSession.setHostId(userHost.getId());
            }
        } else {
            Long id = newSession.getHostId();

            guestHost = guestUserRepository.findById(id).orElse(null);
            if (guestHost == null) {
                userHost = userRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Host user not found"));
            }
        }

        newSession.setRoundLimit(DEFAULT_ROUND_LIMIT);
        newSession.setCurrentMovieIndex(0);
        newSession.setSessionMovieIds(List.of());
        newSession.setCreationDate(new java.util.Date());
        newSession.setSessionCode(UUID.randomUUID().toString().substring(0, 5));
        newSession.setStatus(SessionStatus.ONLINE);
        newSession.setTimePerRound(DEFAULT_TIME_PER_ROUND);
        newSession.setSessionToken(UUID.randomUUID().toString());
        newSession.setJoinedUsers(1); // Initialize joined users to 1 since the host is joining

        newSession = sessionRepository.save(newSession);
        sessionRepository.flush();

        if (guestHost != null) {
            guestHost.setCurrentSession(newSession);
            guestUserRepository.save(guestHost);
            guestUserRepository.flush();
        } else {
            userHost.setCurrentSession(newSession);
            userRepository.save(userHost);
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

        session.setJoinedUsers(session.getJoinedUsers() + 1);
        sessionRepository.save(session);
        sessionRepository.flush();

        // I suggest that we do the regular PUT fetch to join and the join handling
        // inside the database is done
        // Then when the frontend receives the OK, it subscribes to the topic and waits
        // for the next movie to be pushed, which happens in the line below when the
        // host clicks "Start Session"

        // The frontend needs to first connect to the websocket enpoint:
        // ws://<our-server>/gs-guide-websocket
        // Then it must subscribe to the topic: /topic/session/{sessionCode}/lobby and
        // /topic/session/{sessionCode}/next to receive the current number of players
        // who have joined and the movie details when the host
        // starts the session and every time they click "Next"

        Map<String, Object> lobbyUpdate = new HashMap<>();
        lobbyUpdate.put("joinedUsers", session.getJoinedUsers());
        lobbyUpdate.put("maxPlayers", session.getMaxPlayers());
        lobbyUpdate.put("usernames", getJoinedUsernames(session));

        // updated number of users who have already joined the session
        messagingTemplate.convertAndSend((String) ("/topic/session/" + sessionCode + "/lobby"), (Object) lobbyUpdate);

        return session;
    }

    private void checkValidSessionCreation(Session newSession, String userToken) {
        String sessionName = newSession.getSessionName();
        Integer maxPlayers = newSession.getMaxPlayers();
        Long hostId = newSession.getHostId();

        if (sessionName == null || sessionName.trim().isEmpty() || maxPlayers == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create Session");
        }

        if ((userToken == null || userToken.isBlank()) && hostId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please try logging in again");
        }
    }

    private void broadcastSessionEnded(String sessionCode) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionCode + "/end", sessionCode);
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
            session.setStatus(SessionStatus.OFFLINE);
            sessionRepository.save(session);
            sessionRepository.flush();

            broadcastSessionEnded(sessionCode);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No more movies available in this session");
        }

        Long movieId = movieIds.get(currentMovieIndex);
        Movie movie = tmdbService.getMovieDetails(movieId);

        session.setCurrentMovieIndex(currentMovieIndex + 1);
        sessionRepository.save(session);
        sessionRepository.flush();

        // This sends the movie object to everyone subscribed to that session's topic

        // IMPORTANT: the frontend needs to subscribe to the topic
        // "/topic/session/{sessionCode}/next" to receive the movie details when this
        messagingTemplate.convertAndSend("/topic/session/" + sessionCode + "/next", movie);

        return movie;
    }

    // this method is needed for correct redirection because of timing issues with the websocket
    public Movie getCurrentMovie(String sessionCode) {
        Session session = sessionRepository.findSessionBySessionCode(sessionCode);

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        List<Long> movieIds = session.getSessionMovieIds();
        Integer currentMovieIndex = session.getCurrentMovieIndex();

        // Session not started yet (host has not called /next once)
        if (movieIds == null || movieIds.isEmpty() || currentMovieIndex == null || currentMovieIndex <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session has not started yet");
        }

        if (session.getStatus() == SessionStatus.OFFLINE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session has ended");
        }

        int currentIndex = currentMovieIndex - 1;

        if (currentIndex < 0 || currentIndex >= movieIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No current movie available");
        }

        Long movieId = movieIds.get(currentIndex);
        return tmdbService.getMovieDetails(movieId);
    }

    public Session updateSessionFilters(String sessionCode, SessionFilterPutDTO dto) {
        Session session = getSessionByCode(sessionCode);

        int roundLimit = dto.getRoundLimit() == null ? DEFAULT_ROUND_LIMIT : dto.getRoundLimit();
        int timePerRound = dto.getTimePerRound() == null ? DEFAULT_TIME_PER_ROUND : dto.getTimePerRound();

        MovieFilters filters = MovieFilters.fromDTO(dto);
        List<Long> sessionMovieIds = tmdbService.discoverMovieIds(roundLimit, filters);

        session.setRoundLimit(roundLimit);
        session.setTimePerRound(timePerRound);
        session.setCurrentMovieIndex(0);
        session.setSessionMovieIds(sessionMovieIds);

        session = sessionRepository.save(session);
        sessionRepository.flush();

        return session;
    }

    public void setVote(VotePutDTO votePutDTO) {

        // first we check if the session exists:
        Session session = sessionRepository.findSessionBySessionCode(votePutDTO.getSessionCode());

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        // then lets check if the user is part of the session:
        String userToken = votePutDTO.getToken();

        if (userToken.startsWith("Guest")) {
            GuestUser guestUser = guestUserRepository.findByToken(userToken);
            if (guestUser == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest user not found");
            }
            if (guestUser.getCurrentSession() == null
                    || !guestUser.getCurrentSession().getSessionCode().equals(votePutDTO.getSessionCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Guest user is not part of the session");
            }
        } else {
            User user = userRepository.findByToken(userToken);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            if (user.getCurrentSession() == null
                    || !user.getCurrentSession().getSessionCode().equals(votePutDTO.getSessionCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of the session");
            }
        }

        // finally we also need to check if the user has already voted for the current
        // movie, if yes we update the existing vote instead of creating a new one
        Vote existingVote = voteRepository.findBySessionCodeAndUserIdAndMovieId(votePutDTO.getSessionCode(),
                votePutDTO.getUserId(),
                votePutDTO.getMovieId());
        if (existingVote != null) {
            existingVote.setScore(votePutDTO.getScore());
            voteRepository.save(existingVote);
            voteRepository.flush();
        } else {
            Vote vote = new Vote();
            vote.setSessionCode(votePutDTO.getSessionCode());
            vote.setUserId(votePutDTO.getUserId());
            vote.setMovieId(votePutDTO.getMovieId());
            vote.setScore(votePutDTO.getScore());
            voteRepository.save(vote);
            voteRepository.flush();
        }
    }

    public List<MovieResultDTO> calculateFullLeaderboard(String sessionCode) {
        Session session = getSessionByCode(sessionCode);

        List<Long> movieIds = session.getSessionMovieIds();

        List<MovieResultDTO> results = new ArrayList<>();

        for (Long movieId : movieIds) {
            Movie movie = tmdbService.getMovieDetails(movieId);

            int likes = voteRepository.countBySessionCodeAndMovieIdAndScore(sessionCode, movieId, 1).intValue();
            int dislikes = voteRepository.countBySessionCodeAndMovieIdAndScore(sessionCode, movieId, -1).intValue();
            int neutrals = voteRepository.countBySessionCodeAndMovieIdAndScore(sessionCode, movieId, 0).intValue();

            Integer summedScore = voteRepository.getSumOfScores(sessionCode, movieId);
            int score = summedScore != null ? summedScore : 0;

            List<SimilarMovieGetDTO> similarMovieDTOs =
                    movie.getSimilarMovies() == null
                            ? List.of()
                            : movie.getSimilarMovies().stream()
                                    .map(ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper.INSTANCE::convertSimilarMovieToDTO)
                                    .toList();

            MovieResultDTO dto = new MovieResultDTO(
                    movie.getId(),
                    movie.getTitle(),
                    score,
                    movie.getPosterPath(),
                    movie.getOverview(),
                    movie.getRating(),
                    movie.getReleaseDate(),
                    movie.getGenres(),
                    similarMovieDTOs,
                    likes,
                    dislikes,
                    neutrals);

            results.add(dto);
        }

        // Sort the results by score in descending order
        // AI generated code, if better options also try them out!
        results.sort((r1, r2) -> r2.getScore().compareTo(r1.getScore()));

        // IMPORTANT: the frontend needs to subscribe to the topic
        // "/topic/session/{sessionCode}/results" to receive the movie results
        messagingTemplate.convertAndSend("/topic/session/" + sessionCode + "/results", results);

        return results;
    }

    public Integer getSessionTiming(String sessionCode) {
        Session session = getSessionByCode(sessionCode);

        return session.getTimePerRound();
    }

    public List<String> getJoinedUsernames(Session session) {
        List<String> usernames = new ArrayList<>();
        List<User> users = userRepository.findAllByCurrentSession(session);
        for (User user : users) {
            usernames.add(user.getUsername());
        }
        List<GuestUser> guestUsers = guestUserRepository.findAllByCurrentSession(session);
        for (GuestUser guest : guestUsers) {
            usernames.add(guest.getUsername());
        }
        return usernames;
    }
}
