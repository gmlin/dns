
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

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
					case "help":
						break;
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
					outToClient.writeBytes(serverResponse + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Connection has been closed.");
		}

	}

	public static void main(String[] args) throws IOException {



		Thread serverThread = new Thread() {
			public void run() {
				try {
					final ServerSocket serverSocket = new ServerSocket(0);

					System.out.println("Server has been started on port " + serverSocket.getLocalPort() + ".");

					Runtime.getRuntime().addShutdownHook(new Thread() {
						public void run() {
							try {
								serverSocket.close();
								System.out.println("Server has been stopped.");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});

					while(!Thread.currentThread().isInterrupted()) {
						Socket connectionSocket = serverSocket.accept();
						new Thread(new ClientRequest(connectionSocket)).start();
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		serverThread.start();

	}

}
