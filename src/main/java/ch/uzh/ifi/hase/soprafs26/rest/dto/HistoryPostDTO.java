package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class HistoryPostDTO {

    private String sessionCode;
    private String token;

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
