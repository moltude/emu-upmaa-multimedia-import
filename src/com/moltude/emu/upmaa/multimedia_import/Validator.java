package com.moltude.emu.upmaa.multimedia_import;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.activation.MimetypesFileTypeMap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.iptc.IptcDescriptor;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.xmp.XmpDescriptor;
import com.drew.metadata.xmp.XmpDirectory;
import com.kesoftware.imu.Map;
import com.kesoftware.imu.Terms;
import com.moltude.emu.upmaa.imu.Connection;

public class Validator {
	// Stores metadata
	private Metadata  METADATA;
	// File object to store the file
	private File FILE;
	// The METADATA_FILE object was added to accomodate the inclusion 
	// of a text file that provides the same multimedia metadata as 
	// the IPTC data from Photo Archives.  The METADATA_FILE object
	// will always point to metadata.txt in the directory currently 
	// being processed
	private File METADATA_FILE;
	
	/**
	 * TODO rewrite this so that the row metadata is put into a map
	 * for easier storage and retrivial.
	 */
	// These represent the indices of metadate values in a String []  
	private int index_identifier;
	private int index_creator;
	private int index_file;
	private int index_resource_type;
	private int index_type;

	/**
	 * imageValidator Constructor
	 * 
	 * @param File object of the image to validate against EMu data
	 */
	
	public Validator( File _file ) throws ImageProcessingException, IOException {
		FILE = _file;
		METADATA_FILE = null;
		METADATA = null;
		
		
		this.setIndexCreator(-1);
		this.setIndexFile(-1);
		this.setIndexObjectNumber(-1);
		this.setIndexResourceType(-1);
		// Type in this case refers to the 'Type' field on the catalog modue
		this.setIndexType(-1);
		
		try {
			if(isImage(FILE))
				METADATA = ImageMetadataReader.readMetadata(FILE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Taken from Stackoverflow post /9643228/
	 * @param file
	 * @return
	 */
	private boolean isImage(File file) {
		String mimetype = URLConnection.guessContentTypeFromName(file.getAbsolutePath());
        
        if(mimetype.contains("image"))
            return true;
        else 
            return false;
	}

	/**
	 * @return the indexCreator
	 */
	private int getIndexCreator() {
		return index_creator;
	}
	
	/**
	 * @param indexCreator the indexCreator to set
	 */
	private void setIndexCreator(int indexCreator) {
		this.index_creator = indexCreator;
	}
	/**
	 * @return the indexResourceType
	 */
	private int getIndexResourceType() {
		return index_resource_type;
	}
	/**
	 * @param indexResourceType the indexResourceType to set
	 */
	private void setIndexResourceType(int indexResourceType) {
		this.index_resource_type = indexResourceType;
	}
	
	private void setIndexType(int indexType) {
		this.index_type = indexType;	
	}
	
	
	/**
	 * @return the indexObjectNumber
	 */
	private int getIndexType() {
		return index_type;
	}

	/**
	 * @return the indexObjectNumber
	 */
	private int getIdentifierIndex() {
		return index_identifier;
	}
	/**
	 * @param indexObjectNumber the indexObjectNumber to set
	 */
	private void setIndexObjectNumber(int indexObjectNumber) {
		this.index_identifier = indexObjectNumber;
	}
	/**
	 * @return the indexFile
	 */
	@SuppressWarnings("unused")
	private int getIndexFile() {
		return index_file;
	}
	/**
	 * @param indexFile the indexFile to set
	 */
	private void setIndexFile(int indexFile) {
		this.index_file = indexFile;
	}
	/**
	 * Validates the image file represented by FILE against catalog data in EMu.
	 * This is done in two possible ways<br>
	 * 		1. Use IPTC ObjectName metadata to get the museum object numbers<br>
	 * 		2. Use metadata.txt to get the museum object numbers<br><br>
	 * In eitehr case, if the image passes validation the IRNs of the catalog records
	 * thie image should be linked to are returned in an int [].  If it does not 
	 * pass validation then validateImages() returns NULL<br>
	 * @param target_id 
	 * @param target_module 
	 *   
	 * @return int [] If FILE passes validation 
	 * @return NULL if FILE does not pass validation
	 */
	public long [] getTargetIrns(String target_module, String target_id) {
		// Open the image and read the data in ObjectNumber
		try {
			if(metadataFileExists()) {
				// see if there is a metadata file to get this image data from
				String metadata_row = findRowInMetadataTextFile(FILE.getName()); 
				if( metadata_row != null) {
					System.out.println(metadata_row);
					// then the metadata file exists and a row was returned
					// now I just need to get the object numbers and return 
					// 
					return getTargetIRN( getIdentifierFromTextFile( metadata_row ), target_module, target_id );
				} else { 
					return null;
				}
			}
			// if metadata file does not exist then use the file metadata field
			else {
				String [] irns = getIdentifierFromMetadata(); 
				if( irns != null )
					return getTargetIRN( irns, target_module, target_id );
				else 
					return null;
			}
		} catch (Exception e) { e.printStackTrace(); return null; }
	}
	
	/**
	 * Returns the target object number from the external metadata.txt file.
	 * <br>
	 * @param data - String of data from metadata.txt
	 * @return String [] of objectNumbers gotten from text file
	 * @return NULL if index of object number is null
	 */
	private String[] getIdentifierFromTextFile(String data) {
		String [] metadata_values = data.split("\t");
		if(getIdentifierIndex() != -1)
			return toArray( metadata_values[getIdentifierIndex()] );
		return null;
	}

	/**
	 * Returns the data in the XMP Title field. This is where UPMAA stores the object numbers in images
	 * 
	 * @return A cleaned String array of object numbers
	 * @return NULL if no value in Objec Name (Title) or if metadata is unreadable 
	 */
	private String [] getIdentifierFromMetadata() {
		String objectName = null;
		try {
			
			/**
			 * XMP
			 * The XMP TAG_TITLE is commented out of com.drew.metadata package and this was
			 * fixed in the local copy of the data. This fixes the 64 character limit problem
			 * in the IPTC Object Name field.
			 */
			XmpDirectory directory = METADATA.getDirectory(XmpDirectory.class);
			XmpDescriptor desc = new XmpDescriptor((XmpDirectory) directory);
			
			// TODO get the fixed library from my work computer
			objectName = desc.getDescription(XmpDirectory.TAG_TITLE);
			
			/**
			 * IPTC
			 * This is no longer beging used because IPTC Object Name (title) has a 64 character limit
			 * and there is no warning // error message when that limit is exceded. The data is just
			 * truncated.
			 */
//			Directory directory = METADATA.getDirectory(IptcDirectory.class);
//			IptcDescriptor desc = new IptcDescriptor((IptcDirectory) directory);
//			String objectName = desc.getObjectNameDescription();
			
			return toArray(objectName);
			
		} catch (NullPointerException e1) { return null; }
		catch (Exception e) { e.printStackTrace(); return null; }
	}
	
	/**
	 * Converts a comma delimited string to a String array. Also removes white space and converts to upper case
	 * 
	 * @param string - A string value
	 * @return String array with extraneous whitespace removed and all upper case
	 */
	private String[] toArray(String string) {
		StringTokenizer strToken = new StringTokenizer(string.trim(),",");
		String [] strings = new String[strToken.countTokens()];
		int t = 0;
		while(strToken.hasMoreTokens()) {
			strings[t] = strToken.nextToken().toUpperCase().trim();
			t++;
		}		
		return strings;
	}

	/**
	 * Uses the supplied objectNumbers as query terms in the Object Number field in EMu
	 * and if a matching EMu record is found for all objectNumbers then it returns 
	 * and integer array of all of the IRNs. If it is unable to match one or more of the objectNumbers
	 * then it returns NULL<br>
	 * 
	 * @param identifiers
	 * @return int[] of IRNs if all objectNumbers match 
	 * @return NULL if one or more don't match
	 * @throws Exception 
	 */
	private long [] getTargetIRN(String [] identifiers, String target_module, String target_id) {
		if(identifiers == null) {
			return null;
		}
		
		Connection connection = new Connection(target_module);
		Map [] rows = null;
		// Build the terms to search
		long [] irns = new long[identifiers.length];				
		Terms t = new Terms();
		t.addOr();
		Terms objectNumber = t.addOr();
		
		for(int i=0;i<identifiers.length;i++) {
			objectNumber.add(target_id, identifiers[i].trim());
		}

		rows = connection.search(t, "irn,"+target_id);
		
		System.out.println("IRN resutls.length " + rows.length);
		
		// Handle invalid object #s
		if(rows == null) 
			return null;
		
		for(Map row : rows) {
			System.out.println(row.getString("irn") + "->" + row.getString("CatObjectNumber"));
		}
		
		/**
		 * Rewrite this to search through the results and add the IRNs where they match
		 * 
		 * This is just really sloppy // fix
		 */
		int irn_index=0;

		// good lord this is bad 
		for(int t1=0;t1<rows.length;t1++) {
			for(int i=0;i<identifiers.length; i++) {
				// Handles the loose object number match
				if(rows[t1].getString(target_id).equalsIgnoreCase(identifiers[i].trim()) ) {
					try {
						irns[irn_index] = new Integer(rows[t1].get("irn").toString()).intValue();
						if(irns[irn_index] == 0) {  }
						irn_index = irn_index+1;
					} catch(ArrayIndexOutOfBoundsException ab) { 
						System.out.println("Inserting more IRNs than expected.  Check "+identifiers); 
					}
				}
			}
		}
		/**
		 * If the index and the number of catalog records match thens alls well if not something bad happended and it returns null
		 */
		if(irn_index == identifiers.length)
			return irns;
		else 
			return null;
	}
	
	/**
	 * Tries to find metadata.txt in the dirctory being processed
	 * 
	 * @return TRUE if metadata.txt exists in the directory currently being processed
	 * 			FALSE if it does not exist
	 */
	private boolean metadataFileExists() {
		File dir = new File(FILE.getParent());
		List <String> filelist = Arrays.asList(dir.list()); 
		if(filelist.contains("metadata.txt")) {
			METADATA_FILE = new File(FILE.getParent()+java.io.File.separator+"metadata.txt");
			getColumnIndices(); 
			return true;
		}
		return false;
	}

	/**
	 * Reads the first line from metadata.txt and determines the indices of the 
	 * columns (relative positions) and sets them.  These indices are used to create
	 * auxMetadata<br>
	 * <br>
	 * The column headers are:<br>
	 * <ul>
	 * <li>object - the emu identifier for the target recor. UPMAA uses CatObjectNumber</li>
	 * <li>file - the file name</li>
	 * <li>resource - the resource type. This may be UPMAA specific, it controls whether a multimedia record is shown online</li>
	 * <li>creator - the creator field for the multimedia record</li>
	 * </ul>
	 * @throws IOException 
	 */
	private void getColumnIndices() {
		BufferedReader br = null;
		String in = "" ;
		try {
			br = new BufferedReader(new FileReader(METADATA_FILE));
			in = br.readLine();
			br.close();
			
			String [] columns = in.split("\t");
			for(int t =0; t<columns.length; t++) {
				if(columns[t].toLowerCase().contains("object") ) 			{ this.setIndexObjectNumber(t); }
				else if(columns[t].toLowerCase().contains("file") ) 		{ this.setIndexFile(t); }
				else if(columns[t].toLowerCase().contains("resource") ) 	{ this.setIndexResourceType(t); }
				else if(columns[t].toLowerCase().contains("creator") ) 		{ this.setIndexCreator(t); }
				else if(columns[t].toLowerCase().contains("type") ) 		{ this.setIndexType(t); }
			}
			
		} catch (IOException e) {
		} finally {
			try {
				br.close();
			} catch (IOException e) {
			}
		}
		
	}

	/**
	 * Searches metadata.txt for a row where the image file name exists
	 * If a row is found it returns the whole line.  If not it returns null
	 * @param name - File name to search for
	 * @return If a match is made, then the line from metadata.txt is returned
	 * 			NULL if no match found
	 * 
	 */
	private String findRowInMetadataTextFile(String name) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(METADATA_FILE));
		
			String in;
			int line = -1;
			while((in = br.readLine() ) !=null) {
				line = in.indexOf(name);
				if(line != -1) {
					br.close();
					return in;
				}
			}
			br.close();
			return null;
			
		} catch (Exception e) {
			return null;	
		}
	}
	
	/**
	 * Pulls image metadata from metadata.txt and puts it into a map
	 * 
	 * @return map of image metadata gathered from metadata.txt
	 */
	public Map getAuxMetadata() {
		
		String re1=".*?";	// Non-greedy match on filler
	    String re2="\\s+";	// Uninteresting: ws
	    String re3=".*?";	// Non-greedy match on filler
	    String re4="(\\s+)";	// White Space 1
	    String re5="(.)";	// Any Single Character 1
	    String re6="( )";	// White Space 2
	    
	    
		if(this.metadataFileExists()) {
			String [] data = this.findRowInMetadataTextFile(FILE.getName()).split("\t");
			Map aux = new Map();
			
			if(getIndexResourceType() != -1)
				aux.put("DetResourceType", data[this.getIndexResourceType()]);
			if(getIndexCreator() != -1)
				aux.put("MulCreator_tab", data[this.getIndexCreator()].replaceAll("\"", "").split(re1+re2+re3+re4+re5+re6) );
			if(getIndexType() != -1)
				aux.put("MulMultimediaType_tab", data[this.getIndexType()].replaceAll("\"", "").split(re1+re2+re3+re4+re5+re6) );
			
			return aux;
		}
		else {
			return new Map();
		}
	}
	
	/**
	 * Uses the image file name and checks whether the image
	 * already exists in EMu.  This is only a presumptive positive
	 * and will require a human to check both images.
	 * 
	 * @return False if there is atleast one record in emultimedia with the smae file name (excluding file extension)<br>
	 * True if there is no record in emultimedia with the smae file name
	 * 
	 */
	public boolean isIdentifierUnique() {
		Connection connection = new Connection("emultimedia");
		Terms terms = new Terms();
		terms.add("MulIdentifier", FILE.getName().substring(0, FILE.getName().indexOf(".") ));
		boolean results = connection.anyMatchingResults(terms);
		
		if( results ) {
			// TODO better logging 
			System.out.println("WARNING: Possible duplicate image exists for file "+FILE.getName() + 
					". Search for  " + FILE.getName().substring(0, FILE.getName().lastIndexOf(".")) + 
					" in the 'Identifier' field in the Multimedia module\r\n");
			return false;
		} 
		
		return true;
	}
	
	/**
	 * 
	 * PRINT METHODS
	 * 
	 */
	
	/**
	 * Prints all of the image metadata to the console
	 */
	public void printImageMetadata () {
		if(METADATA == null) {
			System.out.println("Image metadata is null");
		}
		else {
			for(com.drew.metadata.Directory directory : METADATA.getDirectories()) {
				for (Tag tag : directory.getTags()) {
			        System.out.println(tag);
			    }
			}
		}
	}
	/**
	 * Prints only the IPTC metadata
	 */
	public void printIPTCMetadata() {
		if(METADATA == null) {
			System.out.println("Image metadata is null");
		}
		else {
			IptcDirectory directory = METADATA.getDirectory(IptcDirectory.class);
			for (Tag tag : directory.getTags()) {
		        System.out.println(tag);
		    }
		}
	}
	/**
	 * Prints the value in the object name field
	 */
	public void printIPTCObjectName() {
		if(METADATA == null) {
			System.out.println("Image metadata is null");
		}
		else {
			IptcDirectory directory = METADATA.getDirectory(IptcDirectory.class);
			IptcDescriptor desc = new IptcDescriptor((IptcDirectory) directory);
			String objectName = desc.getObjectNameDescription();
			System.out.println(objectName);
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getObjectName() {
		if(METADATA == null) {
			return "Image metadata is null";
		}
		else {
			try {
				IptcDirectory directory = METADATA.getDirectory(IptcDirectory.class);
				IptcDescriptor desc = new IptcDescriptor((IptcDirectory) directory);
				return desc.getObjectNameDescription();
			}catch (NullPointerException e) { return "Image metadata is null"; }
		}
	}
	
	/**
	 * Get the IPTC by-line 
	 * 
	 * @return The IPTC By-Line in a String or NULL if there was an error.
	 */
	private String getByLine() {
		if(METADATA == null) {
			return null;
		}
		else {
			try {
				IptcDirectory directory = METADATA.getDirectory(IptcDirectory.class);
				IptcDescriptor desc = new IptcDescriptor((IptcDirectory) directory);
				return desc.getByLineDescription();
			}catch (NullPointerException e) { return null; }
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private String getCaption() {
		if(METADATA == null) {
			return "";
		}
		else {
			try {
				IptcDirectory directory = METADATA.getDirectory(IptcDirectory.class);
				IptcDescriptor desc = new IptcDescriptor((IptcDirectory) directory);
				if(desc.getCaptionDescription() == null) 
					return "";
				else if(desc.getCaptionDescription() == "null")
					return "";
				// Handles when the caption matches the objectName
				else if(desc.getCaptionDescription() == this.getObjectName())
					return "";
				// Handles auto added descriptive information.  
				else if(desc.getCaptionDescription().contains("%") || desc.getCaptionDescription().startsWith("File") || desc.getCaptionDescription().contains("+"))
					return "";
				else
					return desc.getCaptionDescription().replaceAll(this.getObjectName(), "");
			}catch (NullPointerException e) { return ""; }
		}
	}

	/**
	 * If present it will return cataloging metedata present in the image
	 * 
	 * @return
	 */
	public Map getImageMetadata() {
		// if is not an image then return an empty map
		if(!isImage(FILE)) {
			System.out.println("This is not an image");
			return new Map();
		}
		
		Map aux = new Map();
		// This will overwrite any existing values in the "MulCreator_tab" key.  If there is a value
		if(this.getByLine() != null && !this.getByLine().contains("Studio"))
			aux.put("MulCreator_tab", new String[] { this.getByLine() });
		else {
			// aux.put("MulCreator_tab", new String[] { "" });
		}
		if(this.getCaption() == null)
			aux.put("MulDescription", "");
		else 
			aux.put("MulDescription", this.getCaption());
	
		return aux;
	}
}