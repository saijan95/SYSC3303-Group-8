// The client sends a character string to the echo server, then waits 
// for the server to send it back to the client.

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	/**
	 * This class represents a client.
	 */
	
   private DatagramSocket sendReceiveSocket;
   private FileManager fileManager;

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
      
      // class for reading and writing files to harddrive 
      fileManager = new FileManager();
   }
   
   private DatagramPacket makeReadWriteRequest(TFTPPackets.TFTPPacketType packetType, byte[] fileName, byte[] mode, InetAddress ipAddress, int port) {
	   /**
	    * Makes a read or write request and returns the reponse packet
	    * 
	    * @param packetType: packet type
	    * @param fileName: name of the file that is requested to be read or written (in bytes)
	    * @param mode: mode (in bytes)
	    * @param ipAddress: server IP address
	    * @param port: server port
	    * 
	    * @Return Datagram packet received from the server after making a RRQ or WRQ request
	    */
	   DatagramPacket datagramPacket = null;
	   
	   if (packetType == TFTPPackets.TFTPPacketType.RRQ) {
		   // get read request packet
		   datagramPacket = DatagramPacketBuilder.getRRQWRQDatagramPacket(TFTPPackets.TFTPPacketType.RRQ, fileName, mode, ipAddress, port);
	   }
	   else {
		   // get write request packet
		   datagramPacket = DatagramPacketBuilder.getRRQWRQDatagramPacket(TFTPPackets.TFTPPacketType.WRQ, fileName, mode, ipAddress, port);
	   }
	   
	   // send request
	   try {
		   sendReceiveSocket.send(datagramPacket);
	   } catch (IOException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot make RRQ/WQR request"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
		
	   // receive response
	   DatagramPacket responseDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
	   try {
		   sendReceiveSocket.receive(responseDatagramPacket);
	   } catch (IOException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot get response for RRQ/WRQ request"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
		
	   return responseDatagramPacket;
   }
   
   private byte[] getFileData(byte[] dataPacketBytes) {
	   /**
	    * Takes the list of bytes from a DATA packet and extracts the data portion of the array
	    * 
	    * @param dataPacketBytes: list of bytes
	    * 
	    * Return list of bytes containing the data
	    */
	   // get file data list of bytes from data packet bytes
	   return Arrays.copyOfRange(dataPacketBytes, 4, dataPacketBytes.length);
   }
   
   private String convertFileDatatoString(byte[] data) {
	   /**
	    * Converts data's file content in bytes to string
	    * 
	    * @param data: file content in bytes
	    * 
	    * @Return file's content in string format
	    */
	   // convert file name from bytes to string
	   return new String(data, StandardCharsets.UTF_8);
   }
   
   public void readFile(String filePath, String mode) {
	   // get file name from file path
	   String fileName = Paths.get(filePath).getFileName().toString();
	   
	   // convert fileName to bytes
	   byte[] fileNameBytes = fileName.getBytes();
	   byte[] modeBytes = mode.getBytes();
	   
	   // make a read request and wait for response
	   DatagramPacket responseDatagramPacket = null;
	   try {
		   responseDatagramPacket = makeReadWriteRequest(TFTPPackets.TFTPPacketType.RRQ, 
				   fileNameBytes, modeBytes, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT);
	   } catch (UnknownHostException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot get localhost address"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   byte[] responseDataBytes = responseDatagramPacket.getData();
	   byte[] responseOPCode = {responseDataBytes[0], responseDataBytes[1]};
	   
	   // check if the opCode in response is of DATA
	   if (TFTPPackets.classifyTFTPPacket(responseOPCode) == TFTPPackets.TFTPPacketType.DATA) {
		   System.out.println(Globals.getVerboseMessage("Client", "received DATA packet from server"));
		   // gets the data bytes from the DATA packet and converts it into a string
		   String fileData = convertFileDatatoString(getFileData(responseDataBytes));
		   System.out.println(Globals.getVerboseMessage("Client", String.format("received file data: %s", fileData)));
	   }
   }
   
   public void writeFile(String filePath, String mode) {
	   // get file name from file path
	   String fileName = Paths.get(filePath).getFileName().toString();
	   
	   // convert fileName to bytes
	   byte[] fileNameBytes = fileName.getBytes();
	   byte[] modeBytes = mode.getBytes();
	   
	   // make a write request and wait for response
	   DatagramPacket responseDatagramPacket = null;
	   try {
		   responseDatagramPacket = makeReadWriteRequest(TFTPPackets.TFTPPacketType.WRQ, 
				   fileNameBytes, modeBytes, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT);
	   } catch (UnknownHostException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot get localhost address"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   byte[] responseDataBytes = responseDatagramPacket.getData();
	   byte[] responseOPCode = {responseDataBytes[0], responseDataBytes[1]};
	   
	   // check if the opCode in response is of ACK
	   if (TFTPPackets.classifyTFTPPacket(responseOPCode) == TFTPPackets.TFTPPacketType.ACK) {
		   System.out.println(Globals.getVerboseMessage("Client", "received ACK packet from server"));
		   
		   // reads a file on client side to create on the server side
		   byte[] fileBytes = fileManager.readFile(filePath);
		   DatagramPacket dataDatagramPacket = DatagramPacketBuilder.getDATADatagram((short) 0, fileBytes, responseDatagramPacket.getSocketAddress());
		   try {
			   // sends DATA packet with the file content
			   sendReceiveSocket.send(dataDatagramPacket);
		   } catch (IOException e) {
			   System.err.println(Globals.getErrorMessage("Client", "cannot send DATA packet to server"));
			   e.printStackTrace();
			   System.exit(-1);
		   }
		   
		   System.out.println(Globals.getVerboseMessage("Client", "sent DATA packet to server"));
		   
		   // recevies an ACK packet indicating that the server successfully wrote the file
		   DatagramPacket ackReceviablePacket = DatagramPacketBuilder.getReceivalbeDatagram();
		   try {
			   sendReceiveSocket.receive(ackReceviablePacket);
		   } catch (IOException e) {
			   System.err.println(Globals.getErrorMessage("Client", "cannot receive ACK packet from server"));
			   e.printStackTrace();
		   }
		   
		   System.out.println(Globals.getVerboseMessage("Client", "received ACK packet from server"));
	   }
   }
   
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
