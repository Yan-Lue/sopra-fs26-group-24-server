package ch.uzh.ifi.hase.soprafs26.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Vote;

@Repository("voteRepository")
public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT SUM(v.score) FROM Vote v WHERE v.sessionCode = :sessionCode AND v.movieId = :movieId")
    Integer getSumOfScores(@Param("sessionCode") String sessionCode, @Param("movieId") Long movieId);

    // Like that we can once the session is over, calculate the total score for each
    // movie and then fetch this all together
    // to the frontend where we can then display the movies in the right order.

    Vote findBySessionCodeAndUserIdAndMovieId(@Param("sessionCode") String sessionCode, @Param("userId") Long userId,
            @Param("movieId") Long movieId);

    Long countBySessionCodeAndMovieIdAndScore(@Param("sessionCode") String sessionCode, @Param("movieId") Long movieId, @Param("score") Integer score);
}
