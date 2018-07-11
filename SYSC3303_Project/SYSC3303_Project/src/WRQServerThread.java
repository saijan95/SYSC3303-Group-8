import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WRQServerThread extends Thread {
	/**
	 * This class is used to communicate further with a client that made a WQR request
	 */
	
	private DatagramSocket datagramSocket;
	private DatagramPacket receivedDatagramPacket;
	private FileManager fileManager;
	
	public WRQServerThread(DatagramPacket receivedDatagramPacket) {
		this.receivedDatagramPacket = receivedDatagramPacket;
		
		try {
			// create a datagram socket to carry on file transfer operation
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot create datagram socket on unspecified port"));
			e.printStackTrace();
		}
		
		fileManager = new FileManager();
	}
	
	private String getFileName(byte[] wrqBytes) {
		int fileNameBytesLength =  0;
		for(int i = 2; i < wrqBytes.length; i++) {
			if (wrqBytes[i] == 0) {
				break;
			}
			
			fileNameBytesLength++;
		}
		
		byte[] fileNameBytes = Arrays.copyOfRange(wrqBytes, 2, 2 + fileNameBytesLength);
		
		return new String(fileNameBytes, StandardCharsets.UTF_8);
	}
	
	private byte[] getFileData(byte[] dataPacketBytes) {
		return Arrays.copyOfRange(dataPacketBytes, 4, dataPacketBytes.length);
	}
	
	public void run() {
		sendMessage();
		cleanUp();
	}
	
	private void sendMessage() {
		// creates an ACK packet for response to WRQ
		DatagramPacket ackPacket = DatagramPacketBuilder.getACKDatagram((short) 0, receivedDatagramPacket.getSocketAddress());
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("sending ACK packet to client &s", receivedDatagramPacket.getSocketAddress())));
		
		// sends acknowledgement
		try {
			datagramSocket.send(ackPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send DATA packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("waiting for DATA packet from client &s", receivedDatagramPacket.getSocketAddress())));
		
		// receives data packet from client
		DatagramPacket receviableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
		try {
			datagramSocket.receive(receviableDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot receive DATA packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("received DATA packet from client &s", receivedDatagramPacket.getSocketAddress())));
		
		String fileName = getFileName(receivedDatagramPacket.getData());
		byte[] data = getFileData(receviableDatagramPacket.getData());
		fileManager.writeFile(fileName, data);
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", String.format("finsihed writing data to file %s", fileName)));
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("sending ACK packet to client &s", receivedDatagramPacket.getSocketAddress())));
		
		// sends acknowledgement
		try {
			datagramSocket.send(ackPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send ACK packet"));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void cleanUp() {
		datagramSocket.close();
	}
}
