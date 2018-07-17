import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator implements Runnable {
	/**
	 * This class represents the error simulator
	 */
	
	private boolean online;
	private DatagramSocket datagramSocket;
	
	// the port of the server that the client is communicating with
	private int serverThreadPort;
	// the port of the client that the server is communicating with
	private int clientPort;
	
	public ErrorSimulator() {
		serverThreadPort = NetworkConfig.SERVER_PORT;
		
		try {
			// create a datagram socket to establish a connection with incoming
			datagramSocket = new DatagramSocket(NetworkConfig.PROXY_PORT);
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot create datagram socket on specified port"));
			e.printStackTrace();
		}
	}
	
	public void run() {
		listen();
	}
	
	private TFTPPacket parseTFTPPacket(byte[] packetBytes, int offset, int packetLength) {
		// creates a tftp packet with the received bytes
		TFTPPacket tftpPacket = null;
		
		try {
			tftpPacket = new TFTPPacket(packetBytes, offset, packetLength);
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		return tftpPacket;
	}
	
	private void listen() {
		online = true;
		
		TFTPPacket tftpPacket;
		byte[] packetBytes;
		
		DatagramPacket receivableDatagramPacket;
		DatagramPacket sendDatagramPacket;
		
		while (online) {
			
			// Receive packet from Client
			receivableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet..."));
			try {
				datagramSocket.receive(receivableDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot receive packages"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			// creates a tftp packet with the received bytes
			packetBytes = receivableDatagramPacket.getData();
			tftpPacket = parseTFTPPacket(packetBytes, receivableDatagramPacket.getOffset(), packetBytes.length);
			
			// checks if the incoming packet is a request packet
			// if so then reset the server port back to the main port
			if (tftpPacket.getPacketType() == TFTPPacketType.RRQ || 
					tftpPacket.getPacketType() == TFTPPacketType.WRQ) {
				
				System.out.println(Globals.getVerboseMessage("Error Simulator", "received packet from client."));
				
				// save client address and port
				clientPort = ((InetSocketAddress) receivableDatagramPacket.getSocketAddress()).getPort();
				
				// save server address and port 
				serverThreadPort = NetworkConfig.SERVER_PORT;
				
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
				
				try {
					sendDatagramPacket = new DatagramPacket(tftpPacket.getPacketBytes(), tftpPacket.getPacketBytes().length, 
							InetAddress.getLocalHost(), serverThreadPort);
					
					datagramSocket.send(sendDatagramPacket);
				} catch (IOException e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
					e.printStackTrace();
					System.exit(-1);
				}	
				
				receivableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
				System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet from server..."));
				
				try {
					datagramSocket.receive(receivableDatagramPacket);
				} catch (IOException e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot receive packages"));
					e.printStackTrace();
					System.exit(-1);
				}
				
				serverThreadPort = ((InetSocketAddress) receivableDatagramPacket.getSocketAddress()).getPort();
				
				// creates a tftp packet with the received bytes
				packetBytes = receivableDatagramPacket.getData();
				tftpPacket = parseTFTPPacket(packetBytes, receivableDatagramPacket.getOffset(), packetBytes.length);
			}
			
			int sendPort = 0;
			if (((InetSocketAddress) receivableDatagramPacket.getSocketAddress()).getPort() == serverThreadPort) {
				System.out.println(Globals.getVerboseMessage("Error Simulator", "recieved packet from server."));
				sendPort = clientPort;
				
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to client..."));
			}
			else {
				System.out.println(Globals.getVerboseMessage("Error Simulator", "recieved packet from client."));
				sendPort = serverThreadPort;
				
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
			}
			
			
			try {
				sendDatagramPacket = new DatagramPacket(tftpPacket.getPacketBytes(), receivableDatagramPacket.getLength(), 
						InetAddress.getLocalHost(), sendPort);
				
				datagramSocket.send(sendDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
				e.printStackTrace();
				System.exit(-1);
			}		
		}
		
		datagramSocket.close();
	}
	
	public void shutdown() {
		System.out.println(Globals.getVerboseMessage("Error Simulator", "shutting down..."));
		
		// wait for any packets to be replayed
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot make current thread go to sleep."));
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (!datagramSocket.isClosed()) {
			// temporary socket is created to send a decoy package to the server so that it can stop listening
			// therefore once it re-evaluates that the boolean online is false it will exit
			try {
				DatagramSocket shutdownClient = new DatagramSocket();
				shutdownClient.send(DatagramPacketBuilder.getShutdownDatagram(InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT));
				shutdownClient.close();
			} catch (UnknownHostException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot find localhost address."));
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packet to server."));
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		System.out.println(Globals.getVerboseMessage("Error Simulator", "goodbye!"));
	}
	
	public static void main(String[] args) {
		ErrorSimulator proxy = null;
		Thread proxyThread = null;
		
		System.out.println("\nSYSC 3033 TFTP Error Simulator");
		System.out.println("1. Start");
		System.out.println("2. Exit");
		System.out.println("Selection: ");
		
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();
		
		if (selection == 1) {
			// create server a thread for it listen on
			proxy = new ErrorSimulator();
			proxyThread = new Thread(proxy);
			proxyThread.start();
		}
		else {
			sc.close();
			System.exit(0);
		}
		
		// shutdown option
		selection = 0;
		while (selection != 1) {
			System.out.println("\nSYSC 3033 TFTP Server");
			System.out.println("1. Shutdown");
			System.out.println("Selection: ");
			
			selection = sc.nextInt();
		}
		
		if (selection == 1) {
			sc.close();
			proxy.shutdown();
			try {
				proxyThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}