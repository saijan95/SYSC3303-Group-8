import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

/**
 * 
 * @author Group 8
 *
 */
public class Client {
	
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
   
   private void makeReadWriteRequest(TFTPPacketType packetType, String fileName, String mode, InetAddress ipAddress, int port) {
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
   
   private void sendErrorPacket(short errorCode, String errorMessage, SocketAddress returnAddress) {
       /**
        * Send ERROR packet to the given host
        * 
        * @param errorCode: error code
        * @param errorMessage: Error message
        * @param ipAddress: server IP address
        * @param port: server port
        */
       DatagramPacket datagramPacket = DatagramPacketBuilder.getERRORDatagram(errorCode, errorMessage, returnAddress);
       
       // send request
       try {
           sendReceiveSocket.send(datagramPacket);
       } catch (IOException e) {
           System.err.println(Globals.getErrorMessage("Client", "cannot send ERROR package"));
           e.printStackTrace();
           System.exit(-1);
       }
   }
   
   public void readFile(String filePath, String mode) {
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
	         
	   // Transfer ID of the server
	   int serverTID = 0; // server TID is not received yet
	   int nextBlockNumber = 1; // expect to receive DATA with valid block number

	   int fileDataLen = DATAPacket.MAX_DATA_SIZE_BYTES;
	   while (fileDataLen == DATAPacket.MAX_DATA_SIZE_BYTES) {
		   DatagramPacket responseDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
		   
		   try {
			   sendReceiveSocket.receive(responseDatagramPacket);
		   } catch (IOException e) {
			   System.err.println(Globals.getErrorMessage("Client", "cannot recieve DATA packet"));
			   e.printStackTrace();
			   System.exit(-1);
		   }
		   
		   // Error handling
		   if (serverTID == 0) {
	           // Get the TID of server (in source port field of the Datagram package)
		       serverTID = responseDatagramPacket.getPort();
		   } else {
		       // Validate if the package comes from the remote host of the same TID
		       int sourceTID = responseDatagramPacket.getPort();
		       if (serverTID != sourceTID) {
		           System.err.println(Globals.getErrorMessage("Client", String.format("receives un-expected transfer ID (expected %d, got %d)", serverTID, sourceTID)));
		           
		           // Send ERROR package with errorCode = 5 to the remote host where the package came from
		           sendErrorPacket((short) 5, "TID does not match", responseDatagramPacket.getSocketAddress());
		           
		           // Just skip this package, continue this connection
		           continue;
		       }
		   }
		   
		   byte[] responseDataBytes = responseDatagramPacket.getData();
		   
		   TFTPPacket tftpPacket = null;
		   try {
			   tftpPacket = new TFTPPacket(responseDataBytes);
		   } catch (TFTPPacketParsingError e) {
			   System.err.println(Globals.getErrorMessage("Client", "cannot parse TFTP Packet"));
			   e.printStackTrace();
			   System.exit(-1);
		   }
		   
		   if (tftpPacket.getPacketType() == TFTPPacketType.DATA) {
			   DATAPacket dataPacket = null;
			   try {
				   dataPacket = new DATAPacket(responseDataBytes);
			   } catch (TFTPPacketParsingError e) {
				   System.err.println(Globals.getErrorMessage("Client", "cannot parse DATA Packet"));
				   e.printStackTrace();
				   System.exit(-1);
			   }
			   
			   short blockNumber = dataPacket.getBlockNumber();

			   // Error handling
			   // Validate blockNumber
			   if (blockNumber != nextBlockNumber) {
			       System.err.println(Globals.getErrorMessage("Client", String.format("receives un-expected DATA block number (expected %d, got %d)", nextBlockNumber, blockNumber)));
                   
                   // Send ERROR package with errorCode = 4 to the remote host
                   sendErrorPacket((short) 4, "Unexpected block number", responseDatagramPacket.getSocketAddress());
                   
                   // Finish this session
                   fileDataLen = 0;
                   continue;
			   }
			   nextBlockNumber++;
			   
			   byte[] fileData = dataPacket.getDataBytes();
			   
			   System.out.println(Globals.getVerboseMessage("Client", String.format("received DATA packet %d from server", blockNumber)));
			   
			   // gets the data bytes from the DATA packet and converts it into a string
			   String fileDataStr = ByteConversions.bytesToString(fileData);
			   
			   System.out.println(Globals.getVerboseMessage("Client", String.format("received file data: %s", fileDataStr)));
		   
			   fileDataLen = fileData.length;
			   
			   DatagramPacket ackDatagramPacket = DatagramPacketBuilder.getACKDatagram(blockNumber, responseDatagramPacket.getSocketAddress());
			   try {
				   sendReceiveSocket.send(ackDatagramPacket);
			   } catch (IOException e) {
				   System.err.println(Globals.getErrorMessage("Client", "cannot receive ACK packet from server"));
				   e.printStackTrace();
			   }
		   }
		   else {
			   
			   // Error Handling
			   if (tftpPacket.getPacketType() == TFTPPacketType.ERROR) {
	               // Got an ERROR package from server
	               ERRORPacket errorPacket = null;
	               try {
	                   errorPacket = new ERRORPacket(responseDataBytes);
	               } catch (TFTPPacketParsingError e) {
	                   System.err.println(Globals.getErrorMessage("Client", "cannot parse ERROR Packet"));
	                   e.printStackTrace();
	                   System.exit(-1);
	               }
	               
	               // Show the error
	               System.out.println(Globals.getVerboseMessage("Client", String.format("received ERROR packet (errorCode=%d, errorMsg=%s) from server", errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
	           
			   } else {
			       // Unexpected packet received
	               System.out.println(Globals.getVerboseMessage("Client", String.format("received unexpected response (opCode=%d) from server.", tftpPacket.getOPCode())));
	               
	               // Send ERROR (errorCode=4) back to the host, indicating illegal operation
	               sendErrorPacket((short) 4, "Unexpected package received", responseDatagramPacket.getSocketAddress());
	           }
			   
			   // Abort this session
               // error package
               fileDataLen = 0;
		   }
	   }
   }
   
   private Queue<DatagramPacket> getStackOfDATADatagramPackets(byte[] fileData, SocketAddress returnAddress) {
	   Queue<DatagramPacket> dataPacketStack = new LinkedList<DatagramPacket>();
		
		int numOfPackets = (fileData.length / DATAPacket.MAX_DATA_SIZE_BYTES) + 1;
		for (int i = 0; i < numOfPackets; i++) {
			short blockNumber = (short) (i + 1);
			
			int start = i * DATAPacket.MAX_DATA_SIZE_BYTES;
			int end = (i + 1) * DATAPacket.MAX_DATA_SIZE_BYTES;
			
			if (end > fileData.length)
				end = fileData.length;
			
			byte[] fileDataPart = Arrays.copyOfRange(fileData, start, end);
			dataPacketStack.add(DatagramPacketBuilder.getDATADatagram(blockNumber, fileDataPart, returnAddress));
		}
		
		return dataPacketStack;
	}
   
   public void writeFile(String filePath, String mode) {
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
	   
	   DatagramPacket responseDatagramPacket = DatagramPacketBuilder.getReceivalbeDatagram();
	   
	   try {
		   sendReceiveSocket.receive(responseDatagramPacket);
	   } catch (IOException e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot recieve DATA packet"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
       
       // Transfer ID of the server is in source port field of the Datagram package
       int serverTID = responseDatagramPacket.getPort();

	   byte[] responseDataBytes = responseDatagramPacket.getData();
	   
	   TFTPPacket tftpPacket = null;
	   try {
		   tftpPacket = new TFTPPacket(responseDataBytes);
	   } catch (TFTPPacketParsingError e) {
		   System.err.println(Globals.getErrorMessage("Client", "cannot parse TFTP Packet"));
		   e.printStackTrace();
		   System.exit(-1);
	   }
	   
	   // check if the opCode in response is of ACK
	   if (tftpPacket.getPacketType() == TFTPPacketType.ACK) {
		   System.out.println(Globals.getVerboseMessage("Client", "received ACK packet from server"));
		   
		   // reads a file on client side to create on the server side
		   byte[] fileData = fileManager.readFile(filePath);
		   Queue<DatagramPacket> dataDatagramStack = getStackOfDATADatagramPackets(fileData, responseDatagramPacket.getSocketAddress());
		   
		   // Keep track of block number to validate ACK packet
		   short blockNumber = 0;
		   boolean sendNextDataBlock = true;
		   
		   while (!dataDatagramStack.isEmpty()) {
		       
		       if (sendNextDataBlock) {
		           blockNumber++;
    			   DatagramPacket dataDatagramPacket = dataDatagramStack.remove();
    			   
    			   try {
    				   // sends DATA packet with the file content
    				   sendReceiveSocket.send(dataDatagramPacket);
    			   } catch (IOException e) {
    				   System.err.println(Globals.getErrorMessage("Client", "cannot send DATA packet to server"));
    				   e.printStackTrace();
    				   System.exit(-1);
    			   }
    			   
    			   System.out.println(Globals.getVerboseMessage("Client", "sent DATA packet to server"));
    			   
    			   // Should only send next DATA packet when a valid ACK received
    			   sendNextDataBlock = false;
		       }
		       
			   // recevies an ACK packet indicating that the server successfully wrote the file
			   DatagramPacket ackReceviablePacket = DatagramPacketBuilder.getReceivalbeDatagram();
			   try {
				   sendReceiveSocket.receive(ackReceviablePacket);
			   } catch (IOException e) {
				   System.err.println(Globals.getErrorMessage("Client", "cannot receive ACK packet from server"));
				   e.printStackTrace();
			   }

		       // Error handling:
		       // Validate if the package comes from the remote host of the same TID
		       int sourceTID = ackReceviablePacket.getPort();
		       if (serverTID != sourceTID) {
		           System.err.println(Globals.getErrorMessage("Client", String.format("receives un-expected transfer ID (expected %d, got %d)", serverTID, sourceTID)));
		           
		           // Send ERROR package with errorCode = 5 to the remote host where the package came from
		           sendErrorPacket((short) 5, "TID does not match", ackReceviablePacket.getSocketAddress());
		           
		           // Just skip this packet, waiting for valid ACK
		           continue;
		       }
		       
		       // Parse response package to see if it is a valid operation
		       byte[] responseAckBytes = ackReceviablePacket.getData();
		       try {
		           tftpPacket = new TFTPPacket(responseAckBytes);
		       } catch (TFTPPacketParsingError e) {
		           System.err.println(Globals.getErrorMessage("Client", "cannot parse TFTP Packet"));
		           e.printStackTrace();
		           System.exit(-1);
		       }
		       
		       if (tftpPacket.getPacketType() == TFTPPacketType.ACK) {
		           // Got an ACK package from server
                   ACKPacket ackPacket = null;
                   try {
                       ackPacket = new ACKPacket(responseAckBytes);
                   } catch (TFTPPacketParsingError e) {
                       System.err.println(Globals.getErrorMessage("Client", "cannot parse ACK Packet"));
                       e.printStackTrace();
                       System.exit(-1);
                   }
                   
                   System.out.println(Globals.getVerboseMessage("Client", "received ACK packet from server"));

                   // Validate block number
                   short ackBlockNumber = ackPacket.getBlockNumber();
                   if (ackBlockNumber != blockNumber) {
                       // Invalid ACK
                       System.out.println(Globals.getVerboseMessage("Client", String.format("got unexpected block number (expected %d, got %d) from ACK packet", blockNumber, ackBlockNumber)));
                       
                       // Send ERROR package with errorCode = 4 to the remote host where the package came from
                       sendErrorPacket((short) 4, "BlockNumber does not match", ackReceviablePacket.getSocketAddress());
                       
                       // Abort this session
                       dataDatagramStack.clear();
                   
                   } else {
                       // valid ACK, continue to send next DATA packet
                       sendNextDataBlock = true;
                   }
		       }
		       else {
		           // Error Handling
	               if (tftpPacket.getPacketType() == TFTPPacketType.ERROR) {
	                   // Got an ERROR package from server
	                   ERRORPacket errorPacket = null;
	                   try {
	                       errorPacket = new ERRORPacket(responseAckBytes);
	                   } catch (TFTPPacketParsingError e) {
	                       System.err.println(Globals.getErrorMessage("Client", "cannot parse ERROR Packet"));
	                       e.printStackTrace();
	                       System.exit(-1);
	                   }
	                   
	                   // Show the error
	                   System.out.println(Globals.getVerboseMessage("Client", String.format("received ERROR packet (errorCode=%d, errorMsg=%s) from server", errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
	               
	               } else {
	                   // Unexpected packet received
	                   System.out.println(Globals.getVerboseMessage("Client", String.format("received unexpected response (opCode=%d) from server.", tftpPacket.getOPCode())));
	                   
	                   // Send ERROR (errorCode=4) back to the host, indicating illegal operation
	                   sendErrorPacket((short) 4, "Unexpected package received", ackReceviablePacket.getSocketAddress());
	               }
	               
	               // Abort this session
	               dataDatagramStack.clear();
		       }
		   }
	   }
	   else {
	       // Error Handling
           if (tftpPacket.getPacketType() == TFTPPacketType.ERROR) {
               // Got an ERROR package from server
               ERRORPacket errorPacket = null;
               try {
                   errorPacket = new ERRORPacket(responseDataBytes);
               } catch (TFTPPacketParsingError e) {
                   System.err.println(Globals.getErrorMessage("Client", "cannot parse ERROR Packet"));
                   e.printStackTrace();
                   System.exit(-1);
               }
               
               // Show the error
               System.out.println(Globals.getVerboseMessage("Client", String.format("received ERROR packet (errorCode=%d, errorMsg=%s) from server", errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
           
           } else {
               // Unexpected packet received
               System.out.println(Globals.getVerboseMessage("Client", String.format("received unexpected response (opCode=%d) from server.", tftpPacket.getOPCode())));
               
               // Send ERROR (errorCode=4) back to the host, indicating illegal operation
               sendErrorPacket((short) 4, "Unexpected package received", responseDatagramPacket.getSocketAddress());
           }
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
