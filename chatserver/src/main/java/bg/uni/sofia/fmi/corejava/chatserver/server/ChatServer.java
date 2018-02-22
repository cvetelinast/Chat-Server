package bg.uni.sofia.fmi.corejava.chatserver.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static bg.uni.sofia.fmi.corejava.chatserver.Constants.*;

import bg.uni.sofia.fmi.corejava.chatserver.ChatRoom;

public class ChatServer {

	private static final int PORT_NUMBER = 4444;

	private String usersFile;

	private String roomsFile;

	private String roomsFolder;

	private Map<ClientThread, String> activeClients;

	private Map<String, String> users;

	private Map<String, ChatRoom> rooms;

	public ChatServer(String usersFile, String roomsFile, String roomsFolder) {
		this.usersFile = Paths.get(usersFile).toString();
		this.roomsFile = Paths.get(roomsFile).toString();
		this.roomsFolder = Paths.get(roomsFolder).toString();
		synchronized (this) {
			this.activeClients = new HashMap<>();
			deserializeUsersMap();
			deserializeRoomsMap();
		}
	}

	public Map<ClientThread, String> getActiveClients() {
		return this.activeClients;
	}

	public Map<String, String> getUsers() {
		return this.users;
	}

	public Map<String, ChatRoom> getRooms() {
		return rooms;
	}

	public synchronized void serializeUsersMap() {
		serializeMap(this.usersFile, true);
	}

	private synchronized void serializeRoomsMap() {
		this.rooms.entrySet().stream().forEach(
				entry -> this.rooms.put(entry.getKey(), entry.setValue(new ChatRoom(entry.getValue().getAdmin()))));
		serializeMap(this.roomsFile, false);
	}

	private synchronized void serializeMap(String fileName, boolean isUsersMapSerialized) {
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName))) {
			if (isUsersMapSerialized) {
				objectOutputStream.writeObject(this.users);
			} else {
				objectOutputStream.writeObject(this.rooms);
			}
		} catch (IOException ioe) {
			System.err.printf("Serializing to file %s failed with IOException.%s", fileName, LINE_SEPARATOR);
			ioe.printStackTrace();
		}
	}

	private synchronized void deserializeUsersMap() {
		deserializeMap(this.usersFile, true);
	}

	private synchronized void deserializeRoomsMap() {
		deserializeMap(this.roomsFile, false);
	}

	private synchronized void deserializeMap(String fileName, boolean isUsersMapDeserialized) {
		File file = new File(fileName);
		if (!file.exists()) {
			if (isUsersMapDeserialized) {
				this.users = new HashMap<String, String>();
				return;
			} else {
				this.rooms = new HashMap<String, ChatRoom>();
				return;
			}
		}
		try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(fileName))) {
			if (isUsersMapDeserialized) {
				this.users = (HashMap) objectInputStream.readObject();
			} else {
				this.rooms = (HashMap) objectInputStream.readObject();
			}
		} catch (IOException ioe) {
			System.err.printf("Deserializing file %s failed with IOException.%s", fileName, LINE_SEPARATOR);
			ioe.printStackTrace();
		} catch (ClassNotFoundException c) {
			System.err.println("Class not found");
			c.printStackTrace();
		}
	}

	public synchronized boolean addRoom(String roomName, String adminName) {
		if (!this.rooms.containsKey(roomName)) {
			this.rooms.put(roomName, new ChatRoom(adminName));
			if (createFileForRoom(roomName)) {
				serializeRoomsMap();
				return true;
			}
		}
		return false;
	}

	public synchronized boolean deleteRoom(String roomName) {
		if (this.rooms.containsKey(roomName)) {
			this.rooms.remove(roomName);
			if (deleteFileForRoom(roomName)) {
				serializeRoomsMap();
				return true;
			}
		}
		return false;
	}

	public boolean joinRoom(String roomName, String username) {
		if (!this.rooms.get(roomName).getParticipants().contains(username)) {
			joinOrLeaveRoom(roomName, username, true);
			return true;
		}
		return false;
	}

	public void leaveRoom(String roomName, String username) {
		if (this.rooms.containsKey(roomName)) {
			if (this.rooms.get(roomName).getParticipants().contains(username)) {
				joinOrLeaveRoom(roomName, username, false);
			}
		}
	}

	private void joinOrLeaveRoom(String roomName, String username, boolean isActionJoining) {
		ChatRoom chatRoom = this.rooms.get(roomName);
		synchronized (this) {
			if (isActionJoining) {
				chatRoom.addUser(username);
			} else {
				chatRoom.deleteUser(username);
			}
			this.rooms.put(roomName, chatRoom);
		}
	}

	private boolean createFileForRoom(String roomName) {
		return createOrDeleteFileForRoom(roomName, true);
	}

	private boolean deleteFileForRoom(String roomName) {
		return createOrDeleteFileForRoom(roomName, false);
	}

	private boolean createOrDeleteFileForRoom(String roomName, boolean isActionCreate) {
		Path roomFile = Paths.get(this.roomsFolder).resolve(roomName + ".txt");
		File file = new File(roomFile.toString());
		try {
			if (isActionCreate) {
				if (file.createNewFile()) {
					return true;
				}
			} else {
				if (file.delete()) {
					return true;
				}
			}
		} catch (Exception e) {
			System.err.printf("An exception with file %s occured.%s", roomFile, LINE_SEPARATOR);
			e.printStackTrace();
		}
		return false;
	}

	public synchronized void writeLine(String answer, String roomName) {
		Path roomFile = Paths.get(this.roomsFolder).resolve(roomName + ".txt");
		try (BufferedWriter output = new BufferedWriter(new FileWriter(roomFile.toString(), true))) {
			output.append(answer);
		} catch (IOException ioe) {
			System.err.printf("Appending line to file %s failed with IOException.%s", roomFile, LINE_SEPARATOR);
			ioe.printStackTrace();
		}
	}

	public String loadChronology(String roomName) {
		String content = "";
		try {
			content = new String(Files.readAllBytes(Paths.get(this.roomsFolder).resolve(roomName + ".txt")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public boolean isUserActive(String username) {
		List<String> result = this.getActiveClients().entrySet().stream()
				.filter(user -> user.getValue().equals(username)).map(user -> user.getValue())
				.collect(Collectors.toList());
		return result.isEmpty() ? false : true;
	}

	public boolean roomContainsActiveUser(String roomName) {
		List<String> users = this.getRooms().get(roomName).getParticipants();
		for (String user : users) {
			if (isUserActive(user)) {
				return true;
			}
		}
		return false;
	}

	public static boolean deleteAllFiles(String usersFile, String roomsFile, String roomsFolder) {
		File users = new File(usersFile);
		File rooms = new File(roomsFile);
		File roomsDir = new File(roomsFolder);
		if (users.exists()) {
			users.delete();
		}
		if (rooms.exists()) {
			rooms.delete();
		}
		File[] contents = roomsDir.listFiles();
		if (contents != null) {
			for (File f : contents) {
				if (!f.delete()) {
					return false;
				}
			}
		}
		return true;
	}

	private void startServer() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
			acceptClients(serverSocket);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + PORT_NUMBER);
			System.exit(1);
		}
	}

	private void acceptClients(ServerSocket serverSocket) {

		System.out.println("Server starts at port: " + serverSocket.getLocalSocketAddress());
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				ClientThread client = new ClientThread(this, socket);
				Thread thread = new Thread(client);
				thread.start();
				activeClients.put(client, "");
			} catch (IOException ex) {
				System.out.println("Accept failed on : " + PORT_NUMBER);
			}
		}
	}

	public static void main(String[] args) {
		ChatServer server = new ChatServer(PATH_USERS_FILE, PATH_ROOMS_FILE, PATH_ROOMS_FOLDER);
		server.startServer();
	}

}