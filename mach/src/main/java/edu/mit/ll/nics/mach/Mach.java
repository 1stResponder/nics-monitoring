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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.mit.ll.nics.mach.AppMgr.Response;
import edu.mit.ll.nics.mach.messages.MachMessageManager;


/**
 * Mach - Monitor And Control / Health
 * 
 * <p>
 * 	Combination Monitor and Control and Health and Status application
 * 	responsible for keeping a registry of components on the node, keeping
 * 	an eye on their health by polling/heartbeats, and if necessary, restarting
 * 	them.
 * </p>
 * 
 * @author jpullen
 * @version 0.0.3
 * 
 * TODO: doShutdown stops hbManager, but then cleanup also calls .stop() on hbManager, but
 *       cleanup isn't called except if there's an initialization failure when starting up..
 *       consolidate doShutdown() and cleanup()
 * 
 */
public class Mach implements MachMBean {
	
	/** Logger */
	private static Logger LOG; // = Logger.getLogger(Mach.class);
	
	/** Holds the path of an alternate properties file if the users chooses to pass one in */
	private final String altPropertiesFile;
	
	// Mach Components
	
	/** The main component registry that all components to be monitored must register with */
	private static ComponentRegistry REGISTRY;
	
	/** The heartbeat manager object that does all the heartbeat and status processing */
	private static HeartbeatManager hbManager;	
	
	/** Database manager object */
	private static DBManager dbManager;
	
	/** MachMessageManager object */
	private static MachMessageManager msgManager;
	

	/**
	 * Public constructor that takes in an alternate property file
	 * 
	 *  @param altPropFile A string containing the path to an alternate property file
	 */
	public Mach(final String altPropFile) {
		
		// If altPropFile is null, it'll just use the default: config/mach.properties in the CWD
		altPropertiesFile = altPropFile;
	}
	
	
	/**
	 * Reads in properties, and initiates validation
	 * 
	 * @return True if all required properties were successfully read, False otherwise
	 */
	private boolean initProperties() {
		
		try {
			return Props.initProperties((altPropertiesFile == null) ? "config/mach.properties" : altPropertiesFile);
		} catch (FileNotFoundException e) {
			LOG.warn("properties not read", e);
			return false;
		} catch (IOException e) {
			LOG.warn("properties not read", e);
			return false;
		}
	}
		
	
	/**
	 * Initializes the HeartbeatManager instance 
	 * 
	 * @return True if initialization succeeded, False otherwise
	 */
	public boolean initHeartbeatManager() {
				
		hbManager = new HeartbeatManager(this);
		
		return hbManager.init();
	}
	
	public boolean initDBManager() {
		dbManager = DBManager.getInstance();
		
		return true; // TODO: maybe make an init within DBManager to call?
	}
		
	
	/**
	 * Makes sure all threads are killed, and everything shuts down properly
	 * 
	 * TODO: consolidate with doShutdown
	 */
	public void cleanup() {
		// TODO:
		LOG.info("Cleaning up...");
		
		// Shutdown HeartbeatManager
		if(hbManager != null) {
			LOG.info("\tStopping HeartbeatManager...");
			if(hbManager.stop()) {
				LOG.info("\tStopped.");				
			} else {
				// Try once more? Not much more to do at this point
				hbManager.stop();
			}
		}
		
		// Shutdown MessageManager
		// TODO: this needs to be hidden behind msgManager, and confirmed it's 
		//		 shutting down properly
		LOG.info("\tStopping MessageManager...");
		msgManager.producer.destroy();
		msgManager.consumer.destroy();
		LOG.info("\t\tStopped.");
		
		// Shutdown DBManager
		LOG.info("\tShutting down DBManager...");
		dbManager.close();
		LOG.info("\t\tShutdown.");

		AlertManager.getInstance().close();
		
		LOG.info("Finished clean up.");
	}
	
	
	/**
	 * main program entry point
	 * 
	 * TODO: have mach run in a thread that keeps the app alive
	 * 
	 * @param args Arguments passed in at the command line
	 */
	public static void main(final String[] args) {
		PropertyConfigurator.configure(Props.log4jProperties);
		
		LOG = Logger.getLogger(Mach.class);
		
		String propFile = null;
		if(args != null && args.length > 0) {
			//log.info("Using alternate properties file: " + args[0]);
			propFile = args[0];
		}
		
		final Mach mach = new Mach(propFile);
		
		//MBeanServer mbs = null;
		//ObjectName name = null;		
		//String hostname = "localhost";
		//String port = "4444";		
		//JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+hostname+":"+port+"/jmxrmi");
		//JMXConnector connector = JMXConnectorFactory.connect(address,null);
		//connection = connector.getMBeanServerConnection();
		
		final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name;
		try {
			name = new ObjectName("edu.mit.ll.nics.mach:type=MachMBean");
			mbs.registerMBean(mach, name);			
			
		} catch (Exception e) {
			LOG.error("Error registering MachMBean", e);			
		}		
		
		
		// TODO: MOVE all of this to a mach.init() call, and 
		//		 have the main app stay alive in a thread
		
		LOG.info("Starting Mach...");
		
		LOG.info("Reading properties file...");
		if(!mach.initProperties()) {
			LOG.fatal("Aborting due to missing required properties");
			mach.cleanup();
			System.exit(1);
		}
		LOG.info("Complete.");
		
		LOG.info("Initializing Registry...");
		REGISTRY = ComponentRegistry.getInstance();
		LOG.info("\tDone.");
		
		LOG.info("Initializing DBManager...");
		mach.initDBManager();
		LOG.info("\tDone.");
		
		LOG.info("Populating registry from database...");
		int numCompFromDB = REGISTRY.initFromDB();
		LOG.info("\tRead " + numCompFromDB + " component" + ((numCompFromDB == 1) ? "" : "s") + 
				" in from database");
		
		// TODO: may want to turn this off so it's not registering on startup every time... 
		// 		 maybe only if flagged as a parameter
		LOG.info("Registering any NEW components listed in mach.reglist...");
		//int numCompFromReglist = REGISTRY.registerViaFile("mach.reglist");
		int numCompFromReglist = REGISTRY.registerViaFile(Props.reglist);
		LOG.info("\tDone. Components registered from reglist: " + numCompFromReglist + "\n");
		
		// TODO: process numCompFromDB and numCompFromReglist... maybe if nothing is in the system
		//       may want to notify or take some action?
		
		LOG.info("Initializing MessageManager...");
		mach.msgManager = MachMessageManager.getInstance();
		if(!mach.msgManager.isInitialized()) {
			LOG.fatal("Aborting.  Failed to initialize Message Manager.");
			mach.cleanup();
			System.exit(1);
		}
		LOG.info("\tDone.");
		
		LOG.info("Initializing Heartbeat Manager...");
		if(!mach.initHeartbeatManager()) {
			LOG.fatal("Aborting.  Failed to initialize Heartbeat Manager.");
			mach.cleanup();
			System.exit(1);
		}
		LOG.info("\tDone.");
		
		LOG.info("Starting discovery...");
		hbManager.discover();
		LOG.info("\tDone.");
		
		// TODO: Probably don't want to start this until the in-memory registry is initialized
		//		 and some kind of discovery task is completed for seeing who we need to send to
		//  WAIT for registry(?) to send a notification to hbManager to update
		//LOG.info("Beginning HB Processing...");
		//hbManager.start();
		//hbManager.startProcessing();
	}
	
	
	/**
	 * Initiates the shutdown sequence, properly stopping the different mach components
	 * and performing cleanup.
	 */
	private void doShutdown() {
		
		try{
			Thread.sleep(3000); // Wait 3 seconds before shutting down
			
			LOG.info("Shutting down...\n");
			
			hbManager.stop();
			hbManager.stopProducer();
			hbManager.stopConsumer();
			
			dbManager.close();
			
			Thread.sleep(2000);
						
			LOG.info("Exiting...\n\n");
			System.exit(0);
			
			
		}catch(Exception e) {
			LOG.warn("Exception while shutting down", e);
			System.exit(1);
		}
		
	}
	

	//=============================================
	// JMX Attributes and Operations
	//=============================================

	@Override
	public int getRegistrantCount() {
		return REGISTRY.getRegistrantsCount();
	}
	
	@Override
	public String getRegistrantNames() {
		
		String retval = null;
		
		// Needs to be a simple type for JMX, so create a string
		// from the List of names
		
		final List<String> names = REGISTRY.getRegistrantNames();
		
		if(names == null) {
			retval = "<No registered components>";
		}else {

			final StringBuilder sb = new StringBuilder();

			for(String name : names) {
				sb.append(name + "\n");
			}
			
			retval = sb.toString();
		}
		
		return retval;
	}	

	@Override
	public boolean isHBManagerRunning() {
		return hbManager.isRunning();
	}
	
	@Override
	public String startHBManager() {
		String retval = null;
		
		if(hbManager.isRunning()) {
			
			retval = "hbManager is already running";
									
		}else{
			hbManager.start();
			retval = "started hbManager";
		}
		
		return retval;
	}
	
	@Override
	public String stopHBManager() {
		
		if(hbManager.isRunning()) {
			hbManager.stop();
			return "stopped hbmanager";
		}else{
			return "hbmanager is not running";
		}
	
	}

	@Override
	public boolean isRabbitConsumerRunning() {
		return hbManager.isConsumerLive();
	}
	
	@Override
	public boolean isRabbitProducerRunning() {
		return hbManager.isProducerLive();
	}
	
	@Override
	public String stopConsumer() {
		
		if(hbManager.isConsumerLive()) {
			hbManager.stopConsumer();
			return "stopped consumer";
		}else{
			return "consumer is not running";
		}
	}
	
	@Override
	public String startConsumer() {
		String retval = null;
		/*
		if(hbManager.isConsumerLive()){
			retval = "consumer is already running";
		}else{
			if(hbManager.startConsumer()){
				retval = "started consumer";
			}else{
				retval = "failed to start consumer";
			}
		}
		
		return retval;
		*/
		
		// TODO:
		return "TODO: this is now handled by MachMessageManager";
	}
	
	@Override
	public String stopProducer() {
		
		if(hbManager.isProducerLive()) {
			hbManager.stopProducer();
			return "stopped producer";
		}else{
			return "producer is not running";
		}
	}
	
	@Override
	public String startProducer() {
		
		/*
		if(hbManager.isProducerLive()) {		
			return "producer is already running";
		}else{
			
			if(hbManager.startProducer()) {
				return "started producer";
			}else{
				return "failed to start producer";
			}
		}*/
		
		// TODO:
		return "this is now managed through MachMessageManager";
	}
	
	@Override
	public String test() {
		String res = "Test Results: \n";
		
		//DBManager.add(new NodeComponent("collabRoomState", "LDDRS.incidents.heartbeat.collab.heartbeat", "localhost", false, true));
		//DBManager.add(new NodeComponent("collabArchiver", "LDDRS.incidents.*.collab.*", "localhost", false, true));
		
		List<NodeComponent> components = dbManager.getAllComponents();
		
		for(NodeComponent c : components) {
			res += c.getName() + "|" + c.getTopic() + "\n";
		}
		
		//res = AppMgr.status("mach");
		
		return res;
	}
	
	@Override
	public String shutdown() {
				
		final Thread shutdownThread = new Thread(new Runnable(){
			public void run() {
				doShutdown();
			}
		});
		shutdownThread.setName("Shutdown Mach");
		shutdownThread.start();
		
		return "shutdown initiated";
	}
	

	@Override
	public String appMgrStart(String component) {
		AppMgr.Response resp = AppMgr.start(component, null);
		return resp.name();
	}


	@Override
	public String appMgrStop(String component) {
		AppMgr.Response resp = AppMgr.stop(component);
		return resp.name();
	}
	
	
	@Override
	public String appMgrRestart(String component) {
		AppMgr.Response resp = AppMgr.restart(component, null);
		return resp.name();
	}

	
	@Override
	public String appMgrStatus(String component) {
		AppMgr.Response resp = AppMgr.status(component);
		return resp.name();
	}
		

}
