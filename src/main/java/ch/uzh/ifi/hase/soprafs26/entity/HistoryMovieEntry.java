package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class HistoryMovieEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "score", nullable = false)
    private Integer score;

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
