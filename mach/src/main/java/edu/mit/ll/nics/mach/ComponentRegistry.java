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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.json.JSONObject;

import edu.mit.ll.nics.mach.messages.MsgParser;

/**
 * Registry of @NodeComponent objects.  They may exist on this node, or a remote node. Due to
 * this, the key for the component is compound, made up of the node and the name of the component.
 * 
 * @author jpullen
 */
public final class ComponentRegistry extends Observable {

	/** Logger */
	private static final Logger LOG = Logger.getLogger(ComponentRegistry.class);
	
	/** Single instance object of this class */
	private static final ComponentRegistry INSTANCE = new ComponentRegistry();	
	
	/** Map containing registered @NodeComponent objects */
	private static Map<String, NodeComponent> registrants = new HashMap<String, NodeComponent>();
	
	
	/**
	 * Private constructor to block instantiation
	 */
	private ComponentRegistry() {
		super();
		PropertyConfigurator.configure(Props.log4jProperties);
	}
	
	
	/**
	 * Returns the single instance
	 * 
	 * @return
	 */
	public static ComponentRegistry getInstance() {
		return INSTANCE;
	}

	
	/**
	 * Initialize registry with NodeComponents stored in DB
	 * 
	 * @return Number of components loaded from database
	 */
	public int initFromDB() {
		LOG.debug("initFromDB: entering");
		
		int retval = 0;
		
		DBManager dbManager = DBManager.getInstance();
		
		// TODO: If this ever gets sufficiently large, we won't necessarily want to load in
		// ALL components... many may be dead/deprecated/etc
		List<NodeComponent> components = dbManager.getAllComponents();
		if(components != null && !components.isEmpty()) {
			LOG.info("Initializing registry from db:");
			
			for(NodeComponent c : components) {
				LOG.info("\tAdding component: " + c.getCid() + "...");
				registrants.put(c.getCid(), c);
				retval++;
			}
			LOG.info("done.");
		}else{
			LOG.info("No components were found in the database");
		}
		
		return retval;
	}

	
	/**
	 * Registers a component with the registry
	 * 
	 * @param component a @NodeComponent object
	 * 
	 * @return true if successfully registered, false otherwise
	 */
	public boolean register(final NodeComponent component) {
		
		if(component == null) {
			LOG.info("component passed in was null, returning from register() with false");
			return false;
		}
		
		boolean retval = true;
						
		try{
			LOG.info("register(): Adding component to registry: " + component.getCid());

			synchronized(ComponentRegistry.class) {
				registrants.put(component.getCid(), component);
			}
			
			setChanged();
			notifyObservers(
				new RegistrationEvent(RegistrationEvents.REGISTERED, component.getCid())
			);
			clearChanged();

		} catch(Exception e) {
			LOG.error("Caught unhandled exception while registering component", e);
			retval = false;
		}
		
		return retval;
	}
	
	
	/**
	 * Helper function that takes a JSONObject representing a Mach register object, and
	 * transforms it into a @NodeComponent object for the register function
	 * 
	 * 
	 * @param json JSONObject containing a registration message
	 * @return true if register(NodeComponent) registers successfully, false otherwise
	 */
	public boolean register(final JSONObject json) {
		
		if(json == null) {
			LOG.debug("Not registering component, JSONObject is null");
			return false;
		}
		
		return register(MsgParser.buildNodeComponentFromRegisterMsg(json));
	}
	
	
	/**
	 * Accepts a filename which contains one registration message per line, and
	 * passes them in to be registered internally.  Will ONLY register them IF
	 * they don't already have an entry in the registry. 
	 * 
	 * @param filename
	 * @return -1 if there was an error, otherwise N, where N is the number of components
	 * 			successfully registered
	 */
	public int registerViaFile(String filename) {
		
		int retval;
		int regcount = 0;
		int registered = 0;
		int regfails = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			
			String line = null;
			NodeComponent compToReg = null;
			LOG.info("\n**********\nRegistering new components in " + filename + ":\n");
			while((line = br.readLine()) != null) {
				
				if(line.isEmpty() || line.startsWith("#")) {
					LOG.debug("\tskipping empty or commented line in registration file");
					continue;
				}
				
				regcount++;
				
				compToReg = MsgParser.buildNodeComponentFromRegisterMsg(line);
				if(!registrants.containsKey(compToReg.getCid())) {
					if(register(compToReg)) {
						LOG.info("\tregistered: " + compToReg.getCid());
						registered++;
					}else{
						LOG.debug("Failed to register component from registration message");
						regfails++;
					}
				}else{
					LOG.info("\tskipping, already registered: " + compToReg.getCid());
				}
			}
			
			LOG.info(String.format("Registered %d of %d components in file %s with %d failures\n", registered, 
					regcount, filename, regfails));
			
			retval = registered;
			
		} catch (FileNotFoundException e) {
			LOG.info("Registration file not found: " + filename);
			LOG.error("Registration file not found", e);
			retval = -1;
		} catch (IOException e) {
			LOG.error("IOException while reading from registration file ("+filename+")");
			retval = -1;
		}
		
		return retval;
	}
	
	
	/**
	 * Removes a component from the registry
	 * 
	 * @param component the NodeComponent to remove
	 * @return true if the component was found and removed, false otherwise
	 */
	public boolean unregister(final NodeComponent component) {		
		boolean retval = true;
		
		synchronized(ComponentRegistry.class) {
			final NodeComponent removed = registrants.remove(component.getCid());
			
			if(removed == null) {
				LOG.debug("Component '"+component.getCid()+"' was not found, so not removing.");
				retval = false;
			} else {
				LOG.debug("Unregistered component: " + removed.getCid());
				setChanged();
				notifyObservers(
					new RegistrationEvent(RegistrationEvents.UNREGISTERED, removed.getCid())
				);
				clearChanged();
			}
		}
		
		return retval;
	}
	
	
	/**
	 * Updates alert info on component
	 * 
	 * @param componentName name of component the alert was triggered for
	 */
	public void alertTriggeredFor(final String componentName) {
		if(registrants.containsKey(componentName)) {
			registrants.get(componentName).alertTriggered(System.currentTimeMillis());
		}else{
			throw new IllegalArgumentException("Component not found in registry: " + componentName);
		}
	}
	
	
	/**
	 * Public Wrapper call to the @registrants contains method
	 * 
	 * @param componentName
	 * @return
	 */
	public synchronized boolean contains(final String componentName) {
		if(registrants != null) {
			return registrants.containsKey(componentName);
		}
		
		return false;
	}
	
	
	/**
	 * Returns list of registrant names
	 *   
	 * @return List of names if at least one registered, empty list otherwise
	 */
	public List<String> getRegistrantNames() {
				
		if(registrants.isEmpty()) {
			// TODO: What's the better choice?  An empty list avoids the caller getting NPEs 
			// 	when they don't check for null, but seems wasteful to send back an empty List			
			return new ArrayList<String>(); 
		}
		
		final List<String> names = new ArrayList<String>();

		for(String registrant : registrants.keySet()) {
			names.add(registrant);
		}
				
		return names;
	}
			
	
	/**
	 * Utility method for retrieving a NodeComponent from the registry
	 * 
	 * @param componentName
	 * @return
	 */
	public NodeComponent get(final String componentName) {
		
		if(registrants.containsKey(componentName)) {
			return registrants.get(componentName);
		}else{
			// TODO: Causes issues for callers not handling it, but at the same time when making
			//	     chained calls like registry.get(blah).setLive(true) you'd get a NPE anyway....
			throw new IllegalArgumentException("Component not found in registry: " + componentName);
		}
	}
	
	
	/**
	 * 
	 * 
	 * @param componentName Name of component to check alert status of
	 * @return true if component is on alert, false otherwise
	 */
	public boolean isComponentOnAlert(final String componentName) {
		
		if(registrants.containsKey(componentName)) {
			return registrants.get(componentName).isOnAlert();
		}else {
			LOG.warn("Component not in registry, so can't report on alert status: " + componentName);
			
			throw new IllegalArgumentException("Component not found in registry: " + componentName);
		}
	}
	
	
	/**
	 * Gets the time of the last alert
	 *  
	 * @param componentName name of component to retrieve alert from
	 * @return the last alert time if there is one, -1 otherwise
	 */
	public long getAlertTimeFor(final String componentName) {
		if(registrants.containsKey(componentName)) {		
			return registrants.get(componentName).getLastAlertTime();
		}else{
			
			LOG.warn("Component not in registry, so cannot retrieve valid alert time: " 
					+ componentName);
			
			// TODO: may want to be more forgiving
			throw new IllegalArgumentException("Component not found in registry: " + componentName);  
		}
	}
	
	
	/**
	 * Clears the alert status on the specified component if it exists, otherwise
	 * an IllegalArgumentException is thrown
	 * 
	 * @param componentName Name of the component to clear alert for
	 */
	public void clearAlertFor(final String componentName) {
		if(registrants.containsKey(componentName)) {
			registrants.get(componentName).setOnAlert(false);
			registrants.get(componentName).setLive(true);
		}else {
			
			LOG.warn("Component not in registry, so cannot clear alert: " 
					+ componentName);
			
			// TODO: may want to be more forgiving
			throw new IllegalArgumentException("Component not found in registry: " + componentName);
		}
	}
	
	
	/**
	 * Retrieves the number of registered components in the registry
	 * 
	 * <p>TODO: rename to something short and reasonable</p>
	 * 
	 * @return size of registrant map
	 */
	public int getRegistrantsCount() {
		return registrants.size();
	}
	
}
