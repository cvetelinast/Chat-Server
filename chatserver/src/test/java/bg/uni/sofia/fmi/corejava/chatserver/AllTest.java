package bg.uni.sofia.fmi.corejava.chatserver;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import bg.uni.sofia.fmi.corejava.chatserver.client.ClientTest;
import bg.uni.sofia.fmi.corejava.chatserver.client.ServerThreadTest;
import bg.uni.sofia.fmi.corejava.chatserver.server.ClientThreadTest;
import bg.uni.sofia.fmi.corejava.chatserver.server.ServerTest;

@RunWith(Suite.class)
@SuiteClasses({ ServerTest.class, ClientThreadTest.class, ClientTest.class, ServerThreadTest.class })
public class AllTest {

}
