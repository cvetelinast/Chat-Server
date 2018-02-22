package bg.uni.sofia.fmi.corejava.chatserver.client;

import static org.junit.Assert.*;

import java.net.Socket;

import org.junit.Before;
import org.junit.Test;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.INVALID_INPUT;

public class ServerThreadTest {

	ServerThread serverThread;

	@Before
	public void serverThreadConstructorTest() {
		this.serverThread = new ServerThread(new Socket());
	}

	@Test
	public void acceptOrDeclineFileTest() {
		assertEquals("Unknown command.", INVALID_INPUT, this.serverThread.acceptOrDeclineFile("command"));
		assertEquals("Decline file.", "The file was declined.", this.serverThread.acceptOrDeclineFile("decline"));
		assertEquals("Accept file with not valid location.", "Wrong file location.",
				this.serverThread.acceptOrDeclineFile("accept"));
	}

}
