import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;


public class ChatClient {

	public static void main(String[] args) throws Exception {
		String host = "76.91.123.97";
		int port = 38002;
		try (Socket socket = new Socket(host, port)) { 
			BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
			PrintStream out = new PrintStream(socket.getOutputStream());
			System.out.println("Connected to " + host + ":" + port + "!\n");
			
			Runnable listen = () -> { 
				try {
					String incoming = "";
					while((incoming = socketIn.readLine()) != null) { 
						System.out.println(incoming + "\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			Thread listener = new Thread(listen);
			listener.start();	//reads incoming messages from the server
			
			
			System.out.println("Enter username");
			String userName = userIn.readLine();
			out.println(userName);
			out.flush();
			
			String message = "";
			while(socket.isConnected()) { 	//sends messages to the server, listener echos back
				message = userIn.readLine();
				out.println(message);
				out.flush();
			}
			socketIn.close();
			userIn.close();
		}
		System.exit(0);
	}//main

}
