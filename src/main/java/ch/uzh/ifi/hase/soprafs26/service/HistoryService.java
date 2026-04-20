package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.entity.HistoryMovieEntry;
import ch.uzh.ifi.hase.soprafs26.entity.Session;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.HistoryPostDTO;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HistoryService {

    private final Logger log =  LoggerFactory.getLogger(HistoryService.class);

    private final HistoryRepository historyRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;

    public HistoryService(HistoryRepository historyRepository, SessionRepository sessionRepository,
                          UserRepository userRepository,
                          VoteRepository voteRepository) {
        this.historyRepository = historyRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
    }

    public void saveHistory(HistoryPostDTO dto) {
        Session session = sessionRepository.findSessionBySessionCode(dto.getSessionCode());

        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session could not be found.");
        }

        User user = validateToken(dto.getToken(), session);

        History existingHistory = historyRepository.findBySessionCodeAndUserId(session.getSessionCode(), user.getId());
        if (existingHistory != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "History already saved.");
        }

        History newHistory = new History();
        newHistory.setSessionName(session.getSessionName());
        newHistory.setSessionCode(session.getSessionCode());
        newHistory.setJoinedUsers(session.getJoinedUsers());
        newHistory.setCreationDate(session.getCreationDate());
        newHistory.setUser(user);

        List<HistoryMovieEntry> entries = new ArrayList<>();
        for (Long movieId : session.getSessionMovieIds()) {
            Integer summedScore = voteRepository.getSumOfScores(session.getSessionCode(), movieId);

            HistoryMovieEntry entry = new HistoryMovieEntry();
            entry.setMovieId(movieId);
            entry.setScore(summedScore != null ? summedScore : 0);
            entries.add(entry);
        }

        newHistory.setMovies(entries);

        historyRepository.save(newHistory);
        historyRepository.flush();
    }

    private User validateToken(String token, Session session) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required.");
        }

        if (token.startsWith("Guest")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please register as User to save history.");
        }

        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        }

        if (user.getCurrentSession() == null ||
                !user.getCurrentSession().getSessionCode().equals(session.getSessionCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of this session.");
        }
        return user;
    }

    public History getHistoryByHistoryId(Long userId, Long historyId) {
        History history =  historyRepository.findByUserIdAndHistoryId(userId, historyId);
        if (history == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "History with historyId " + historyId + " not found.");
        }
        return history;
    }

    public List<History> getHistoriesOfUser(Long userId) {

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserId is required.");
        }
        return this.historyRepository.findAllByUserId(userId);
    }

    public void deleteHistory(Long userId, Long historyId) {
        if (historyRepository.existsById(historyId) && userRepository.existsById(userId) &&
                historyRepository.findByUserIdAndHistoryId(userId, historyId) != null) {
            historyRepository.deleteById(historyId);
            historyRepository.flush();
            log.debug("Deleted history with id: {}", historyId);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User with UserId " + userId + " has no History with Id " + historyId + ".");
        }
    }
}
