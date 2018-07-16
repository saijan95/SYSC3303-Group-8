import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class provides an interface for objects to write or read files from hard drive
 * 
 * @author Group 8
 */

public class FileManager {

	public FileManager() {}
	
	/**
	 * Read file from hard dive and return data in list of bytes
	 * 
	 * @param fileName: fileName
	 * 
	 * Return list of bytes 
	 */
	public byte[] readFile(String fileName) {

		
		Path path = Paths.get(fileName);
		
		byte[] fileBytes = null;
		try {
			// read all contents of the file
			fileBytes = Files.readAllBytes(path);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			System.exit(-1);
		}
		
		return fileBytes;
	}
	
	/**
	 * Writes file data in bytes to a harddrive with the given file name
	 * 
	 * @param fileName: file name
	 * @param data: file data in byte form
	 */
	public void writeFile(String fileName, int blockNumber, byte[] data) {
		File file = new File(fileName);
		
		// create a new file if it does not exist
		try {
			file.createNewFile();
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", String.format("cannot create file %s.", fileName)));
			e.printStackTrace();
			System.exit(-1);
		}
		
		// write to file
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file, true);
			fileOutputStream.write(data);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			System.err.println(Globals.getErrorMessage("FileManager", String.format("cannot find file %s.", fileName)));
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", String.format("cannot write to file %s.", fileName)));
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
