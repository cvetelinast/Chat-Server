package bg.uni.sofia.fmi.corejava.chatserver.client;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import bg.uni.sofia.fmi.corejava.chatserver.client.Client;

public class ClientTest {

	Client client;

	@Before
	public void constructorTest() {
		this.client = new Client("localhost", 4444);
	}

	@Test
	public void userWantsToDisconnectTest() {
		assertTrue(this.client.wantToDisconnect("disconnect"));
		assertFalse(this.client.wantToDisconnect("connect"));
		assertFalse(this.client.wantToDisconnect("command"));
	}

}
