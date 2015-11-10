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

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.hibernate.cfg.HbmBinder;
import org.json.*;

import edu.mit.ll.nics.common.alert.*;
import edu.mit.ll.nics.common.rabbitmq.client.*;
import edu.mit.ll.nics.mach.messages.HeartbeatMessage;
import edu.mit.ll.nics.mach.messages.MachMessage;
import edu.mit.ll.nics.mach.messages.MachMessageManager;
import edu.mit.ll.nics.mach.messages.MachMessageType;
import edu.mit.ll.nics.mach.messages.MsgParser;


import com.rabbitmq.client.ShutdownSignalException;

/**
 * <p>
 * 		The HeartbeatManager class manages the sending of heartbeats,
 *    	processing the responses, and sending alerts if there are no
 *    	responses from live components, or certain conditions are met
 *    	to trigger an alert.
 * </p>
 * 
 * 
 * @author jpullen
 *
 */
public class HeartbeatManager implements Observer {
	
	/** Logger */
	private static final Logger LOG = Logger.getLogger(HeartbeatManager.class);

	/** Reference to the @ComponentRegistry */
	private static ComponentRegistry registry;
	
	/** Reference to the MachMessageManager object */
	private static MachMessageManager msgManager;
	
	/** Reference to the alertManager object */
	private AlertManager alertManager;
	
	/** Email alert object used for sending alerts when conditions are triggered */
	private EmailAlertFactory emailAlertFactory;

	
	// ====== Data Members ======
	
	/** Main thread that does the heartbeat processing work */
	private Thread heartbeatThread;
	
	/** Executor for scheduling ProcessHeartbeatsTask */
	private ScheduledExecutorService processHeartbeatsScheduler;
	
			
	/** Map of components to the timestamp of their last response
	 *  
	 *  <p>TODO: make a Concurrent Hashmap?</p> 
	 *  <p>TODO: move to the ComponentRegistry class</p>
	 */	
	private Map<String, Long> componentTimestamps;

	/** Flag for whether or not to continue processing heartbeats; governs main heartbeat thread loop */
	private boolean keepProcessing;	
	
	/** The last time heartbeat/response latency was processed */
	private long lastProcessTime = 0;

	
	/**
	 * Constructor accepting Mach instance
	 * 
	 * @param mach Reference to parent mach instance
	 */
	public HeartbeatManager(Mach mach) {
		
		PropertyConfigurator.configure(Props.log4jProperties);
		
		registry = ComponentRegistry.getInstance();
		registry.addObserver(this);
		
		msgManager = MachMessageManager.getInstance();
		msgManager.addObserver(this);
		
		alertManager = AlertManager.getInstance();
		emailAlertFactory = alertManager.getEmailAlertFactory();
		componentTimestamps = new ConcurrentHashMap<String, Long>();
	}
	
	
	/**
	 * Takes care of initializing the Heartbeat system
	 * - Configure log4j
	 *   
	 * - TODO: this doesn't seem that necessary any more, since having moved the rabbit clients
	 * 	 	   to the MachMessageManager class  
	 * 
	 * @return True if system was initialized with no errors, False otherwise.  If false, this
	 * 		   signifies a fatal error, and execution should not continue.
	 */
	public boolean init() {
		// TODO: any extra initialization here
		
		return true;
	}	
	

	/**
	 * Initializes the heartbeat thread
	 */
	private void initHeartbeat() {
		keepProcessing = true;
		
		heartbeatThread = new Thread(new Runnable(){
			
			long curTime;
			long lastSendTime = 0;			
			
			@Override
			public void run() {
				while(keepProcessing) {
					curTime = System.currentTimeMillis();
															
					try {

						if(heartbeatThread.isInterrupted()){
							keepProcessing = false;
							break;
						}
						
						
						if((curTime - lastSendTime) >= Props.heartbeatSendInterval) { 
							sendHeartbeats();
							lastSendTime = curTime;
						}
						
						/* TODO: !!! Possibly implement these actions with Scheduler
						 * 		 and TimerTask... removes the need for this thread
						 * 	     and having to sleep
						 */
						
						// Sleep to allow time for responses and to stop
						// 100% CPU utilization
						Thread.sleep(500); // TODO: make configurable
						
						/* MOVED to ProcessHeartbeatsTask
						// Process acks from components to check for any stale/dead components
						if((curTime - lastProcessTime) >= Props.processAcksInterval) {
							
							// Process messages for new ACKs
							processMessages(msgManager.getLatestMessages());							
							
							// Process component timestamps, checking for stale components
							processComponentHeartbeats();
							
							lastProcessTime = curTime;
							
						}
						*/
					
					} catch (InterruptedException e) {
						LOG.error("Heartbeat processing thread was interrupted");
						keepProcessing = false;
						break;
					} catch (Exception e) {
						LOG.error("Caught unhandled exception in heartbeat processing loop", e);
					}
					
				}
				
				LOG.info("Heartbeat Thread loop exited.");
			}
		});

		// Name the thread
		heartbeatThread.setName("MachHeartbeatThread");
		
		// If application exits, JVM will exit if the only threads running are daemon threads
		heartbeatThread.setDaemon(true);
	}
	
	
	/**
	 * <p>Starts the timer that executes the ProcessHeartbeatsTask in
	 * daemon mode.</p>
	 * 
	 * <p>The timer will execute on a fixed interval set by the
	 * Props.processAcksInterval property</p>
	 */
	public void startProcessing() {
		LOG.info("Starting ProcessHeartbeats task...");
		
		if(processHeartbeatsScheduler == null) {
			// TODO:
			// Only need one thread, since this scheduler only works with one task... 
			// in future should make configurable, and maybe have one scheduler to 
			// all tasks?  Probably how it's meant to be used
			processHeartbeatsScheduler = Executors.newScheduledThreadPool(1);
		}
		
		processHeartbeatsScheduler.scheduleWithFixedDelay(new ProcessHeartbeatsTask(), 0, 
				Props.processAcksInterval, TimeUnit.MILLISECONDS);
		
		LOG.info("\tstarted.");
	}
	
	
	/**
	 * Stops the scheduler that executes the ProcessHeartbeatsTask, first tries to
	 * wait for pending tasks, and if it takes too long, forces current tasks to stop
	 * and aborts any pending tasks.
	 */
	public void stopProcessing() {
		LOG.info("Stopping ProcessHeartbeats task...");
				
		final long start = System.currentTimeMillis();
		processHeartbeatsScheduler.shutdown();
		while(!processHeartbeatsScheduler.isTerminated()) {
			
			if((System.currentTimeMillis() - start) > 10 * 1000) { // TODO: Make configurable
				LOG.warn("processHeartbeatsScheduler took too long to shutdown. Not waiting any longer."); 
				processHeartbeatsScheduler.shutdownNow();
				break;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOG.error("Interrupted exception while waiting for processHeartbeatsScheduler to shutdown: " +
						e.getLocalizedMessage());
				break;
			}
		}
		
		if(processHeartbeatsScheduler.isTerminated()) {
			LOG.info("\tprocessHeartbeatsScheduler successfully shutdown ("+(System.currentTimeMillis() - start)+"ms)");
		} else {
			LOG.info("\tprocessHeartBeatsScheduler failed to shutdown successfully, but a best attempt was made.");
		}
	}

	
	/**
	 * Starts sending heartbeats to registered components
	 */
	public void start() {
		if(heartbeatThread == null) {
			initHeartbeat();
		}

		heartbeatThread.start();
		
		LOG.info("HeartbeatManager started");
	}
	
	
	/**
	 * Stops the heartbeat thread
	 * 
	 * @return True if successfully stopped, False otherwise
	 */
	public boolean stop() {
		
		boolean retval = true;
		
		if(heartbeatThread == null) {
			LOG.info("HeartbeatManager was null, nothing to stop");
		}else {
			heartbeatThread.interrupt();

			try{Thread.sleep(500);}catch(InterruptedException ie) {
				LOG.error("Interrupted exception while sleeping...: " + ie.getLocalizedMessage());
			}

			if(!heartbeatThread.isAlive()) {
				heartbeatThread = null; // NOPMD by jpullen on 8/10/11 1:34 PM, encourage GC by nulling here?

				LOG.info("HeartbeatManager stopped");
				
			} else {
				LOG.warn("Failed to kill heartbeat thread");
				retval = false;
			}

		}
		
		// TODO: see if we want to handle this separately
		stopProcessing();

		return retval;
	}
	
	/**
	 * Sends a heartbeat message to the specified component
	 * 
	 * @param component
	 * 
	 * @return true if successfully sends heartbeat<br>
	 * 		   false if:<br>
	 * 			- component's topic wasn't set<br>
	 * 			- failed to parse HeartbeatMessage into JSON<br> 
	 * 				
	 */
	private boolean sendHeartbeat(final NodeComponent component) {
		
		String topic = component.getTopic();
		
		if(topic == null || topic.isEmpty()) {
			// This state shouldn't occur, since you can't create a NodeComponent
			// object without a topic, but checking anyway, especially with allowing
			// creating invalid NodeComponent objects for DB4O.
			// TODO: send an alert?
			LOG.debug("No valid topic set on NodeComponent " + component + "; not sending heartbeat.");
			
			return false;
		}
		
		// TODO: Dynamically retrieve time and node
		// TODO: body field isn't necessary... factor out of message class
		// TODO: keep a heartbeat message around, and just change the field instead
		//		 of creating a new one every time
		HeartbeatMessage hbMsg = new HeartbeatMessage(
				"mach", // name
				"localhost", // node
				Util.timestampNow(), // time 
				"body", // body... TODO: get rid of this
				"request", // type of hb message / hbtype
				"HEARTBEAT", null, // message... sort of unnecessary, except for backward compatibility
				component.getCid()); 
		
		
		String msgBody = MsgParser.machToJSON(hbMsg);
		
		if(msgBody == null) {
			LOG.error("Parsing MachMessage to JSON string failed, not sending heartbeat!");
			return false;
		}
		
		if(msgManager.sendMessage(topic, msgBody)) {
			LOG.debug("Sent HEARTBEAT to component: '" + component.getCid() + "' on topic '" + topic + "'");
		}else{
			LOG.warn("Failed to send heartbeat to component '" + component +"' on topic '" + topic + "'");
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Convenience method for calling @sendHeartbeat(NodeComponent) by specifying
	 * just the component's name
	 * 
	 * @param component
	 * @return
	 */
	private boolean sendHeartbeat(String component) {
		return sendHeartbeat(registry.get(component));
	}
	
	
	/**
	 * Goes through registry and sends a heartbeat to each live component
	 * on the topic they specified
	 * 
	 * TODO: change return type to int, and return the count of heartbeats sent?
	 * 
	 * @return True if sending heartbeats was successful, False otherwise
	 */
	private boolean sendHeartbeats() {
		
		/* TODO: - Will eventually go through registry for live components and
		 *         the topic to send on... for now, testing with collabRoomState
		 *       - Work out relation between componentTimestamps hash and the registry...
		 *         currently the timestmap hashmap is a stand in for the more fully featured
		 *         registry that will be implemented
		 */
		
		if(isComponentTimestampsNull() || componentTimestamps.isEmpty()) {
			LOG.info("componentTimestamps is empty, so not sending any heartbeats");
			return false;
		}
		
		String topic;		
		int heartbeatsSent = 0;
		int fails = 0;
		NodeComponent nodeComponent;
		HeartbeatMessage hbMsg;
		
		// Synchronized to disallow modifications to the hash while we're sending out heartbeats...
		// TODO: review for proper threading/concurrent access best practices
		synchronized(this) {
			for(String component : componentTimestamps.keySet()) {
				
				if((nodeComponent = registry.get(component)) != null) {
					
					if(sendHeartbeat(nodeComponent)) {
						heartbeatsSent++;
					}else{
						fails++;
					}
					
				}else{
					LOG.debug("nodeComponent not found in registry.  Not sending heartbeat: " + component);
				}
				
				nodeComponent = null;
				
			} // for each component in componentTimestamps
			
			LOG.info(String.format("Sent %1d heartbeat" + ((heartbeatsSent == 1) ? "" : "s" +
				", with %d failure" + ((fails == 1) ? "" : "s")), heartbeatsSent, fails));
			
		} // synchronized block
		
		return true;
	}

	
	/**
	 * <p>Processes heartbeat/status messages.  The message is sent to this method by
	 * HeartbeatManager's update() function, since it's observing the MachMessageManager</p>
	 * 
	 * <p>Expects to receive only HeartbeatMessage objects, as this is all HeartbeatManager
	 * is interested in.</p>
	 * 
	 * @param msg String or HeartbeatMessage object
	 * 
	 * @return true if the message was processed successfully, false otherwise
	 */
	private boolean processMessage(final Object msg) {
		
		LOG.info("\n\n**** PROCESSING SINGLE MESSAGE ****\n");
		
		String strMsg = null;
		HeartbeatMessage mmMsg = null;
		
		if(msg instanceof String) {
			strMsg = (String) msg;
			
			if(!strMsg.startsWith("{")) {
				// This is probably an old component that's sending just it's name
				
				if(registry.contains(strMsg)) {				
					addComponentTimestamp(strMsg, System.currentTimeMillis());
					return true;
				}else {
					LOG.info("Received String from component not in the registry: '" +
							strMsg + "'.  If this is a valid component, then please register it first");
					// TODO: send email alert
					return false;
				}
				
				
			}else {
				try{
					mmMsg = (HeartbeatMessage) MsgParser.parse(strMsg);
				}catch(ClassCastException e) {
					LOG.warn("Message received was not a HeartbeatMessage, not processing", e);
					return false;
				}
			}
		}		
		
		else if (msg instanceof HeartbeatMessage) {
			
			mmMsg = (HeartbeatMessage) msg;
			
		} else {
			
			LOG.info("Received unrecognized message type, not processing: |" + msg.toString() + "|");
			return false;
		}
		
		if(mmMsg != null) {
			
			// FINALLY PROCESS IT!
			// TODO: add current time, or the time in the message?
			// TODO: upgrade to something more robust, over the componentTimestmap hash
			// TODO: ugly, refine all this checking registry... addCompnentTimestamp should
			//		 do that.
			
			if(registry.getRegistrantNames().contains(mmMsg.getName())) {				
				addComponentTimestamp(mmMsg.getName(), System.currentTimeMillis());
				return true;
			}else {
				LOG.info("Received HeartbeatMessage from component not in the registry: '" +
						mmMsg.getName() + "'.  If this is a valid component, then please register it first");
				// TODO: send email alert
				return false;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Processes the various messages that come into Mach
	 *
	 * TODO: move to MachMessageManager ?!?
	 * 
	 * @param messages List of messages to process
	 * @return True if processed at least 1 message, false otherwise
	 */
	private boolean processMessages(final List<String> messages) {
		/* TODO:
		 * - Messages will contain:
		 *   - a timestamp
		 *   - an id/key identifying the component that sent it.  
		 *   - topic to reply to
		 *   Possibly also some other feed specific metadata for Mach to
		 *   act on.  may include timestmap for last time the component took some action/
		 *   received a message from its feed, etc...
		 */
		
		

		if(messages == null || messages.isEmpty()) {
			LOG.debug("Not processing messages, list is empty.");
			return false;
		}
		
		LOG.debug("Processing message list of size: " + messages.size());
		
		
		String name = null;
		String strTime = null;
		String node = null;
				
		JSONObject obj = null;
		JSONObject machMsg = null;
		JSONObject body = null;
		MachMessageType type = null;
		
		
		long curTime;
		for(String msg : messages) {
			curTime = System.currentTimeMillis();
			
			

			/* TODO:
			 * 
			 * Parse out:
			 *  - component name
			 *  - ?
			 * 
			 * Needs to process multiple message types, as yet undefined
			 * 	- status (should also contain custom status info, eventually)
			 *  - register?
			 *  - unregister?
			 * 
			 * update component's timestamp in the componentTimestamps map
			 */
			
/* All this JSON processing has been moved out to MsgParser, etc			
			try{
				
				 obj = new JSONObject(msg);
				 
				 machMsg = obj.getJSONObject("mach");
				 type = MachMessageType.valueOf(machMsg.getString("type"));
				 
				 
				 name = machMsg.getString("name");
				 node = machMsg.getString("node");
				 strTime = machMsg.getString("timestamp");
				 body = machMsg.getJSONObject("body");
				 
				 
				 switch(type) {
				 	case STATUS:
				 		String status = body.getString("status");
				 		// TODO: process this or some form of a status field against the
				 		//		 category of this component
				 		
				 		// TODO: simply adding timestamp... should interact with registry?
				 		addComponentTimestamp(name, curTime);
				 		break;
				 		
				 	case REGISTER:				 					 		
				 		
				 		registry.register(machMsg);
				 		
				 		break;
				 		
				 	case UNREGISTER:
				 		// TODO:
				 		//registry.unregister(component);
				 		break;
				 		
				 	case CONTROL:
				 		
				 		break;
				 		
				 	default:
				 		LOG.warn("Unknown MachMessageType: " + type.toString());
				 }
				 
				 				 
				
			}catch(JSONException e) {
				LOG.error("Exception parsing JSON message", e);
				// TODO: Just ignore?  Could send an alert...
								
				// Probably wasn't JSON, so if it's just a string with the name of a component, then
				// add it:
				if(msg != null && !msg.isEmpty()) {
					
					// Check registry since the component may not be in the timestamp map yet
					if(registry.getRegistrantNames().contains(msg)) {
						
						addComponentTimestamp(msg, curTime);
					}else {
						LOG.info("Received non-JSON message that wasn't a recognized component: '" +
								msg + "'.  If this is a valid component, then please register it first");
						// TODO: send email alert
					}
				}
			} // end JSONException try/catch

*/
		} // end for each message

			
			
		return true;
	}
	
	
	/**
	 * Helper method for adding component/timestamp pairs to the hash.
	 * Uses lazy instantiation, and will be one point of access for logging.
	 * 
	 * @param component Name of component to add/update timestamp for
	 * @param time Timestamp for when the heartbeat was received
	 */
	private void addComponentTimestamp(final String component, final long time) {

		if(isComponentTimestampsNull()) {
			componentTimestamps = new HashMap<String, Long>();
		}

		synchronized(this) {
			componentTimestamps.put(component, time);
		}
		
		LOG.debug("Added/Updated timestamp for component: " + component + 
				"("+ new Timestamp(time).toString() +")");
	}
	
	
	/**
	 * Helper method for removing the specified component from the 
	 * @componentTimestamps HashMap.
	 * 
	 * @param component Name of the component to remove from @componentTimestamps
	 * 
	 * @return True if object was found, False otherwise
	 */
	private boolean removeComponentTimestamp(final String component) {
		
		if(isComponentTimestampsNull()) {
			return false;
		}
		
		Long result = componentTimestamps.remove(component);
		if(result != null) {
			LOG.info("Successfully removed componentTimestamp for: " + component);
			return true;
		}
		
		LOG.info("Specified component '"+component+"' was not found in componentTimestamps");
		return false;
	}

	
	/**
	 * Checks every entry in the timestamp hashmap to see when the last time
	 * an ack was received from a component.  If the elapsed time exceeds the
	 * stale threshold, then an action particular to that component is taken.
	 */
	private synchronized void processComponentHeartbeats() {
		/* TODO:
		 *  Loop through registry and check last timestamps...
		 * if it exceeds a threshold, find out what action to take (this
		 * will later be based on categories and other metadata the component provides)
		 */
		long curTime;
		long componentTime;

		if(isComponentTimestampsNull()) {
			LOG.info("No components registered, not processing last received timestamps");
			return;
		}
		
		String info;
		NodeComponent tempComponent = null;
		
		// TODO: Better place try/catches for narrowing down units of work
		try {
		
			for(String component : componentTimestamps.keySet()) {
				
				curTime = System.currentTimeMillis();
				componentTime = componentTimestamps.get(component);
				
				tempComponent = registry.get(component);
				
				if( ((curTime - componentTime) > Props.staleThreshold) /* Filter on islive? */ ) {
	
					/* TODO: Need to get an action to take here, too.... maybe something you
					 * 		 poll from the component registry information
					 */
					
					if(registry.isComponentOnAlert(component)) {
						
						LOG.debug("Component '" + component + "' is already on alert, not sending new alert.");
						
						// TODO: IF has been on alert for longer than threshold, restart app
						if((curTime - componentTime) > (Props.periodBeforeRestart*60*1000) ) {
						   //(curTime - tempComponent.getLastRestartTime()) > (Props.periodBeforeRestart*60*1000)	) {
							
							if(tempComponent.getLastRestartTime() == 0) {
								// kludgy...
								registry.get(component).setLastRestartTime(curTime);
							}
							
							if((curTime - tempComponent.getLastRestartTime()) > (Props.periodBeforeRestart*60*1000)	) {
							
								LOG.debug("The " + Props.PROP_PERIOD_BEFORE_RESTART + 
										" threshold was exceeded, so restarting component " + component);
								registry.get(component).setLive(false);
								registry.get(component).setLastRestartTime(System.currentTimeMillis());
								AppMgr.restartComponent(tempComponent.getName());
								registry.get(component).setLive(true);
								
							}
						}
					}else {
						LOG.debug("Alert triggered for component: " + component);
						
						// Tell the registry a component triggered an alert
						registry.alertTriggeredFor(component);
						
						info = "The component failed to respond, and may not be functioning properly.\n\n";
						info += "Time of alert: " + new Timestamp(curTime).toString() + "\n" +
							    "Last ACK: " + new Timestamp( componentTimestamps.get(component) ).toString() + "\n";
						
						sendEmailAlert(component, info, false /* don't ignore timeout */);
					}
	
				} else {
	
					
					if(registry.isComponentOnAlert(component)) {
						
						registry.clearAlertFor(component);
						
						info = "Received a response from a component that previously triggered an alert.";
						
						
						//String diffTimeStr = Util.timeBetweenAlertAndAck(registry.getAlertTimeFor(component),
						//		componentTimestamps.get(component));
						
						Timestamp alertTime = new Timestamp( registry.getAlertTimeFor(component) );
						Timestamp responseTime = new Timestamp( componentTimestamps.get(component) );
						
						// TODO: break down into separate hours, minutes, seconds, and 
						//		 display properly based on how long it was
						
						long diff = responseTime.getTime() - alertTime.getTime();
						
						float mins = (diff / (60000f));
						DecimalFormat df = new DecimalFormat("#.##");
						
						info += "\n\nTime of alert: " + alertTime.toString();
						info += "\nTime of response: " + responseTime.toString();
						
						info += "\nMinutes between: " + df.format(mins);
						
						info += "\n\nThe component took longer than expected to respond, but appears to be " +
							    "sending ACKs again.";
						
						sendEmailAlert(component, info, true /* ignore timeout */);
					}
				}
			}
		} catch(Exception e) {
			LOG.error("Caught unhandled exception while processing component timestamps: " 
					+ e.getMessage(), e);
		}

	}

	
	/**
	 * Sends an email alert.  Depends on the emailConsumer component running on the
	 * same node.
	 * 
	 * TODO: Integrate with emailAlerts manager
	 * TODO: Add more robust message contents, with specific errors
	 * 
	 * 
	 * @param componentName Name of the component the alert is associated with
	 * @param info Any extra metadata to include in the email
	 */
	private void sendEmailAlert(String componentName, String info, boolean force) {
		/* TODO: Set up proper emails to send to and from...also to include
		 * 		 more information in the body of the email...
		 */

		if(!alertManager.containsAlertFor(componentName)) {
						
			EmailAlert emailAlert = emailAlertFactory.createEmailAlert(Props.rabbitEmailEndpoint,				 
					Props.emailTo, 
					Props.emailFrom, 
					componentName);	
			
			emailAlert.setTimeout(Props.emailAlertTimeout);
			
			alertManager.addEmailAlert(componentName, emailAlert);
		}
		
		alertManager.sendAlertEmail(componentName, info, force);

	}

	
	/**
	 * Gives status on whether or not the main heartbeat manager thread
	 * is currently running.
	 * 
	 * @return True if Heartbeat thread is running, False otherwise
	 */
	public boolean isRunning() {
		if(heartbeatThread == null) {
			return false;
		} else {		 
			return heartbeatThread.isAlive();
		}
	}
	
	
	/**
	 * Returns count of components that are actively responding to heartbeats
	 * 
	 * TODO: initially just the size of the component hash... if apps are shutdown
	 *       or not responding (and assuming we didn't restart them), we could
	 *       filter down the count by those that have responded since the last interval.  
	 * 
	 * @return
	 */
	public int getLiveComponentCount() {
		if(isComponentTimestampsNull()) {
			return 0;
		}else{
			return componentTimestamps.size();
		}
	}	
	
	
	/**
	 * <p>Kicks off a discovery process to decide which components are live and
	 * supposed to be responding</p>
	 * 
	 * <p>Assumes the registry has already been populated from the db</p>
	 * 
	 * <p>
	 * TODO: pull out into a DiscoveryManager ?
	 * </p>
	 * 
	 */
	public void discover() {
		LOG.info("entering discovery phase...");
		
		if(registry.getRegistrantsCount() > 0) {
						
			LOG.info("\tcomponents in registry to ping: " + registry.getRegistrantsCount());
			
			Thread t = new Thread(new Runnable(){

				@Override
				public void run() {
					
					try{
						
						// Send off a heartbeat to each component in the registry to see who's listening
						for(String cmpName : registry.getRegistrantNames()) {				
							sendHeartbeat(registry.get(cmpName));
						}
						
						// Give them time to reply... TODO: how long?  Make configurable
						Thread.sleep(35000);
						
						// By now, any components that responded will have updated a timestamp entry
						// in the componentTimestamps hash... so we should be ready to start processing
						// them and sending heartbeats regularly.  Notify the proper parties:
						
						//if(!componentTimestamps.isEmpty()) {
							LOG.info("DISCOVERY: Found online: " + componentTimestamps.size());
						//}									
						
					
					}catch(InterruptedException e) {
						LOG.error("Sleep interrupted while waiting for components to respond");
					}catch(Exception e) {
						LOG.error("Caught unhandled exception in discovery thread: " + 
								e.getMessage(), e);
					}finally{
						
						
						// TODO: just calling here to force the adding of components that don't yet know
						//		 how to send a message on their own later
						populateComponentsHashFromRegistry();
						
						
						// Regardless of results, start up the processing, so we're ready
						// to handle newly registered components during runtime
						
						// Start sending regular heartbeats to components
						start();
						
						// Start the heartbeat processing task
						startProcessing();
					}
				}
				
			});
			
			t.setDaemon(true);
			t.setName("hbManager Discovery task");
			
			t.start();
			
		}else{
			LOG.info("\tno components in registry, so not sending any heartbeats to see who's online");
		}
	}
	
	
	/**
	 * Method to populate the local componentsTimestamp hash with components
	 * from the registry
	 * 
	 * <p>This is mostly for dealing with components that will never send something
	 * on their own.  So until components are also sending, we still need to trigger them
	 * with a heartbeat, even if they're not really there (in case MACH started while they were
	 * down, or they went down but need to come back up and be pinged.</p>
	 * 
	 */
	public void populateComponentsHashFromRegistry() {
		LOG.trace("populateComponentHashFromRegistry() entered...");
		
		if(registry.getRegistrantsCount() > 0) {
			
			LOG.info("Populating componentTimestamps with all components in registry");
			int count = 0;
			for(String comp : registry.getRegistrantNames()) {
				if(!componentTimestamps.containsKey(comp)) {
					componentTimestamps.put(comp, System.currentTimeMillis());
					count++;
				}
			}
			
			LOG.info("\tAdded " + count + "component" + ((count == 1) ? "" : "s"));
			
		} else {
			LOG.info("No registered components; not populating componentTimestamps");
		}
	}
	
	
	/**
	 * <p>
	 * Called with updates when various other Mach components we're observing
	 * notify HeartbeatManager of events
	 * </p>
	 * 
	 * @param observable Class that's notifiying us of an event
	 * @param object Type of event we're being notified of
	 */
	@Override
	public void update(Observable observable, Object object) {
		// TODO:  Will receive notifications here when a component is registered/unregistered?
				
		
		if(observable instanceof ComponentRegistry) {
				
			RegistrationEvent event = null;

			if(object instanceof RegistrationEvent) {
				event = (RegistrationEvent) object;
			}else{
				// TODO:  handle this?  more for telling developer that parameter type changed
				throw new IllegalArgumentException();
			}
			

			if(event.getEvent() == RegistrationEvents.REGISTERED) {
				
				LOG.info("hbManager was notified that a component registered (NOTE: taking no action): " + 
						event.getComponentId());

			} else if(event.getEvent() == RegistrationEvents.UNREGISTERED) { // Component was removed

				removeComponentTimestamp(event.getComponentId());

			} else {
				LOG.error("Event type unrecognized: " + event.getEvent());
				throw new IllegalArgumentException();
			}
		
		} // if ComponentRegistry
		
		
		
		else if(observable instanceof MachMessageManager) {
			
			
			if(object instanceof HeartbeatMessage) {
				LOG.info("Observed HeartbeatMessage from MachMessageManager");
				
				// TODO: make it instanceof MachMessage and case through them?
				//		 but the HBManager is only really concerned with heartbeats
				
				processMessage((MachMessage)object);
				
			} else if(object instanceof String) {
				// TESTING:  Assume it's a string of a message, and turn it into a message
				//           to be processed.
				
				String msg = (String) object;				
				LOG.debug("string value: ===========\n" + msg + "\n========");
				processMessage(msg);
				
				//MachMessage machMsg = MsgParser.parse(msg);
				
				//if(machMsg == null) {
				//	LOG.info("String from MachMessageManager was not a valid MachMessage, so not processing!");
				//} else {
					//processMessage(machMsg);
				//}
				
			} else {
				LOG.info("Observed unsupported Object from MachMessageManager: " + 
						object.getClass().getName());
			}
			
		} // if MachMessageManager
	}
	
	
	/**
	 * 
	 * @return True if producer is connected, False otherwise
	 */
	public boolean isProducerLive() {
		
		if(msgManager.producer == null)
			return false;
		
		return msgManager.producer.isConnected();
	}
	
	
	/**
	 * TODO: Have this check if it properly closed the connection, and 
	 * return a boolean
	 * 
	 * TODO: Don't really want this method working in the producer...  the MessageManager is now
	 *       the component to deal with it.  This is a holdover from when the producer was a member of
	 *       this class.
	 */
	public void stopProducer() {
		
		if(msgManager.producer != null) {			
			msgManager.producer.destroy(); // This closes the connection and nulls it
			msgManager.producer = null;
		}
	}
	
	
	/**
	 * TODO: delete/replace... the producer will be initialized when MachMessageManager is created
	 * 
	 * @return True if producer initialized properly, false otherwise
	 *
	public boolean startProducer() {
		return initRabbitProducer();
	}*/
	
	
	/**
	 * TODO: deprecated.  This should be asked of the MachMessageManager, not directly here.
	 * 
	 * @return True if producer is connected, False otherwise
	 */
	public boolean isConsumerLive() {
		
		if(msgManager.consumer == null)
			return false;
		
		return msgManager.consumer.isConnected();
	}
	
	
	/**
	 * TODO: Have this check if it properly closed the connection, and 
	 * return a boolean
	 * 
	 * TODO: deprecated... this should be done via MachMessageManager, not here
	 */
	public void stopConsumer() {
		
		if(msgManager.consumer != null) {			
			msgManager.consumer.destroy(); // Verify this unsubscribes and closes conn properly			
			msgManager.consumer = null;
		}
	}
	
	
	/**
	 * TODO: delete... consumer is initialized via the MachMessageManager
	 * 
	 * @return True if consumer initialized properly, false otherwise
	 *
	public boolean startConsumer() {
		return initRabbitConsumer();
	}*/
	
	
	/**
	 * Synchronized utility method for checking state of componentTimestamps
	 * to improve synchronization/concurrent access 
	 * 
	 * @return
	 */
	private synchronized boolean isComponentTimestampsNull() {
		return componentTimestamps == null;
	}
	
	
	/** 
	 * Runnable for processing heartbeat response times
	 * 
	 * @author jpullen
	 *
	 */
	private final class ProcessHeartbeatsTask implements Runnable {

		private long curTime;
		
		@Override
		public void run() {		
			LOG.debug("PROCESSHEARTBEATS TASK...");

			curTime = System.currentTimeMillis();
			
			// Make sure it hasn't been too short of an interval... depending on how the scheduled task
			// is called, it could execute twice in succession to catch up
			// TODO: get a Property for this subinterval value
			if((curTime - lastProcessTime) >= 3) { //Props.processAcksInterval) {															
				
				// Process component timestamps, checking for stale components
				
				try{
					processComponentHeartbeats();
				}catch(Exception e){
					LOG.error("\n\n\n*** EXCEPTION PROCESSING HEARTBEATS ***\n\n" + 
							e.getLocalizedMessage() + "\n\n*********************************************",e);
				}
				
				
				lastProcessTime = curTime;
			}
			
		}
		
	}
}
