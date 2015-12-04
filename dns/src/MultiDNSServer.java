
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

public class MultiDNSServer {
	
	private static TreeMap<String, String> database;

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
					commandArgs = clientCommand.split(" ");
					try {
						switch (commandArgs[0]) {
						case "put":
							serverResponse = put(commandArgs[1], commandArgs[2]);
							break;
						case "get":
							serverResponse = get(commandArgs[1]);
							break;
						case "del":
							serverResponse = del(commandArgs[1]);
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
			}
			System.out.println("A connection has been closed.");
		}

	}

	@SuppressWarnings("unchecked")
	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		File f = new File(args[0]);
		if (f.exists()) {
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			database = (TreeMap<String, String>) ois.readObject();
			ois.close();
			fis.close();
		}
		else {
			database = new TreeMap<String, String>();
		}
		final ServerSocket serverSocket = new ServerSocket(0);
		serverSocket.getInetAddress();
		System.out.println(InetAddress.getLocalHost().getHostName() + ":" + serverSocket.getLocalPort());

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					serverSocket.close();
					System.out.println("Server has been stopped.");
					FileOutputStream fos = new FileOutputStream(args[0]);
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
				new Thread(new ClientConnection(connectionSocket)).start();
			}
			catch (SocketException e) {
			}
		}
	}

	public static String put(String name, String value) {
		synchronized(database) {
			if (database.containsKey(name)) {
				database.put(name, value);
				return "The record has been updated.";
			}
			else {
				database.put(name, value);
				return "The record has been added.";
			}
		}
	}

	public static String get(String name) {
		synchronized(database) {
			if (!database.containsKey(name)) {
				return "404 Not Found\n"
						+ "The record for " + name + " cannot be found.";
			}
			else {
				return "200 OK\n" + database.get(name);
			}
		}
	}

	public static String del(String name) {
		synchronized(database) {
			if (!database.containsKey(name)) {
				return "404 Not Found\n"
						+ "The record for " + name + " cannot be found.";
			}
			else {
				database.remove(name);
				return "200 OK\nThe record has been removed.";
			}
		}
	}

	public static String browse() {
		synchronized(database) {
			String records = "";
			for (String name : database.keySet()) {
				records += "Name: " + name + "\n";
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
