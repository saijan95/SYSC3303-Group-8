import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

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
		handleRRQConnection();
		cleanUp();
	}
	
	private void handleRRQConnection() {
		RRQWRQPacket requestPacket = null;
		try {
			requestPacket = new RRQWRQPacket(receivedDatagramPacket.getData());
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		// creates an DATA packet for response to RRQ
		String fileName = requestPacket.getFileName();
		
		// read the whole file
		byte[] fileData = fileManager.readFile(fileName);
		
		Queue<DatagramPacket> dataDatagramPacketStack = getStackOfDATADatagramPackets(fileData);
		
		int packetCounter = 1;
		while (!dataDatagramPacketStack.isEmpty()) {
			DatagramPacket dataDatagramPacket = dataDatagramPacketStack.remove();
			
			System.out.println(Globals.getVerboseMessage("RRQServerThread", 
					String.format("sending DATA packet %d to client %s",packetCounter, dataDatagramPacket.getAddress())));
			try {
				datagramSocket.send(dataDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot send DATA packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			DatagramPacket receviableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			
			try {
				datagramSocket.receive(receviableDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot receive ACK packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			packetCounter++;
		}
	}
	
	private Queue<DatagramPacket> getStackOfDATADatagramPackets(byte[] fileData) {
		Queue<DatagramPacket> dataPacketStack = new LinkedList<DatagramPacket>();
		
		int numOfPackets = (fileData.length / DATAPacket.MAX_DATA_SIZE_BYTES) + 1;
		for (int i = 0; i < numOfPackets; i++) {
			short blockNumber = (short) (i + 1);
			
			int start = i * DATAPacket.MAX_DATA_SIZE_BYTES;
			int end = (i + 1) * DATAPacket.MAX_DATA_SIZE_BYTES;
			
			if (end > fileData.length)
				end = fileData.length;
			
			byte[] fileDataPart = Arrays.copyOfRange(fileData, start, end);
			dataPacketStack.add(DatagramPacketBuilder.getDATADatagram(blockNumber, fileDataPart, receivedDatagramPacket.getSocketAddress()));
		}
		
		return dataPacketStack;
	}
	
	private void cleanUp() {
		datagramSocket.close();
	}
}
