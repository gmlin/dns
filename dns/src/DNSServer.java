
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.TreeMap;

public class DNSServer {

	//database using a TreeMap within a TreeMap in the form of (type, (name, value)), all string values
		private static TreeMap<String, TreeMap<String, String>> database;

	
	//helper class ClientConnection
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
				
				//loop until user client enters exit command
				while (!connectionSocket.isClosed()) {
					clientCommand = inFromClient.readLine();
					//split user client commands into an array
					commandArgs = clientCommand.split(" ");
					try {
						//set server response depending on user client command (unless exit is called)
						switch (commandArgs[0]) {
						case "put":
							serverResponse = put(commandArgs[1], commandArgs[2], commandArgs[3]);
							break;
						case "get":
							serverResponse = get(commandArgs[1], commandArgs[2]);
							break;
						case "del":
							serverResponse = del(commandArgs[1], commandArgs[2]);
							break;
						case "browse":
							serverResponse = browse();
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
			}
			System.out.println("A connection has been closed.");
		}

	}
	//end of ClientRequest code

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		try {
			File f = new File("database");
			//get database TreeMap from database file if file exists
			if (f.exists()) {
				FileInputStream fis = new FileInputStream(f);
				ObjectInputStream ois = new ObjectInputStream(fis);
				database = (TreeMap<String, TreeMap<String, String>>) ois.readObject();
				ois.close();
				fis.close();
			}
			//otherwise initialize empty database TreeMap
			else {
				database = new TreeMap<String, TreeMap<String, String>>();
			}
			final ServerSocket serverSocket = new ServerSocket(0);

			serverSocket.getInetAddress();
			System.out.println("Server has been started on " + 
					InetAddress.getLocalHost().getHostName() + ":" + serverSocket.getLocalPort() + ".");

			//save database TreeMap onto database file when server closes (ctrl-c on command line)
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						serverSocket.close();
						System.out.println("Server has been stopped.");
						FileOutputStream fos = new FileOutputStream("database");
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(database);
						oos.close();
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});

			//while server is running, create a new thread for each client, requests processed in helper class
			while(true) {
				try {
					Socket connectionSocket = serverSocket.accept();
					new Thread(new ClientConnection(connectionSocket)).start();
				}
				catch (SocketException e) {
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	//synchronized(database) in all following command methods to prevent concurrent access conflicts
	
	public static String put(String name, String value, String type) {
		synchronized(database) {
			if (!database.containsKey(type)) {
				database.put(type, new TreeMap<String, String>());
			}
			if (database.get(type).containsKey(name)) {
				database.get(type).put(name, value);
				return "The record has been updated.";
			}
			else {
				database.get(type).put(name, value);
				return "The record has been added.";
			}
		}
	}

	public static String get(String name, String type) {
		synchronized(database) {
			if (!database.containsKey(type) || !database.get(type).containsKey(name)) {
				return "404 Not Found\n"
						+ "The record of type " + type + 
						" and name " + name + " cannot be found.";
			}
			else {
				return "200 OK\n" + database.get(type).get(name);
			}
		}
	}

	public static String del(String name, String type) {
		synchronized(database) {
			if (!database.containsKey(type) || !database.get(type).containsKey(name)) {
				return "404 Not Found\n"
						+ "The record of type " + type + 
						" and name " + name + " cannot be found.";
			}
			else {
				database.get(type).remove(name);
				return "200 OK\nThe record has been removed.";
			}
		}
	}

	public static String browse() {
		synchronized(database) {
			String records = "";
			for (String type : database.keySet()) {
				for (String name : database.get(type).keySet()) {
					records += "Name: " + name + "\t"
							+ "Type: " + type + "\n";
				}
			}
			if (records.equals("")) {
				return "200 OK\nThe database is empty.";
			}
			else {
				return "200 OK\n" + records;
			}
		}
	}

}
