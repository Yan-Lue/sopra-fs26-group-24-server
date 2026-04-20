package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.uzh.ifi.hase.soprafs26.constant.SessionStatus;

@Entity
@Table(name = "sessions") // I still have to create a
public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @Column(nullable = false)
    private String sessionName;

    @Column(nullable = false, unique = true)
    private String sessionCode;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Column(nullable = true, name = "joinedUsers")
    private Integer joinedUsers;

    @Column(nullable = true)
    private Integer roundLimit;

    @Column(nullable = false)
    private Integer currentMovieIndex;

    @Column(nullable = false)
    private Long hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private Date creationDate;

    @Column(nullable = false, unique = true)
    private String sessionToken;

    @Column(nullable = false)
    private Integer timePerRound;

    @Column(nullable = false)
    private Integer votesReceivedThisRound; 

    @ElementCollection
    @CollectionTable(name = "session_session_movie_id", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "movie_id", nullable = false)
    private List<Long> sessionMovieIds = new ArrayList<>();

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Integer getJoinedUsers() {
        return joinedUsers;
    }

    public void setJoinedUsers(Integer joinedUsers) {
        this.joinedUsers = joinedUsers;
    }

    public Integer getRoundLimit() {
        return roundLimit;
    }

    public void setRoundLimit(Integer roundLimit) {
        this.roundLimit = roundLimit;
    }

    public Integer getCurrentMovieIndex() {
        return currentMovieIndex;
    }

    public void setCurrentMovieIndex(Integer currentMovieIndex) {
        this.currentMovieIndex = currentMovieIndex;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Integer getTimePerRound() {
        return timePerRound;
    }

    public void setTimePerRound(Integer timePerRound) {
        this.timePerRound = timePerRound;
    }

    public List<Long> getSessionMovieIds() {
        return sessionMovieIds;
    }

    public void setSessionMovieIds(List<Long> sessionMovieIds) {
        this.sessionMovieIds = sessionMovieIds;
    }

    public Integer getVotesReceivedThisRound() {
        return votesReceivedThisRound;
    }

    public void setVotesReceivedThisRound(Integer votesReceivedThisRound) {
        this.votesReceivedThisRound = votesReceivedThisRound;
    }

    //increment and reset round votes
}
