import java.util.Arrays;

/**
 * This class represents the ERROR TFTP packet
 * 
 * @author Group 8
 *
 */
public class ERRORPacket extends TFTPPacket {
	private short errorCode; 
	private String errorMessage;
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	private ERRORPacket() {
		super();
	}
	
	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public ERRORPacket(byte[] packetBytes) throws TFTPPacketParsingError {
		super(packetBytes);
		parseErrorCode();
		parseErrorMessage();
	}
	
	/**
	 * Reads bytes in the position 2 and 3 to parse the block number
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseErrorCode() throws TFTPPacketParsingError {
		if (super.packetBytes.length < 4)
			throw new TFTPPacketParsingError("error parsing error code");
		
		byte[] errorCodeBytes = {packetBytes[2], packetBytes[3]};
		errorCode = ByteConversions.bytesToShort(errorCodeBytes);
	}
	
	/**
	 *  Reads packet bytes to parse the error message
	 *  
	 * @throws TFTPPacketParsingError
	 */
	private void parseErrorMessage() throws TFTPPacketParsingError {
		if (super.packetBytes.length < 5) {
			throw new TFTPPacketParsingError("error parsing error message");
		}
		
		byte[] errorMessageBytes = Arrays.copyOfRange(super.packetBytes, 4, super.packetBytes.length);
		errorMessage = ByteConversions.bytesToString(errorMessageBytes);
	}
	
	/**
	 * Getter function for returning error code
	 * 
	 * @return block number
	 */
	public short getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Getter function for returning error message
	 * 
	 * @return block number
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Returns an ERROR packet given the attributes
	 * 
	 * @param blockNumber ERROR packet attribute
	 * @return ERROR packet containing the array of bytes that form the packet and the
	 * 					  initialized attributes
	 */
	public static ERRORPacket buildPacket(short errorCode, String errorMessage) {
		ERRORPacket errorPacket = new ERRORPacket();
		
		byte[] errorCodeBytes = ByteConversions.shortToBytes(errorCode);
		byte[] errorMessageBytes = ByteConversions.stringToBytes(errorMessage);
		
		byte[] packetBytes = new byte[3 + errorCodeBytes.length + errorCodeBytes.length];
		
		short opCode = 5;
		
		// convert opCode to bytes
		byte[] opCodeBytes = ByteConversions.shortToBytes(opCode);
		
		packetBytes[0] = opCodeBytes[0];
		packetBytes[1] = opCodeBytes[1];
				
		packetBytes[2] = errorCodeBytes[0];
		packetBytes[3] = errorCodeBytes[1];

		// append fileName bytes to data bytes
		int c = 4;
		for (int i = 0; i < errorMessageBytes.length; i++) {
			packetBytes[c] = errorMessageBytes[i];
			c++;
		}
		
		packetBytes[c] = 0;
		
		errorPacket.opCode = opCode;
		errorPacket.errorCode = errorCode;
		errorPacket.errorMessage = errorMessage;
		errorPacket.packetBytes = packetBytes;
		
		return errorPacket;
	}
}
