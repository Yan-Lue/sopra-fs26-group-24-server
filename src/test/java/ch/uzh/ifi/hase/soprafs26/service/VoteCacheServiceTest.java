package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.Vote;
import ch.uzh.ifi.hase.soprafs26.repository.VoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteCacheServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private VoteCacheService voteCacheService;

    @Test
    void addVote_and_getCachedVoteCount() {
        int c1 = voteCacheService.addVote("S1", 100L, 1L, 1);
        assertEquals(1, c1);
        int c2 = voteCacheService.addVote("S1", 100L, 2L, -1);
        assertEquals(2, c2);

        voteCacheService.addVote("S1", 100L, 1L, 0);
        assertEquals(2, voteCacheService.getCachedVoteCount("S1", 100L));
        assertEquals(0, voteCacheService.getCachedVoteCount("S1", 999L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void flushSession_createsNewVotes_whenNoExisting() {
        // prepare cache: two movies under same session
        voteCacheService.addVote("S2", 200L, 10L, 1);
        voteCacheService.addVote("S2", 200L, 11L, 0);
        voteCacheService.addVote("S2", 201L, 12L, -1);

        when(voteRepository.findBySessionCodeAndMovieIdAndUserIdIn(eq("S2"), eq(200L), anyList()))
                .thenReturn(List.of());
        when(voteRepository.findBySessionCodeAndMovieIdAndUserIdIn(eq("S2"), eq(201L), anyList()))
                .thenReturn(List.of());

        voteCacheService.flushSession("S2");

        // two saveAll calls expected (one per movie)
        verify(voteRepository, times(2)).saveAll(anyList());
        verify(voteRepository).flush();

        // inspect one of the saved lists
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(voteRepository, atLeastOnce()).saveAll(captor.capture());
        List<Vote> savedAny = (List<Vote>) captor.getAllValues().get(0);
        assertFalse(savedAny.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void flushSession_updatesExistingVotes_andCreatesNewOnes() {
        // cache has two users for one movie
        voteCacheService.addVote("S3", 300L, 20L, 1);
        voteCacheService.addVote("S3", 300L, 21L, -1);

        // existing vote for user 20
        Vote existing = new Vote();
        existing.setSessionCode("S3");
        existing.setMovieId(300L);
        existing.setUserId(20L);
        existing.setScore(0); // will be updated to 1

        when(voteRepository.findBySessionCodeAndMovieIdAndUserIdIn(eq("S3"), eq(300L), anyList()))
                .thenReturn(List.of(existing));

        voteCacheService.flushSession("S3");

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(voteRepository).saveAll(captor.capture());
        List<Vote> saved = (List<Vote>) captor.getValue();

        // should contain updated existing (user 20) and new (user 21)
        assertEquals(2, saved.size());
        boolean saw20 = saved.stream().anyMatch(v -> v.getUserId().equals(20L) && v.getScore().equals(1));
        boolean saw21 = saved.stream().anyMatch(v -> v.getUserId().equals(21L) && v.getScore().equals(-1));
        assertTrue(saw20);
        assertTrue(saw21);

        verify(voteRepository).flush();
    }

    @Test
    void flushSession_noCache_noInteractions() {
        // nothing cached for this session
        voteCacheService.flushSession("NOPE");
        verifyNoInteractions(voteRepository);
    }
}