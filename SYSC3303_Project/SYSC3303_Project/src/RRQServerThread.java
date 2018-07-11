import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RRQServerThread extends Thread {
	/**
	 * This class is used to communicate further with a client that made a WQR request
	 */
	
	private DatagramSocket datagramSocket;
	private DatagramPacket receivedDatagramPacket;
	private FileManager fileManager;
	
	public RRQServerThread(DatagramPacket receivedDatagramPacket) {
		this.receivedDatagramPacket = receivedDatagramPacket;

		try {
			// create a datagram socket to carry on file transfer operation
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot create datagram socket on unspecified port"));
			e.printStackTrace();
		}
		
		fileManager = new FileManager();
	}
	
	public void run() {
		sendMessage();
		cleanUp();
	}
	
	private String getFileName(byte[] rrqBytes) {
		int fileNameBytesLength =  0;
		for(int i = 2; i < rrqBytes.length; i++) {
			if (rrqBytes[i] == 0) {
				break;
			}
			
			fileNameBytesLength++;
		}
		
		byte[] fileNameBytes = Arrays.copyOfRange(rrqBytes, 2, 2 + fileNameBytesLength);
		
		return new String(fileNameBytes, StandardCharsets.UTF_8);
	}
	
	private void sendMessage() {
		// creates an DATA packet for response to RRQ
		String fileName = getFileName(receivedDatagramPacket.getData());
		byte[] file = fileManager.readFile(fileName);
		DatagramPacket dataPacket = DatagramPacketBuilder.getDATADatagram((short) 0, file, receivedDatagramPacket.getSocketAddress());
		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", 
				String.format("sending DATA packet to client %s", dataPacket.getAddress())));
		try {
			datagramSocket.send(dataPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot send DATA packet"));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void cleanUp() {
		datagramSocket.close();
	}
}
