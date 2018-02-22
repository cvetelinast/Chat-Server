package bg.uni.sofia.fmi.corejava.chatserver.server;

import static bg.uni.sofia.fmi.corejava.chatserver.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bg.uni.sofia.fmi.corejava.chatserver.ChatRoom;
import bg.uni.sofia.fmi.corejava.chatserver.server.ChatServer;
import bg.uni.sofia.fmi.corejava.chatserver.server.ClientThread;

public class ClientThreadTest {

	ClientThread clientThread;

	ChatServer server;

	@Before
	public void clientThreadConstructionTest() {
		ChatServer.deleteAllFiles(PATH_TEST_USERS_FILE, PATH_TEST_ROOMS_FILE, PATH_TEST_ROOMS_FOLDER);
		this.server = new ChatServer(PATH_TEST_USERS_FILE, PATH_TEST_ROOMS_FILE, PATH_TEST_ROOMS_FOLDER);
		this.clientThread = new ClientThread(this.server, new Socket());
	}

	@After
	public void deleteFilesTest() {
		ChatServer.deleteAllFiles(PATH_TEST_USERS_FILE, PATH_TEST_ROOMS_FILE, PATH_TEST_ROOMS_FOLDER);
	}

	@Test
	public void registerTest() {
		assertEquals("Resister", "Registration successful.",
				this.clientThread.loginOrRegister(new String[] { "register", "Cveti", "12345" }, false));
		assertEquals("Resister", "This user exists.",
				this.clientThread.loginOrRegister(new String[] { "register", "Cveti", "password" }, false));
		assertEquals("Resister", WRONG_NUMBER_OF_ARGS,
				this.clientThread.loginOrRegister(new String[] { "register", "Cveti" }, false));
	}

	@Test
	public void loginTest() {
		assertEquals("Resister", "Registration successful.",
				this.clientThread.loginOrRegister(new String[] { "register", "Cveti", "12345" }, false));
		assertEquals("Login", "Login with username Cveti was successful.",
				this.clientThread.loginOrRegister(new String[] { "login", "Cveti", "12345" }, true));
		assertEquals("Login", "Wrong username.",
				this.clientThread.loginOrRegister(new String[] { "login", "cveti", "12345" }, true));
		assertEquals("Login", "Wrong password.",
				this.clientThread.loginOrRegister(new String[] { "login", "Cveti", "123456" }, true));
		assertEquals("Login", WRONG_NUMBER_OF_ARGS,
				this.clientThread.loginOrRegister(new String[] { "login", "Cveti" }, true));
	}

	@Test
	public void activeUsersTest() {
		assertEquals("Active users", "There's no one online.", this.clientThread.listActiveUsers());
		this.server.getActiveClients().put(this.clientThread, "Cveti");
		assertEquals("Active users", "Cveti", this.clientThread.listActiveUsers());
		this.server.getActiveClients().put(new ClientThread(this.server, new Socket()), "Vesi");
		String answer = this.clientThread.listActiveUsers();
		assertTrue("Active users", answer.contains("Cveti"));
		assertTrue("Active users", answer.contains("Vesi"));
	}

	@Test
	public void sendTest() {
		this.server.getActiveClients().put(this.clientThread, "Cveti");
		assertEquals("Send", "Wrong username.", this.clientThread.send(new String[] { "send", "username", "Hi!" }));
		this.server.getUsers().put("Vesi", "12345");
		assertEquals("Send", "The user is not online.", this.clientThread.send(new String[] { "send", "Vesi", "Hi!" }));
		this.server.getActiveClients().put(new ClientThread(this.server, new Socket()), "Vesi");
		assertEquals("Send", "The message was sent.",
				this.clientThread.send(new String[] { "send", "Vesi", "Hi!", "How", "are", "you?" }));
		assertEquals("Send", WRONG_NUMBER_OF_ARGS, this.clientThread.send(new String[] { "send", "username" }));
	}

	@Test
	public void createRoomTest() {
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("Create room.", "The room already exists.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("Create room.", WRONG_NUMBER_OF_ARGS,
				this.clientThread.createRoom(new String[] { "create_room" }));
	}

	@Test
	public void deleteRoomTest() {
		assertEquals("Delete room.", "Not existing room.",
				this.clientThread.deleteRoom(new String[] { "delete_room", "Room1" }));
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("Delete room.", "The room was deleted successfully.",
				this.clientThread.deleteRoom(new String[] { "delete_room", "Room1" }));
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		this.server.getRooms().put("Room1", new ChatRoom("Vesi"));
		assertEquals("Delete room.", "You don't have permission to delete this room.",
				this.clientThread.deleteRoom(new String[] { "delete_room", "Room1" }));
		assertEquals("Delete room.", WRONG_NUMBER_OF_ARGS,
				this.clientThread.deleteRoom(new String[] { "delete_room" }));
	}

	@Test
	public void joinRoomTest() {
		assertEquals("Join room.", "Not existing room.",
				this.clientThread.joinOrLeaveRoom(new String[] { "join_room", "Room1" }, true));
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("Join room.", "You joined successfully.\n",
				this.clientThread.joinOrLeaveRoom(new String[] { "join_room", "Room1" }, true));
		assertEquals("Join room.", WRONG_NUMBER_OF_ARGS,
				this.clientThread.joinOrLeaveRoom(new String[] { "join_room" }, true));
	}

	@Test
	public void leaveRoomTest() {
		this.clientThread.setUsername("Cveti");
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("Join room.", "You joined successfully.\n",
				this.clientThread.joinOrLeaveRoom(new String[] { "join_room", "Room1" }, true));
		assertEquals("Leave room.", "You left the room successfully.",
				this.clientThread.joinOrLeaveRoom(new String[] { "leave_room", "Room1" }, false));
		assertEquals("Leave room.", "You are not in that room, you can't leave it.",
				this.clientThread.joinOrLeaveRoom(new String[] { "leave_room", "Room1" }, false));
		assertEquals("Leave room.", "Not existing room.",
				this.clientThread.joinOrLeaveRoom(new String[] { "leave_room", "Room2" }, false));
		assertEquals("Leave room.", WRONG_NUMBER_OF_ARGS,
				this.clientThread.joinOrLeaveRoom(new String[] { "leave_room" }, false));
	}

	@Test
	public void listRoomsTest() {
		assertEquals("List rooms with at least one user online.", "There's no room with users online.",
				this.clientThread.listRooms(new String[] { "list_rooms" }));
		this.server.getActiveClients().put(this.clientThread, "Cveti");
		this.clientThread.setUsername("Cveti");
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("Join room.", "You joined successfully.\n",
				this.clientThread.joinOrLeaveRoom(new String[] { "join_room", "Room1" }, true));
		assertEquals("List rooms with at least one user online.", "Room1",
				this.clientThread.listRooms(new String[] { "list_rooms" }));
		assertEquals("List rooms with at least one user online.", "Wrong number of arguments.",
				this.clientThread.listRooms(new String[] { "list_rooms", "argument" }));
	}

	@Test
	public void listUsersInRoomTest() {
		this.server.getActiveClients().put(this.clientThread, "Cveti");
		this.clientThread.setUsername("Cveti");
		assertEquals("Create room.", "The room was created successfully.",
				this.clientThread.createRoom(new String[] { "create_room", "Room1" }));
		assertEquals("List users in room.", "The room Room1 has admin Cveti and no active participants. ",
				this.clientThread.listUsersInRoom(new String[] { "list_users", "Room1" }));
		assertEquals("Join room.", "You joined successfully.\n",
				this.clientThread.joinOrLeaveRoom(new String[] { "join_room", "Room1" }, true));
		assertEquals("List users in room.", "The room Room1 has admin Cveti and active users Cveti",
				this.clientThread.listUsersInRoom(new String[] { "list_users", "Room1" }));
		assertEquals("List users in room.", "Not existing room.",
				this.clientThread.listUsersInRoom(new String[] { "list_users", "Room2" }));
		assertEquals("List users in room.", WRONG_NUMBER_OF_ARGS,
				this.clientThread.listUsersInRoom(new String[] { "list_users", "Room2", "argument" }));
	}

}
