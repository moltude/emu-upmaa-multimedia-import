package com.moltude.emu.upmaa.mutlimedia_import_sample;

import com.kesoftware.imu.Map;
import com.moltude.emu.upmaa.multimedia_import.Directory;


/**
 * Sample main 
 * @author scottwilliams
 *
 */
public class Main {

	public static void main(String[] args) {
		Directory directory = new Directory();
		
		Map genericMetadata = null;
		Map settings = null;
		directory.doImport("", genericMetadata, settings);
	}

}
