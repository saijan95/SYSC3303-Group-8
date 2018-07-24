import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class provides an interface for objects to write or read files from hard drive
 * 
 * @author Group 8
 */

public class FileManager {
	// directory where the file that are transferred will be saved
	private static String destinationDirectoryStr = "transfered_files";
	
	/**
	 * This class has the necessary variables to indicate the result
	 * of read or write to the hard drive
	 * 
	 * @author Group 8
	 */
	public class FileManagerResult {
		public byte[] fileBytes = null;
		boolean accessViolation = false;
		boolean fileNotFound = false;
		boolean diskFull = false;
		boolean error = false;
	}

	public FileManager() {}
	
	/**
	 * Read file from hard dive and return data in list of bytes
	 * 
	 * @param fileName: fileName
	 * 
	 * Return FileManagerResult containing the list of bytes read or error flagged
	 */
	public FileManagerResult readFile(String fileName) {		
		FileManagerResult res = new FileManagerResult();
		
		File file = new File(fileName);
		
		byte[] fileBytes = new byte[(int)file.length()];
		
		try {
			FileInputStream fileInputStream = new FileInputStream(file); 
			fileInputStream.read(fileBytes);
			fileInputStream.close();
			res.fileBytes = fileBytes;
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;
			
			// if the error message contains "cannot find the file"
			// then set fileNotFound to true
			if (e.getMessage().contains("cannot find the file"))
				res.fileNotFound = true;
			
			
			// set error flag
			res.error = true;
		}
		
		return res;
	}
	
	/**
	 * Writes file data in bytes to a hard drive with the given file name
	 * 
	 * @param fileName: file name
	 * @param data: file data in byte form
	 * 
	 * Return FileManagerResult containing the errors flags 
	 */
	public FileManagerResult writeFile(String fileName, byte[] data) {
		FileManagerResult res = new FileManagerResult();
		
		fileName = System.getProperty("user.dir") + File.separator + destinationDirectoryStr + File.separator + fileName;
		File file = new File(fileName);
		
		// create a new file if it does not exist
		try {
			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			
			file.createNewFile();
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;
			
			// Check error message to see if the error is about disk full
			if (e.getMessage().contains("not enough space") || e.getMessage().contains("Not enough space")
			        || e.getMessage().contains("No space left"))
			    res.diskFull = true;
			
			// set error flag
			res.error = true;
		}
		
		// stop the write function if an error occurred
		if (res.error) {
			return res;
		}
			
		// write to file
		try {
			// append to previous data on file
			FileOutputStream fileOutputStream = new FileOutputStream(file, true);
			fileOutputStream.write(data);
			fileOutputStream.close();
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;
			
			// set error flag
			res.error = true;
		}
		
		return res;
	}
}
