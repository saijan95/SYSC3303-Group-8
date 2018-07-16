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
	
	@Override
	public void run() {
		handleWRQConnection();
		cleanUp();
	}
	
	private void handleWRQConnection() {
		RRQWRQPacket requestPacket = null;
		try {
			requestPacket = new RRQWRQPacket(receivedDatagramPacket.getData());
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot parse TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
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
		
		int dataLen = DATAPacket.MAX_DATA_SIZE_BYTES;
		while (dataLen == DATAPacket.MAX_DATA_SIZE_BYTES) { 
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
	
			DATAPacket dataPacket = null;;
			try {
				dataPacket = new DATAPacket(receviableDatagramPacket.getData());
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

			fileManager.writeFile(fileName, blockNumber, fileData);
			
			System.out.println(Globals.getVerboseMessage("WRQServerThread", String.format("finsihed writing data to file %s", fileName)));
			
			dataLen = fileData.length;
			
			System.out.println(Globals.getVerboseMessage("WRQServerThread", 
					String.format("sending ACK packet for DATA Packet %d to client %s", blockNumber, receivedDatagramPacket.getSocketAddress())));
			
			ackPacket = DatagramPacketBuilder.getACKDatagram(blockNumber, receivedDatagramPacket.getSocketAddress());
			
			// sends acknowledgement
			try {
				datagramSocket.send(ackPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send ACK packet"));
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	private void cleanUp() {
		datagramSocket.close();
	}
}
