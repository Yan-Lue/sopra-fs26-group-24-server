package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "history")
public class History implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(nullable = false)
    private String sessionName;

    @Column(nullable = false)
    private String sessionCode;

    @Column(nullable = false)
    private Integer joinedUsers;

    @Column(nullable = false)
    private Date creationDate;

    @Column(nullable = false)
    private Long userId;

    @SuppressWarnings("java:S1948")
    @ElementCollection
    @CollectionTable(name = "history_movie_id", joinColumns = @JoinColumn(name = "historyId"))
    private List<HistoryMovieEntry> movies = new ArrayList<>();

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionCode()  {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Integer getJoinedUsers() {
        return joinedUsers;
    }

    public void setJoinedUsers(Integer joinedUsers) {
        this.joinedUsers = joinedUsers;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<HistoryMovieEntry> getMovies() {
        return movies;
    }

    public void setMovies(List<HistoryMovieEntry> movies) {
        this.movies = movies;
    }
}
