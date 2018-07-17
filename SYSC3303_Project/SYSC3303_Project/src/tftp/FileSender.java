package tftp;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileSender {
	
	private TFTPSocket 	fileSocket;
	private InetAddress 	remoteHost;
	private int				remotePort;
	private FileInputStream	file;
	
	private byte[] buffer;
	
	public FileSender(String path, InetAddress dest, int port, TFTPSocket sock) throws FileNotFoundException {
		fileSocket = sock;
		remoteHost = dest;
		remotePort = port;
		
		file = new FileInputStream(path);
		buffer = new byte[512];
	}
	
	public void send() throws TFTPTransferError {
		
		
	}
	
	private void sendData(short blockNum) {
	}
	

}
