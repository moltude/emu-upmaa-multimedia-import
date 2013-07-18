package com.moltude.emu.upmaa.mutlimedia_import_sample;

import com.moltude.emu.upmaa.multimedia_import.Directory;


/**
 * Sample main 
 * @author scottwilliams
 *
 */
public class Main {

	public static void main(String[] args) {
		Directory directory = new Directory();
		
		// uses the default.properties file in ./conf/
		directory.importFiles();
		
		// To use another config file pass the path to the method		
		// directory.importFiles("photo_studio.properties");
	}

}
