/**
 * This class represents the TFTP packet
 * 
 * @author Group 8
 *
 */
public class TFTPPacket {
	protected byte[] packetBytes;
	
	protected short opCode;
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	protected TFTPPacket() {}
	
	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public TFTPPacket(byte[] packetBytes) throws TFTPPacketParsingError {
		this.packetBytes = packetBytes;
		parseOPCode();
	}
	
	/**
	 * Reads bytes to parse the OP Code
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseOPCode() throws TFTPPacketParsingError {
		if (packetBytes.length < 2)
			throw new TFTPPacketParsingError("error parsing OPCode");
		
		byte[] opCodeBytes = {packetBytes[0], packetBytes[1]};
		opCode = ByteConversions.bytesToShort(opCodeBytes);
		
		if (opCode < 1 || opCode > 5) 
			throw new TFTPPacketParsingError("invalid OPCode");
	}
	
	/**
	 * Getter function for returning packet bytes array
	 * 
	 * @return block number
	 */
	public byte[] getPacketBytes() {
		return packetBytes;
	}
	
	/**
	 * Getter function for returning OP code
	 * 
	 * @return block number
	 */
	public short getOPCode() {
		return opCode;
	}
	
	/**
	 * Returns TFTP packet type depending the OP code
	 * 
	 * @return block number
	 */
	public TFTPPacketType getPacketType() {
		if (opCode == 1)
			return TFTPPacketType.RRQ;
		else if (opCode == 2)
			return TFTPPacketType.WRQ;
		else if (opCode == 3)
			return TFTPPacketType.DATA;
		else if (opCode == 4)
			return TFTPPacketType.ACK;
		else
			return TFTPPacketType.ERROR;
	}
}
