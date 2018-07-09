import java.net.DatagramPacket;
import java.net.SocketAddress;

public class DatagramPacketBuilder {
	/**
	 * This class contains all the methods to create a RRQ, WQR, DATA, ACK and ERROR datagram packets
	 * Other datagram packets creation methods will be found here.
	 */
	
	
	public static DatagramPacket getReceivalbeDatagram() {
		/**
		* Returns an empty datagram packet for receiving data.
		*
		* @return empty datagram packet
		*/
		
		// returns an empty datagram packet for receiving data.
		byte[] receiveBytes = new byte[NetworkConfig.DATAGRAM_PACKET_MAX_LEN];
		return new DatagramPacket(receiveBytes, receiveBytes.length);
	}
	
	public static DatagramPacket getDATADatagram(short blockNumber, byte[] data, SocketAddress returnAddress) {
		/**
		* Returns a datagram packet in DATA format
		* 
		* @param blockNumber: the blockNumber
		* @param data: a list of bytes containing the data
		* @param returnAddress: return socket address where the datagram packet will be sent to
		*
		* @return datagram packet in DATA format
		*/
		
		// create a properly sized bytes array
		byte[] dataBytes = new byte[4 + data.length];
		
		// convert opCode to bytes
		short opCode = 3;
		dataBytes[0] = (byte)(opCode & 0xff);
		dataBytes[1] = (byte)((opCode >> 8) & 0xff);
		
		// convert block number to bytes
		dataBytes[2] = (byte)(blockNumber & 0xff);
		dataBytes[3] = (byte)((blockNumber >> 8) & 0xff);
		
		// append the bytes in the data list to the packet's list of bytes
		int c = 5;
		for (int i = 0; i < data.length; i++) {
			dataBytes[c] = data[i];
			c++;
		}
		
		return new DatagramPacket(dataBytes, dataBytes.length, returnAddress);
	}
	
	public static DatagramPacket getACKDatagram(short blockNumber, SocketAddress returnAddress) {
		/**
		* Returns a datagram packet in ACK format
		* 
		* @param blockNumber: the blockNumber
		* @param returnAddress: return socket address where the datagram packet will be sent to
		*
		* @return datagram packet in ACK format
		*/
		
		// create a properly sized bytes array
		byte[] dataBytes = new byte[4];
		
		// convert opCode to bytes
		short opCode = 4;
		dataBytes[0] = (byte)(opCode & 0xff);
		dataBytes[1] = (byte)((opCode >> 8) & 0xff);
		
		// convert block number to bytes
		dataBytes[2] = (byte)(blockNumber & 0xff);
		dataBytes[3] = (byte)((blockNumber >> 8) & 0xff);
		

		return new DatagramPacket(dataBytes, dataBytes.length, returnAddress);
	}
}