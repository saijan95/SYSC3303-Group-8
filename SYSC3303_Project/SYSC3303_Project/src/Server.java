import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Server implements Runnable {
	/**
	 * This class represents a server.
	 * It is used to accept incoming WRQ or RRQ requests 
	 */
	private boolean online;
	private DatagramSocket datagramSocket;
	private FileManager fileManager;
	
	public Server() {
		try {
			// create a datagram socket to establish a connection with incoming
			// RRQ and WRQ connections
			datagramSocket = new DatagramSocket(NetworkConfig.SERVER_PORT);
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("Server", "cannot create datagram socket on specified port"));
			e.printStackTrace();
		}
		
		fileManager = new FileManager();
	}
	
	public void run() {
		listen();
	}
	
	private String getFileName(byte[] wrqBytes) {
		int fileNameBytesLength =  0;
		for(int i = 2; i < wrqBytes.length; i++) {
			if (wrqBytes[i] == 0) {
				break;
			}
			
			fileNameBytesLength++;
		}
		
		byte[] fileNameBytes = Arrays.copyOfRange(wrqBytes, 2, 2 + fileNameBytesLength);
		
		return new String(fileNameBytes, StandardCharsets.UTF_8);
	}
	
	private byte[] getFileData(byte[] dataPacketBytes) {
		return Arrays.copyOfRange(dataPacketBytes, 4, dataPacketBytes.length);
	}
	
	private void handleReadRequest(DatagramPacket request) {
		// creates an DATA packet for response to RRQ
		String fileName = getFileName(request.getData());
		byte[] file = fileManager.readFile(fileName);
		DatagramPacket dataPacket = DatagramPacketBuilder.getDATADatagram((short) 0, file, request.getSocketAddress());
		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", 
				String.format("sending DATA packet to client %s", dataPacket.getAddress())));
		try {
			datagramSocket.send(dataPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot send DATA packet"));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void handleWriteRequest(DatagramPacket request) {
		// creates an ACK packet for response to WRQ
		DatagramPacket ackPacket = DatagramPacketBuilder.getACKDatagram((short) 0, request.getSocketAddress());
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("sending ACK packet to client %s", request.getSocketAddress())));
		
		// sends acknowledgement
		try {
			datagramSocket.send(ackPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send DATA packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("waiting for DATA packet from client %s", request.getSocketAddress())));
		
		// receives data packet from client
		DatagramPacket receviableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
		try {
			datagramSocket.receive(receviableDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot receive DATA packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("received DATA packet from client %s", request.getSocketAddress())));
		
		String fileName = getFileName(request.getData());
		byte[] data = getFileData(receviableDatagramPacket.getData());
		fileManager.writeFile(fileName, data);
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", String.format("finsihed writing data to file %s", fileName)));
		
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", 
				String.format("sending ACK packet to client %s", request.getSocketAddress())));
		
		// sends acknowledgement
		try {
			datagramSocket.send(ackPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot send ACK packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
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
			
			if (receiveDataBytes.length > 2) {
				// classify if the receive datagram packet it RRQ or WRQ
				byte[] opCodeBytes = {receiveDataBytes[0], receiveDataBytes[1]};
				TFTPPackets.TFTPPacketType packetType = TFTPPackets.classifyTFTPPacket(opCodeBytes);
				
				if (packetType == TFTPPackets.TFTPPacketType.RRQ) {
					System.out.println(Globals.getVerboseMessage("Server", "RRQ request recevied."));
					handleReadRequest(receiveDatagramPacket);
				}
				else if (packetType == TFTPPackets.TFTPPacketType.WRQ) {
					System.out.println(Globals.getVerboseMessage("Server", "WRQ request received."));
					handleWriteRequest(receiveDatagramPacket);
				}
			}
		}
		
		datagramSocket.close();
	}
	
	public void shutdown() {
		System.out.println(Globals.getVerboseMessage("Server", "shutting down..."));
		online = false;
				
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
				shutdownClient.send(new DatagramPacket(new byte[0], 0, InetAddress.getLocalHost(), NetworkConfig.SERVER_PORT));
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
