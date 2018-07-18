import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class contains all the methods to create a RRQ, WQR, DATA, ACK and ERROR datagram packets
 * Other datagram packets creation methods will be found here.
 * 
 * @author Group 8
 *
 */
public class DatagramPacketBuilder {
	/**
	 * Returns an empty datagram packet for receiving data.
	 * 
	 * @return empty datagram packet
	 */
	public static DatagramPacket getReceivalbeDatagram() {
		// returns an empty datagram packet for receiving data.
		byte[] receiveBytes = new byte[NetworkConfig.DATAGRAM_PACKET_MAX_LEN];
		return new DatagramPacket(receiveBytes, receiveBytes.length);
	}
	
	/**
	 * Returns a packet used to signal server shutdown
	 * 
	 * @param serverAddress  address of the server that is to be shutdown
	 * @param serverPort     address of the port that is to be shutdown
	 * 
	 * @return shutdown datagram packet
	 */
	public static DatagramPacket getShutdownDatagram(InetAddress serverAddress, int serverPort) {
		// shutdown packet should not have data in it
		byte[] shutdownBytes = new byte[0];
		return new DatagramPacket(shutdownBytes, shutdownBytes.length, serverAddress, serverPort);
	}
	
	/**
	* Returns a datagram packet RRQ/WRQ format
	* 
	* @param packetType  packet type
	* @param fileName    name of the file to request from the server
	* @param mode        mode used to write or read file on the server
	* @param ipAddress   IP address where the datagram packet will be sent to
	* @param port        port where the datagram packet will bent to 
	*
	* @return datagram packet in DATA format
	*/
	public static DatagramPacket getRRQWRQDatagramPacket(TFTPPacketType packetType, String fileName, String mode, InetAddress ipAddress, int port) {
		RRQWRQPacket requestPacket = RRQWRQPacket.buildPacket(packetType, fileName, mode);
		return new DatagramPacket(requestPacket.packetBytes, requestPacket.packetBytes.length, ipAddress, port);
	}
	
	/**
	* Returns a datagram packet in DATA format
	* 
	* @param blockNumber   the blockNumber
	* @param data          a list of bytes containing the data
	* @param returnAddress return socket address where the datagram packet will be sent to
	*
	* @return datagram packet in DATA format
	*/
	public static DatagramPacket getDATADatagram(short blockNumber, byte[] data, SocketAddress returnAddress) {		
		DATAPacket dataPacket = DATAPacket.buildPacket(blockNumber, data);
		return new DatagramPacket(dataPacket.packetBytes, dataPacket.packetBytes.length, returnAddress);
	}
	
	/**
	* Returns a datagram packet in ACK format
	* 
	* @param blockNumber    the blockNumber
	* @param returnAddress  return socket address where the datagram packet will be sent to
	*
	* @return datagram packet in ACK format
	*/
	
	public static DatagramPacket getACKDatagram(short blockNumber, SocketAddress returnAddress) {
		ACKPacket ackPacket = ACKPacket.buildPacket(blockNumber);
		return new DatagramPacket(ackPacket.packetBytes, ackPacket.packetBytes.length, returnAddress);
	}
	
	/**
	 * Returns a datagram packet in ERROR format
	 * 
	 * @param errorCode     error code
	 * @param errorMessage  additional information about the error
	 * @param returnAddress return socket address where the datagram packet will be sent to
	 * 
	 * @return datagram packet in ERROR format
	 */
	public static DatagramPacket getERRORDatagram(short errorCode, String errorMessage, SocketAddress returnAddress) {
		ERRORPacket errorPacket = ERRORPacket.buildPacket(errorCode, errorMessage);
		return new DatagramPacket(errorPacket.packetBytes, errorPacket.packetBytes.length, returnAddress);
	}
	
	/**
	 * Return a stack of DATA datagram packets each containing maximum of 512 bytes of the file
	 * @param fileData       bytes from the whole file
	 * @param returnAddress  address where the datagram packets will be sent to 
	 * 
	 * @return stack of datagram packets
	 */
	public static Queue<DatagramPacket> getStackOfDATADatagramPackets(byte[] fileData, SocketAddress returnAddress) {
		Queue<DatagramPacket> dataPacketStack = new LinkedList<DatagramPacket>();
		
		// calculate the number of packets will be required to transfer the whole file
		int numOfPackets = (fileData.length / DATAPacket.MAX_DATA_SIZE_BYTES) + 1;
		for (int i = 0; i < numOfPackets; i++) {
			short blockNumber = (short) (i + 1);
			
			int start = i * DATAPacket.MAX_DATA_SIZE_BYTES;
			int end = (i + 1) * DATAPacket.MAX_DATA_SIZE_BYTES;
			
			if (end > fileData.length)
				end = fileData.length;
			
			byte[] fileDataPart = Arrays.copyOfRange(fileData, start, end);
			dataPacketStack.add(DatagramPacketBuilder.getDATADatagram(blockNumber, fileDataPart, returnAddress));
		}
		
		return dataPacketStack;
	}
}
