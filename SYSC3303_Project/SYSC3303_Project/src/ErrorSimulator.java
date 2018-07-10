import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ErrorSimulator {
	
	private boolean online;
	private DatagramSocket receiveSocket, sendAndReceiveSocket;
	private DatagramPacket sendServerPacket, sendClientPacket;
	
	public ErrorSimulator() {
		try {
			// create a datagram socket to establish a connection with incoming
			// RRQ and WRQ connections
			receiveSocket = new DatagramSocket(NetworkConfig.PROXY_PORT);
			sendAndReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot create datagram socket on specified port"));
			e.printStackTrace();
		}
	}
	
	public void listen() {
		online = true;
		
		while (online) {
			
			// Receive packet from Client
			DatagramPacket receiveClientDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet from client..."));
			try {
				receiveSocket.receive(receiveClientDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot receive packages"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			
			// Send packet to Server
			byte[] receiveDataBytes = receiveClientDatagramPacket.getData();
			try {
				sendServerPacket = new DatagramPacket(receiveDataBytes, receiveDataBytes.length, 
					InetAddress.getLocalHost(), NetworkConfig.SERVER_PORT);
			} catch (UnknownHostException e) {
				System.out.print("Unknown Host Exception: likely:");
				System.out.println("Packet failed to create.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
			
			try {
				sendAndReceiveSocket.send(sendServerPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			
			// Receive packet from Server
			DatagramPacket receiveServerDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet from server..."));
			try {
				sendAndReceiveSocket.receive(receiveServerDatagramPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot receive packages"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			
			// Send packet to Client
			receiveDataBytes = receiveServerDatagramPacket.getData();
			try {
				sendClientPacket = new DatagramPacket(receiveDataBytes, receiveDataBytes.length, 
					InetAddress.getLocalHost(), receiveClientDatagramPacket.getPort());
			} catch (UnknownHostException e) {
				System.out.print("Unknown Host Exception: likely:");
				System.out.println("Packet failed to create.\n" + e);
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to client..."));
			
			try {
				sendAndReceiveSocket.send(sendClientPacket);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
				e.printStackTrace();
				System.exit(-1);
			}
			
		}
		
		receiveSocket.close();
		sendAndReceiveSocket.close();
	}
	
	public void shutdown() {
		System.out.println(Globals.getVerboseMessage("Error Simulator", "shutting down..."));
		online = false;
				
		// wait for any connections that are to be classified
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot make current thread go to sleep."));
			e.printStackTrace();
			System.exit(-1);
		}
		
		if ((!receiveSocket.isClosed()) && (!sendAndReceiveSocket.isClosed())) {
			// temporary socket is created to send a decoy package to the server so that it can stop listening
			// therefore once it re-evaluates that the boolean online is false it will exit
			try {
				DatagramSocket shutdownClient = new DatagramSocket();
				shutdownClient.send(new DatagramPacket(new byte[0], 0, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT));
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
		
		System.out.println("\nSYSC 3033 TFTP Error Simulator");
		System.out.println("1. Start");
		System.out.println("2. Exit");
		System.out.println("Selection: ");
		
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();
		
		if (selection == 1) {
			proxy = new ErrorSimulator();
			proxy.listen();
		}
		else {
			System.exit(0);
		}
		
		selection = 0;
		while (selection != 1) {
			System.out.println("\nSYSC 3033 TFTP Server");
			System.out.println("1. Shutdown");
			System.out.println("Selection: ");
			
			selection = sc.nextInt();
		}
		
		if (selection == 1) {
			proxy.shutdown();
		}
	}
}