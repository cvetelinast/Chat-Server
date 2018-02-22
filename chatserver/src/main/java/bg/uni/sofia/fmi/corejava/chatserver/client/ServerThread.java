package bg.uni.sofia.fmi.corejava.chatserver.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.*;

public class ServerThread implements Runnable {

	private static final Scanner SCANNER = new Scanner(System.in);

	private Socket socket;

	PrintWriter serverOut;

	private String username;

	private final LinkedList<String> messagesToSend;

	private boolean hasMessages;

	private boolean isInRoom;

	private boolean shouldLoadChronology;

	private boolean acceptFile;

	private String directoryToAcceptFile;

	public ServerThread(Socket socket) {
		this.socket = socket;
		this.serverOut = null;
		messagesToSend = new LinkedList<String>();
		this.hasMessages = false;
		this.username = "";
		this.isInRoom = false;
		this.shouldLoadChronology = false;
		this.acceptFile = false;
		this.directoryToAcceptFile = "";
	}

	public boolean acceptFile() {
		return acceptFile;
	}

	public void setAcceptFile(boolean acceptFile) {
		this.acceptFile = acceptFile;
	}

	public boolean shouldLoadChronology() {
		return shouldLoadChronology;
	}

	public boolean isInRoom() {
		return isInRoom;
	}

	public void setInRoom(boolean isInRoom) {
		this.isInRoom = isInRoom;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void addNextMessage(String message) {
		synchronized (messagesToSend) {
			hasMessages = true;
			messagesToSend.push(message);
		}
	}

	public String acceptOrDeclineFile(String answer) {
		if (answer.startsWith("accept")) {
			String[] splitCommand = answer.split(" ");
			if (splitCommand.length == 2) {
				File directory = new File(Paths.get(splitCommand[1]).toString());
				if (!directory.exists()) {
					try {
						directory.createNewFile();
					} catch (IOException e) {
						System.err.println("An IOException occured.");
						e.printStackTrace();
					}
				}
				this.acceptFile = true;
				this.directoryToAcceptFile = directory.toString();
				return "The file will be saved.";
			}
			return "Wrong file location.";
		} else if (answer.equals("decline")) {
			return "The file was declined.";
		}
		return INVALID_INPUT;
	}

	private String menuForWaitingFile() {
		System.out.println(FILE_OPTIONS_MENU);
		String input = "";
		while (input.trim().equals("") && !input.startsWith("accept") && !input.startsWith("decline")) {
			System.out.println(WRONG_INPUT_PROMPT);
			input = SCANNER.nextLine();
		}
		input = "accept D:\\Cveti_Documents\\Test\\save\\picture.jpg";
		return acceptOrDeclineFile(input);
	}

	private void receiveFile(InputStream serverInStream) {
		InputStream in = serverInStream;
		OutputStream out = null;
		try {
			out = new FileOutputStream(this.directoryToAcceptFile);
		} catch (FileNotFoundException ex) {
			System.out.println("File not found. ");
		}
		byte[] bytes = new byte[16 * 1024];
		int count;
		try {
			while ((count = in.read(bytes)) > 0) {
				out.write(bytes, 0, count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.acceptFile = false;
	}

	@Override
	public void run() {
		try {
			this.serverOut = new PrintWriter(socket.getOutputStream(), false);
			InputStream serverInStream = socket.getInputStream();
			Scanner serverIn = new Scanner(serverInStream);
			while (!socket.isClosed()) {
				if (hasMessages) {
					String nextSend = "";
					synchronized (messagesToSend) {
						nextSend = messagesToSend.pop();
						hasMessages = !messagesToSend.isEmpty();
					}
					serverOut.println(nextSend);
					serverOut.flush();
				}
				if (serverInStream.available() > 0) {
					if (serverIn.hasNextLine()) {
						String reply = serverIn.nextLine();
						System.out.println(reply);
						if (reply.startsWith("Login with username ")) {
							this.setUsername(reply.split(" ")[3]);
						} else if (reply.contains("Sending a file")) {
							System.out.println(this.menuForWaitingFile());
							if (this.acceptFile) {
								receiveFile(serverInStream);
							}
							serverInStream = socket.getInputStream();
						} else if (reply.contains("joined successfully")) {
							this.shouldLoadChronology = true;
						} else if (reply.contains(this.username + " left the room.")) {
							this.shouldLoadChronology = false;
							this.isInRoom = false;
						}
					}
				}
			}
		} catch (IOException ex) {
			System.err.println("An IOException occured.");
			ex.printStackTrace();
		}

	}
}