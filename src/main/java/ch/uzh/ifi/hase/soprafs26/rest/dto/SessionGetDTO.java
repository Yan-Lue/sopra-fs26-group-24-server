package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class SessionGetDTO {
    private String sessionCode;
    private String sessionToken;

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
}
