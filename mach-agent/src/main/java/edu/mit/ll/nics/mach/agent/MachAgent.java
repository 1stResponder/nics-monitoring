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
package edu.mit.ll.nics.mach.agent;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;

import edu.mit.ll.nics.mach.messages.AlertMessage;
import edu.mit.ll.nics.mach.messages.HeartbeatMessage;
import edu.mit.ll.nics.mach.messages.MachMessage;
import edu.mit.ll.nics.mach.messages.MsgParser;

/**
 * Mach Agent (camel processor) class.  Meant to handle the processing of
 * mach messages, as well as sending responses.
 * 
 * @author jpullen
 *
 */
public class MachAgent implements Processor {

	/** Logger */
	private static final Logger log = Logger.getLogger(MachAgent.class);
	
	
	/** 
	 * The URI mach is listening for messages from components on.
	 * 
	 * <p>Default: rabbit://localhost:5672? TODO<p>
	 * 
	 * <p>
	 * TODO: This may be unnecessary and handled in the route.  This bean can be used
	 *       to check conditions on the message, and affect where the message is routed
	 *       to.  But since we want to send mach messages, that's better done within this
	 *       bean.
	 * </p>
	 */
	private String machUri;
	
	
	/** The name of the component this agent is acting on */
	private String componentName;
		
	/** The node this component is on */
	private String node;
		
	/** Producer template for sending MACH messages within processor */
	private ProducerTemplate prodTemplate = null;	
	
	/** Last time a heartbeat request was sent to MACH */
	private long lastMachRequestSent = 0;
	
	/** Last time a heartbeat request was replied to by MACH */
	private long lastMachResponseReceived = 0;
	
	/** The last time a data message was received (non-mach message)*/
	private long lastProcessTime = 0;
	
	/** 
	 * Interval at which the agent send MACH a heartbeat request
	 * <p>Default: 5</p> 
	 */	
	private int hbIntervalInMinutes = 5;
	
	/** 
	 * The content of a heartbeat/ping message that's not wrapped in JSON. This is just for
	 * backward compatibility.
	 * <p>Default: "HEARTBEAT"</p>
	 */
	private String nonJsonHeartbeatContent = "HEARTBEAT";
	
	/** Executor service that will execute Runnable tasks */
	private ScheduledExecutorService execService;
	
	/** Task for sending heartbeats to MACH */
	private HeartbeatTask heartbeatTask;
	
	private LastDataReceivedTask lastDataReceivedTask;
	
	/**
	 *  If time since data was received from producer exceeds this value,
	 *  an alert will be sent to MACH
	 *  
	 *   <p>Default: 600000</P
	 */
	private long noDataAlertInMS = 10 * 60 * 1000;
	
	
	/**
	 * Default constructor
	 */
	public MachAgent() {
		PropertyConfigurator.configure("log4j.properties");
		
		heartbeatTask = new HeartbeatTask();
		lastDataReceivedTask = new LastDataReceivedTask();
				
		execService = Executors.newScheduledThreadPool(3);
		
		execService.scheduleAtFixedRate(heartbeatTask, hbIntervalInMinutes, 
				hbIntervalInMinutes, TimeUnit.MINUTES);
		
		// TODO: make interval a configurable property
		execService.scheduleAtFixedRate(lastDataReceivedTask, 0, 
				1, TimeUnit.MINUTES);
	}
	
	
	/** 
	 * Processes the incoming message, presumed to be a mach message,
	 * and passes it on to be parsed
	 * 
	 * TODO: REFACTOR... way too much if/else nesting
	 * 
	 * @param exchange The incoming camel exchange
	 */
	@Override
	public void process(Exchange exchange) throws Exception {
		
		if(prodTemplate == null) {
			prodTemplate = exchange.getContext().createProducerTemplate();
		}
		
		String body = exchange.getIn().getBody(String.class);
		
		log.debug("Received incoming message: " + body);
				
		if(body != null && !body.equals("")) {
			body = body.trim();
						
			log.debug("entered: parse");
			boolean isMachMsg = MsgParser.isMachMsg(body);
			
			// This is for backward compatibility, accepting a simple plain text PING/HEARTBEAT
			log.debug("Does body:'"+body+"' == '" + nonJsonHeartbeatContent + "' ?");
			if(!isMachMsg && body.equals(nonJsonHeartbeatContent)) {
				
				// TODO: Fix HeartbeatMessage to autofill time and version if null is provided
				// TODO: Check what the body is used for in this case
				HeartbeatMessage msgPing = new HeartbeatMessage(componentName, node, 
						""+System.currentTimeMillis(), "??? BODY?", 
						/* type */ "response", /* message */ null, /* version */ null);
								
				
				//JSONObject json = MsgParser.getMachJSONObject(msgPing);
				String json = MsgParser.machToJSON(msgPing);
				
				if(json != null) {
					//Message msg = exchange.getIn();
					//msg.setBody(json.toString());
					//msg.setBody(json);
					//exchange.setOut(msg);
					sendMsgToMach(machUri, json);
				} else {
					log.debug("Attempt to turn HeartBeat/response message into JSON failed! Not sending message to MACH");
				}
				
			} else if(isMachMsg){
			
				handleMachMessage(body);
						
			} else {
				log.debug("Message wasn't plaintext HEARTBEAT/PING or a MACH message.  Not processing." + 
						" body:\n'" + body + "'\n");
				lastProcessTime = System.currentTimeMillis();
			}
			
			
		}else {
			log.debug("Body of message was null/empty. Not processing.");
		}
		
	}
	
	
	/**
	 * Sends a MACH JSON message to the configured MACH endpoint
	 * 
	 * @param endpoint The Endpoint to send to
	 * @param msg The message body to send to MACH.  Usually a JSON mach message.
	 * 
	 * @return True if the message was sent successfully, False otherwise 
	 */
	private boolean sendMsgToMach(String endpoint, String msg) {
		boolean retval = true;
		
		if(prodTemplate == null) {
			// Severe error if you can get here w/o the producer being initialized.  Nothing
			// will get sent to MACH.  Alternative would be to pass the exchange around so 
			// the prodTemplate can get re-created
			
			log.error("Producer Template is unexpectedly null.  Unable to send messages to MACH!");
			retval = false;
		} else {
			try {
				// TODO: Any other massaging of body need done first?			
				prodTemplate.sendBody(endpoint, msg);
				log.debug("Sent message to mach: " + msg);
			} catch(Exception e) {
				log.error("Caught unhandled exception while sending message to MACH endpoint: " + 
						e.getMessage() + "\nmachUri: " + machUri + "\nmsg: " + msg + "\n", e);
				retval = false;
			}
		}
		
		return retval;
	}
	
	
	/**
	 * Sends an alert message to MACH 
	 * 
	 * @param message Human readable message, will be used in email
	 * @param alertType The type of alert
	 */
	private void sendAlert(String message, AlertMessage.AlertType alertType) {

		AlertMessage alert = new AlertMessage(componentName, node, null, null,
				message, alertType);
		
		String json = MsgParser.machToJSON(alert);
		
		if(json == null) {
			log.debug("JSON came back null from MsgParser.machToJSON(alert).\n" +
					String.format("component: %s\node: %s, message: %s, alertType: %s",
							componentName, node, message, alertType.toString()));
		} else {
			log.debug("Sending JSON alert message to sendMsgToMach");
			sendMsgToMach(machUri, json);
		}
	}
	
	
	/**
	 * Parses MACH message, and sends response, if need be
	 * 
	 * @param body String containing JSON MACH message
	 */
	private void handleMachMessage(String body) {
		MachMessage machMsg = MsgParser.parse(body);
		
		if(machMsg == null) {
			log.debug("Parsed message is null, not doing anything");

			//exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE); // TODO: TEST				
		} else {
		
			if(machMsg instanceof HeartbeatMessage) {
				HeartbeatMessage hbMsg = (HeartbeatMessage) machMsg;
								
				// In the future, may also receive registration requests
				// Change the message to a response, and update the timestamp
				// Also ensure that the componentName is correct
				// Also supporting response types from MACH as an extra measure to know
				// if MACH went down
				
				if(!hbMsg.getName().equals(componentName)) {
					log.debug("componentName of incoming message does not match this component:\n"+					
							"\tincoming: " + hbMsg.getName() + "\n" +
							"\texpected: " + componentName + "\nStopping processing.");
					
					//exchange.setProperty(Exchange.ROUTE_STOP, true);
					// do nothing
				} else {
				
					if(hbMsg.getType().equals("request")) {
						
						log.debug("received a heartbeat request");
						
						// Set the type to response, since we're responding to mach
						hbMsg.setType("response");
						
						// Update the timestamp
						hbMsg.setTimestamp(""+System.currentTimeMillis());
						
						// Get the message in JSON format
						JSONObject json = MsgParser.getMachJSONObject(hbMsg);						
						
						// Set the body of the out message to the json string
						//Message outMsg = exchange.getIn();
						//outMsg.setBody(json.toString());
						//exchange.setOut(outMsg);
						
						//prodTemplate.sendBody(machUri, json.toString());
						sendMsgToMach(machUri, json.toString());
						
					} else if(hbMsg.getType().equals("response")) {
						// TODO: Handle response...
						log.debug("Processing a heartbeat response from MACH...");
						lastMachResponseReceived = System.currentTimeMillis();
						
						
						
						// TODO: IMPLEMENT
						
						
					} else {
						log.debug("Received a heartbeat type other than 'request': " + 
								hbMsg.getType() + ". Stopping processing.");
						
						// TODO: Do anything else?
						
						//exchange.setProperty(Exchange.ROUTE_STOP, true);
					}
				
				} // componentName doesn't match
				
			} else {
				log.debug("Received message type: " + machMsg.getClass().getCanonicalName() +
						".  The agent does not currently process this type.  Stopping processing.");
							
				//exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
			} // Instance of a HeartbeatMessage
		
		} // machMsg not null
	}
	
		
	/**
	 * Task to be scheduled for sending MACH periodic heartbeat
	 * requests.
	 * 
	 * @author jpullen
	 *
	 */
	private final class HeartbeatTask implements Runnable {

		@Override
		public void run() {
			// Need to wrap all in a try, since an exception will stop the task from running again
			try{
				log.debug("!!!Sending heartbeat request to MACH!!!");
				sendMsgToMach(machUri, "msg");
			} catch(Exception e) {
				log.error("Unhanlded exception while sending heartbeat request to MACH URI: " 
						+ e.getMessage(), e);
			}
		}
	}
	
	
	/**
	 * Task that checks if the time since the last message was received exceeds
	 * the specified threshold, and if so, sends an alert message to MACH
	 * 
	 * @author jpullen
	 */
	private final class LastDataReceivedTask implements Runnable {

		@Override
		public void run() {
			try{
				if((System.currentTimeMillis() - lastProcessTime) > noDataAlertInMS) {
					sendAlert(String.format("Data from producer last seen: %s.\nThreshold: %d (ms)",
							new Timestamp(lastProcessTime).toString(), noDataAlertInMS),
							AlertMessage.AlertType.NO_DATA_THRESHOLD_EXCEEDED);
				}
			} catch(Exception e) {
				log.error("Unhandled exception while checking last data received threshold: " 
						+ e.getMessage());
			}
		}
		
	}
	
	
	// ####### Getters and Setters #############
	

	public String getMachUri() {
		return machUri;
	}

	public void setMachUri(String machUri) {
		this.machUri = machUri;
	}


	public final String getComponentName() {
		return componentName;
	}

	public final void setComponentName(String componentName) {
		this.componentName = componentName;
	}


	public final String getNode() {
		return node;
	}

	public final void setNode(String node) {
		this.node = node;
	}

	public final String getNonJsonHeartbeatContent() {
		return nonJsonHeartbeatContent;
	}

	public final void setNonJsonHeartbeatContent(String nonJsonHeartbeatContent) {
		this.nonJsonHeartbeatContent = nonJsonHeartbeatContent;
	}


	public final int getHbIntervalInMinutes() {
		return hbIntervalInMinutes;
	}


	public final void setHbIntervalInMinutes(int hbIntervalInMinutes) {
		this.hbIntervalInMinutes = hbIntervalInMinutes;
	}
}
