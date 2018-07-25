import java.net.InetAddress;

public class ErrorHandler {
	TFTPSocket tftpSocket;
	
	public ErrorHandler(TFTPSocket datagramSocket) {
		this.tftpSocket = datagramSocket;
	}
	
    private void sendErrorPacket(short errorCode, String errorMessage, InetAddress remoteAddress, int remotePort) {
        ERRORPacket errorPacket = TFTPPacketBuilder.getERRORDatagram(errorCode, errorMessage, remoteAddress, remotePort);
       
        System.err.println(Globals.getErrorMessage("ErrorHandler", String.format("sending error packet, errorCode: %d, errorMessage: %s", errorCode, errorMessage)));
        tftpSocket.send(errorPacket);
   }
    
	public void sendFileNotFoundErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.FILE_NOT_FOUND, errorMessage, remoteAddress, remotePort);
	}
	
	public void sendAccessViolationErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.ACCESS_VIOLATION, errorMessage, remoteAddress, remotePort);
	}
	
	public void sendDiskFullErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.DISK_FULL, errorMessage, remoteAddress, remotePort);
	}
	
	public void sendIllegalOperationErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.ILLEGAL_TFTP_OPERATION, errorMessage, remoteAddress, remotePort);
	}
	
	public void sendUnknownTrasnferIDErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.UNKNOWN_TID, errorMessage, remoteAddress, remotePort);
	}
	
	public void sendFileExistsErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.FILE_EXISTS, errorMessage, remoteAddress, remotePort);
	}
}
