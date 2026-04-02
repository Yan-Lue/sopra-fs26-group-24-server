package ch.uzh.ifi.hase.soprafs26.constant;

public enum UserStatus {
	ONLINE, OFFLINE;

	public static UserStatus fromString(String status) {
		for (UserStatus userStatus : UserStatus.values()) {
			if (userStatus.name().equalsIgnoreCase(status)) {
				return userStatus;
			}
		}
		throw new IllegalArgumentException("Invalid user status: " + status);
	}
}
