import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MultiDNSManager {

	private static List<Process> processes = new ArrayList<Process>();
	private static HashMap<String, String> map = new HashMap<String, String>();

	public static void main(String[] args) throws IOException {

		File f = new File("manager.in");
		if (!f.exists()) {
			System.out.println("manager.in does not exist in this directory.");
		}
		else {

			final ServerSocket serverSocket = new ServerSocket(0);
			System.out.println("Manager has been started on " + 
					serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + ".");

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
			while (true);
		}
	}
}
