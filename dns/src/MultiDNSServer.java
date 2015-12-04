
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.TreeMap;

public class MultiDNSServer {

	public static class ClientRequest implements Runnable {

		private Socket connectionSocket;

		public ClientRequest(Socket connectionSocket) {
			this.connectionSocket = connectionSocket;
		}

		public void run() {
			String clientCommand;
			String[] commandArgs;
			String serverResponse = "";

			System.out.println("A connection has been made.");
			try {
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				while (!connectionSocket.isClosed()) {
					clientCommand = inFromClient.readLine();
					commandArgs = clientCommand.split(" ");
					try {
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
						default:
							serverResponse = "400 Bad Request\nIncorrect command.";
							break;
						}
					}
					catch (ArrayIndexOutOfBoundsException e) {
						serverResponse = "400 Bad Request\nMissing arguments.";
					}
					if (!clientCommand.equals("exit")) {
						outToClient.writeInt(serverResponse.length());
						outToClient.writeBytes(serverResponse);
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Connection has been closed.");
		}

	}

	private static TreeMap<String, TreeMap<String, String>> database;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, ClassNotFoundException {

		File f = new File("database");
		if (f.exists()) {
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			database = (TreeMap<String, TreeMap<String, String>>) ois.readObject();
			ois.close();
			fis.close();
		}
		else {
			database = new TreeMap<String, TreeMap<String, String>>();
		}
		final ServerSocket serverSocket = new ServerSocket(0);

		System.out.println("Server has been started on port " + serverSocket.getLocalPort() + ".");

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

		while(true) {
			try {
				Socket connectionSocket = serverSocket.accept();
				new Thread(new ClientRequest(connectionSocket)).start();
			}
			catch (SocketException e) {
			}
		}
	}

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
					records += "\nName: " + name + "\n"
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
