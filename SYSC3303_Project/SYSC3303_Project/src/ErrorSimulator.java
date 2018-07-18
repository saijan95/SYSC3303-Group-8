import java.io.*;
import java.net.*;
import java.util.Random;
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
	
	// error code to simulate
	private int errorSelection;
	// type of error to simulate
	private TFTPPacketType errorOp;
	// packet's block number to modify
	private short errorBlock;
	
	
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
	
	private DatagramPacket establishNewConnection(byte[] packetBytes, SocketAddress returnAddress) {
		System.out.println(Globals.getVerboseMessage("Error Simulator", "received packet from client."));
		
		// save client address and port
		clientPort = ((InetSocketAddress) returnAddress).getPort();
		
		// save server address and port 
		serverThreadPort = NetworkConfig.SERVER_PORT;
		
		System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
		
		try {
			DatagramPacket sendDatagramPacket = new DatagramPacket(packetBytes, packetBytes.length, 
					InetAddress.getLocalHost(), serverThreadPort);
			
			datagramSocket.send(sendDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
			e.printStackTrace();
			System.exit(-1);
		}	
		
		System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet from server..."));
		
		DatagramPacket receivableDatagramPacket = null;
		try {
			receivableDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
			datagramSocket.receive(receivableDatagramPacket);
			serverThreadPort = ((InetSocketAddress) receivableDatagramPacket.getSocketAddress()).getPort();
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot receive packages"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		return receivableDatagramPacket;
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
			
			// shutdown signal
			// if data packet is empty that means it should shutdown
			if (receivableDatagramPacket.getLength() == 0) {
				online = false;
				continue;
			}
			
			// creates a tftp packet with the received bytes
			packetBytes = receivableDatagramPacket.getData();
			tftpPacket = parseTFTPPacket(packetBytes, receivableDatagramPacket.getOffset(), packetBytes.length);
			
			// checks if the incoming packet is a request packet
			// if so then reset the server port back to the main port
			if (tftpPacket.getPacketType() == TFTPPacketType.RRQ || 
					tftpPacket.getPacketType() == TFTPPacketType.WRQ) {
				
				byte[] receiveDataBytes = receivableDatagramPacket.getData();
				if (errorSelection == 2 && (errorOp == TFTPPacketType.RRQ || errorOp == TFTPPacketType.WRQ)) {
					receiveDataBytes = simulateIllegalOperationError(receivableDatagramPacket.getData(), receivableDatagramPacket.getOffset(), 
							receivableDatagramPacket.getLength(),  errorSelection, errorOp, errorBlock);
				}

				receivableDatagramPacket = establishNewConnection(receiveDataBytes, receivableDatagramPacket.getSocketAddress());
				
				// creates a tftp packet with the received bytes
				packetBytes = receivableDatagramPacket.getData();
				tftpPacket = parseTFTPPacket(packetBytes, receivableDatagramPacket.getOffset(), packetBytes.length);
			}
	
			byte[] receiveDataBytes = receivableDatagramPacket.getData();
			
			if ((errorSelection == 2) || (errorSelection == 3)) {
				receiveDataBytes = simulateIllegalOperationError(receiveDataBytes, receivableDatagramPacket.getOffset(), 
						receivableDatagramPacket.getLength(),  errorSelection, errorOp, errorBlock);
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
			
			if (errorSelection == 3) {
				try {
					DatagramSocket tempSocket = new DatagramSocket();
					sendDatagramPacket = new DatagramPacket(receiveDataBytes, receivableDatagramPacket.getLength(), 
							InetAddress.getLocalHost(), sendPort);
					
					tempSocket.send(sendDatagramPacket);
					tempSocket.close();
				} catch (IOException e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
					e.printStackTrace();
					System.exit(-1);
				}	
			}
			else {
				try {
					sendDatagramPacket = new DatagramPacket(receiveDataBytes, receivableDatagramPacket.getLength(), 
							InetAddress.getLocalHost(), sendPort);
					
					datagramSocket.send(sendDatagramPacket);
				} catch (IOException e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot send packages"));
					e.printStackTrace();
					System.exit(-1);
				}	
			}
		}
		
		datagramSocket.close();
	}

	//Checks the PacketType, and block, and simulates the appropriate error on it
	private byte[] simulateIllegalOperationError(byte[] packet, int offset, int packetLength, int code, TFTPPacketType op, short block) {
		byte[] corruptedPacketBytes = packet;
		
		//Code to tamper here
		TFTPPacket tftp = null;
		try {
			tftp = new TFTPPacket(packet, offset, packetLength);
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP Packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (tftp.getPacketType() == op) {
			
			if ((op == TFTPPacketType.WRQ) || (op == TFTPPacketType.RRQ)) {
				
				RRQWRQPacket rrqwrq = null;
				
				try {
					rrqwrq = new RRQWRQPacket(packet, offset, packetLength);
				} catch (TFTPPacketParsingError e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet"));
					e.printStackTrace();
					System.exit(-1);
				}
				
				
				if (code == 2) //corrupt opcode
					corruptedPacketBytes =  corruptOpCode(rrqwrq);
				else if (code == 3) //corrupt mode
					corruptedPacketBytes =  corruptMode(rrqwrq);
				
			}
			
			else if (op == TFTPPacketType.DATA) {
				if (code == 2) { //corrupts op code only, there is not mode in DATA
					DATAPacket data = null;

					try {
						data = new DATAPacket(packet, offset, packetLength);
					} catch (TFTPPacketParsingError e) {
						System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet"));
						e.printStackTrace();
						System.exit(-1);
					}

					if (data.getBlockNumber() == block)
						corruptedPacketBytes = corruptOpCode(data);
				}
			} 
			else {
				if (code == 2) { //corrupts op code only, there is not mode in ACK
					ACKPacket ack = null;

					try {
						ack = new ACKPacket(packet, offset, packetLength);
					} catch (TFTPPacketParsingError e) {
						System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP ACK Packet"));
						e.printStackTrace();
						System.exit(-1);
					}

					if (ack.getBlockNumber() == block) {
						corruptedPacketBytes = corruptOpCode(ack);
					}
				}
			}
		}
		
		return corruptedPacketBytes;
	}
	
	//Hardcodes a wrong OPcode into the packet and returns the byte
	public byte[] corruptOpCode(TFTPPacket data) {

		// Corrupt op code
		byte[] corrupt = data.getPacketBytes();
		// hardcoded corruption
		corrupt[0] = 1;
		corrupt[1] = 5;

		return corrupt;
	}
	
	//Hardcodes a wrong mode, and returns a byte
	public byte[] corruptMode(RRQWRQPacket rrqwrq) {
		String mode = pickRandomMode(rrqwrq.getMode());
		
		RRQWRQPacket corruptRRQWRQPacket = RRQWRQPacket.buildPacket(rrqwrq.getPacketType(), rrqwrq.getFileName(), mode);
		return corruptRRQWRQPacket.getPacketBytes();
	}
	
	//Picks a random mode
	public String pickRandomMode(String x) {
		String[] modes = {"netascii", "octet", "mail"};
		int random = 0;
		while (modes[random] == x) {
			random = new Random().nextInt(modes.length);
		}
		return modes[random];
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
		System.out.println("2. Invalid TFTP");
		System.out.println("3. Invalid Transfer ID");
		System.out.println("4. Exit");
		System.out.println("Selection: ");
		
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();
		
		if (selection != 4) {
			// create server a thread for it listen on
			proxy = new ErrorSimulator();
			proxy.errorSelection = selection; //so the errorSimulator knows what to do
			
			if (proxy.errorSelection == 2) {
				//Invalid TFTP
				System.out.println("Which operation would you like to corrupt?");
				System.out.println("1. READ");
				System.out.println("2. WRITE");
				System.out.println("3. DATA");
				System.out.println("4. ACK");
				System.out.println("5. Any"); //
				System.out.println("6. Exit");
				System.out.println("Selection: ");

				selection = sc.nextInt();
				
				if (selection == 6) {
					sc.close();
					System.exit(0);
				} 
				else {
					switch(selection) {
					case 1: proxy.errorOp = TFTPPacketType.RRQ;
						break;
					case 2: proxy.errorOp = TFTPPacketType.WRQ;
						break;
					case 3: proxy.errorOp = TFTPPacketType.DATA;
						break;
					case 4: proxy.errorOp = TFTPPacketType.ACK;
						break;
					default:
						break;
					}
					
					if (proxy.errorOp == TFTPPacketType.ACK || proxy.errorOp == TFTPPacketType.DATA) {
						System.out.println("Which block would you like to corrupt?");
						selection = sc.nextInt();
						proxy.errorBlock = (short)selection;
					}
				}
			}
			
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