package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "votes")
public class Vote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long voteId;

    @Column(nullable = false)
    private String sessionCode;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private Integer score;

    public Long getVoteId() {
        return voteId;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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
