import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public final class EchoClient {

	public static void main(String[] args) throws Exception { 
		try (Socket socket = new Socket("localhost", 22222)) { 
			BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.println(socketIn.readLine());
			
			PrintStream out = new PrintStream(socket.getOutputStream());
			
			while(true) { 
				System.out.print("Client> ");
				String input = userIn.readLine();
				
				if (input.equalsIgnoreCase("quit")) 
					break;
				else { 
					out.println(input);
					out.flush();
					System.out.println(socketIn.readLine());
				}
			}
			System.out.println("Exiting...");
			socketIn.close();
			out.close();
		}
	}
}
