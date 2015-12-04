import java.io.BufferedInputStream;
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

		String type;
		String[] address;
		String clientCommand;
		Socket serverSocket = null;
		byte[] serverResponse;
		int responseLength;
		String[] lines;
		
		try {
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			Socket managerSocket = new Socket(args[0], Integer.parseInt(args[1]));
			managerSocket.setSoTimeout(10000);
			DataOutputStream outToManager = new DataOutputStream(managerSocket.getOutputStream());
			DataInputStream inFromManager = new DataInputStream(managerSocket.getInputStream());
			DataOutputStream outToServer = null;
			DataInputStream inFromServer = null;
			while (!managerSocket.isClosed()) {
				if (serverSocket == null) {
					System.out.println("Enter the record type that you are interested in:");
					type = inFromUser.readLine();
					outToManager.writeBytes("server " + type + "\n");
					responseLength = inFromManager.readInt();
					serverResponse = new byte[responseLength];
					inFromManager.readFully(serverResponse);
					lines = new String(serverResponse).split("\n");
					System.out.println(lines[0]);
					address = lines[1].split(":");
					serverSocket = new Socket(address[0], Integer.parseInt(address[1]));
					serverSocket.setSoTimeout(10000);
					outToServer = new DataOutputStream(serverSocket.getOutputStream());
					inFromServer = new DataInputStream(serverSocket.getInputStream());
					System.out.println("Connected to server for record type " + type + ".");
				}
				else {
					System.out.println("Enter a command:");
					
					clientCommand = inFromUser.readLine();
	
					if (clientCommand.equals("exit")) {
						outToServer.writeBytes(clientCommand + "\n");
						inFromServer.close();
						outToServer.close();
						outToManager.writeBytes(clientCommand + "\n");
						inFromManager.close();
						outToManager.close();
						inFromUser.close();
						serverSocket.close();
						managerSocket.close();
						
					}
					else if (clientCommand.equals("help")) {
						System.out.println(helpMessage);
					}
					else {
						outToServer.writeBytes(clientCommand + "\n");
						//return server response
						responseLength = inFromServer.readInt();
						serverResponse = new byte[responseLength];
						inFromServer.readFully(serverResponse);
						System.out.println(new String(serverResponse));
					}
				}
			}
				
			/*
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
			*/
		}
		catch (UnknownHostException e) {
			System.out.println("Cannot connect to manager with the provided hostname and port.");
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Missing hostname or port.");
		}
	}

}
