import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * Client for a single name server
 */

public class DNSClient {

	private static final String helpMessage = "\nhelp: prints a list of supported commands\n"
			+ "\nput [name] [value] [type]: adds name record of the specified type "
			+ "to the database or updates the record with the new value\n"
			+ "\nget [name] [type]: returns the value of the record with the "
			+ "provided name and type\n"
			+ "\ndel [name] [type]: removes the record with the provided "
			+ "name and type from the database\n"
			+ "\nbrowse: displays the name and type of all current name records "
			+ "in the database\n"
			+ "\nexit: terminates connection with the server and exits the program\n";

	// socket timeout in ms
	private static final int SO_TIMEOUT = 10000;

	public static void main(String[] args) throws IOException {

		String clientCommand;
		byte[] serverResponse;
		int responseLength;
		try {
			BufferedReader inFromUser = new BufferedReader(
					new InputStreamReader(System.in));
			Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1]));
			clientSocket.setSoTimeout(SO_TIMEOUT);
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			DataInputStream inFromServer = new DataInputStream(
					clientSocket.getInputStream());
			System.out.println("Connected to server.");

			while (!clientSocket.isClosed()) {
				System.out.println("Enter a command:");
				clientCommand = inFromUser.readLine();
				if (clientCommand != null) {
					if (clientCommand.equals("exit")) {
						outToServer.writeBytes(clientCommand + "\n");
						inFromServer.close();
						outToServer.close();
						inFromUser.close();
						clientSocket.close();
					} else if (clientCommand.equals("help")) {
						System.out.println(helpMessage);
					} else {
						// send request
						outToServer.writeBytes(clientCommand + "\n");
						// get length of response
						responseLength = inFromServer.readInt();
						serverResponse = new byte[responseLength];
						// receive response
						inFromServer.readFully(serverResponse);
						System.out.println(new String(serverResponse));
					}
				}
			}
		} catch (UnknownHostException e) {
			System.out
					.println("Cannot connect to server with the provided hostname and port.");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Missing hostname or port.");
		}
	}

}
