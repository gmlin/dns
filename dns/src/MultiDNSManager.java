import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;


public class MultiDNSManager {

	private static List<Process> processes = new ArrayList<Process>();
	
	public static void main(String[] args) throws IOException {

		File f = new File("manager.in");
		if (!f.exists()) {
			System.out.println("manager.in does not exist in this directory.");
		}
		else {
			System.out.println("Manager has been started.");
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);

			String line;
			
			while ((line = br.readLine().trim()) != null) {
				if (!line.equals("")) {
					processes.add(Runtime.getRuntime().exec("java MultiDNSServer " + line));
					System.out.println("Server for record type " + line + " has been started.");
				}
			}
			br.close();
			fr.close();
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					for (Process process : processes) {
						process.destroy();
						System.out.println("Server has been stopped.");
					}
					System.out.println("Manager has been stopped.");
				}
			});
		}
	}
}
