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
			else {
				serverResponse = inFromServer.readLine();
				System.out.println(serverResponse);
			}	
			
		}
	}
}
