import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import javax.xml.bind.DatatypeConverter;


public class CryptoClient {
	private final String HOST = "45.50.5.238";
	private final int PORT = 38008;
	private final short IPV4_HEADERSIZE = 20; //ipv4 header size is 20(bytes)
	private final short UDP_HEADERSIZE = 8; //8 bytes
	private final int udpPort = 38008; //38008
	
	private byte[] sourceAddr = new byte[4];
	private byte[] destAddr = new byte[4];
	private InputStream socketIn;
	private OutputStream out;
	private Socket socket;
	private Cipher cipher;
	private Key key;
	
	public static void main(String[] args) throws IOException, InvalidKeyException, 
	NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
	BadPaddingException, ClassNotFoundException {
		
		CryptoClient c = new CryptoClient();
		c.run();
	}
	
	private void run() throws IOException, InvalidKeyException, 
	NoSuchAlgorithmException, NoSuchPaddingException,
	IllegalBlockSizeException, BadPaddingException, ClassNotFoundException { 		
	
		//use old destination address "76.91.123.97" because thats what the server accepts
		destAddr[0] = 0b01001100; //76
		destAddr[1] = 0b01011011; //91
		destAddr[2] = 0b01111011; //123
		destAddr[3] = 0b01100001; //97
		
		sourceAddr[0] = 0b01111111; //127
		sourceAddr[1] = 0b0;		//0
		sourceAddr[2] = 0b01; //1
		sourceAddr[3] = 0b01; //1

		short counter = 0;
		short DATASIZE = 2;
		
		socket = new Socket();
		socket = SocketFactory.getDefault().createSocket(HOST, PORT);
		socketIn = socket.getInputStream();
		out = socket.getOutputStream();
		System.out.println("Connected to " + HOST + ":" + PORT + "!\n");
		
		byte[] handshake = initHandshake();
		
		out.write(handshake);
		byte[] b = new byte[4];
		int count = socketIn.read(b);
		System.out.println("Handshake response: 0x" + DatatypeConverter.printHexBinary(b));
		
		long totalTime = 0;
		while(counter < 10) { //send 12 packets
			byte[] randomData = new byte[DATASIZE];
				new Random().nextBytes(randomData);
			byte[] udp = buildUDP(randomData);
			byte[] packet = buildPackage(udp);
			byte[] encrypted = cipher.doFinal(packet);
				
			long start = System.currentTimeMillis() % 1000;
				out.write(encrypted);
				b = new byte[4];
				count = socketIn.read(b);
			long stop = System.currentTimeMillis() % 1000;
				
			System.out.println(counter + ") 0x" + DatatypeConverter.printHexBinary(b) + " | " + (stop-start) + "(ms)");
			totalTime += (stop-start);
			DATASIZE *= 2;
			counter++;
		}
		System.out.println("\nAll packets sent... Average time:" + (totalTime/counter) + "(ms)" + "\nClosing connection");
		exit();
	}
	
	
	private byte[] initHandshake() throws IOException, NoSuchAlgorithmException, 
	NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, 
	BadPaddingException, ClassNotFoundException {
		
		ObjectInputStream in = new ObjectInputStream(new FileInputStream("public.bin"));
		RSAPublicKey rsakey = (RSAPublicKey) in.readObject();
		Cipher rsa = Cipher.getInstance("RSA");
		rsa.init(Cipher.ENCRYPT_MODE, rsakey);
		
		cipher = Cipher.getInstance("AES");
		key = KeyGenerator.getInstance("AES").generateKey();
		cipher.init(Cipher.ENCRYPT_MODE, key);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(key);
		
		byte[] encrypted = rsa.doFinal(bos.toByteArray());
		byte[] udp = buildUDP(encrypted);
		byte[] packet = buildPackage(udp);
		
		bos.close();
		oos.close();
		in.close();
		return packet;
		
	}
	
	private byte[] buildUDP(byte[] data) { 
		byte[] udp = new byte[UDP_HEADERSIZE + data.length];
		
		udp[0] = 0; //source PORT
		udp[1] = 0;
		
		short port = (short) udpPort; //udpPort = 38008 (int)
		udp[2] = (byte) ((port >>> 8) & 0xFF); //destination port
		udp[3] = (byte) (port & 0xFF);
		
		short len = (short) (UDP_HEADERSIZE + data.length);
		udp[4] = (byte) ((len >>> 8) & 0xFF); //length
		udp[5] = (byte) (len & 0xFF);
		
		udp[6] = 0; //initialize checksum to 0 before calculating
		udp[7] = 0;
		
		for(int i=UDP_HEADERSIZE, j=0; j < data.length; ++i, ++j) //data potion of udp packet
			udp[i] = data[j]; //data
		
		short checksum = (short) udpChecksum( byteToShort(udp), len);	//TODO: Modify checksum algorithm to work on byte[]
		udp[6] = (byte) ((checksum >>> 8) & 0xFF);
		udp[7] = (byte) (checksum & 0xFF);
		
		return udp;
	}
	
	
	
	
	private long udpChecksum(short[] buf, short length) { 
		long sum = 0;
		short udp[] = new short[buf.length + 6];
		
		udp[0] = sourceAddr[0];
		udp[0] <<= 8;
		udp[0] |= sourceAddr[1];
		udp[1] = sourceAddr[2];
		udp[1] <<= 8;
		udp[1] |= sourceAddr[3];
		
		udp[2] = destAddr[0];
		udp[2] <<= 8;
		udp[2] |= destAddr[1];
		udp[3] = destAddr[2];
		udp[3] <<= 8;
		udp[3] |= destAddr[3];
		
		udp[4] = 0;
		udp[4] <<= 8;
		udp[4] |= 0x11;
		
		udp[5] = length;
		
		for(int i=6, j=0; i < udp.length; ++i, ++j) 
			udp[i] = buf[j];
		
		for(int i=0; i<udp.length; ++i) { 
			sum += (udp[i] & 0xFFFF);
			if((sum & 0xFFFF0000) > 0) { 
				sum &= 0xFFFF;
				sum++;
			}
		}
		sum = ~(sum & 0xFFFF);
		return sum;
	}
	
	
	
	
	
	
	private byte[] buildPackage(byte[] data) { 
		byte[] packet = new byte[IPV4_HEADERSIZE + data.length];
		packet[0] = 0x4;
		packet[0] <<= 4;
		packet[0] |= 0x5;
		
		packet[1] = 0;
		
		short tempshort = (short) (IPV4_HEADERSIZE + data.length);
		packet[2] = (byte) ((tempshort >>> 8) & 0xFF);
		packet[3] = (byte) (tempshort & 0xFF); 
		
		packet[4] = 0;
		packet[5] = 0;
		
		packet[6] = 0b010;
		packet[6] <<= 5;
		
		packet[6] |= 0b0;
		packet[7] = 0b0;
		
		packet[8] = 0b00110010; //ttl = 50
		
		packet[9] = 0x11; //protocol = udp
		
		for(int i=12, j=0; i<16; ++i, ++j) 
			packet[i] = sourceAddr[j];
		
		for(int i=16, j=0; i<20; ++i, ++j) 
			packet[i] = destAddr[j];
		
		//really bad and inefficient way of doing the checksum
		tempshort = (short) checksum( byteToShort(packet) ); //TODO: Modify checksum algorithm to eliminate need for conversion	#FINALSWEEK
		packet[10] = (byte) ((tempshort >>> 8) & 0xFF);
		packet[11] = (byte) (tempshort & 0xFF);
		
		for(int i=20, j=0; j<data.length; ++i, ++j)
			packet[i] = data[j];
		
		return packet;
	}
	
	private long checksum(short[] buf) { 
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
	
	private byte[] toByteArray(short[] message) {
		byte[] b = new byte[message.length * 2];
		for (int i = 0, j = 0; i < message.length; i++, j += 2) {
			b[j + 1] |= (message[i] & 0xFF);
			message[i] >>>= 8;
			b[j] |= (message[i] & 0xFF);
		}
		return b;
	}
	
	private short[] byteToShort(byte[] message) {
		short[] shortMessage = new short[(message.length + 1) / 2];
		for (int i = 0, j = 0; j < message.length - 1; i++, j += 2) {
			shortMessage[i] |= (message[j] & 0xFF);
			shortMessage[i] <<= 8;
			shortMessage[i] |= (message[j + 1] & 0xFF);
		}
		return shortMessage;
	}

	public void printPacket(byte[] sendIt) {
		System.out.println("ACKET LENGTH: " + sendIt.length);
		for (int i = 0; i < sendIt.length; i++) {
			String temp = "";
			for (int j = 7; j >= 0; j--)
				temp += ((0b1 << j & sendIt[i]) > 0) ? "1" : "0";
			if (i % 4 == 0) 
				System.out.println();
			while (temp.length() < 8) 
				temp += ("0" + temp);
			System.out.print(temp.substring(temp.length() - 8) + " ");
		}
		System.out.println();
	}
	
	private void exit() throws IOException { 
		socket.close();
		socketIn.close();
		out.close();
	}

}