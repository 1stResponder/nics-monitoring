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
package edu.mit.ll.nics.mach.messages;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.json.JSONException;
import org.json.JSONObject;

import edu.mit.ll.nics.mach.messages.AlertMessage.AlertType;


/**
 * Utility class for parsing mach JSON messages into the
 * proper MachMessage type
 * 
 * @author jpullen
 *
 */
public final class MsgParser {

	private static final Logger LOG = Logger.getLogger(MsgParser.class);
	
	
	/**
	 * Private constructor
	 */
	private MsgParser() {
		PropertyConfigurator.configure("log4j.properties");
	}	
	
	
	/**
	 * Parses a string containing JSON into a MachMessage object
	 * 
	 * @param msg String containing a mach message in JSON format
	 * 
	 * @return a MachMessage object of the appropriate type, or null if
	 * 		   it failed to parse a MachMessage
	 */
	public static MachMessage parse(String msg) {
						
		
		// This check guarantees the string is a mach json 
		// message with a valid type field
		if(!isMachMsg(msg)) {
			return null;
		}
		
		MachMessage retval = null;		
		JSONObject mach;
		
		try{
			mach = new JSONObject(msg).getJSONObject(MsgParser.MACH);
			
			String name = mach.optString(MsgParser.NAME, null);
			String node = mach.optString(MsgParser.NODE, null);
			String type = mach.optString(MsgParser.TYPE, null);
			String time = mach.optString(MsgParser.TIME, null);
			String body = mach.optString(MsgParser.BODY, null);
			String vers = mach.optString(MsgParser.VERSION, null);
			
			// TODO: if any of the above are null that are required, then need to bail
			
			if(type != null) {
								
				MachMessageType mtype = MachMessageType.valueOf(type.toUpperCase());				
				
				JSONObject jsonBody = mach.getJSONObject(MsgParser.BODY); // TODO: check for exception?
				
				switch(mtype) {
				
					case REGISTER:
						String topic = jsonBody.optString(MsgParser.TOPIC, null);
						String path = jsonBody.optString(MsgParser.PATH, null);
						String category = jsonBody.optString(MsgParser.CATEGORY, null);
						String metadata = jsonBody.optString(MsgParser.METADATA, null);
						String appMgrName = jsonBody.optString(MsgParser.APPMGRNAME, null);
						
						String isJMX = jsonBody.optString(MsgParser.ISJMXENABLED, null);
						boolean b_isJMX = false;
						if(isJMX != null) {
							b_isJMX = (isJMX.equals("true") ? true : false);
						}
						
						String isCamel = jsonBody.optString(MsgParser.ISCAMELAPP, null);
						boolean b_isCamel = false;
						if(isCamel != null) {
							b_isCamel = (isCamel.equals("true") ? true : false);
						}
						
						retval = new RegisterMessage(name, node, time,
								topic, path, category, metadata, appMgrName, b_isJMX, b_isCamel, vers);
						
						break;
						
					case UNREGISTER:
						String reason = jsonBody.optString(MsgParser.REASON, null);
						
						retval = new UnregisterMessage(name, node, time, reason, vers);
						
						break;
						
					case HEARTBEAT:
						String hbtype;
						String message;
						
						hbtype = jsonBody.optString(MsgParser.HBTYPE, null);
						message = jsonBody.optString(MsgParser.MESSAGE, null);
						
						// TODO: if they're null, this isn't a valid message... set retval to null?
						
						retval = new HeartbeatMessage(name, node, time, body, hbtype, message, vers);
						
						break;
					
					case ALERT:
						String messageBody;
						AlertType alertType = null;
						
						messageBody = jsonBody.optString(MsgParser.MESSAGE, "");
						
						String strAlertType = jsonBody.optString(MsgParser.ALERTTYPE, null);
						if(strAlertType == null) {
							alertType = AlertType.UNDEFINED;
						} else {
							try {
								alertType = AlertType.valueOf(strAlertType);
								
							} catch(Exception e) {
								LOG.error("Exception occurred while creating AlertType value from String: " + 
										strAlertType);
							} finally {
								if(alertType == null) {
									alertType = AlertType.UNDEFINED;
								}
							}
						}												
						
						retval = new AlertMessage(name, node, time, vers, messageBody, alertType);
						
				
					case CONTROL:
						
						retval = null;
						break;
						
					case STATUS:						
						
						retval = null;
						break;
						
					default:
						LOG.warn("UNKNOWN message type: " + mtype.name());
						retval = null;
				}
				
			}
			
		
		}catch(JSONException e) {
			LOG.error("parse(String): JSONException while parsing message", e);
			retval = null;
		}
						
		return retval;
	}
	
	
	/**
	 * Initializes a @NodeComponent object from the fields of a 
	 * Mach JSON message.  If it fails to build the @NodeComponent,
	 * then null will be returned.
	 * 
	 * TODO: Move out of this MsgParser... only mach should really be using
	 *       the NodeComponent class?  
	 * 
	 * @param object Expects a JSONObject or a String 
	 * @return a NodeComponent object populated from the JSON message if successful, null otherwise
	 */
/*	
	public static NodeComponent buildNodeComponentFromRegisterMsg(Object object) {
		
		JSONObject machMessage = null;
		NodeComponent nodeComponent = null;
		
		// TODO:RELIC of copying from Mach... refactor
		//assert(Assert.nodeComponent_memberCount == 11);
		
					
		
		if((machMessage = getMachJSONObject(object)) == null) {
			// Leave logging to getMachJSONObject()
			return null;
		}		
				
		
		String type = machMessage.optString("type", null);
		if(type == null || type.isEmpty()) {
			LOG.info("No message type defined, so aborting creation of a NodeComponent");
			return null;
		}else {
			// TODO: may shorten to "reg"
			if(!type.equals("register")) {
				LOG.info("Aborting creation of NodeComponent. Expected a 'register' message, but" +
						" received a '" + type + "' message.");
				return null;
			}
		}
		
		
		String name = machMessage.optString("name", null);		
		String node = machMessage.optString("node", null);
		
		JSONObject body = machMessage.optJSONObject("body");
		if(body == null) {
			LOG.info("The body of the registration message was null. Aborting NodeComponent construction");
			return null;
		}
		
		String topic = body.optString("topic", null);

		// TODO: validate these 3 required fields
		if(name == null || topic == null || node == null) {
			LOG.info("One or more of the 3 required fields is null, so aborting NodeComponent creation:\n" +
					"\tname: " + name + "\n" +
					"\ttopic: " + topic + "\n" +
					"\tnode: " + node + "\n");

			return null;
		}

		nodeComponent = new NodeComponent(name,	topic, node,
				false, // is jmxEnabled
				true   // isCamelComponent
			);
		
		// Set other provided fields
		// TODO: Add assert on number of fields to set here
			

		String category;
		if((category = body.optString("category", null)) != null) {
			// TODO:  do a smarter lookup of category compares...  valueOf is too strict.
			//		  ideally we'd call toLowercase() on them both and compare.
			
			Category c = null;
			try{
				c = Category.valueOf(category);
			}catch(Exception e) {
				LOG.warn("Unknown category.  May not match the exact case of the Category identifier: " +
						category);
			}
			
			if(c != null) {			
				nodeComponent.setCategory(c);
			}
		}

		String metadata;
		if((metadata = body.optString("metadata", null)) != null) {
			nodeComponent.setMetadata(metadata);
		}

		String path;
		if((path = body.optString("path", null)) != null) {
			nodeComponent.setPath(path);
		}
				
		
		return nodeComponent;
	}
*/	
	
	/**
	 * Adds or updates the fields of the @NodeComponent passed in with the values
	 * in the JSON message
	 * 
	 * TODO: moving NodeComponent utilities out of this parser
	 * 
	 * @param nodeComponent Existing nodeComponent object that will be updated
	 * @param json JSON object representing a mach message that will populate the nodeComponent
	 * @return A @NodeComponent object updated from values from the JSON message 
	 */
	/*public static NodeComponent addUpdateFields(NodeComponent nodeComponent, JSONObject json) {
		// TODO: implement
		return null;
	}*/
	
	
	/**
	 * Processes the input, whether it's a string containing JSON, or
	 * a JSONObject, and returns the "mach" JSONobject.
	 * 
	 * @param object Either a String containing JSON, or a JSONObject believed to
	 * 		  contain a "mach" JSONObject
	 * 
	 * @return The "mach" JSON object if exists, null otherwise
	 */
	public static JSONObject getMachJSONObject(Object object) {
		JSONObject retval = null;
		
		JSONObject msg;
		JSONObject machMessage;
		
		if(object instanceof String) {

			try {
				msg = new JSONObject((String)object);
				machMessage = msg.getJSONObject("mach");
				
				retval = machMessage;

			} catch (JSONException e) {
				LOG.error("getMachJSONObject(Object): JSONException while parsing String for 'mach' " +
						"JSONObject", e);
			}

		}else if(object instanceof JSONObject) {			

			msg = (JSONObject) object;

			try {
				machMessage = msg.getJSONObject("mach");
				retval = machMessage;
				
			} catch (JSONException e) {
				LOG.error("getMachJSONObject(Object): JSONException while processing JSONObject " + 
						"for 'mach' JSONObject", e);
			}

		}else {
			LOG.error("getMachJSONObject(Object): Received unexpected data type: " + 
					object.getClass().getName() + " Expected either String or JSONObject.");
		}

		return retval;
	}
	
	
	/**
	 * Verifies whether or not a string contains a valid mach message by testing:
	 *  - if it's a JSON message
	 *  - if it has a "mach" key
	 *  - if the mach object has a "type" key
	 *  - if the "type" key is a known type 
	 *  
	 *  TODO: integrate json-schema, and do a full compliancy check
	 * 
	 * @param msg The string to test for being a mach message or not
	 * 
	 * @return True if the string is a valid JSON message that contains a 'mach' object 
	 * 		which contains a valid 'type' value.
	 * 		False otherwise. 
	 */
	public static boolean isMachMsg(String msg) {
		try {
			
			JSONObject json = new JSONObject(msg);			
			JSONObject mach = json.getJSONObject("mach");
			
			String type;
			// If the mach message has a type field, verify it's a known type
			// otherwise return false
			if((type = mach.optString("type", null)) != null) {
				
				// Make sure the type matches a known mach message type
				for(MachMessageType mtype : MachMessageType.values()) {
					if(MachMessageType.valueOf(type.toUpperCase()).equals(mtype) ) {
						return true;
					}
				}				
			}
			
			LOG.info("String did not contain a valid mach message");
			return false;
			
		} catch (JSONException e) {
			//LOG.error("isMachMsg(String): JSONException while checking if String is a MachMessage", e);
			LOG.error("isMachMsg(String): JSONException while checking if String is a MachMessage: " 
					+ e.getLocalizedMessage());
			return false;
		}
	}
	
	
	/**
	 * Builds a JSON string from a MachMessage
	 * 
	 * <p>
	 * 		TODO: Rename to machToJSONString for better clarity, or
	 *    	allow for specifying whether you want the object or the String
	 * </p>
	 * 
	 * @param mach The MachMessage object to transform to a JSON String
	 * 
	 * @return A String containing the JSON message representing the MachMessage
	 */
	public static String machToJSON(MachMessage machMsg) {
		LOG.debug("entered machToJSON...");
		
		if(machMsg == null) {
			LOG.debug("parameter machMsg is null, returning null");
			return null;
		}
		
		String retval = null;
		
		JSONObject mach = new JSONObject();
		JSONObject body = new JSONObject();
		
		try{
			
			if(machMsg instanceof RegisterMessage) {
				LOG.debug("\tIncoming message is RegisterMessage");
				/*				
				body.append(MsgParser.APPMGRNAME, ((RegisterMessage) machMsg).getAppMgrName());
				body.append(MsgParser.CATEGORY, ((RegisterMessage) machMsg).getCategory()); 
				body.append(MsgParser.ISCAMELAPP, "TODO"); 
				body.append(MsgParser.ISJMXENABLED, "TODO"); 
				body.append(MsgParser.METADATA, ((RegisterMessage) machMsg).getMetadata());
				body.append(MsgParser.PATH, ((RegisterMessage) machMsg).getPath());
				body.append(MsgParser.TOPIC, ((RegisterMessage) machMsg).getTopic());
				*/
				
				body.put(MsgParser.APPMGRNAME, ((RegisterMessage) machMsg).getAppMgrName());
				body.put(MsgParser.CATEGORY, ((RegisterMessage) machMsg).getCategory()); 
				body.put(MsgParser.ISCAMELAPP, "TODO"); 
				body.put(MsgParser.ISJMXENABLED, "TODO"); 
				body.put(MsgParser.METADATA, ((RegisterMessage) machMsg).getMetadata());
				body.put(MsgParser.PATH, ((RegisterMessage) machMsg).getPath());
				body.put(MsgParser.TOPIC, ((RegisterMessage) machMsg).getTopic());
				
			
			} else if (machMsg instanceof UnregisterMessage) {
				LOG.debug("\tIncoming message is UnregiserMessage");
								
				body.put(MsgParser.REASON, ((UnregisterMessage) machMsg).getReason());
				
				
			} else if (machMsg instanceof HeartbeatMessage) {
				LOG.debug("\tIncoming message is HeartbeatMessage");
				
				body.put(MsgParser.HBTYPE, ((HeartbeatMessage) machMsg).getType());
				body.put(MsgParser.MESSAGE, ((HeartbeatMessage) machMsg).getMessage());
				
			} else if (machMsg instanceof AlertMessage) {
				LOG.debug("\tIncoming message is AlertMessage");
				
				body.put(MsgParser.MESSAGE, ((AlertMessage) machMsg).getMesssageBody());
				body.put(MsgParser.ALERTTYPE, ((AlertMessage) machMsg).getAlertType().toString());
				
				
			} else {
				LOG.debug("\tIncoming message type is unhandled: " + machMsg.getMessageType().toString() );
				// TODO: Add case for other messages once they get added
				
			}
			
			// TODO: topic?
			mach.put(MsgParser.TYPE, ((MachMessageType)machMsg.getMessageType()).name());
			mach.put(MsgParser.NAME, machMsg.getName());
			mach.put(MsgParser.NODE, machMsg.getNode());
			mach.put(MsgParser.TIME, machMsg.getTimestamp());
			mach.put(MsgParser.VERSION, machMsg.getVersion());
			
			mach.put(MsgParser.BODY, body);
			
			retval = mach.toString();
			
		}catch(JSONException e) {
			LOG.error("JSONException transforming MachMessage to JSON string", e);
			retval = null;
		}
		
		return retval;
	}
	
	
	// CONSTANTS
	// Message fields
	public static final String MACH = "mach";
	public static final String TYPE = "type";
	public static final String NAME = "name";
	public static final String TIME = "time";
	public static final String NODE = "node";
	public static final String BODY = "body";
	public static final String PATH = "path";
	public static final String TOPIC = "topic";
	public static final String VERSION = "version";
	public static final String METADATA = "metadata";
	public static final String CATEGORY = "category";
	public static final String APPMGRNAME = "appMgrName";
	public static final String REASON = "reason";
	public static final String HBTYPE = "type";
	public static final String ALERTTYPE = "alertType";
	public static final String MESSAGE = "message";
	
	public static final String ISJMXENABLED = "isJMXenabled";
	public static final String ISCAMELAPP = "isCamelApp";
	
}
