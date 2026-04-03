package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

import java.io.Serializable;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	// done using Gemini: Like this the ID's are sequentially created for both guest
	// and normal users which shoudl avoid ID collisions in the frontend
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shared_user_sequence")
	@SequenceGenerator(name = "shared_user_sequence", sequenceName = "shared_user_sequence", allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserStatus status;

	@Column(nullable = false)
	private String password;

	@Column(nullable = true)
	private String bio;

	@Column(nullable = false, unique = true)
	private String email;

	@ManyToOne
	@JoinColumn(name = "current_session_id", nullable = true)
	private Session currentSession;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Session getCurrentSession() {
		return currentSession;
	}

	public void setCurrentSession(Session currentSession) {
		this.currentSession = currentSession;
	}
}
