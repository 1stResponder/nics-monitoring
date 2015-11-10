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

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.db4o.*;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.query.Predicate;


/**
 * The DBManager manages all aspects of interacting with the
 * database behind Mach, and handling all CRUD operations on
 * the known components registry
 *  
 *  
 * @author jpullen
 *
 */
public final class DBManager implements Observer {

	/** Logger */
	private static final Logger LOG = Logger.getLogger(DBManager.class);
	
	/** Single db instance */
	private static ObjectContainer db;
	
	/** Single instance of DBManager */
	private static final DBManager INSTANCE = new DBManager();
	
	/** Reference to ComponentRegistry */
	private static final ComponentRegistry registry = ComponentRegistry.getInstance();
	
	
	/**
	 * Private constructor
	 */
	private DBManager(){
		super();
		
		PropertyConfigurator.configure(Props.log4jProperties);
		
		ComponentRegistry.getInstance().addObserver(this);
		
		// TODO: look into configuration options
		//EmbeddedConfiguration config = Db4oEmbedded.newConfiguration();
		
		//db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(),
			//	Props.db4oFilename);
		checkAndInit();
	}
		
	
	/**
	 * Checks if the database is open, and if not, opens it
	 */
	private synchronized void checkAndInit() {
		if(isClosed()) {
			LOG.info("initializing db4o database...");
			db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(),
					Props.db4oFilename);
			LOG.info("\tdone.");
		}
	}
	
	
	/**
	 * Returns the singleton instance of this class
	 * 
	 * @return 
	 */
	public static DBManager getInstance() {
		return INSTANCE;
	}
	
	
	/**
	 * Checks to see if the db4o database is closed or not
	 *  
	 * @return true if db is closed, false otherwise
	 */
	private synchronized boolean isClosed(){
		if(db == null) {
			return true;
		}else {
			return db.ext().isClosed();
		}
	}
	
	
	/**
	 * Add/Update a NodeComponent object in the database
	 * 
	 * If a matching component exists, then it will be pulled from the db, updated, then
	 * stored.  But if the component does not exist, it's simply inserted.
	 * 
	 * @param component The component to add
	 */
	public void add(NodeComponent component) {
		
		NodeComponent toStore;
		
		// TODO:  What if the data in the db is more current than what's coming in?  Like if it's receiving
		//		  an initial registration message for some reason, when it's already been registered and running
		//		  with new values and history?
		
		// Check to see if this object has been previously stored... we're using
		// the name as a pseudo index
		NodeComponent found = getNodeComponent(new NodeComponent(component.getName()));
		if(found != null) {
			// TODO: update fields... this seems ugly... would have to go through and copy over each
			//		 field?  Might have to implement a deep copy method?  Maybe a NodeComponent.equals() as
			//		 well...
			
			LOG.info("*** This component was previously stored.  Updating fields... ***");
			
			found.setAlertCount(component.getAlertCount());
			found.setCamelApp(component.isCamelApp());
			found.setCategory(component.getCategory());
			found.setJmxEnabled(component.isJmxEnabled());
			found.setLastAlertTime(component.getLastAlertTime());
			found.setLive(component.isLive());
			found.setMetadata(component.getMetadata());
			//found.setName(name) // Don't think these should be changing... ?
			//found.setNode(node)			
			found.setPath(component.getPath());
			found.setTopic(component.getTopic());
			
			toStore = found;
		}else {
			toStore = component;
		}				
		
		synchronized (DBManager.class) {
			LOG.info("*** Storing component: " + component.getName());
			checkAndInit();
			db.store(toStore);
			db.commit();
		}
	}
	
	
	/**
	 * Experimental db4o get function....
	 * 
	 * TODO: finish properly, it's initially just test code
	 * 
	 * @Pre-condition: assumes you've populated the component object with a searchable field
	 * 		with the rest nulled/zeroed, etc... ugly, but that's how queryByExample works
	 * 
	 * @param component Example NodeComponent with fields to search on populated
	 * 
	 * @return The stored NodeComponent if found, otherwise, null
	 */
	public NodeComponent getNodeComponent(NodeComponent component) {
		NodeComponent retComponent = null;
		
		final long start = System.currentTimeMillis();
		
		checkAndInit();
		
		ObjectSet<NodeComponent> components = db.queryByExample(component);
		
		if(components != null && components.hasNext()) {
			retComponent = components.next();
		}
				
		LOG.debug("getNodeComponent(NodeComponent) executed in: " + (System.currentTimeMillis() - start) + "ms");
		
		return retComponent;
	}
	
	
	@SuppressWarnings("serial")
	public NodeComponent getNodeComponentByName(final String component) {
		NodeComponent resultComponent;
		
		List <NodeComponent> results = db.query(new Predicate<NodeComponent>() {
		   public boolean match(NodeComponent nodeComponent) {
		      return nodeComponent.getName().toLowerCase().equals(component.toLowerCase());
		   }
		});
		
		if(results == null){
			resultComponent = null;
		}else{
			// TODO: what if more than one is returned?
			if(results.size() > 1){
				LOG.warn("More than one component exists in database under the name: " + component);
			}
			
			resultComponent = results.get(0);
		}
		
		return resultComponent;
	}
	
	
	/**
	 * Returns all NodeComponent objects from the database
	 * 
	 * @return List of all NodeComponents in database
	 */
	public List<NodeComponent> getAllComponents() {
		return db.query(NodeComponent.class);
	}
	
	
	/**
	 *  
	 * @return list of NodeComponents set as Live
	 */
	@SuppressWarnings("serial")
	public List<NodeComponent> getLiveComponents() {
		return db.query(new Predicate<NodeComponent>() {
			public boolean match(NodeComponent nodeComponent) {
				return nodeComponent.isLive();
			}
		});
	}
	
	
	/**
	 * Calls commit() on the db, then closes it
	 * 
	 * TODO: think I read that closing automatically calls commit, so this
	 * 		is redundant... confirm and remove
	 */
	public void close() {
		try{
			db.commit();
		}finally{
			db.close();
		}
	}


	@Override
	public void update(Observable o, Object obj) {
		
		if(o instanceof ComponentRegistry) {
			
			if(obj instanceof RegistrationEvent) {
				
				RegistrationEvent regEvent = (RegistrationEvent) obj;
				
				// *** Handle a newly registered component ***
				
				switch(regEvent.getEvent()) {
				
					
					case REGISTERED:
						NodeComponent component = registry.get(regEvent.getComponentId());
						if(component != null) {
							add(component);
						}						
						
						break;
						
					
					case UNREGISTERED:
						// TODO:
						break;
						
					default:
						
				
				}
				
				
			}
		}
		
	}
}
