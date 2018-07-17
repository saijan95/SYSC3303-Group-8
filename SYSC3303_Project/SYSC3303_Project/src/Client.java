import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.Scanner;

/**
 * This class represents the client1
 * @author Group 8
 *
 */
public class Client {
	
   private DatagramSocket sendReceiveSocket;
   private FileManager fileManager;

   /**
    * Constructor
    */
   public Client()
   {
      try {
         // Construct a datagram socket and bind it to any available 
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
    	 System.err.println(Globals.getErrorMessage("Client", "cannot create datagram socket"));
         se.printStackTrace();
         System.exit(-1);
      }
      
      // class for reading and writing files to hard drive 
      fileManager = new FileManager();
   }
   
   /**
    * Makes a read or write request and returns the response packet
    * 
    * @param packetType   packet type
    * @param fileName     name of the file that is requested to be read or written (in bytes)
    * @param mode         mode (in bytes)
    * @param ipAddress    server IP address
    * @param port         server port
    * 
    * @Return Datagram packet received from the server after making a RRQ or WRQ request
    */
   private void makeReadWriteRequest(TFTPPacketType packetType, String fileName, String mode, InetAddress ipAddress, int port) {

	   DatagramPacket datagramPacket = null;
	   
	   if (packetType == TFTPPacketType.RRQ) {
		   // get read request packet
		   datagramPacket = DatagramPacketBuilder.getRRQWRQDatagramPacket(TFTPPacketType.RRQ, fileName, mode, ipAddress, port);
	   }
	   else {
		   // get write request packet
		   datagramPacket = DatagramPacketBuilder.getRRQWRQDatagramPacket(TFTPPacketType.WRQ, fileName, mode, ipAddress, port);
	   }
	   
	   // send request
	   try {
		   sendReceiveSocket.send(datagramPacket);
	   } catch (IOException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot make RRQ/WQR request"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
   }
   
   /**
    * Handle DATA packets received from server with file data
    * 
    * @param filePath  path of the file that the client requests
    * @param mode      mode of request
    */
   public void readFile(String filePath, String mode) {
	   DatagramPacket receiveDatagramPacket;
	   DatagramPacket sendDatagramPacket;
	   TFTPPacket tftpPacket;
	   DATAPacket dataPacket;
	   
	   // get file name from file path
	   String fileName = Paths.get(filePath).getFileName().toString();
	   
	   // make a read request and wait for response
	   try {
		   makeReadWriteRequest(TFTPPacketType.RRQ, 
				   fileName, mode, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT);
	   } catch (UnknownHostException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot get localhost address"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   	// receive all data packets from server that wants to transfer a file.
		// once the data length is less than 512 bytes then stop listening for
		// data packets from the server
	   int fileDataLen = NetworkConfig.DATAGRAM_PACKET_MAX_LEN;
	   while (fileDataLen == NetworkConfig.DATAGRAM_PACKET_MAX_LEN) {
		   // receive datagram packet
		   receiveDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
		   
		   try {
			   sendReceiveSocket.receive(receiveDatagramPacket);
		   } catch (IOException e) {
			   System.err.println(Globals.getErrorMessage("Client", "cannot recieve DATA packet"));
			   e.printStackTrace();
			   System.exit(-1);
		   }
		   
		   // Parse a TFTPPacket to figure out what kind of packet was received
		   try {
			   tftpPacket = new TFTPPacket(receiveDatagramPacket.getData(), receiveDatagramPacket.getOffset(), receiveDatagramPacket.getLength());
		   
			   // check if the received packet is a DATA packet  
			   if (tftpPacket.getPacketType() == TFTPPacketType.DATA) {
				   try {
					   // Parse a data packet
					   dataPacket = new DATAPacket(receiveDatagramPacket.getData(), receiveDatagramPacket.getOffset(), receiveDatagramPacket.getLength());
					   
					   short blockNumber = dataPacket.getBlockNumber();
					   byte[] fileData = dataPacket.getDataBytes();
					   
					   System.out.println(Globals.getVerboseMessage("Client", String.format("received DATA packet %d from server", blockNumber)));
					   
					   // gets the data bytes from the DATA packet and converts it into a string
					   String fileDataStr = ByteConversions.bytesToString(fileData);
					   
					   System.out.println(Globals.getVerboseMessage("Client", String.format("received file data: %s", fileDataStr)));
				   
					   // save the length of the received packet
					   fileDataLen = receiveDatagramPacket.getLength();
					   
					   // send ACK packet
					   sendDatagramPacket = DatagramPacketBuilder.getACKDatagram(blockNumber, receiveDatagramPacket.getSocketAddress());
					   try {
						   sendReceiveSocket.send(sendDatagramPacket);
					   } catch (IOException e) {
						   System.err.println(Globals.getErrorMessage("Client", "cannot send ACK packet to server"));
						   e.printStackTrace();
					   }
					   
					   System.out.println(Globals.getVerboseMessage("Client", String.format("sent ACK packet %d to server", blockNumber)));
					   
				   } catch (TFTPPacketParsingError e) {
					   System.err.println(Globals.getErrorMessage("Client", "cannot parse DATA Packet"));
					   e.printStackTrace();
					   System.exit(-1);
				   }   
			   }
			   else {
				   // error package
				   fileDataLen = 0;
			   }
			   
		   } catch (TFTPPacketParsingError e) {
			   System.err.println(Globals.getErrorMessage("Client", "cannot parse TFTP Packet"));
			   e.printStackTrace();
			   System.exit(-1);
		   }
	   }
   }
   
   /**
    * Handle sending DATA packets to server 
    * 
    * @param filePath path of the file client wants to write to
    * @param mode     mode of the request
    */
   public void writeFile(String filePath, String mode) {
	   DatagramPacket receiveDatagramPacket;
	   
	   // get file name from file path
	   String fileName = Paths.get(filePath).getFileName().toString();
	   
	   // make a write request and wait for response
	   try {
		   	makeReadWriteRequest(TFTPPacketType.WRQ, 
				   fileName, mode, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT);
	   } catch (UnknownHostException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot get localhost address"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   receiveDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
	   
	   // receive ACK packet confirming WRQ request
	   try {
		   sendReceiveSocket.receive(receiveDatagramPacket);
	   } catch (IOException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot recieve ACK packet"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   // Parse a TFTPPacket to figure out what kind of packet was received
	   TFTPPacket tftpPacket = null;
	   try {
		   tftpPacket = new TFTPPacket(receiveDatagramPacket.getData(), receiveDatagramPacket.getOffset(), receiveDatagramPacket.getLength());
	   } catch (TFTPPacketParsingError e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot parse TFTP Packet"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   // check if the opCode in response is of ACK
	   if (tftpPacket.getPacketType() == TFTPPacketType.ACK) {
		   System.out.println(Globals.getVerboseMessage("Client", String.format("received ACK packet %d to server", (short) 0)));
		   
		   // reads a file on client side to create on the server side
		   byte[] fileData = fileManager.readFile(filePath);
		   
		   // create list of DATA datagram packets that contain up to 512 bytes of file data
		   Queue<DatagramPacket> dataDatagramStack = DatagramPacketBuilder.getStackOfDATADatagramPackets(fileData, receiveDatagramPacket.getSocketAddress());
		   
		   int packetCounter = 1;
		   DatagramPacket dataDatagramPacket;
		   while (!dataDatagramStack.isEmpty()) {
			   // send each datagram packet in order and wait for acknowledgement packet from the server
			   dataDatagramPacket = dataDatagramStack.remove();
			   
			   try {
				   // sends DATA packet with the file content
				   sendReceiveSocket.send(dataDatagramPacket);
			   } catch (IOException e) {
				   System.err.println(Globals.getErrorMessage("Client", "cannot send DATA packet to server"));
				   e.printStackTrace();
				   System.exit(-1);
			   }
			   
			   System.out.println(Globals.getVerboseMessage("Client", String.format("sent DATA packet %d to server", packetCounter)));
			   
			   // recevies an ACK packet indicating that the server successfully wrote the file
			   DatagramPacket ackReceviablePacket = DatagramPacketBuilder.getReceivalbeDatagram();
			   try {
				   sendReceiveSocket.receive(ackReceviablePacket);
			   } catch (IOException e) {
				   System.err.println(Globals.getErrorMessage("Client", "cannot receive ACK packet from server"));
				   e.printStackTrace();
			   }
			   
			   System.out.println(Globals.getVerboseMessage("Client", String.format("received ACK packet from server", packetCounter)));
			   packetCounter++;
		   }
	   }
   }
   
   /**
    * Closes the datagram socket when the connection is finished
    */
   public void shutdown() {
	   sendReceiveSocket.close();
   }

   public static void main(String args[])
   {
      Client c = new Client();
      
      Scanner sc = new Scanner(System.in);
      
      int userInput = 0;
	  do
	  {
		  System.out.println("\nSYSC 3033 Client");
		  System.out.println("1. Write file to Server");
		  System.out.println("2. Read file from Server");
		  System.out.println("3. Close Client");
		  System.out.print("Enter choice (1-3): ");
		  userInput = sc.nextInt();
		  sc.nextLine();

		  if (userInput == 1)
		  {
			  System.out.print("Enter path to file to write to Server: ");
		      String filePath = sc.nextLine();
		
		      c.writeFile(filePath, "asciinet");
		  }
		  else if (userInput == 2)
		  {
			  System.out.print("Enter the file name to read from Server: ");
		      String fileName = sc.nextLine();
		      
			  c.readFile(fileName, "asciinet");
		  }
		  else if (userInput == 3)
		  {
		      c.shutdown();
		  }
		  else
		  {
			  System.out.println("Wrong input number!\nEnter integer number 1-3!");
		  }
	  }
	  while (userInput != 3);
	  
	  sc.close();
   }
}
