package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;
import java.util.List;

public class SessionStateGetDTO {
    private String sessionCode;
    private String status;
    private Integer currentMovieIndex;
    private MovieGetDTO currentMovie;
    private Instant roundStartedAt;
    private Integer timePerRound;
    private Integer joinedUsers;
    private Integer votesReceived;
    private Integer totalRounds;
    private List<String> usernames;
    private String hostUsername;

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCurrentMovieIndex() {
        return currentMovieIndex;
    }

    public void setCurrentMovieIndex(Integer currentMovieIndex) {
        this.currentMovieIndex = currentMovieIndex;
    }

    public MovieGetDTO getCurrentMovie() {
        return currentMovie;
    }

    public void setCurrentMovie(MovieGetDTO currentMovie) {
        this.currentMovie = currentMovie;
    }

    public Instant getRoundStartedAt() {
        return roundStartedAt;
    }

    public void setRoundStartedAt(Instant roundStartedAt) {
        this.roundStartedAt = roundStartedAt;
    }

    public Integer getTimePerRound() {
        return timePerRound;
    }

    public void setTimePerRound(Integer timePerRound) {
        this.timePerRound = timePerRound;
    }

    public Integer getJoinedUsers() {
        return joinedUsers;
    }

    public void setJoinedUsers(Integer joinedUsers) {
        this.joinedUsers = joinedUsers;
    }

    public Integer getVotesReceived() {
        return votesReceived;
    }

    public void setVotesReceived(Integer votesReceived) {
        this.votesReceived = votesReceived;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }
}
