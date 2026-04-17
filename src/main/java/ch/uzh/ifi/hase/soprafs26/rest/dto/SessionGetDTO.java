package ch.uzh.ifi.hase.soprafs26.rest.dto;
import java.util.List;

public class SessionGetDTO {
    private String sessionCode;
    private String sessionToken;
    private long sessionId;
    private long hostId;
    private Integer joinedUsers;
    private List<String> usernames;

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

    public Integer getJoinedUsers() {
        return joinedUsers;
    }

    public void setJoinedUsers(Integer joinedUsers) {
        this.joinedUsers = joinedUsers;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }
}
