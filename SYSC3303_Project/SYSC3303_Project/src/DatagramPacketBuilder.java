import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;

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
	
	public static DatagramPacket getShutdownDatagram(InetAddress serverAddress, int serverPort) {
		byte[] shutdownBytes = new byte[NetworkConfig.DATAGRAM_PACKET_MAX_LEN];
		shutdownBytes[0] = 0;
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
}
