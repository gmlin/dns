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

/*
 * Server that manages a database containing name records of a single type
 */

public class MultiDNSServer {

	// contains name records of a single type, maps names to values
	private static TreeMap<String, String> database;

	// helper class that handles a single client's requests
	private static class ClientConnection implements Runnable {

		// socket that connects client to the server
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
							case "put":
								serverResponse = put(commandArgs[1],
										commandArgs[2]);
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
			}
			System.out.println("A connection has been closed.");
		}

	}

	@SuppressWarnings("unchecked")
	public static void main(final String[] args) throws IOException,
			ClassNotFoundException {

		// record type is name of file
		File f = new File(args[0]);

		// deserialize database from file if the file exists
		if (f.exists()) {
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			database = (TreeMap<String, String>) ois.readObject();
			ois.close();
			fis.close();
		} else {
			database = new TreeMap<String, String>();
		}
		final ServerSocket serverSocket = new ServerSocket(0);
		serverSocket.getInetAddress();

		// prints hostname and port to stdout for manager to read
		System.out.println(InetAddress.getLocalHost().getHostName() + ":"
				+ serverSocket.getLocalPort());

		// run when program ends
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					serverSocket.close();
					System.out.println("Server has been stopped.");
					FileOutputStream fos = new FileOutputStream(args[0]);
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					// serializes database to file
					oos.writeObject(database);
					oos.close();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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

	// creates or update record of specified name and value
	public static String put(String name, String value) {
		// locks database for duration of operation for concurrency
		synchronized (database) {
			if (database.containsKey(name)) {
				database.put(name, value);
				return "The record has been updated.";
			} else {
				database.put(name, value);
				return "The record has been added.";
			}
		}
	}

	// get record of the specified name
	public static String get(String name) {
		// locks database for duration of operation for concurrency
		synchronized (database) {
			if (!database.containsKey(name)) {
				return "404 Not Found\n" + "The record for " + name
						+ " cannot be found.";
			} else {
				return "200 OK\n" + database.get(name);
			}
		}
	}

	// delete record of the specified name
	public static String del(String name) {
		// locks database for duration of operation for concurrency
		synchronized (database) {
			if (!database.containsKey(name)) {
				return "404 Not Found\n" + "The record for " + name
						+ " cannot be found.";
			} else {
				database.remove(name);
				return "200 OK\nThe record has been removed.";
			}
		}
	}

	// gets the name of all records
	public static String browse() {
		// locks database for duration of operation for concurrency
		synchronized (database) {
			String records = "";
			for (String name : database.keySet()) {
				records += "Name: " + name + "\n";
			}
			if (records.equals("")) {
				return "200 OK\nThe database is empty.";
			} else {
				return "200 OK\n" + records;
			}
		}
	}

}
