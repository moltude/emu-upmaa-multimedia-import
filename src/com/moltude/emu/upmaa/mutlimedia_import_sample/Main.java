package com.moltude.emu.upmaa.mutlimedia_import_sample;

import com.moltude.emu.upmaa.multimedia_import.Directory;


/**
 * Sample main 
 * @author scottwilliams
 *
 */
public class Main {

	public static void main(String[] args) {
		 conservation_import();
	}
	
	public static void conservation_import() {
		Directory directory = new Directory("conservation_tessa.properties");
		
		directory.importFiles();
	}
	
	public static void orig(String args[]) {
		Directory directory;

		 // Use default.properties
//		 directory = new Directory();
		 
		 if(args[0] != null) {
			 // Run the jar
			 System.out.println(args[0]);
			 directory = new Directory(args[0]);
			 directory.importFiles();
		 }
	}

}
