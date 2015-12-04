import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;



public class MultiDNSClient {

	private static final String helpMessage = "\nhelp: prints a list of supported commands\n"
			+ "\nput [name] [value]: adds name record to the database "
			+ "or updates the record with the new value\n"
			+ "\nget [name]: returns the value of the record with the "
			+ "provided name\n"
			+ "\ndel [name]: removes the record with the provided "
			+ "name from the database\n"
			+ "\nbrowse: displays the name of all current name records "
			+ "in the database\n"
			+ "\nexit: terminates connection with the manager and server and exits the program\n";

	public static void main(String[] args) throws IOException {

		String clientCommand;
		byte[] serverResponse;
		int responseLength;
		try {
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1]));
			clientSocket.setSoTimeout(10000);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
			while (!clientSocket.isClosed()) {

				clientCommand = inFromUser.readLine();

				if (clientCommand.equals("exit")) {
					outToServer.writeBytes(clientCommand + "\n");
					inFromServer.close();
					outToServer.close();
					inFromUser.close();
					clientSocket.close();;
				}
				else if (clientCommand.equals("help")) {
					System.out.println(helpMessage);
				}
				else {
					outToServer.writeBytes(clientCommand + "\n");
					responseLength = inFromServer.readInt();
					serverResponse = new byte[responseLength];
					inFromServer.readFully(serverResponse);
					System.out.println(new String(serverResponse));
				}
			}	
		}
		catch (UnknownHostException e) {
			System.out.println("Cannot connect to server with the provided hostname and port.");
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Missing hostname or port.");
		}
	}

}
