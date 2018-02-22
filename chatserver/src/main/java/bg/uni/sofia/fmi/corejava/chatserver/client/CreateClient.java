package bg.uni.sofia.fmi.corejava.chatserver.client;

import static bg.uni.sofia.fmi.corejava.chatserver.Constants.NOT_CONNECTED_OPTIONS_MENU;
import static bg.uni.sofia.fmi.corejava.chatserver.Constants.WRONG_INPUT_PROMPT;
import java.util.Scanner;

public class CreateClient {

	private static final Scanner SCANNER = new Scanner(System.in);

	private static String promptForConnection() {
		System.out.println(NOT_CONNECTED_OPTIONS_MENU);
		String input = SCANNER.nextLine();
		while (input.trim().equals("") || !input.startsWith("connect")) {
			System.out.println(WRONG_INPUT_PROMPT);
			input = SCANNER.nextLine();
		}
		return input;
	}

	public static void main(String[] args) {
		String connect = promptForConnection();
		String[] separateInput = connect.split(" ");
		if (separateInput.length == 3) {
			Client client = new Client(separateInput[1], Integer.parseInt(separateInput[2]));
			client.startClient();
		}
	}

}
