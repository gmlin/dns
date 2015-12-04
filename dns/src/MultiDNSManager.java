import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Manages servers that contain name records of a single type and helps
 * clients connect to the server of a specified type
 */

public class MultiDNSManager {

	// list of the server processes started by the manager
	private static List<Process> processes = new ArrayList<Process>();

	// maps record type to server address information
	private static HashMap<String, String> map = new HashMap<String, String>();

	// well-defined TCP port this program runs on
	private static final int MANAGER_PORT = 14260;

	// helper class that handles a single client's requests
	private static class ClientConnection implements Runnable {

		// socket that connects client to the manager
		private Socket connectionSocket;

		public ClientConnection(Socket connectionSocket) {
			this.connectionSocket = connectionSocket;
		}

		public void run() {
			String clientCommand;
			String[] commandArgs;
			String serverResponse = "";

			System.out.println("A connection has been made.");
			try {
				final BufferedReader inFromClient = new BufferedReader(
						new InputStreamReader(connectionSocket.getInputStream()));
				final DataOutputStream outToClient = new DataOutputStream(
						connectionSocket.getOutputStream());

				// run when thread ends
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							outToClient.close();
							inFromClient.close();
							connectionSocket.close();
						} catch (IOException e) {
						}
					}
				});

				while (!connectionSocket.isClosed()) {
					clientCommand = inFromClient.readLine();
					// separates arguments by whitespace
					if (clientCommand != null) {
						commandArgs = clientCommand.split(" ");
						try {
							// performs actions based on client's first argument
							switch (commandArgs[0]) {
							case "connect":
								serverResponse = server(commandArgs[1]);
								break;
							case "type":
								serverResponse = server(commandArgs[1]);
								break;
							case "exit":
								outToClient.close();
								inFromClient.close();
								connectionSocket.close();
								break;
							// if first argument isn't recognized
							default:
								serverResponse = "400 Bad Request\nIncorrect command.";
								break;
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							serverResponse = "400 Bad Request\nMissing arguments.";
						}
						if (!clientCommand.equals("exit")) {
							// send length of response
							outToClient.writeInt(serverResponse.length());
							// send response
							outToClient.writeBytes(serverResponse);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("A connection has been closed.");
		}

	}

	public static void main(String[] args) throws IOException {

		File f = new File("manager.in");
		if (!f.exists()) {
			System.out.println("manager.in does not exist in this directory.");
		} else {

			final ServerSocket serverSocket = new ServerSocket(MANAGER_PORT);
			System.out.println("Manager has been started on "
					+ InetAddress.getLocalHost().getHostName() + ":"
					+ serverSocket.getLocalPort() + ".");

			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);

			String type;
			Process process;
			BufferedReader reader;
			String address;

			// starts new server process for each line in manager.in
			while ((type = br.readLine()) != null) {
				type = type.trim();
				if (!type.equals("")) {
					process = Runtime.getRuntime().exec(
							"java MultiDNSServer " + type);
					processes.add(process);
					reader = new BufferedReader(new InputStreamReader(
							process.getInputStream()));
					address = reader.readLine();
					map.put(type, address);
					System.out.println("Server for record type " + type
							+ " has been started on " + address + ".");
					reader.close();
				}
			}
			br.close();
			fr.close();

			// run when program ends
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {

					try {
						// end each server process
						for (Process process : processes) {
							process.destroy();
							System.out.println("Server has been stopped.");
						}
						serverSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("Manager has been stopped.");
				}
			});

			// creates a new socket and thread for each client
			while (true) {
				try {
					Socket connectionSocket = serverSocket.accept();
					new Thread(new ClientConnection(connectionSocket)).start();
				} catch (SocketException e) {
				}
			}
		}
	}

	// get the server address information for the specified type
	public static String server(String type) {
		if (map.containsKey(type)) {
			return "200 OK\n" + map.get(type);
		} else {
			return "404 Not Found\nType not found.";
		}
	}

}
