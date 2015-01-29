import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;


public class Ipv4Client {
	public static void main(String[] args) throws IOException, InterruptedException {
		String host = "76.91.123.97";
		int port = 38003;
		short DATASIZE = 2;
		int counter = 1;
		
		byte version = 0b0100; //version 4 (4 bits)
		byte ihl = 0b0101; //length of header (5) (4 bits)
		
		byte tos = 0b0; //dont implement (8 bits)
		
		short length = 0b10100; //size of header (5) + data size [2 bytes]
		
		byte ident[] = new byte[2]; //dont implement (16 bits)
			ident[0] = 0b0;
			ident[1] = 0b0;
			
		byte flags = 0b010; //dont fragment  (3 bits)
		byte fragOffset[] = new byte[2]; //13 bits, dont implement
			fragOffset[0] = 0b0;
			fragOffset[1] = 0b0;
			
		byte ttl = 0b00110010; //time to live - assume 50 (1 byte)
		
		byte protocol = 0b0110; //TCP protocol (6) [1 byte]
		
		byte source[] = new byte[4];
			source[0] = 0b01111111; //127
			source[1] = 0b0;		//0
			source[2] = 0b01; //1
			source[3] = 0b01; //1
			
		byte dest[] = new byte[4];
			dest[0] = 0b01001100; //76
			dest[1] = 0b01011011; //91
			dest[2] = 0b01111011; //123
			dest[3] = 0b01100001; //97
		
		try (Socket socket = new Socket(host, port)) { 
			BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
			
			Scanner kb = new Scanner(System.in);
			while(counter < 13) { 
				kb.nextLine();
				byte[] packet = new byte[20 + DATASIZE];
				byte data[] = new byte[DATASIZE];
				new Random().nextBytes(data);
				
				packet[0] = version;
				packet[0] <<= 4;
				packet[0] |= ihl;
				
				packet[1] = tos;
				
				short tempshort = (short) (length + DATASIZE);
				byte b2 = (byte) ((tempshort >>> 8) & 0xFF);
				byte b1 = (byte) (tempshort & 0xFF); 

				packet[2] = b2;
				packet[3] = b1; //adjust based on data size
				
				packet[4] = ident[1];
				packet[5] = ident[0];
				
				packet[6] = flags;
				packet[6] <<= 5;
				
				packet[6] |= fragOffset[0];
				packet[7] = fragOffset[1];
				
				packet[8] = ttl;
				
				packet[9] = protocol;
				
				for(int i=12, j=0; i<16; ++i, ++j) 
					packet[i] = source[j];
				
				for(int i=16, j=0; i<20; ++i, ++j) 
					packet[i] = dest[j];
				
				for(int i=20, j=0; j<data.length; ++i, ++j)
					packet[i] = data[j];
				
				short[] sary = byteToShort(packet);
				short[] t = new short[1];
				t[0] = (short) checkSum(sary);
				byte t1[] = toByteArray(t);

				packet[10] = t1[0]; //checksum locations
				packet[11] = t1[1];
				
				out.write(packet);
				System.out.println(counter);
				DATASIZE *= 2;
				counter ++;
			}
			System.out.println("Closing connection");
			socketIn.close();
			out.close();
		}
		System.exit(0);
	}
	
	private static byte[] toByteArray(short[] packet) {
		int j = 0;
		byte[] bArray = new byte[(packet.length << 1)];
		for (int i = 0; i < packet.length; ++i) {
	    	bArray[j + 1] |= (packet[i] & 0xFF);
	    	packet[i] >>>= 8;
	    	bArray[j] |= (packet[i] & 0xFF);
	    	j += 2;
		}
		return bArray;
    }
	
	private static long checkSum(short[] buf) { 
		long sum = 0;
		for(int i=0; i<10; ++i) { 
			sum += (buf[i] & 0xFFFF);
			if ((sum & 0xFFFF0000) > 0) { //carry 
				sum &= 0xFFFF;
				sum++;
			}
		}
		sum = ~(sum & 0xFFFF);
		return sum;
	}

	private static short[] byteToShort(byte[] message) {
		short[] shortMessage = new short[(message.length + 1) / 2];
		for (int i = 0, j = 0; j < message.length - 1; i++, j += 2) {
			shortMessage[i] |= (message[j] & 0xFF);
			shortMessage[i] <<= 8;
			shortMessage[i] |= (message[j + 1] & 0xFF);
		}
		return shortMessage;
	}
}
