import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class EchoServer {

	public static void main(String[] args) throws Exception { 
		try (ServerSocket serverSocket = new ServerSocket(22222)) { 
			while(true) { 
				try (Socket socket = serverSocket.accept()) { 
					BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String address = socket.getInetAddress().getHostAddress();
					System.out.printf("Client connected: %s%n", address);
					
					PrintStream out = new PrintStream(socket.getOutputStream());
					out.printf("Client Connected: %s!%n", address);
					
					String in;
					while((in = br.readLine()) != null) { 
						out.println("Server> " + in);
						out.flush();
					}
				}
			}
		}
	}
}