/**
 * The Directory class is used to read and process 
 * files in a folder for ingestion into EMu.
 * 
 */
package com.moltude.emu.upmaa.multimedia_import;

/**
 * @author Scott Williams
 * @Date
 */


import java.io.File;
import java.io.FileFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;


import com.kesoftware.imu.Map;

public class Directory {
	// Directory on store array that images will be added to
	private File directory 			= new File("");
	private File error_directory 	= new File("");
	private File success_directory 	= new File("");
	
	// Log files for this process
	static Logger logger = LogManager.getLogger(com.moltude.emu.upmaa.multimedia_import.Directory.class.getName());
	
	//  Need to change these to Loogers 
	//	private LogFileWriter errorLogFile;
	//	private LogFileWriter logFile;
	
	// Files is a list of all of the image files in the IMAGE_DIRECTORY
	private File [] FILES;
	
	/**
	 * Default constructor. 
	 */
	public Directory() {
		
	}
	
	/**
	 * 
	 * @param dir - The directory to process 
	 */
	public Directory(String dir) {
		setLogFiles(dir);
	}

	/**
	 * 
	 * @param dir - Directory to process
	 */
	private void setDirectory(String dir) {
		String date_folder = getDateFolder();
		
		directory 			= new File(dir);
		error_directory 	= new File(dir+java.io.File.separator+"Error"+java.io.File.separator+date_folder);
		success_directory 	= new File(dir+java.io.File.separator+"Success"+java.io.File.separator+date_folder);
		
		if(!error_directory.exists())
			error_directory.mkdirs();
		
		if(!success_directory.exists())
			success_directory.mkdirs();
		
		// List of the image files 
		// TODO change this so that it doesn't only include image files 
		FILES = directory.listFiles(new ImageFileFilter());
	}
	

	/**
	 * 	Creates a ./logs/ directory within the processesing directory and creates log files for 
	 * @param dir
	 */
	public void setLogFiles(String dir) {
		dir = dir + java.io.File.separator + "logs" + java.io.File.separator + getDateFolder();
		
		
//		logFile = new LogFileWriter(new File(dir + java.io.File.separator + "emu-multimedia-import.txt"));
//		errorLogFile = new LogFileWriter(new File(dir + java.io.File.separator + "emu-multimedia-import-error_log.txt"));
		// logger = new ImageHarvesterLogger(logFile, dir);
	}
	
	/**
	 * Get the current date as yyyy.MM.dd
	 * 
	 * @return String of the current date as yyyy.MM.dd
	 */
	private String getDateFolder() {
		return DateTime.now().toString("yyyy.MM.dd"); 
	}
	
	/**
	 * Imports all of the images in the specfied directory 
	 * @param directory
	 * @param genericMetadata
	 */
	public void doImport(String directory, Map genericMetadata, Map settings) {
		if(directory == null || !(new File(directory).exists()) ) {
			logger.fatal("Could not locate import directory. Exiting!");
			System.exit(-1);
		}
		setLogFiles( directory );
		setDirectory( directory );
		// Put the stock image metadata in all images and do not resize the images provided
		readDirectory(genericMetadata, settings.getBoolean("dup_check"));
	}
	
	/**
	 * Read the image directory and process image if needed
	 * @param stockImageMetadata
	 * @param resizeImage
	 */
	private void readDirectory(Map stockImageMetadata, boolean checkForDuplicateImage) {
		try {
			if(!filesToImportExist()) {
				// if there are no images to import
				logger.warn("There are no files to improt");
				return;
			}
			// For every file in the directory
			for(File file : FILES ) {
				boolean addImage = true;
				// imageValidator take the file and compares the file's metadata
				// to data in EMu
				Validator validator = new Validator(file);
				// Checks EMu for an existing image with a similar file name (to cut down on duplicate images).
				if( checkForDuplicateImage ) {
					if ( validator.isIdentifierUnique() ) {
						logErrorMessage("WARNING: Possible duplicate image exists for file "+file.getName() + 
								". Search for  " + file.getName().substring(0, file.getName().lastIndexOf(".")) + 
								" in the 'Identifier' field in the Multimedia module\r\n");
						moveFile(file, error_directory);
						addImage = false;
					}
				} 

				if( addImage ){
					int [] catIrns = validator.getCatalogIrns();
					// If all of the object numbers in the metadata match data in EMu 
					if(catIrns != null) {
						stockImageMetadata.putAll( validator.getAuxMetadata()   );
						stockImageMetadata.putAll( validator.getImageMetadata() );
						createEMuRecords(catIrns, stockImageMetadata, file);
					} else { 	
						// Image metadata does not match EMu data
						logErrorMessage("Could not find matching catalog record(s) in EMu for " + 
								validator.getObjectName() + " | File "+file.getName()+" moved to Error folder\r\n");
						moveFile(file, error_directory);
					} // end else 
				} // end else 
			} // end for loop
		} catch (Exception e ) { 
			// TODO log the exception 
		}
	}

	
	/**
	 * Uploads image to EMu and links it to catalog records. If required the ImageImporter
	 * will resize the image
	 *  
	 * @param catIrns 
	 * @param stockImageMetadata
	 * @param file 
	 */
	private void createEMuRecords(int[] catIrns, Map stockImageMetadata, File file) {
		try {
			// import and link the images
			
			// TODO make this smoother
			Import impoter = new Import(stockImageMetadata);
			// Try to upload and link the image to all catalog records
			int import_status = impoter.doImport(file, catIrns);
			// If importAndLinkImage returns 1 then it was sucessful for image and linking to all objects
			if(import_status == 1) { 
				// 	on success move the image file to another 'success directory'		
				moveFile(file, success_directory);
				
				// TODO 
				// Find some other way to log each object + image
//				logger.logObjects(catIrns, file.getName());
			}
			// If the image couldn't be linked to all catalog records then log it but move it to success since the image has been added to EMu
			else if (import_status == 0) {
				logErrorMessage("Could not attach to all catalog records. \t File "+file.getName()+" moved to Error folder.");
				 moveFile(file, error_directory);
			}
			// If the image couldn't be uploaded then move it to error
			else if (import_status == -1) {
				logErrorMessage("Error importing image. \t File "+file.getName()+" moved to Error folder.");
				moveFile(file, error_directory);
			}
		} catch (Exception e) { 
			logger.error(e.getMessage());
		} 
	}

	/**
	 * Write the error message to the errorLogFile
	 * 
	 * @param msg - error message to wirte
	 */
	private void logErrorMessage(String msg) {
		logger.error(msg);
	}

	/**
	 * Moves a file from the source directory to the dest directory
	 * 
	 * @param src
	 * @param dest
	 * @throws Exception
	 */
	private void moveFile(File src, File dest) {
		try {
			src.renameTo( new File(dest.getAbsolutePath()+java.io.File.separator+src.getName()) );
		} catch(NullPointerException e) { 
			logger.error(e.getMessage());
		}
	}
	
	/**
	 * Is there anything to process?
	 * 
	 * @return true if there is // false if not
	 */
	private boolean filesToImportExist() {
		if(FILES.length > 0)
			return true;
		else 
			return false;
	}	
}

/**
 * Valid files to import into EMu
 * 
 * @author scottwilliams
 *
 */
class ImageFileFilter implements FileFilter {
	  @Override
	public boolean accept(File pathname) {
	    if (pathname.getName().toLowerCase().endsWith(".jpg") && !pathname.getName().startsWith(".") )
	      return true;
	    if (pathname.getName().toLowerCase().endsWith(".jpeg") && !pathname.getName().startsWith(".") )
	      return true;
	    if (pathname.getName().toLowerCase().endsWith(".tiff") && !pathname.getName().startsWith(".") )
		      return true;
	    if (pathname.getName().toLowerCase().endsWith(".bmp") && !pathname.getName().startsWith(".") )
		      return true;
	    if (pathname.getName().toLowerCase().endsWith(".tif") && !pathname.getName().startsWith(".") )
		      return true;
	    if (pathname.getName().toLowerCase().endsWith(".pdf") && !pathname.getName().startsWith(".") )
		      return true;
	    return false;
	  }
	}