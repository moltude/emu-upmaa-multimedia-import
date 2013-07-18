package com.moltude.emu.upmaa.multimedia_import;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.util.StringUtils;

import com.kesoftware.imu.IMuException;
import com.kesoftware.imu.Map;
import com.kesoftware.imu.Terms;
import com.moltude.emu.upmaa.imu.Connection;

public class Import {
	// Holds the image metadata to be added to the emultimedia record
	private Map imageMetadata;
	// Columns to be return from a query to the catalog module
	private final String [] COLUMNS = {
			"MulMultiMediaRef_tab.(irn)",
			"MulMultimediaType_tab",
			"MulMultimediaNotes0",
			"irn",
			"CatObjectNumber"
		};
	
	// Logger 
	static Logger logger = LogManager.getLogger(com.moltude.emu.upmaa.multimedia_import.Import.class.getName());
	
	/**
	 * Constructor
	 * @param logFile 
	 */
	public Import(Map _metadata) {
		imageMetadata = new Map();
		if(_metadata != null) {
			imageMetadata.putAll(_metadata);
		}
		else {
			imageMetadata = new Map();
		}
		
		loadProperties();
	}
	
	/**
	 * Loads the properties from the config.properties file
	 */
	private void loadProperties() {
		Properties properties = new Properties();
		InputStream fis;
		try {
			fis = this.getClass().getClassLoader()
					.getResourceAsStream("config.properties");

			if (fis != null) {
				properties.load(fis);
				fis.close();
			} else {
				logger.fatal("Could not load config properties. Exiting!");
				System.exit(0);
			}
			if (fis != null) {
			    fis.close();
			}
		} catch (FileNotFoundException fnfe) {
			logger.error(fnfe.getMessage());
		} catch (IOException ioe) {
			logger.error(ioe.getMessage());
		}
	}
	
	/**
	 * Called on to upload and link the image to records in the target module<br> 
	 * <br>
	 * @param file
	 * @param catIrns
	 * @return int values indicating error or success<br>
	 *		1: 	If image was successfully uploaded and linked to all catalog records<br> 
	 *		0:	If upload success but failed to attach multimedia record to one or more catalog records<br>
	 *	   -1: 	If unable to upload image, because no image was uploaded there was also no attempt to link it 
	 *			to any catalog records<br>
	 */
	public int doImport(File file, int[] catIrns) {
		// TODO finish writing uploadImageToEmu pending response from KE
		long emultimedia_irn = -1;
		emultimedia_irn = uploadImage(file, getObjectNumbers(catIrns) + ".  "+ imageMetadata.get("MulDescription") );
		// If failed to upload image to EMu then return 		
		if(emultimedia_irn == -1) {
			return -1;
		}		
		// If image upload success then attach the multimedia record to catalog records
		else {
			boolean attachedToAllobjects = true;
	
			for(int target_irn : catIrns) {
				// add this irn to the mulRef column of the catalog record
				if( attachMultimedia(emultimedia_irn, target_irn, "ecatalogue", getCatalogType(catIrns.length) ) == false ) {
					// if the script was unable to link the image to one of the catalog records then 
					// returns false, then log the one it failed on
					attachedToAllobjects = false;
				}
			}
			// if able to attach image record to all catalog records return true
			if(attachedToAllobjects) {	
				return 1;
			}
			// if unable to attach image record to at least one catalog record then return 0
			else { 
				return 0;
			}
		}	
	}

	/**
	 * Add the supplied image to the first position in the multimediaRef array<br>
	 * 
	 * @param emultimedia_irn
	 * @param catType 
	 * @param i
	 * @return 
	 */
	private boolean attachMultimedia(long emultimedia_irn, long target_irn, String target_module, String catType) {
		Map updates = new Map();
		Connection con = new Connection(target_module);
		
		con.connect();
		Map target_record = con.search(target_irn, COLUMNS);
		
		// If no target record was found
		if(target_record == null ) 
			return false;
		
		// Put this image in the first position
		Map []image = target_record.getMaps("MulMultiMediaRef_tab");
		// IRN
		String [] existingIRNs = new String [image.length];
		// Notes
		String [] existingNotes = new String[image.length];
		// Type
		String [] existingType = new String[image.length];
		
		for(int t = 0; t<image.length; t++ ) {
			existingIRNs[t] = image[t].getString("irn");
			existingNotes[t] = image[t].getString("MulMultimediaNotes0" );
			existingNotes[t] = image[t].getString("MulMultimediaNotes0" );
		}
		// Updates to the multimedia module
		 updates.put("MulMultiMediaRef_tab", addArray(existingIRNs, new String [] { Long.toString(emultimedia_irn) } ));
		 updates.put("MulMultimediaNotes0", addArray( existingNotes, new String[] { "" /* TODO standard note text */ } ));
		 updates.put("MulMultimediaType_tab", addArray( existingType, new String[] { catType }) );
		
		 // Pass updates
		con.connect();
		try {
			con.updateRecord(target_irn, updates, null);
		} catch (IMuException e) {
		}
		con.disconnect();
		
		return true;
	}

	/**
	 * Connect to the multimedia module and upload file to the server.  The multimedia
	 * record will be created with standard metadata<br>
	 * <br>
	 * Process: <br>
	 * 1. Create shell multimedia record <br>
	 * 2. Pass file handle to 'master' column<br>
	 * 
	 * @param file
	 * @param description 
	 * @return the IRN of the multimedia record that was created or -1 if unable to create record
	 * 
	 */
	private long uploadImage(File file, String description) {
		Connection con = new Connection("emultimedia");
		Map resource = new Map();
		FileInputStream fs = null;
		try {
			// Create shell multimedia record
			con.connect();
			long irn = con.createRecord(imageMetadata); // Updata with default metadata { Museum for Publication; Penn Museum Photo Studio }
			con.disconnect();			
			// Pass update to 'master' column to create additional resolutions and full image validation
			imageMetadata = new Map(); 
			// this was included per instructions on users form. See posting:  
			// http://www.kesoftware.com/emuusers-forum/topic/381.html#p1242
			imageMetadata.put( "MulDocumentType", "M" );
			// This is probably unnecessary but whatever
			imageMetadata.put( "MulIdentifier",  file.getName() );
			// Image description
			imageMetadata.put("MulDescription", "Photograph of "+description);
			// Image data
			resource.put("name", file.getName() );		
			fs = new FileInputStream(file); // File object pointing at ./sample_images/image.jpeg
			resource.put("data", fs);
			imageMetadata.put( "master", resource );
			try { 
				con.connect();
				con.updateRecord(irn, imageMetadata, null);
				con.disconnect();
			} catch(IMuException e) {
				// If an imuex was thrown then there was probably some kind of error in creating the 
				// record and it should be deleted
				// TODO delete record if it exists
				logger.error("Unable to create record.");
				return -1; 
			}
			// Return the IRN of the multimedia record that was created
			return irn;
		} catch (IOException ioe) {
			logger.error(ioe.getMessage());
			return -1;
		}
		finally {
			if (fs != null) {
				try {
					fs.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	/**
	 * Returns a properly formatted string of the object numbers in the photo. Numbers are
	 * seperated by commas and the last number is joined with an ' and '.<br>
	 * <br>
	 * e.g.<br>
	 * <br>
	 * <ObjectA>, <ObjectB> and <ObjectC><br>
	 * E115, L-606-125 and C255<br>
	 * 
	 * @param target_irns
	 * @return String of object numbers in the photo
	 */
	private String getObjectNumbers(int[] target_irns) {
		Connection con = new Connection("ecatalogue");
		Terms term = new Terms();
		Terms irns = term.addOr();
		
		for(int t=0;t<target_irns.length; t++) {
			irns.add("irn", Integer.toString(target_irns[t]) );
		}
		
		con.connect();
		Map[] maps = con.search(term,"CatObjectNumber");
		
		ArrayList<String> object_numbers = new ArrayList<String>();
		
		for(Map map : maps) {
			object_numbers.add(map.getString("CatObjectNumber"));
		}
		
		StringBuilder sb = new StringBuilder(StringUtils.arrayToCommaDelimitedString(object_numbers.toArray()));
		sb.replace(sb.lastIndexOf(","), sb.lastIndexOf(",")+1, "and");
		
		return sb.toString();
	}
	
	/**
	 * If there is more than one catalog record then the Type field on the catalog module should be set
	 * to "In Group Overview" else "Primary View"<br>
	 * 
	 * @param length
	 * @return String of the Catalog module Type field
	 */
	private String getCatalogType(int length ) {
		// If the MulMultimediaType field isn't set then set it to
		// either "Primary View" or "In Group Overview". If it was 
		// already set then return the specified value
		if(imageMetadata.getString("MulMultimediaType_tab") == null || imageMetadata.getString("MulMultimediaType_tab").isEmpty())  {
			if(length == 1) 
				return "Primary View";
			else 
				return "In Group Overview";
		} else 
			return imageMetadata.getString("MulMultimediaType_tab"); 
	}

	/**
	 * Added A to B and returns C 
	 * 
	 * @param A
	 * @param B
	 * @return
	 */
	private String [] addArray(String [] A, String [] B) {
		String [] C = new String [A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);
		return C;
	}

}
