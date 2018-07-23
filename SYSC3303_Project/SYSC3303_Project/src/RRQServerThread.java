import java.net.InetAddress;
import java.util.Queue;

/**
 * This class is used to communicate further with a client that made a WQR request
 */
public class RRQServerThread extends Thread {
	private TFTPSocket tftpSocket;
	private TFTPPacket requestPacket;
	
	private FileManager fileManager;
	private ErrorHandler errorHandler;
	
	private InetAddress remoteAddress;
	private int remotePort;
	
	private Queue<DATAPacket> dataPacketStack;
	
	/**
	 * Constructor
	 * 
	 * @param receivedDatagramPacket request datagram packet received from client
	 */
	public RRQServerThread(TFTPPacket requestPacket) {
		this.requestPacket = requestPacket;
		
		tftpSocket = new TFTPSocket();
		
		remoteAddress = requestPacket.getRemoteAddress();
		remotePort = requestPacket.getRemotePort();
		
		fileManager = new FileManager();
		errorHandler = new ErrorHandler(tftpSocket);
	}
	
	/**
	 * For threading purposes
	 */
	public void run() {
		handleRRQConnection();
		cleanUp();
	}
	
	private void sendDATAPacketToClient(DATAPacket dataPacket) {		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", 
				String.format("sending DATA packet %d to client %s:%d", dataPacket.getBlockNumber(), dataPacket.getRemoteAddress(), dataPacket.getRemotePort())));
		
		// send DATA datagram packet
		tftpSocket.send(dataPacket);
	}
	
	private ACKPacket receiveACKPacketFromClient(short expectedBlockNumber) {	
		ACKPacket ackPacket = null;
		
		while (ackPacket == null) {
			TFTPPacket receivePacket = tftpSocket.receive();
			
			if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
					receivePacket.getRemotePort() != remotePort) {
				String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
						remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				
				System.err.println(Globals.getErrorMessage("RRQServerThread", errorMessage));	
				errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, remoteAddress, remotePort);
				return null;
			}
		
			if (receivePacket.getPacketType() == TFTPPacketType.ACK) {
				try {
					ackPacket = new ACKPacket(receivePacket);
					
					if (ackPacket.getBlockNumber() != expectedBlockNumber) {
						String errorMessage = String.format("unexpected ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, ackPacket.getBlockNumber());
						System.err.println(Globals.getErrorMessage("RRQServerThread", errorMessage));
						errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
						ackPacket = null;
						continue;
					}
					
				} catch(TFTPPacketParsingError e) {
					String errorMessage = String.format("cannot parse ACK packet %d", expectedBlockNumber);
					System.err.println(Globals.getErrorMessage("RRQServerThread", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
					continue;
				}
				
				System.out.println(Globals.getVerboseMessage("RRQServerThread", 
						String.format("received ACK packet %d from client %s%d", ackPacket.getBlockNumber(), remoteAddress, remotePort)));
			}
			else if (receivePacket.getPacketType() == TFTPPacketType.ERROR) {
				ERRORPacket errorPacket = null;
				
				try {
					errorPacket = new ERRORPacket(receivePacket);
				} catch (TFTPPacketParsingError e) {
					String errorMessage = "cannot parse ERROR packet";
					System.err.println(Globals.getErrorMessage("RRQServerThread", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				System.out.println(Globals.getVerboseMessage("RRQServerThread", 
						String.format("received ERROR packet from client %s%d, errorCode: %d, errorMessage: %s", remoteAddress, 
								remotePort, errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
				
				if (errorPacket.getErrorCode() == ERRORPacket.ILLEGAL_TFTP_OPERATION) {
					continue;
				}
				else {
					return null;
				}
			}
			else {
				String errorMessage = "invalid TFTP packet";
				System.err.println(Globals.getErrorMessage("RRQServerThread", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				receivePacket = null;
				continue;
			}
		}
		
		return ackPacket;
	}
	
	/**
	 * Handles sending DATA datagram packets to client
	 */
	private void handleRRQConnection() {
		RRQWRQPacket rrqPacket = null;
		
		// parse read request packet
		try {
			rrqPacket = new RRQWRQPacket(requestPacket);
		} catch (TFTPPacketParsingError e) {
			/*
			 * If an invalid read request is sent, then and error packet with 
			 * error code 4 will be sent to the client
			 * 
			 * The current read request thread will be terminated
			 */
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse RRQ TFTP packet"));
			errorHandler.sendIllegalOperationErrorPacket("invalid RRQ TFTP packet", remoteAddress, remotePort);
			return;
		}
		
		// get the file name requested by the client
		String fileName = rrqPacket.getFileName();
		
		// read the whole file requested by the client
		byte[] fileData = fileManager.readFile(fileName);
		
		// create list of DATA datagram packets that contain up to 512 bytes of file data
		dataPacketStack = TFTPPacketBuilder.getStackOfDATADatagramPackets(fileData, remoteAddress, remotePort);
		
		while (!dataPacketStack.isEmpty()) {
			// send each datagram packet in order and wait for acknowledgement packet from the client
			DATAPacket dataPacket = dataPacketStack.peek();
			
			sendDATAPacketToClient(dataPacket);
			
			ACKPacket ackPacket = receiveACKPacketFromClient(dataPacket.getBlockNumber());
			if (ackPacket == null) {
				break;
			}
			
			dataPacketStack.poll();
		}
		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", "connection finished"));
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		System.out.println(Globals.getVerboseMessage("RRQServerThread", "socket closed"));
		tftpSocket.close();
	}
}
