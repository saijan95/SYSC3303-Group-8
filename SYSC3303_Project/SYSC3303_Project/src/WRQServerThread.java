import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
public class WRQServerThread extends Thread {
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

	/**
	 * 
	 */
	@Override
	public void run() {
		handleWRQConnection();
		cleanUp();
	}
	
	/**
	 * Handles DATA datagram packets received from client
	 */
	private void handleWRQConnection() {
		RRQWRQPacket requestPacket = null;
		try {
			requestPacket = new RRQWRQPacket(receivedDatagramPacket.getData(), receivedDatagramPacket.getOffset(), receivedDatagramPacket.getLength());
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot parse TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		// creates an ACK packet for response to WRQ
		// block number is 0
		DatagramPacket ackDatagramPacket = DatagramPacketBuilder.getACKDatagram((short) 0, receivedDatagramPacket.getSocketAddress());
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("sending ACK packet %d to client %s", 0, receivedDatagramPacket.getSocketAddress())));
		
		// sends acknowledgement to write request
		try {
			datagramSocket.send(ackDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send ACK packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		// receive all data packets from client that wants to transfer a file
		// once the data length is less than 512 bytes then stop listening for
		// data packets from the client
		int dataLen = NetworkConfig.DATAGRAM_PACKET_MAX_LEN;
		while (dataLen == NetworkConfig.DATAGRAM_PACKET_MAX_LEN) { 
			System.out.println(Globals.getVerboseMessage("WRQServerThread", 
					String.format("waiting for DATA packet from client %s", receivedDatagramPacket.getSocketAddress())));
			
			// receives data packet from client
			byte[] d = new byte[NetworkConfig.DATAGRAM_PACKET_MAX_LEN];
			DatagramPacket receviableDatagramPacket = new DatagramPacket(d, d.length);
			try {
				datagramSocket.receive(receviableDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot receive DATA packet"));
				e.printStackTrace();
				System.exit(-1);
			}
	
			// parse DATA packet
			DATAPacket dataPacket = null;;
			try {
				dataPacket = new DATAPacket(receviableDatagramPacket.getData(), receviableDatagramPacket.getOffset(), receviableDatagramPacket.getLength());
			} catch (TFTPPacketParsingError e) {
				System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot parse DATA packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			String fileName = requestPacket.getFileName();
			short blockNumber = dataPacket.getBlockNumber();
			byte[] fileData = dataPacket.getDataBytes();
			
			System.out.println(Globals.getVerboseMessage("WRQServerThread", 
					String.format("received DATA packet %d from client %s", blockNumber, receivedDatagramPacket.getSocketAddress())));

			// write file data from DATA packet to hard drive
			fileManager.writeFile(fileName, fileData);
			
			System.out.println(Globals.getVerboseMessage("WRQServerThread", String.format("finsihed writing data to file %s", fileName)));
			
			// save the length of file data that was just saved
			dataLen = receviableDatagramPacket.getLength();
			
			// send acknowledgement packet for the data packet received
			System.out.println(Globals.getVerboseMessage("WRQServerThread", 
					String.format("sending ACK packet for DATA Packet %d to client %s", blockNumber, receivedDatagramPacket.getSocketAddress())));
			
			ackDatagramPacket = DatagramPacketBuilder.getACKDatagram(blockNumber, receivedDatagramPacket.getSocketAddress());
			
			// sends acknowledgement
			try {
				datagramSocket.send(ackDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send ACK packet"));
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", "connection is finsihed"));
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		System.out.println(Globals.getVerboseMessage("WRQServerThread", "closing socket"));
		datagramSocket.close();
	}
}
