package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class VoteCacheService {

    private final VoteRepository voteRepository;
    private final ConcurrentMap<String, ConcurrentMap<Long, ConcurrentMap<Long, Integer>>> cache = new ConcurrentHashMap<>();

    public VoteCacheService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    public int addVote(String sessionCode, Long movieId, Long userId, Integer score) {
        ConcurrentMap<Long, ConcurrentMap<Long, Integer>> sessionMap = cache.computeIfAbsent(sessionCode, k -> new ConcurrentHashMap<>());
        ConcurrentMap<Long, Integer> movieMap = sessionMap.computeIfAbsent(movieId, k -> new ConcurrentHashMap<>());
        movieMap.put(userId, score);
        return movieMap.size();
    }

    public int getCachedVoteCount(String sessionCode, Long movieId) {
        ConcurrentMap<Long, ConcurrentMap<Long, Integer>> sessionMap = cache.get(sessionCode);

        if (sessionMap == null) {
            return 0;
        }
        ConcurrentMap<Long, Integer> movieMap = sessionMap.get(movieId);
        return movieMap == null ? 0 : movieMap.size();
    }


    @Transactional
    public void flushSession(String sessionCode) {
        ConcurrentMap<Long, ConcurrentMap<Long, Integer>> sessionMap = cache.remove(sessionCode);
        if (sessionMap == null) return;
        for (Map.Entry<Long, ConcurrentMap<Long, Integer>> movieEntry : sessionMap.entrySet()) {
            Long movieId = movieEntry.getKey();
            ConcurrentMap<Long, Integer> movieMap = movieEntry.getValue();
            if (movieMap == null || movieMap.isEmpty()) continue;

            List<Long> userIds = new ArrayList<>(movieMap.keySet());
            List<Vote> existing = voteRepository.findBySessionCodeAndMovieIdAndUserIdIn(sessionCode, movieId, userIds);
            Map<Long, Vote> existingByUser = new HashMap<>();
            for (Vote v : existing) existingByUser.put(v.getUserId(), v);

            List<Vote> toSave = new ArrayList<>();
            for (Map.Entry<Long, Integer> e : movieMap.entrySet()) {
                Long userId = e.getKey();
                Integer score = e.getValue();
                Vote v = existingByUser.get(userId);
                if (v != null) {
                    v.setScore(score);
                    toSave.add(v);
                } else {
                    Vote nv = new Vote();
                    nv.setSessionCode(sessionCode);
                    nv.setUserId(userId);
                    nv.setMovieId(movieId);
                    nv.setScore(score);
                    toSave.add(nv);
                }
            }
            voteRepository.saveAll(toSave);
        }
        voteRepository.flush();
    }
}
