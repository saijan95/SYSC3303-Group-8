package tftp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class TFTPSocket {
	
	private DatagramSocket backing;
	
	public TFTPSocket(int port) throws TFTPSocketException {
		
		try {
			backing = new DatagramSocket(port);
		} catch (SocketException se) {
			throw new TFTPSocketException("Unable to bind socket", se);
		}
	}
	
	
	public TFTPSocket() throws TFTPSocketException {
		
		try {
			backing = new DatagramSocket();
		} catch (SocketException se) {
			throw new TFTPSocketException("Unable to bind socket", se);
		}
	}
	
	
	/**
	 * Send a TFTP packet over the network
	 * @param packet
	 * @throws TFTPSocketException
	 */
	public void send(TFTPDatagram packet) throws TFTPSocketException {
		
		try {
			backing.send(packet.getBackingDP());
		} catch (IOException se) {
			throw new TFTPSocketException("Failed to send TFTP packet", se);
		}
	}
	
	
	/**
	 * Attempts to receive a TFTP packet from the network
	 * @return
	 * @throws TFTPSocketException
	 */
	public TFTPPacket receive() throws TFTPSocketException {
		
		TFTPDatagram received = new TFTPDatagram();
		try {
			backing.receive(received.getBackingDP());
			// TODO: basic format validation to go here
			return received;
		} catch (IOException ie) {
			throw new TFTPSocketException("Failed to receive TFTP packet", ie);
		}
	}
	
}
