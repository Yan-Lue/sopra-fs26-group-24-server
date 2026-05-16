package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class SessionStatusGetDTO {
    private Integer maxPlayers;
    private Integer joinedUsers;
    private List<String> usernames;
    private String hostUsername;

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
