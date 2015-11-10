/**
 * Copyright (c) 2008-2015, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.mach;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Props class
 * 
 * Loads and holds project-wide properties so
 * the various components of Mach have access
 * to them.
 * 
 * @author jpullen
 *
 */
public final class Props {
		
	private static final Logger LOG = Logger.getLogger(Props.class);
	private static Properties properties;
	
	// Keep track of how many properties are set here
	private static final int PROPERTY_COUNT = 13; // NOPMD by jpullen on 8/10/11 2:06 PM, will use in a unit test
	
	
	/**
	 * Private default constructor to block instantiation
	 */
	private Props() {		
	}
	
	
	/**
	 * Initializes Properties object and populates variables
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static boolean initProperties(final String propertiesFile) throws FileNotFoundException, IOException {
		properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));
		return populate();
	}
		
	
	/**
	 * Populates member variables with values from property
	 * @return
	 */
	private static boolean populate() {
		boolean retval = true;
		
		String propVal;
		
		// Mach properties
		
		
		propVal = properties.getProperty(PROP_HEARTBEAT_SEND_INTERVAL);
		heartbeatSendInterval = Util.parseLongFromString(propVal, 30000);
		
		propVal = properties.getProperty(PROP_PROCESS_ACKS_INTERVAL);
		processAcksInterval = Util.parseLongFromString(propVal, 5000);
		
		propVal = properties.getProperty(PROP_STALE_THRESHOLD);
		staleThreshold = Util.parseLongFromString(propVal, 60000);
		
		propVal = properties.getProperty(PROP_THREAD_SLEEP);
		threadSleep = Util.parseLongFromString(propVal, 500);
		
		propVal = properties.getProperty(PROP_EMAIL_ALERT_TIMEOUT);
		emailAlertTimeout = Util.parseIntFromString(propVal, 120);
		
		propVal = properties.getProperty(PROP_PERIOD_BEFORE_RESTART);
		periodBeforeRestart = Util.parseLongFromString(propVal, 5);
		
		
		// rabbit client properties		
		rabbitUsername = properties.getProperty(PROP_RABBIT_USERNAME);
		rabbitPassword = properties.getProperty(PROP_RABBIT_PASSWORD);
		rabbitHost = properties.getProperty(PROP_RABBIT_HOST);
		
		propVal = properties.getProperty(PROP_RABBIT_PORT);		
		rabbitPort = Util.parseIntFromString(propVal, 5672);			
				
		rabbitEmailEndpoint = properties.getProperty(PROP_RABBIT_EMAIL_ENDPOINT);
		emailFrom = properties.getProperty(PROP_EMAIL_FROM);
		emailTo = properties.getProperty(PROP_EMAIL_TO);
		
		// Initial topics to listen on
		final String strTopics = properties.getProperty(PROP_INITIAL_TOPICS);
		if(strTopics == null || strTopics.equals("")) {
			LOG.warn("No initial topics will be consumed");
		} else {
			initialTopics = Util.getTopicsFromProperty(strTopics);
		}
		
		db4oFilename = properties.getProperty(PROP_DB4O_FILENAME);
		if(db4oFilename == null || db4oFilename.isEmpty()) {
			db4oFilename = "data/mach.db4o";
		}
		
		appMgrPath = properties.getProperty(PROP_APPMGR_PATH);
		if(appMgrPath == null || appMgrPath.isEmpty()) {
			appMgrPath = "/home/dmit/lddrsDeploy";
		}
		
		reglist = properties.getProperty(PROP_REGLIST);
		if(reglist == null || reglist.isEmpty()) {
			reglist = "config/mach.reglist";
		}
		
		log4jProperties = properties.getProperty(PROP_LOG4JPROPERTIES);
		if(log4jProperties == null || log4jProperties.isEmpty()) {
			log4jProperties = "config/log4j.properties";
		}
		
		// === Validate Properties ===
		
		final List<String> errors = validateProperties();
		if(!errors.isEmpty()){
			final StringBuilder sb = new StringBuilder();
			
			sb.append("Failed to parse required properties: \n");
			for(String err : errors) {
				sb.append("\t" + err);
			}
						
			retval = false;
		}
		
		return retval;
	}
	
	
	/**
	 * Validates properties, and returns a list of the properties required
	 * to continue.  If any required properties are not set, the app should
	 * exit.
	 *  
	 * @return errors List of properties and the problem with them
	 */
	private static List<String> validateProperties() { // NOPMD by jpullen on 8/10/11 2:14 PM
		final List<String> errors = new ArrayList<String>();
		
		// Add any required properties that fail validation to the list
		if(rabbitUsername == null || rabbitUsername.equals("")) {
			errors.add("rabbitUsername not set");
		}
		if(rabbitPassword == null || rabbitPassword.equals("")) {
			errors.add("rabbitPassword not set");
		}			
		if(rabbitHost == null || rabbitHost.equals("")) {
			errors.add("rabbitHost not set");
		}
		if(rabbitEmailEndpoint == null || rabbitEmailEndpoint.equals("")) {
			errors.add("rabbitEmailEndpoint not set");
		}
		if(emailFrom == null || emailFrom.equals("")) {
			errors.add("emailFrom not set");
		}
		if(emailTo == null || emailTo.equals("")) {
			errors.add("emailTo not set");
		}
		if(log4jProperties == null || log4jProperties.equals("")) {
			errors.add("log4jPropertiesFile not set");
		}
		
		return errors;
	}

	
	//***********************************************
	// Constant property names
	//***********************************************
	
	public static final String PROP_RABBIT_HOST = "rabbitHost";
	public static final String PROP_RABBIT_PORT = "rabbitPort";
	public static final String PROP_RABBIT_USERNAME = "rabbitUsername";
	public static final String PROP_RABBIT_PASSWORD = "rabbitPassword";
	
	public static final String PROP_RABBIT_EMAIL_ENDPOINT = "rabbitEmailEndpoint";
	public static final String PROP_EMAIL_TO = "emailTo";
	public static final String PROP_EMAIL_FROM = "emailFrom";
	public static final String PROP_EMAIL_ALERT_TIMEOUT = "emailAlertTimeout";
	
	public static final String PROP_HEARTBEAT_SEND_INTERVAL = "heartbeatSendInterval";
	public static final String PROP_PROCESS_ACKS_INTERVAL = "processAcksInterval";
	public static final String PROP_STALE_THRESHOLD = "staleThreshold";
	public static final String PROP_THREAD_SLEEP = "threadSleep";
	public static final String PROP_PERIOD_BEFORE_RESTART = "periodBeforeRestart";
	
	public static final String PROP_INITIAL_TOPICS = "initialTopics";
	
	public static final String PROP_DB4O_FILENAME = "db4oFilename";
	
	public static final String PROP_APPMGR_PATH = "appMgrPath";
	
	public static final String PROP_REGLIST = "reglist";
	
	public static final String PROP_LOG4JPROPERTIES = "log4jPropertiesFile";

	//***********************************************
	// Properties
	//***********************************************
	
	public static String rabbitHost;
	public static int rabbitPort;
	public static String rabbitUsername;
	public static String rabbitPassword;
	
	public static String rabbitEmailEndpoint;
	public static String emailTo;
	public static String emailFrom;
	public static int emailAlertTimeout;
	
	public static long heartbeatSendInterval;
	public static long processAcksInterval;
	public static long staleThreshold;
	public static long threadSleep;
	public static long periodBeforeRestart;
	
	public static List<String> initialTopics;
	
	public static String db4oFilename;
	
	public static String reglist;
	
	public static String appMgrPath;
	
	public static String log4jProperties = "config/log4j.properties";
}
