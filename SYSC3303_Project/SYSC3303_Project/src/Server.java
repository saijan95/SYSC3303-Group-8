import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Server implements Runnable {
	/**
	 * This class represents a server.
	 * It is used to accept incoming WRQ or RRQ requests 
	 */
	private boolean online;
	private DatagramSocket datagramSocket;
	
	public Server() {
		try {
			// create a datagram socket to establish a connection with incoming
			// RRQ and WRQ connections
			datagramSocket = new DatagramSocket(NetworkConfig.SERVER_PORT);
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("Server", "cannot create datagram socket on specified port"));
			e.printStackTrace();
		}
	
	}
	
	@Override
	public void run() {
		listen();
	}
	
	private void listen() {
		online = true;
		
		while (online) {
			DatagramPacket receiveDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			
			System.out.println(Globals.getVerboseMessage("Server", "waiting for packet..."));
			
			try {
				datagramSocket.receive(receiveDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Server", "cannot receive packages"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			byte[] receiveDataBytes = receiveDatagramPacket.getData();
			
			/*
			if (receiveDataBytes[0] == 0) {
				online = false;
				continue;
			}
			*/
			
			if (receiveDataBytes.length > 2) {
				
				// classify if the receive datagram packet it RRQ or WRQ
				TFTPPacket requestPacket = null;
				try {
					requestPacket = new TFTPPacket(receiveDataBytes, receiveDatagramPacket.getOffset(), receiveDatagramPacket.getLength());
				} catch (TFTPPacketParsingError e) {
					System.err.println(Globals.getErrorMessage("Server", "cannot parse TFTP packet"));
					e.printStackTrace();
					System.exit(-1);
				}
				
				TFTPPacketType packetType = requestPacket.getPacketType();
				
				if (packetType == TFTPPacketType.RRQ) {
					System.out.println(Globals.getVerboseMessage("Server", "RRQ request recevied."));
					RRQServerThread rrqServerThread = new RRQServerThread(receiveDatagramPacket);
					rrqServerThread.start();
				}
				else if (packetType == TFTPPacketType.WRQ) {
					System.out.println(Globals.getVerboseMessage("Server", "WRQ request received."));
					WRQServerThread wrqServerThread = new WRQServerThread(receiveDatagramPacket);
					wrqServerThread.start();
				}
			}
		}
		
		datagramSocket.close();
	}
	
	public void shutdown() {
		System.out.println(Globals.getVerboseMessage("Server", "shutting down..."));
				
		// wait for any connections that are to be classified
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("Server", "cannot make current thread go to sleep."));
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (!datagramSocket.isClosed()) {
			// temporary socket is created to send a decoy package to the server so that it can stop listening
			// therefore once it re-evaluates that the boolean online is false it will exit
			try {
				DatagramSocket shutdownClient = new DatagramSocket();
				shutdownClient.send(DatagramPacketBuilder.getShutdownDatagram(InetAddress.getLocalHost(), NetworkConfig.SERVER_PORT));
				shutdownClient.close();
			} catch (UnknownHostException e) {
				System.err.println(Globals.getErrorMessage("Server", "cannot find localhost address."));
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Server", "cannot send packet to server."));
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		System.out.println(Globals.getVerboseMessage("Server", "goodbye!"));
	}
	
	public static void main(String[] args) {
		Server server = null;
		Thread serverThread = null;
		
		System.out.println("\nSYSC 3033 TFTP Server");
		System.out.println("1. Start");
		System.out.println("2. Exit");
		System.out.println("Selection: ");
		
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();
		
		if (selection == 1) {
			// create server a thread for it listen on
			server = new Server();
			serverThread = new Thread(server);
			serverThread.start();
		}
		else {
			sc.close();
			System.exit(0);
		}
		
		// shuttdown option
		selection = 0;
		while (selection != 1) {
			System.out.println("\nSYSC 3033 TFTP Server");
			System.out.println("1. Shutdown");
			System.out.println("Selection: ");
			
			selection = sc.nextInt();
		}
		
		if (selection == 1) {
			server.shutdown();
			sc.close();
			try {
				serverThread.join(1000);
			} catch (InterruptedException e) {
				System.err.println(Globals.getErrorMessage("Server Main", "cannot close server thread"));
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
