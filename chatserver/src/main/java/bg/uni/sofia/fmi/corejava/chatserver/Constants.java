package bg.uni.sofia.fmi.corejava.chatserver;

import java.io.File;

public interface Constants {

	public static final String LINE_SEPARATOR = System.lineSeparator();

	public static final String UNLOGGED_OPTIONS_MENU = String.join(LINE_SEPARATOR, "register <username> <password>",
			"login <username> <password>", "disconnect");

	public static final String LOGGED_OPTIONS_MENU = String.join(LINE_SEPARATOR, "send <username> <message>",
			"send_file <username> <file_location>", "create_room <room_name>", "delete_room <room_name>",
			"join_room <room_name>", "leave_room <room_name>", "list_users", "list_rooms", "list_users <room>",
			"disconnect");

	public static final String FILE_SEPARATOR = File.separator;

	public static final String NOT_CONNECTED_OPTIONS_MENU = String.join(LINE_SEPARATOR,
			"Welcome to Chat Server! Enter:", "connect <host> <port>", "to connect.");

	public static final String FILE_OPTIONS_MENU = String.join(LINE_SEPARATOR, "Enter:", "accept <file location>", "or",
			"decline");

	public static final String PATH_USERS_FILE = "src/resource/ser/users.ser";

	public static final String PATH_ROOMS_FILE = "src/resource/ser/rooms.ser";

	public static final String PATH_ROOMS_FOLDER = "src/resource/txt";

	public static final String PATH_TEST_USERS_FILE = "src/resource/ser/usersTests.ser";

	public static final String PATH_TEST_ROOMS_FILE = "src/resource/ser/roomsTests.ser";

	public static final String PATH_TEST_ROOMS_FOLDER = "src/resource/txtTests";

	public static final String WRONG_NUMBER_OF_ARGS = "Wrong number of arguments.";

	public static final String INVALID_INPUT = "Invalid input.";

	public static final String WRONG_INPUT_PROMPT = "Wrong input, enter again.";

}
