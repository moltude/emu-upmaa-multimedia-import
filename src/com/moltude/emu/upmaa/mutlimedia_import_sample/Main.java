package com.moltude.emu.upmaa.mutlimedia_import_sample;

import com.moltude.emu.upmaa.multimedia_import.Directory;


/**
 * Sample main 
 * @author scottwilliams
 *
 */
public class Main {

	public static void main(String[] args) {
		 Directory directory;

		 // Use default.properties
//		 directory = new Directory();
		 
		 if(args[0] != null) {
			 // Run the jar
			 directory = new Directory(args[0]);
			 directory.importFiles();
		 }
	}

}
