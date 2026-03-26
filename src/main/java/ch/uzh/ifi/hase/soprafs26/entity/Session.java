package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import java.io.Serializable;
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

    @Column(nullable = true)
    private Integer roundLimit;

    @Column(nullable = false)
    private Long hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false)
    private Date creationDate;

    @Column(nullable = false, unique = true)
    private String sessionToken;

    @ElementCollection
    private List<Long> sessionMovieIds;

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

    public Integer getRoundLimit() {
        return roundLimit;
    }

    public void setRoundLimit(Integer roundLimit) {
        this.roundLimit = roundLimit;
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

    public List<Long> getSessionMovieIds() {
        return sessionMovieIds;
    }

    public void setSessionMovieIds(List<Long> sessionMovieIds) {
        this.sessionMovieIds = sessionMovieIds;
    }
}
