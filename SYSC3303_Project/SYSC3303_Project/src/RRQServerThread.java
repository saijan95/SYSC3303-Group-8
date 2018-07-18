import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Queue;

/**
 * This class is used to communicate further with a client that made a WQR request
 */
public class RRQServerThread extends Thread {
	private DatagramSocket datagramSocket;
	private DatagramPacket receivedDatagramPacket;
	private FileManager fileManager;
	
	private SocketAddress serverSocketAddress;
	
	/**
	 * Constructor
	 * 
	 * @param receivedDatagramPacket request datagram packet received from client
	 */
	public RRQServerThread(DatagramPacket receivedDatagramPacket) {
		this.receivedDatagramPacket = receivedDatagramPacket;
		this.serverSocketAddress = receivedDatagramPacket.getSocketAddress();

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
	 * Sends an error packet with the Unknown ID error code to the client
	 * 
	 * @param errorMessage
	 */
	private void sendUnknownTIDErrorPacket(String errorMessage) {
		DatagramPacket errorDatagramPacket = DatagramPacketBuilder.getERRORDatagram(ERRORPacket.UNKNOWN_TID, errorMessage, serverSocketAddress);
		
		System.err.println(Globals.getErrorMessage("RRQServerThread", String.format("sending error packet, errorCode: %d, errorMessage: %s", ERRORPacket.UNKNOWN_TID, errorMessage)));
		
		try {
			datagramSocket.send(errorDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot send ERROR TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Sends an error packet with the Illegal Operation error code to the client
	 * 
	 * @param errorMessage
	 */
	private void sendIllegalOperationErrorPacket(String errorMessage) {
		DatagramPacket errorDatagramPacket = DatagramPacketBuilder.getERRORDatagram(ERRORPacket.ILLEGAL_TFTP_OPERATION, errorMessage, serverSocketAddress);
		
		System.err.println(Globals.getErrorMessage("RRQServerThread", String.format("sending error packet, errorCode: %d, errorMessage: %s", ERRORPacket.ILLEGAL_TFTP_OPERATION, errorMessage)));
		
		try {
			datagramSocket.send(errorDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot send ERROR TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Handles sending DATA datagram packets to client
	 */
	private void handleRRQConnection() {
		RRQWRQPacket requestPacket = null;
		
		// parse read request packet
		try {
			requestPacket = new RRQWRQPacket(receivedDatagramPacket.getData(), receivedDatagramPacket.getOffset(), receivedDatagramPacket.getLength());
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse TFTP packet"));
			
			// send an illegal packet error to the client
			sendIllegalOperationErrorPacket("invalid WRQ packet");
			
			return;
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
				
				if (!receviableDatagramPacket.getSocketAddress().equals(serverSocketAddress)) {
					sendUnknownTIDErrorPacket("data packet came from wrong client");
					continue;
				}
				
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot receive ACK packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			try {
				TFTPPacket tftpPacket = new TFTPPacket(receviableDatagramPacket.getData(), receviableDatagramPacket.getOffset(), receviableDatagramPacket.getLength());
				if (tftpPacket.getPacketType() == TFTPPacketType.ERROR) {
					ERRORPacket errorPacket = new ERRORPacket(receviableDatagramPacket.getData(), receviableDatagramPacket.getOffset(), receviableDatagramPacket.getLength());
					
					System.err.println(Globals.getErrorMessage("RRQServerThread", String.format("received error packet %s", errorPacket)));
					
					short errorCode = errorPacket.getErrorCode();
					if (errorCode == ERRORPacket.ILLEGAL_TFTP_OPERATION) {
						// terminate connection
						return;
					}
					else if (errorCode == ERRORPacket.UNKNOWN_TID) {
						// continue to the next packet 
						continue;
					}
				}
			} catch (TFTPPacketParsingError e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse TFTP packet"));
				
				// send an illegal packet error to the client
				sendIllegalOperationErrorPacket("invalid TFTP packet");
				
				e.printStackTrace();
				return;
			}
			
			ACKPacket ackPacket = null;
			try {
				ackPacket = new ACKPacket(receviableDatagramPacket.getData(), receviableDatagramPacket.getOffset(), receviableDatagramPacket.getLength());
			} catch (TFTPPacketParsingError e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse ACK packet"));
				
				// send an illegal packet error to the client
				sendIllegalOperationErrorPacket("invalid ACK packet");
				
				e.printStackTrace();
				return;
			}

			
			System.out.println(Globals.getVerboseMessage("RRQServerThread", 
					String.format("received ACK packet %d to client %s", ackPacket.getBlockNumber(), dataDatagramPacket.getAddress())));
			
			packetCounter++;
		}
		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", "connection finished"));
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		System.out.println(Globals.getVerboseMessage("RRQServerThread", "socket closed"));
		datagramSocket.close();
	}
}
