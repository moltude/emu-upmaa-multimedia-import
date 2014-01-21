package com.moltude.emu.upmaa.mutlimedia_import_sample;

import com.kesoftware.imu.IMuException;
import com.kesoftware.imu.Map;
import com.kesoftware.imu.Module;
import com.kesoftware.imu.ModuleFetchResult;
import com.kesoftware.imu.Terms;
import com.moltude.emu.upmaa.imu.Connection;
import com.moltude.emu.upmaa.multimedia_import.Directory;


/**
 * Sample main 
 * @author scottwilliams
 *
 */
public class Main {

	public static void main(String[] args) {	
		 // conservation_import();
	}
	
	public static void conservation_import() {
		Directory directory = new Directory("conservation_tessa.properties");
		directory.importFiles();
	}
	
	// /emu-upmaa-multimedia-import/demo/sample_import
	public static void use_sampe() {
		Directory directory = new Directory("/emu-upmaa-multimedia-import/demo/sample_import/");
		directory.importFiles();
	}
	
	public static void orig(String args[]) {
		Directory directory;

		 // Use default.properties
		 // directory = new Directory();
		 
		 if(args[0] != null) {
			 // Run the jar
			 System.out.println(args[0]);
			 directory = new Directory(args[0]);
			 directory.importFiles();
		 }
	}

}
