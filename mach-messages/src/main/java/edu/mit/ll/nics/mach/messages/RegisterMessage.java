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

/**
 * Represents a MACH register message
 * 
 * @author jpullen
 *
 */
public final class RegisterMessage extends MachMessage {
	
	/** The MachMessageType of the message */
	private final MachMessageType type = MachMessageType.REGISTER;
	
	/** The topic mach is to send heartbeats to */
	private String topic;
	
	/** The path to where the component is running locally */
	private String path;
	
	/** The MACH Category of component it is */
	private String category;
	
	/** 
	 * Maybe notes or extra info... possibly an area of key/value pairs
	 *  
	 * OPTIONAL
	 */
	private String metadata;
	
	/** 
	 * The appMgrName the component is set up with appMgr with on the local node 
	 *
	 * REQUIRED
	 */
	private String appMgrName;
	
	/** Whether or not this component exposes JMX, and is enabled */
	private boolean isJMXEnabled;
	
	/** Whether or not this component uses Camel.
	 * 
	 * TODO: may do away with this, and just have it implied by its Category
	 */
	private boolean isCamelApp;
	
	
	
	/**
	 * Full argument constructor
	 * 
	 * @param name
	 * @param node
	 * @param timestamp
	 * @param path
	 * @param category
	 * @param metadata
	 * @param appMgrName 
	 * @param isJMXEnabled
	 * @param isCamelApp
	 */
	public RegisterMessage(String name, String node, String timestamp,
			String topic, String path, String category, String metadata, String appMgrName, 
			boolean isJMXEnabled, boolean isCamelApp, String version) {
		
		super(name, node, timestamp, version);
		
		this.topic = topic;
		this.path = path;
		this.category = category;
		this.metadata = metadata;
		this.appMgrName = appMgrName;
		this.isJMXEnabled = isJMXEnabled;
		this.isCamelApp = isCamelApp;
		
	}

	
	@Override
	public Object getMessageType() {		
		return type;
	}

	public String getTopic() {
		return topic;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public String getPath() {
		return path;
	}

	public String getCategory() {
		return category;
	}


	public void setCategory(String category) {
		this.category = category;
	}


	public void setPath(String path) {
		this.path = path;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public String getAppMgrName() {
		return appMgrName;
	}

	public void setAppMgrName(String appMgrName) {
		this.appMgrName = appMgrName;
	}

	
	public boolean isJMXEnabled() {
		return isJMXEnabled;
	}

	public void setJMXEnabled(boolean isJMXEnabled) {
		this.isJMXEnabled = isJMXEnabled;
	}

	public boolean isCamelApp() {
		return isCamelApp;
	}

	public void setCamelApp(boolean isCamelApp) {
		this.isCamelApp = isCamelApp;
	}
}
