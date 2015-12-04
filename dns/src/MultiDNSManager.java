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

public class MultiDNSManager {

	private static List<Process> processes = new ArrayList<Process>();
	private static HashMap<String, String> map = new HashMap<String, String>();
	
	private static final int MANAGER_PORT = 14260;
	
	private static class ClientConnection implements Runnable {

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
				final BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				final DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							outToClient.close();
							inFromClient.close();
							connectionSocket.close();
						}
						catch (IOException e) {
						}
					}
				});
				
				while (!connectionSocket.isClosed()) {
					clientCommand = inFromClient.readLine();
					//split user client commands into an array
					commandArgs = clientCommand.split(" ");
					try {
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
						//if first argument from user isn't recognized
						default:
							serverResponse = "400 Bad Request\nIncorrect command.";
							break;
						}
					}
					//error if user enters wrong parameters
					catch (ArrayIndexOutOfBoundsException e) {
						serverResponse = "400 Bad Request\nMissing arguments.";
					}
					//write response to client, if they didn't choose exit command
					if (!clientCommand.equals("exit")) {
						outToClient.writeInt(serverResponse.length());
						outToClient.writeBytes(serverResponse);
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
		}
		else {

			final ServerSocket serverSocket = new ServerSocket(MANAGER_PORT);
			serverSocket.getInetAddress();
			System.out.println("Manager has been started on " + 
					InetAddress.getLocalHost().getHostName() + ":" + serverSocket.getLocalPort() + ".");

			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);

			String type;
			Process process;
			BufferedReader reader;
			String address;

			while ((type = br.readLine()) != null) {
				type = type.trim();
				if (!type.equals("")) {
					process = Runtime.getRuntime().exec("java MultiDNSServer " + type);
					processes.add(process);
					reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					address = reader.readLine();
					map.put(type, address);
					System.out.println("Server for record type " + type + " has been started on " 
							+ address + ".");
					reader.close();
				}
			}
			br.close();
			fr.close();

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {

					try {
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
			while (true) {
				try {
					Socket connectionSocket = serverSocket.accept();
					new Thread(new ClientConnection(connectionSocket)).start();
				}
				catch (SocketException e) {
				}
			}
		}
	}

	public static String server(String type) {
		if (map.containsKey(type)) {
			return "200 OK\n" + map.get(type);
		}
		else {
			return "404 Not Found\nType not found.";
		}
	}
	
	
}
