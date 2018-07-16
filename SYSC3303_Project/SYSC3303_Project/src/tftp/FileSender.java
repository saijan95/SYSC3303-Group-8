package tftp;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileSender {
	
	private DatagramSocket 	fileSocket;
	private InetAddress 	remoteHost;
	private int				remotePort;
	private FileInputStream	file;
	
	private byte[] buffer;
	
	public FileSender(String path, InetAddress dest, int port, DatagramSocket sock) throws FileNotFoundException {
		fileSocket = sock;
		remoteHost = dest;
		remotePort = port;
		
		file = new FileInputStream(path);
		buffer = new byte[512];
	}
	
	public void send() throws TFTPTransferError {
		
		
	}
	
	private void sendNextData() {
		
	}
	

}
