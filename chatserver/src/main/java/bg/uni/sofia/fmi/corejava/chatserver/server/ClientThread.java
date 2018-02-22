package bg.uni.sofia.fmi.corejava.chatserver.server;

import static bg.uni.sofia.fmi.corejava.chatserver.Constants.INVALID_INPUT;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.WRONG_NUMBER_OF_ARGS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import bg.uni.sofia.fmi.corejava.chatserver.Command;

public class ClientThread implements Runnable {

	private Socket socket;

	private PrintWriter clientOut;

	private ChatServer server;

	private OutputStream outputStream;

	private InputStream inputStream;

	private String username;

	private String roomName;

	public ClientThread(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		try {
			this.outputStream = socket.getOutputStream();
			this.inputStream = socket.getInputStream();
		} catch (IOException e) {
			System.err.println("An IOException occured.");
			e.printStackTrace();
		}
		this.username = "";
		this.roomName = "";
	}

	private PrintWriter getWriter() {
		return clientOut;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	private String login(String username, String password) {
		if (this.server.getUsers().get(username).equals(password)) {
			this.username = username;
			synchronized (this.server.getActiveClients()) {
				this.server.getActiveClients().put(this, username);
			}
			return "Login with username " + username + " was successful.";
		}
		return "Wrong password.";
	}

	private String register(String username, String password) {
		synchronized (this.server) {
			this.server.getUsers().put(username, password);
			this.server.serializeUsersMap();
		}
		return "Registration successful.";
	}

	public String loginOrRegister(String[] inputSeparatedWords, boolean isActionLogin) {
		if (inputSeparatedWords.length == 3) {
			String username = inputSeparatedWords[1];
			String password = inputSeparatedWords[2];
			if (this.server.getUsers().containsKey(username)) {
				if (isActionLogin) {
					return login(username, password);
				} else {
					return "This user exists.";
				}
			} else if (isActionLogin) {
				return "Wrong username.";
			} else {
				return register(username, password);
			}
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	public String listActiveUsers() {
		String result = this.server.getActiveClients().entrySet().stream().filter(name -> !name.getValue().equals(""))
				.map(name -> String.valueOf(name.getValue())).collect(Collectors.joining(", "));
		return result.length() > 0 ? result : "There's no one online.";
	}

	public String send(String[] inputSeparatedWords) {
		if (inputSeparatedWords.length >= 3) {
			String username = inputSeparatedWords[1];
			String[] restOfElements = Arrays.copyOfRange(inputSeparatedWords, 2, inputSeparatedWords.length);
			String message = String.join(" ", restOfElements);
			if (this.server.getActiveClients().containsValue(username)) {
				sendToSpecificPerson(this.username + " > " + message, username);
				return "The message was sent.";
			} else if (this.server.getUsers().containsKey(username)) {
				return "The user is not online.";
			}
			return "Wrong username.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	private String sendFile(String[] inputSeparatedWords) {
		if (inputSeparatedWords.length == 3) {
			String username = inputSeparatedWords[1];
			String fileLocation = inputSeparatedWords[2];
			if (this.server.getActiveClients().containsValue(username)) {
				String filename = Paths.get(fileLocation).getFileName().toString();
				sendToSpecificPerson(this.username + " > Sending a file: " + fileLocation, username);
				sendFileToPerson(username, filename);
				return "The file was sent.";
			} else if (this.server.getUsers().containsKey(username)) {
				return "The user is not online.";
			}
			return "Wrong username.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	public String createRoom(String[] inputSeparatedWords) {
		if (inputSeparatedWords.length == 2) {
			String roomName = inputSeparatedWords[1];
			if (this.server.addRoom(roomName, this.username)) {
				return "The room was created successfully.";
			}
			return "The room already exists.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	public String deleteRoom(String[] inputSeparatedWords) {
		if (inputSeparatedWords.length == 2) {
			String roomName = inputSeparatedWords[1];
			if (this.server.getRooms().containsKey(roomName)) {
				if (this.server.getRooms().get(roomName).getAdmin().equals(this.username)) {
					if (this.server.deleteRoom(roomName)) {
						this.roomName = "";
						return "The room was deleted successfully.";
					}
				}
				return "You don't have permission to delete this room.";
			}
			return "Not existing room.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	private String joinRoom(String roomName) {
		this.server.joinRoom(roomName, this.username);
		this.roomName = roomName;
		String chronology = this.server.loadChronology(roomName);
		String answer = "You joined successfully." + "\n" + chronology;
		return answer;
	}

	private String leaveRoom(String roomName) {
		if (this.server.getRooms().get(roomName).getParticipants().contains(this.username)) {
			this.server.leaveRoom(roomName, this.username);
			this.roomName = "";
			return "You left the room successfully.";
		}
		return "You are not in that room, you can't leave it.";
	}

	public String joinOrLeaveRoom(String[] inputSeparatedWords, boolean isActionJoin) {
		if (inputSeparatedWords.length == 2) {
			String roomName = inputSeparatedWords[1];
			if (this.server.getRooms().containsKey(roomName)) {
				if (isActionJoin) {
					return joinRoom(roomName);
				} else {
					return leaveRoom(roomName);
				}
			}
			return "Not existing room.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	public String listRooms(String[] inputSeparatedWords) {
		if (inputSeparatedWords.length == 1) {
			String result = this.server.getRooms().entrySet().stream()
					.filter(room -> this.server.roomContainsActiveUser(room.getKey()))
					.map(nameRoom -> nameRoom.getKey()).collect(Collectors.joining(", "));
			return result.length() > 0 ? result : "There's no room with users online.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	public String listUsersInRoom(String[] inputSeparatedWords) {
		if (inputSeparatedWords.length == 2) {
			String roomName = inputSeparatedWords[1];
			if (this.server.getRooms().containsKey(roomName)) {
				String adminName = this.server.getRooms().get(roomName).getAdmin();
				List<String> users = this.server.getRooms().get(roomName).getParticipants().stream()
						.filter(user -> this.server.isUserActive(user)).collect(Collectors.toList());
				String participants;
				if (users.isEmpty()) {
					participants = "no active participants. ";
				} else {
					participants = "active users " + String.join(", ", users);
				}
				return "The room " + roomName + " has admin " + adminName + " and " + participants;
			}
			return "Not existing room.";
		}
		return WRONG_NUMBER_OF_ARGS;
	}

	public static boolean isInEnum(String command) {
		for (Command c : Command.values()) {
			if (c.name().equals(command)) {
				return true;
			}
		}
		return false;
	}

	private void serverReply(String[] inputSeparatedWords) {
		String serverReply = "";
		if (!isInEnum(inputSeparatedWords[0])) {
			serverReply = INVALID_INPUT;
		} else {
			Command command = Command.valueOf(inputSeparatedWords[0]);
			if (this.username.equals("")) {
				switch (command) {
				case register:
					serverReply = loginOrRegister(inputSeparatedWords, false);
					break;
				case login:
					serverReply = loginOrRegister(inputSeparatedWords, true);
					break;
				default:
					serverReply = INVALID_INPUT;
				}
			} else {
				switch (command) {
				case send:
					serverReply = send(inputSeparatedWords);
					break;
				case send_file:
					serverReply = sendFile(inputSeparatedWords);
					break;
				case create_room:
					serverReply = createRoom(inputSeparatedWords);
					break;
				case delete_room:
					serverReply = deleteRoom(inputSeparatedWords);
					break;
				case join_room:
					serverReply = joinOrLeaveRoom(inputSeparatedWords, true);
					break;
				case leave_room:
					serverReply = joinOrLeaveRoom(inputSeparatedWords, false);
					break;
				case list_rooms:
					serverReply = listRooms(inputSeparatedWords);
					break;
				case list_users:
					if (inputSeparatedWords.length == 1) {
						serverReply = listActiveUsers();
						break;
					} else if (inputSeparatedWords.length == 2) {
						serverReply = listUsersInRoom(inputSeparatedWords);
						break;
					}
				default:
					serverReply = INVALID_INPUT;
				}
			}
		}
		sendOnlyMe(serverReply);
	}

	private void sendOnlyMe(String serverReply) {
		for (ClientThread thatClient : server.getActiveClients().keySet()) {
			if (this.equals(thatClient)) {
				PrintWriter thatClientOut = thatClient.getWriter();
				if (thatClientOut != null) {
					thatClientOut.write(serverReply + "\r\n");
					thatClientOut.flush();
				}
			}
		}
	}

	private void sendToSpecificPerson(String serverReply, String username) {
		for (ClientThread thatClient : server.getActiveClients().keySet()) {
			if (thatClient.username.equals(username)) {
				PrintWriter thatClientOut = thatClient.getWriter();
				if (thatClientOut != null) {
					thatClientOut.write(serverReply + "\r\n");
					thatClientOut.flush();
				}
			}
		}
	}

	private void sendToParticipantsInRoom(String serverReply) {
		boolean isLeaving = false;
		boolean isJoining = false;
		String answer;
		if (serverReply.equals(Command.leave_room.name())) {
			isLeaving = true;
			answer = " - " + this.username + " left the room. -\n";
			this.server.leaveRoom(roomName, this.username);
		} else if (serverReply.endsWith("joined the chat room.")) {
			isJoining = true;
			answer = " - " + serverReply + " -\n";
		} else {
			answer = this.username + " > " + serverReply + "\r\n";
		}
		for (ClientThread thatClient : server.getActiveClients().keySet()) {
			if (thatClient.roomName.equals(this.roomName)) {
				PrintWriter thatClientOut = thatClient.getWriter();
				if (thatClientOut != null) {
					thatClientOut.write(answer);
					thatClientOut.flush();
				}
			}
		}
		if (!isJoining) {
			if (!isLeaving) {
				this.server.writeLine(answer, this.roomName);
			} else {
				this.roomName = "";
			}
		}
	}

	private void sendFileToPerson(String username, String location) {
		OutputStream out = null;
		for (ClientThread thatClient : server.getActiveClients().keySet()) {
			if (thatClient.username.equals(username)) {
				PrintWriter thatClientOut = thatClient.getWriter();
				if (thatClientOut != null) {
					out = thatClient.outputStream;
				}
			}
		}
		// File file = new File(Paths.get(location).toString());
		File file = new File("D:\\Cveti_Documents\\Test\\background.jpg");
		byte[] bytes = new byte[16 * 1024];
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.printf("The file %s was not found.", file);
			e.printStackTrace();
		}

		int count;
		try {
			while ((count = in.read(bytes)) > 0) {
				out.write(bytes, 0, count);
			}
		} catch (IOException e) {
			System.err.println("An IOException occured.");
			e.printStackTrace();
		} finally {
			try {
				out.close();
				in.close();
				socket.close();
			} catch (IOException e) {
				System.err.println("An IOException occured.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		this.clientOut = new PrintWriter(this.outputStream, false);
		Scanner in = new Scanner(this.inputStream);
		while (!socket.isClosed()) {
			if (in.hasNextLine()) {
				String input = in.nextLine();
				if (!this.roomName.equals("")) {
					this.sendToParticipantsInRoom(input);
				} else {
					String[] inputSeparatedWords = input.split(" ");
					serverReply(inputSeparatedWords);
				}
			}
		}
	}
}
