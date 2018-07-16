package tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class TFTPDatagram implements TFTPPacket {
	
	private ByteBuffer body;
	private DatagramPacket backing;
	
	/**
	 * Constructs a new TFTPDatagram backed by a DatagramPacket
	 */
	public TFTPDatagram() {
		backing = new DatagramPacket(new byte[516], 516);
		body = ByteBuffer.wrap(backing.getData());
	}
	
	/**
	 * Constructs a new TFTPDatagram targeted for the specified host and port
	 * @param host
	 * @param port
	 */
	public TFTPDatagram(InetAddress host, int port) {
		this();
		backing.setAddress(host);
		backing.setPort(port);
	}

	@Override
	public void setPayload(byte[] pld) throws TFTPPacketException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Extract the payload field for multiple packet types.
	 * may be extended later to process RRQ/WRQ payloads into file paths
	 * @param offset
	 * @return
	 */
	private byte[] getOffsetPayload(int offset) {
		
		int plen = backing.getLength() - offset; // length of the payload
		byte[] pl = new byte[plen];
		body.get(pl, offset, plen); // fill array with payload data or error message
		return pl;
	}
	
	@Override
	public byte[] getPayload() throws TFTPPacketException {
		
		switch(getType()) {
		case DATA:
			return getOffsetPayload(4);
		case ERROR:
			return getOffsetPayload(4);
		case ACK:
			return new byte[0];
		case RRQ:
			return getOffsetPayload(2);
		case WRQ:
			return getOffsetPayload(2);
		default:
			throw new TFTPPacketException("Payload field not defined for this packet type");
		}
	}

	@Override
	public void setOpcode(short op) {
		body.putShort(0, op);
	}

	@Override
	public short getOpcode() {
		return body.getShort(0);
	}

	@Override
	public void setParameter(short param) {
		body.putShort(2, param);

	}

	@Override
	public short getParameter() throws TFTPPacketException {
		
		switch(getType()) {
		case RRQ:
			throw new TFTPPacketException("Parameter not defined for RRQ packets");
		case WRQ:
			throw new TFTPPacketException("Parameter not defined for WRQ packets");
		case NONE:
			throw new TFTPPacketException("Parameter not defined for this packet type");
		default:
			return body.getShort(2);	
		}
	}

	@Override
	public TFTPPacketType getType() {
		switch (getOpcode()) {
		case 01:
			return TFTPPacketType.RRQ;
		case 02:
			return TFTPPacketType.WRQ;
		case 03:
			return TFTPPacketType.DATA;
		case 04:
			return TFTPPacketType.ACK;
		case 05:
			return TFTPPacketType.ERROR;
		default:
			return TFTPPacketType.NONE;
		}
	}

}
