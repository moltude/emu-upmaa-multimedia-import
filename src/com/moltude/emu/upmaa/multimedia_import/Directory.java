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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

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
	
	// Files is a list of all of the files in the directory to process
	private File [] FILES;
	
	// Settings
	private Map settings;
	
	/**
	 * Loads the default properties file 
	 */
	public Directory() {
		loadSettings("default.properties");
	}
	
	/**
	 * Loads a specific properties file stored in ./conf
	 */
	public Directory(String file_name) {
		loadSettings(file_name);
	}
	
	/**
	 * Load settings from the specified properties file. See default.properties_sample for an example
	 * properties file 
	 * 
	 * @param folder
	 */
	public Directory(File folder) {
		if(validate(folder))
			loadSettings(getPropertiesFile(folder.getAbsolutePath()));
		else {
			System.out.println("There was a problem validating the directory.");
		}
	}
	
	/**
	 * This is the path to process. This folder must contain one and only one *.properties file
	 * 
	 * @param folder
	 * @return
	 */
	private boolean validate(File folder) {
		File[] files = folder.listFiles(new PropertiesFileFilter());
		
		if(files.length == 1 )
			return true;
		return false;
	}
	
	private String getPropertiesFile(String folder) {
		File file = new File(folder);
		File[] files = file.listFiles(new PropertiesFileFilter());
		
		return files[0].toString();
	}
	
	
	/**
	 * 
	 * @param properties_file
	 */
	public void importFiles() {
		if(filesToImportExist())
			doImport();
	}		

	/**
	 * Starts the ball running for the import // Set log files, status directories 
	 * Validates the provided directory
	 * 
	 */
	private void doImport( ) {
		// Get directory from *.properties file
		String directory = settings.getString("directory");
		
		if(directory == null || !(new File(directory).exists()) ) {
			logger.fatal("Could not locate import directory. Exiting!");
			System.exit(-1);
		}
		
		createProcessingFolders( directory );
		readDirectory( settings );
	}
	
	/**
	 * Load the settings for to process the images for the .properties file
	 * 
	 * @param properties_file
	 */
	private void loadSettings(String properties_file) {
		if(settings == null)
			settings = new Map();
		
		Map metadata = new Map();
		Properties properties = new Properties();
		InputStream is = null;
		try {
			if(new File(properties_file).exists() ) {
				is = new FileInputStream(new File(properties_file));
			} else {
				is = this.getClass().getClassLoader().getResourceAsStream(properties_file);
			}
			
			if(is != null) {
				properties.load(is); 
				is.close();
			} else {
				System.out.println("Could not load properties " + properties_file);
				System.exit(0);
			}
			
			Iterator<Object> iterator = properties.keySet().iterator();
			
			while(iterator.hasNext()) {
				String key = (String) iterator.next();
				String value = properties.getProperty(key);
				// if the setting is metadata then put it in the metadata map
				if(key.startsWith("metadata.")) {
					// If mapping to multi-valued field convert String to String array
					if(key.endsWith("_tab")) {
						metadata.put(key.replace("metadata.", ""), StringUtils.delimitedListToStringArray(value, "|"));
					} else {
						metadata.put(key.replace("metadata.", ""), value);
					}
				} else {
					settings.put(key, value);
				}
			}
			
			settings.put("metadata",metadata);
			 
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void printSettings() {
		if(settings==null || settings.size()==0) {
			System.out.println("There is nothing in settings..exiting()");
			System.exit(0);
		}
		
		Iterator <String> iterator = settings.keySet().iterator();
		
		while(iterator.hasNext()) {
			String key = iterator.next();
			String value = settings.getString(key);
			System.out.println(key + " -> " + value);
		}
	}
	
	/**
	 * Creates the ./Error and ./Success folders within the directory to process. These folders also have dated sub-folders where the images will be moved.  This is confgiured to
	 * support applications that are run on a schedule.<br><br>
	 * --dir<br>
	 * ----Error<br>
	 * ------yyyy.MM.dd<br>
	 * ----Success<br>
	 * ------yyyy.MM.dd<br>
	 * ----logs<br>
	 * ------yyyy.MM.dd<br><br>
	 * @param dir - Directory to process
	 */
	private void createProcessingFolders(String dir) {
		String date_folder = getDateFolder();
		
		directory 			= new File(dir);
		error_directory 	= new File(dir+java.io.File.separator+"Error"+java.io.File.separator+date_folder);
		success_directory 	= new File(dir+java.io.File.separator+"Success"+java.io.File.separator+date_folder);
		
		if(!error_directory.exists())
			error_directory.mkdirs();
		
		if(!success_directory.exists())
			success_directory.mkdirs();
		
		// log files 
		// dir = dir + java.io.File.separator + "logs" + java.io.File.separator + getDateFolder();	
		// logFile = new LogFileWriter(new File(dir + java.io.File.separator + "emu-multimedia-import.txt"));
		// errorLogFile = new LogFileWriter(new File(dir + java.io.File.separator + "emu-multimedia-import-error_log.txt"));
		// logger = new ImageHarvesterLogger(logFile, dir);

		// List of the image files 
		// TODO change this so that it doesn't only include image files 
		FILES = directory.listFiles(new ImageFileFilter());
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
	 * Read the image directory and process image if needed
	 * @param settings
	 * @param resizeImage
	 */
	private void readDirectory(Map settings) {
		try {
			Map metadata = settings.getMap("metadata");
			String target_module = settings.getString("target_module");
			String target_id = settings.getString("target_id");
			
			for(File file : FILES ) {
				// imageValidator take the file and compares the file's metadata
				// to data in EMu
				Validator validator = new Validator(file);
				// Checks EMu for an existing image with a similar file name (to cut down on duplicate images).
				if( settings.getString("unique_identifier").equalsIgnoreCase("true") && validator.isIdentifierUnique() ) {
					moveFile(file, error_directory);
					continue;
				}

				long [] target_irns = validator.getTargetIrns(target_module, target_id);
				// If the target irns could not all be resolved then log error and continue
				if(target_irns == null) {
					// Image metadata does not match EMu data
					logErrorMessage("Could not find matching catalog record(s) in EMu for " + 
							validator.getObjectName() + " | File "+file.getName()+" moved to Error folder\r\n");
					moveFile(file, error_directory);
					continue;
				}
				// If all of the object numbers in the metadata match data in EMu 
				// Get metadata from metadata.txt (if exists)
				metadata.putAll( validator.getAuxMetadata() );
				// Get metadata from file (if present)
				metadata.putAll( validator.getImageMetadata() );
				
				// create Emu record
				createMultimediaRecord(target_irns, metadata, file);
				
			} // end for loop
		} catch (Exception e ) { 
			// TODO log the exception 
			e.printStackTrace();
		}
	}

	
	/**
	 * Uploads image to EMu and links it to catalog records. 
	 *  
	 * @param target_irns - irns of target records to link the new emultimedia to
	 * @param metadata
	 * @param file 
	 */
	private void createMultimediaRecord(long[] target_irns, Map metadata, File file) {
		String target_module = settings.getString("target_module");
		String target_id = settings.getString("target_id");
		try {
			// TODO make this smoother
			Import impoter = new Import(metadata);
			// Try to upload and link the image to all catalog records
			int import_status = impoter.doImport(file, target_irns, target_module, target_id);
			// If importAndLinkImage returns 1 then it was sucessful for image and linking to all objects
			if(import_status == 1) { 
				// 	on success move the image file to another 'success directory'		
				moveFile(file, success_directory);
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
			e.printStackTrace();
		} 
	}

	/**
	 * Write the error message to the errorLogFile
	 * 
	 * @param msg - error message to wirte
	 */
	private void logErrorMessage(String msg) {
		System.out.println(msg);
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
			System.out.println(src.getAbsolutePath() + " --> " + dest.getAbsolutePath());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Is there anything to process?
	 * 
	 * @return true if there is // false if not
	 */
	private boolean filesToImportExist() {
		if(FILES == null) {
			FILES = new File(settings.getString("directory")).listFiles(new ImageFileFilter());
		}

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


/**
 * Valid properties files
 *
 *
 */
class PropertiesFileFilter implements FileFilter {
	  @Override
	public boolean accept(File pathname) {
	    if (pathname.getName().toLowerCase().endsWith(".properties") && !pathname.getName().startsWith(".") )
	      return true;
	    return false;
	  }
	}
