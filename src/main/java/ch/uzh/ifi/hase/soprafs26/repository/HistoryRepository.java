package ch.uzh.ifi.hase.soprafs26.repository;

import ch.uzh.ifi.hase.soprafs26.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("historyRepository")
public interface HistoryRepository extends JpaRepository<History, Long> {

    History findByHistoryId(Long historyId);

    List<History> findAllByUserId(Long userId);

    History findBySessionCodeAndUserId(String sessionCode, Long userId);
}
