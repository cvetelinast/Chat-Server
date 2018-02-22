package bg.uni.sofia.fmi.corejava.chatserver.server;

import static bg.uni.sofia.fmi.corejava.chatserver.Constants.PATH_TEST_ROOMS_FILE;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.PATH_TEST_ROOMS_FOLDER;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.PATH_TEST_USERS_FILE;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import bg.uni.sofia.fmi.corejava.chatserver.server.ChatServer;

public class ServerTest {

	ChatServer server;

	@Before
	public void serverConstructionTest() {
		ChatServer.deleteAllFiles(PATH_TEST_USERS_FILE, PATH_TEST_ROOMS_FILE, PATH_TEST_ROOMS_FOLDER);
		this.server = new ChatServer(PATH_TEST_USERS_FILE, PATH_TEST_ROOMS_FILE, PATH_TEST_ROOMS_FOLDER);
	}

	@Test
	public void addRoomTest() {
		assertTrue("Create room.", this.server.addRoom("Room1", "Ivan"));
		assertFalse("Don't create room.", this.server.addRoom("Room1", "Cveti"));
	}

	@Test
	public void deleteRoomTest() {
		assertTrue("Create room.", this.server.addRoom("Room1", "Ivan"));
		assertTrue("Delete room.", this.server.deleteRoom("Room1"));
		assertFalse("Don't delete room.", this.server.deleteRoom("Room1"));
	}

	@Test
	public void joinRoomTest() {
		assertTrue("Create room.", this.server.addRoom("Room1", "Ivan"));
		assertTrue("Join room.", this.server.joinRoom("Room1", "Ivan"));
		assertFalse("Join room.", this.server.joinRoom("Room1", "Ivan"));
	}

}
