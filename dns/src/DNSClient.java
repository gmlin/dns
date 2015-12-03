import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;



public class DNSClient {

	public static void main(String[] args) throws IOException {
		
		String clientCommand;
		String serverResponse;
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1]));
		clientSocket.setSoTimeout(10000);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		while (!clientSocket.isClosed()) {
			
			clientCommand = inFromUser.readLine();
			
			outToServer.writeBytes(clientCommand + "\n");
			if (clientCommand.equals("exit")) {
				inFromUser.close();
				clientSocket.close();
			}
			else if (clientCommand.equals("help")) {
				printHelp();
			}
			else {
				serverResponse = inFromServer.readLine();
				System.out.println(serverResponse);
			}	
			
		}
	}

	private static void printHelp() {
		String helpMessage;
		helpMessage = "\nhelp: prints a list of supported commands\n"
		+ "\nput [name] [value] [type]: adds name record to the database "
		+ "or updates the record with the new value\n"
		+ "\nget [name] [type]: returns the value of the record with the "
		+ "provided name and type\n"
		+ "\ndel [name] [type]: removes the record with the provided "
		+ "name and type from the database\n"
		+ "\nbrowse: displays the name and type of all current name records "
		+ "in the database\n"
		+ "\nexit: terminates connection with the server and exits the program\n";
		System.out.println(helpMessage);
	}
}
