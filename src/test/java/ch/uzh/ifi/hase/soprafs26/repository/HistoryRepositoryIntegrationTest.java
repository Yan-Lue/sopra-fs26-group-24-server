package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import ch.uzh.ifi.hase.soprafs26.entity.HistoryMovieEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class HistoryRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HistoryRepository historyRepository;

    @BeforeEach
    void setup() {
        HistoryMovieEntry first = new HistoryMovieEntry();
        first.setMovieId(1L);
        first.setScore(3);

        HistoryMovieEntry second = new HistoryMovieEntry();
        second.setMovieId(2L);
        second.setScore(-1);

        History history = new History();
        history.setSessionName("Test Round");
        history.setSessionCode("ABCDE");
        history.setJoinedUsers(3);
        history.setCreationDate(new Date());
        history.setUserId(7L);
        history.setMovies(List.of(first, second));

        entityManager.persist(history);
        entityManager.flush();
    }

    @Test
    void findSessionCodeAndUserId_success() {
        History found = historyRepository.findBySessionCodeAndUserId("ABCDE", 7L);

        assertNotNull(found);
        assertEquals("Test Round", found.getSessionName());
        assertEquals(2, found.getMovies().size());
        assertEquals(1L, found.getMovies().get(0).getMovieId());
    }

    @Test
    void findAllByUserId_success() {
        List<History> found = historyRepository.findAllByUserId(7L);

        assertEquals(1, found.size());
        assertEquals("ABCDE", found.get(0).getSessionCode());
    }
}
