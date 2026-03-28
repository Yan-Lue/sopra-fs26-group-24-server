package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class SessionPostDTO {

    private String sessionName;
    private Integer maxPlayers;
    private Integer roundLimit;
    private Long hostId;

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
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
}
