package bg.uni.sofia.fmi.corejava.chatserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatRoom implements Serializable {

	private static final long serialVersionUID = 1L;

	private String admin;

	private List<String> participants;

	public ChatRoom(String admin) {
		this.admin = admin;
		this.participants = new ArrayList<>();
	}

	public void addUser(String user) {
		this.participants.add(user);
	}

	public void deleteUser(String user) {
		this.participants.remove(user);
	}

	public String getAdmin() {
		return admin;
	}

	public List<String> getParticipants() {
		return participants;
	}

}
