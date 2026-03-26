package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

import java.io.Serializable;

@Entity
@Table(name = "guest_users")
public class GuestUser implements Serializable {

    private static final long serialVersionUID = 1L;

    // done using Gemini: Like this the ID's are sequentially created for both guest
    // and normal users which shoudl avoid ID collisions in the frontend
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shared_user_sequence")
    @SequenceGenerator(name = "shared_user_sequence", sequenceName = "shared_user_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}