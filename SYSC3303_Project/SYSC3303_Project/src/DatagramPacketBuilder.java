import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class DatagramPacketBuilder {
	/**
	 * This class contains all the methods to create a RRQ, WQR, DATA, ACK and ERROR datagram packets
	 * Other datagram packets creation methods will be found here.
	 */
	
	private static byte[] convertOPCodeToBytes(short opCode) {
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(opCode);
		return buffer.array();
	}
	
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
	
	public static DatagramPacket getRRQWRQDatagramPacket(TFTPPackets.TFTPPacketType packetType, byte[] fileName, byte[] mode, InetAddress ipAddress, int port) {
		/**
		* Returns a datagram packet RRQ/WRQ format
		* 
		* @param packetType: packet type
		* @param fileName: a list of bytes containing the file name
		* @param mode: a list of bytes containing the mode
		* @param ipAddress: ip address where the datagram packet will be sent to
		* @param port: port where the datagram packet will bent to 
		*
		* @return datagram packet in DATA format
		*/
		
		byte[] dataBytes = new byte[4 + fileName.length + mode.length];
		
		// assign opCode depending on package type
		// opCode is 1 if RRQ
		// opCode is 2 if WRQ
		short opCode = 0;
		if (packetType == TFTPPackets.TFTPPacketType.RRQ) {
			opCode = 1;
		}
		else if (packetType == TFTPPackets.TFTPPacketType.WRQ) {
			opCode = 2;
		}
		else {
			return null;
		}
		
		// convert opCode to bytes
		byte[] opCodeBytes = convertOPCodeToBytes(opCode);
		
		dataBytes[0] = opCodeBytes[0];
		dataBytes[1] = opCodeBytes[1];
		
		// append fileName bytes to data bytes
		int c = 2;
		for (int i = 0; i < fileName.length; i++) {
			dataBytes[c] = fileName[i];
			c++;
		}
		
		dataBytes[c] = 0;
		c++;
		
		// append mode bytes to data bytes
		for (int i = 0; i < mode.length; i++) {
			dataBytes[c] = mode[i];
			c++;
		}
		
		dataBytes[c] = 0;
		c++;
		
		return new DatagramPacket(dataBytes, dataBytes.length, ipAddress, port);
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
		byte[] opCodeBytes = convertOPCodeToBytes(opCode);
				
		dataBytes[0] = opCodeBytes[0];
		dataBytes[1] = opCodeBytes[1];
		
		// convert block number to bytes
		byte[] blockNumberBytes = convertOPCodeToBytes(blockNumber);
				
		dataBytes[2] = blockNumberBytes[0];
		dataBytes[3] = blockNumberBytes[1];
		
		// append the bytes in the data list to the packet's list of bytes
		int c = 4;
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
		byte[] opCodeBytes = convertOPCodeToBytes(opCode);
				
		dataBytes[0] = opCodeBytes[0];
		dataBytes[1] = opCodeBytes[1];
		
		// convert block number to bytes
		byte[] blockNumberBytes = convertOPCodeToBytes(blockNumber);
				
		dataBytes[2] = blockNumberBytes[0];
		dataBytes[3] = blockNumberBytes[1];
		

		return new DatagramPacket(dataBytes, dataBytes.length, returnAddress);
	}
}
