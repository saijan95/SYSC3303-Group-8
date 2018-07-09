import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class RRQServerThread extends Thread {
	/**
	 * This class is used to communicate further with a client that made a WQR request
	 */
	
	private DatagramSocket datagramSocket;
	private DatagramPacket receivedDatagramPacket;
	
	public RRQServerThread(DatagramPacket receivedDatagramPacket) {
		this.receivedDatagramPacket = receivedDatagramPacket;

		try {
			// create a datagram socket to carry on file transfer operation
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot create datagram socket on unspecified port"));
			e.printStackTrace();
		}
	}
	
	public void run() {
		sendMessage();
		cleanUp();
	}
	
	private void sendMessage() {
		// creates an DATA packet for response to RRQ
		DatagramPacket dataPacket = DatagramPacketBuilder.getDATADatagram((short) 0, new byte[0],receivedDatagramPacket.getSocketAddress());
		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", 
				String.format("sending DATA packet to client &s", dataPacket.getAddress())));
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
