import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;


public class Ipv6Client {
	private final static short HEADERSIZE = 40;

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		String host = "76.91.123.97";
		int port = 38004;
		short counter = 1;
		short DATASIZE = 2;
		
		try (Socket socket = new Socket(host, port)) { 
			InputStream socketIn = (socket.getInputStream());
			PrintStream out = new PrintStream(socket.getOutputStream());
			System.out.println("Connected to " + host + ":" + port + "!\n");
			
			while(counter < 13) { //send 12 packets
				byte[] packet = buildPacket(DATASIZE);
				out.write(packet);
				byte[] b = new byte[4];
				int count = socketIn.read(b);
				System.out.println(counter + ") " + DatatypeConverter.printHexBinary(b));
				DATASIZE *= 2;
				counter++;
			}
			System.out.println("\nAll packets sent...Closing connection");
			socketIn.close();
			out.close();
		} //end try
		System.exit(0);
	}
	
	private static byte[] buildPacket(short DATASIZE) { 
		byte packet[] = new byte[HEADERSIZE + DATASIZE];
		byte data[] = new byte[DATASIZE];
		new Random().nextBytes(data);
		
		packet[0] = 0b0110; //ip version 6
		packet[0] <<= 4;	//dont implement traffic class
		
		packet[1] = 0b0;	//dont implement flow label
		packet[2] = 0b0;
		packet[3] = 0b0;
		packet[4] = 0b0;	//end flow label
		
		short payload = DATASIZE;
		byte b2 = (byte) ((payload >>> 8) & 0xFF);
		byte b1 = (byte) (payload & 0xFF); 
		packet[4] = b2;
		packet[5] = b1;
		
		packet[6] = 0x11; //udp
		
		packet[7] = 0b010100; //ttl = 20
		
		for (int i=8; i<18; ++i) //source address
			packet[i] = 0b0;
		packet[18] = (byte)0xFF;	//1111 Implement assuming it is a valid IPv4 address that has been 
		packet[19] = (byte)0xFF;	//1111 extended to IPv6 for a device that does not use IPv6
		packet[20] = 0b01111111; //127
		packet[21] = 0b0;		//0
		packet[22] = 0b01; //1
		packet[23] = 0b01; //1	*END SOURCE ADDR*
		
		for(int i=24; i<34; ++i)  //destination address - same implementation as source
			packet[i] = 0b0;
		packet[34] = (byte) 0xFF; 
		packet[35] = (byte) 0xFF;
		packet[36] = 0x4C; //76
		packet[37] = 0x5B; //91
		packet[38] = 0x7B; //123
		packet[39] = 0x61; //97 - end destination address
		
		for(int i=0; i<DATASIZE; ++i)
			packet[HEADERSIZE + i] = data[i]; 
		
		return packet;
	}
}