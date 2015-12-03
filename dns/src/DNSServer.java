
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
import java.util.TreeMap;

public class DNSServer {

	public static class ClientRequest implements Runnable {

		private Socket connectionSocket;

		public ClientRequest(Socket connectionSocket) {
			this.connectionSocket = connectionSocket;
		}

		public void run() {
			String clientCommand;
			String serverResponse = "";

			System.out.println("A connection has been made.");
			try {
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				while (!connectionSocket.isClosed()) {
					clientCommand = inFromClient.readLine();
					switch (clientCommand) {
					case "put":
						break;
					case "get":
						break;
					case "del":
						break;
					case "browse":
						break;
					case "exit":
						connectionSocket.close();
						break;
					default:
						break;
					}
					if (!clientCommand.equals("exit")) {
						outToClient.writeBytes(serverResponse + "\n");
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Connection has been closed.");
		}

	}

	private static TreeMap<String, TreeMap<String, String>> database;

	public static void main(String[] args) throws IOException {

		Thread serverThread = new Thread() {
			@SuppressWarnings("unchecked")
			public void run() {
				try {
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
					
					while(!Thread.currentThread().isInterrupted()) {
						try {
							Socket connectionSocket = serverSocket.accept();
							new Thread(new ClientRequest(connectionSocket)).start();
						}
						catch (IOException e) {
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		};
		serverThread.start();

	}

}
