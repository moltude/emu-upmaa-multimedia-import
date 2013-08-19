package com.moltude.emu.upmaa.imu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.springframework.util.StringUtils;

import com.kesoftware.imu.IMuException;
import com.kesoftware.imu.Map;
import com.kesoftware.imu.Module;
import com.kesoftware.imu.ModuleFetchResult;
import com.kesoftware.imu.Session;
import com.kesoftware.imu.Terms;

/**
 * This class handles the DB connection and updates.  This is a custom UPMAA layer above the KE iMu API because I 
 * got sick and tired of writing all the god damn connect statements and debugging into all of my g-d code. 
 * 
 * This is also helpful for debugging the dropped imu connections that still hasn't been resolved yet.
 * 
 * @author Scott Williams
 * 
 */

public class Connection {
	// iMu seesion instance
	private Session session;

	// the module to connect to
	private String module = null;
	// imu properties
	private String address	= null;
	private int port 		= -1;
	private String user 	= null;
	private String pass 	= null;

	/**
	 * Constructor 
	 */
	public Connection() {
		this( null );
	}

	/**
	 * 
	 * @param module - The module to connect to 
	 */
	public Connection(String _module ) {
		module = _module;
		loadProperties();
	}
	
	/**
	 * Loads the properties from the imu.properties file
	 * This should be stored 
	 */
	private void loadProperties() {	
		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = this.getClass().getClassLoader().getResourceAsStream("imu.properties");
			
			if(is != null) {
				properties.load(is); 
				is.close();
			} else {
				System.out.println("Could not get imu properties");
				System.exit(0);
			}

			address = properties.getProperty("host");			 
			port = new Integer(properties.getProperty("port")).intValue();
			user = properties.getProperty("user","emu");
			pass = properties.getProperty("pass");
			
		} catch (FileNotFoundException fnfe) {
			
		} catch (IOException ioe) {
			
		}
	}

	/**
	 * Prints the configuration properties
	 */
	@SuppressWarnings("unused")
	private void printProperties () {
		System.out.println("**** iMu Connection Properties ****");
		System.out.println("Host = " + this.address);
		System.out.println("Port = " + this.port );
		System.out.println("User = " + this.user );
		System.out.println("Pass = " + this.pass );
	}

	/**
	 * Opens a connection to the EMu Production server
	 * 
	 * @return True if it was successfully connected, False if unable to connect 
	 */
	public boolean connect() {
		int x = 0;
		boolean isConnected = doConnect();

		// This is all debugging until the dropped connection bug is resolved by 
		// KE Software.
		// While unable to connect and number of attempts less than 10
		while (!isConnected && x < 10) {
			try {
				// TODO log4j logging 
				// sleep for 5 seconds
				Thread.currentThread();
				Thread.sleep(5000);
				x += 1;
				// try again but do not attempt more than 10 connections before fail
				isConnected = doConnect();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}
		} // end while
		return isConnected;
	} // end method

	/**
	 * Connection to the imu sever 
	 * 
	 * @return True on successful connection 
	 * 			False if unsuccessful
	 */
	private boolean doConnect() {
		try {
			if (isOpen()) {
				session.disconnect();
			}
			session = new Session(address, port);
			session.connect();
			session.login(user, pass);
			return true;
		} catch (IMuException imuex) {
			// This is all debugging until the dropped connection bug is resolved by 
			// KE Software.
			// TODO Error log with imuex data
			session.disconnect();
			return false;
		} catch (Exception e) {
			// TODO Error log
			session.disconnect();
			return false;
		}
	}

	/**
	 * Closes the connection to the EMu server
	 * 
	 * @return True if successfully disconnected<br> 
	 * 		   False if unsuccessful
	 */
	public boolean disconnect() {
		session.disconnect();
		if(this.isOpen())
			return false;
		else 
			return true;
	}

	/**
	 * Returns an instance of the Session
	 * 
	 * @return Session instance
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * 
	 * SERACH METHODS
	 * 
	 */
	
	/**
	 * Searches emu for the provided Terms
	 * 
	 * @param object
	 * @return
	 */
	public Module search(Terms object) {
		Module m = doSearch(object);
		while (m == null) {
			Thread.currentThread();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Error log
			}
			m = doSearch(object);
		}
		return m;
	}
	
	/**
	 * Searches EMu for the supplied IRN
	 * 
	 * @param key
	 * @return The result of that query
	 */
	public Module search(long key) {
		try {
			if (!isOpen()) {
				connect();
			}
			Module m = new Module(module, session);
			m.findKey(key);
			return m;
		} catch (Exception e) {
			// TODO ERROR log
			disconnect();
			return null;
		}
	}
	
	/**
	 * 
	 * @param key
	 * @param fetch_columns
	 * @return
	 */
	public Map search(long key, String fetch_columns) {
		Module module = search(key);
		try {
			ModuleFetchResult mfr = module.fetch("start",0, 1, fetch_columns);
			if(mfr.getCount() != 1)
				return null;
			return mfr.getRows()[0];
		} catch (IMuException e) {
			return null;
		}
	}
	
	/**
	 * 
	 * @param key
	 * @param fetch_columns
	 * @return
	 */
	public Map search(long key, String[] fetch_columns) {
		return search(key, StringUtils.arrayToCommaDelimitedString(fetch_columns));
	}
	
	/**
	 * Search and return the results as a Map[] 
	 * <br>
	 * @param terms Search terms
	 * @param fetch_columns Columns to return from EMu
	 * @return Returns the requested columns from the matching records or null if there was an error 
	 */
	public Map [] search(Terms terms, String fetch_columns) {
		Module m = search(terms);
		ModuleFetchResult mfr;
		try {
			mfr = m.fetch("start",0, -1, fetch_columns);
		} catch (IMuException e) {
			return null;
		}
		
		return mfr.getRows();
	}

	
	/**
	 * Queries EMu
	 * 
	 * @param terms The query to run
	 * @return - The result of that query
	 */
	private Module doSearch(Terms terms) {
		try {
			// attempt to reconnect 4evr
			// added because sometimes all the liceneses are in use...
			while (!isOpen()) {
				Thread.currentThread();
				Thread.sleep(1000);
				connect();
			}
			Module m = new Module(module, session);
			m.findTerms(terms);
			return m;
		} catch (IMuException imuex) {
			// TODO Error log
			return null;
		} catch (Exception e) {
			// TODO Error log
			disconnect();
			return null;
		}
	}
	
	
	/**
	 * Search for emu records and returns true if any exist false if not
	 * 
	 * @param object
	 * @return
	 */
	public boolean anyMatchingResults(Terms object) {
		Module m = search(object);
		try {
			ModuleFetchResult results = m.fetch("start", 0, 1, "");
			Map [] rows = results.getRows();
			// if there are no resutls return false
			if (rows == null || rows.length == 0) {
				return false;
			} else
				return true;
		} catch (IMuException e) {
			// TODO Error log
		}
		return false;
	}

	/**
	 * Updates a single record in EMu
	 * 
	 * @param key - The record to updated
	 * @param values - The values to insert or change
	 * @param column - The columns to update
	 * @throws IMuException 
	 */
	public void updateRecord(long key, Map values, String column) throws IMuException {
		try {
			if (!isOpen()) {
				connect();
			}
			Module m = new Module(module, session);
			m.findKey(key);
			m.fetch("start", 0, 1);
			if (column != null)
				m.update("start", 0, 1, values, column);
			else
				m.update("start", 0, 1, values);
		} catch (Exception e) {
			// TODO log4j ERROR msg
			e.printStackTrace();
			disconnect();
			throw new IMuException("Error updating reocrds");
		}
	}

	/**
	 * Tests whether the connection to the imu service is open
	 * 
	 * @param con
	 * @return
	 */
	private boolean isOpen() {
		if (session == null)
			return false;
		else if ((session).getContext() != null) 
			return true;
		return false;
	}

	/**
	 * Creates a new record 
	 * 
	 * @param metadata
	 * @return the irn of the new record
	 */
	public long createRecord(Map metadata) {
		try {
			if (!isOpen()) {
				connect();
			}
			Module m = new Module(module, session);
			Map newRecord = m.insert(metadata); 
			return newRecord.getLong("irn"); 
		} catch (Exception e) {
			disconnect();
			return -1;
		}
	}

	/**
	 * Search Emu CatObjectNumber field for objectId and return the CatObjectName_tab for the matching record, else return null
	 * <br>
	 * @param objectId - Object number to search for 
	 * @return - Object Name as commas seperated string
	 * 
	 */
	public String getObjectName(String objectId) {
		Terms terms = new Terms();
		terms.add("CatObjectNumber", objectId);
		this.connect();
		Map [] results = search(terms, "CatObjectNumber,CathObjectName_tab");

		for(Map row : results) {
			if(row.getString("CatObjectNumber").equalsIgnoreCase(objectId))
				return StringUtils.arrayToCommaDelimitedString(results[0].getStrings("CatObjectName_tab") );
		}
	
		return null;
	}
}