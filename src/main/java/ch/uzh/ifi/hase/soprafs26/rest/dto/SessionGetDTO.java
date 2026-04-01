package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class SessionGetDTO {
    private String sessionCode;
    private String sessionToken;
    private long sessionId;
    private long hostId;

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getSessionId() {
        return sessionId;
    }

    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
}
