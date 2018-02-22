package bg.uni.sofia.fmi.corejava.chatserver.client;

import static bg.uni.sofia.fmi.corejava.chatserver.Constants.LOGGED_OPTIONS_MENU;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.UNLOGGED_OPTIONS_MENU;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.WRONG_INPUT_PROMPT;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import bg.uni.sofia.fmi.corejava.chatserver.Command;

public class Client {

	private static final Scanner SCANNER = new Scanner(System.in);

	private String serverHost;

	private int serverPort;

	ServerThread serverThread;

	Thread serverAccessThread;

	Socket socket;

	public Client(String host, int portNumber) {
		this.serverHost = host;
		this.serverPort = portNumber;
	}

	private void showMenuForUnlogged() {
		showMenu(UNLOGGED_OPTIONS_MENU);
	}

	private void showMenuForLogged() {
		showMenu(LOGGED_OPTIONS_MENU);
	}

	public boolean wantToDisconnect(String command) {
		return command.equals(Command.disconnect.name());
	}

	private void showMenu(String menu) {
		System.out.println("Enter one of the following options:");
		System.out.println(menu);
		String input = SCANNER.nextLine();
		while (input.trim().equals("")) {
			System.out.println(WRONG_INPUT_PROMPT);
			input = SCANNER.nextLine();
		}
		if (wantToDisconnect(input)) {
			this.serverAccessThread.interrupt();
			try {
				this.socket.close();
			} catch (IOException e) {
				System.err.println("An IOException with closing the socket occured.");
				e.printStackTrace();
			}
		} else {
			this.serverThread.addNextMessage(input);
		}
	}

	public void startClient() {
		try {
			socket = new Socket(serverHost, serverPort);
			Thread.sleep(1000);
			System.out.println("Connected to server successfully.");

			this.serverThread = new ServerThread(socket);
			this.serverAccessThread = new Thread(this.serverThread);
			this.serverAccessThread.start();
			while (serverAccessThread.isAlive() && !serverAccessThread.isInterrupted()) {
				Thread.sleep(500);
				if (serverThread.getUsername().equals("")) {
					showMenuForUnlogged();
				} else {
					if (serverThread.shouldLoadChronology()) {
						if (serverThread.isInRoom()) {
							String input = SCANNER.nextLine();
							serverThread.addNextMessage(input);
						} else {
							serverThread.setInRoom(true);
							serverThread.addNextMessage(serverThread.getUsername() + " joined the chat room.");
						}
					} else {
						showMenuForLogged();
					}
				}
			}
		} catch (IOException ex) {
			System.err.println("Connection error.");
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			System.err.println("An InterruptedException occured.");
		}
	}
}