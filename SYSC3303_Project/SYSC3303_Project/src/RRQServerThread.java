import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Queue;

public class RRQServerThread extends Thread {
	/**
	 * This class is used to communicate further with a client that made a WQR request
	 */
	
	private DatagramSocket datagramSocket;
	private DatagramPacket receivedDatagramPacket;
	private FileManager fileManager;
	
	/**
	 * Constructor
	 * 
	 * @param receivedDatagramPacket request datagram packet received from client
	 */
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
	
	/**
	 * For threading purposes
	 */
	public void run() {
		handleRRQConnection();
		cleanUp();
	}
	
	/**
	 * Handles sending DATA datagram packets to client
	 */
	private void handleRRQConnection() {
		RRQWRQPacket requestPacket = null;
		
		try {
			requestPacket = new RRQWRQPacket(receivedDatagramPacket.getData(), receivedDatagramPacket.getOffset(), receivedDatagramPacket.getLength());
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		// get the file name requested by the client
		String fileName = requestPacket.getFileName();
		
		// read the whole file requested by the client
		byte[] fileData = fileManager.readFile(fileName);
		
		// create list of DATA datagram packets that contain up to 512 bytes of file data
		Queue<DatagramPacket> dataDatagramPacketStack = DatagramPacketBuilder.getStackOfDATADatagramPackets(fileData, receivedDatagramPacket.getSocketAddress());
		
		int packetCounter = 1;
		while (!dataDatagramPacketStack.isEmpty()) {
			// send each datagram packet in order and wait for acknowledgement packet from the client
			DatagramPacket dataDatagramPacket = dataDatagramPacketStack.remove();
			
			System.out.println(Globals.getVerboseMessage("RRQServerThread", 
					String.format("sending DATA packet %d to client %s", packetCounter, dataDatagramPacket.getAddress())));
			
			// send DATA datagram packet
			try {
				datagramSocket.send(dataDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot send DATA packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			// receive ACK datagram packet
			DatagramPacket receviableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			
			try {
				datagramSocket.receive(receviableDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot receive ACK packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			ACKPacket ackPacket = null;
			try {
				ackPacket = new ACKPacket(receviableDatagramPacket.getData(), receviableDatagramPacket.getOffset(), receviableDatagramPacket.getLength());
			} catch (TFTPPacketParsingError e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse ACK packet"));
				e.printStackTrace();
				System.exit(-1);
			}

			
			System.out.println(Globals.getVerboseMessage("RRQServerThread", 
					String.format("received ACK packet %d to client %s", ackPacket.getBlockNumber(), dataDatagramPacket.getAddress())));
			
			packetCounter++;
		}
		
		System.out.println(Globals.getErrorMessage("RRQServerThread", "connection finished"));
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		System.out.println(Globals.getErrorMessage("RRQServerThread", "socket closed"));
		datagramSocket.close();
	}
}
