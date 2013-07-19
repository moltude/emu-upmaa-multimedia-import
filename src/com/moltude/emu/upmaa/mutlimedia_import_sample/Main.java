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
		 // import photos from penn museum photo studio
		 directory = new Directory("photo_studio.properties");
		 
		directory.importFiles();
	}

}
