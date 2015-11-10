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

/**
 * Represents a component that will be added to the @ComponentRegistry
 *  
 * 
 * @author jpullen
 *
 */
public class NodeComponent {
		
	/** Name of the component, should match the component's soa name/appMgr name */
	private String name;
	
	/** Topic that @Mach will send heartbeats to */
	private String topic;
	
	/** Location on local disk 
	 * TODO: Not really necessary if using appMgr to control UNLESS a single app is running off multiple
	 * 		 configs, which is the case for the transportBridge and placemarkUdop, among others.
	 */
	private String path;
	
	/** hostname of the node this component is on */
	private String node;
	
	/** String to contain any extra info or notes */
	private String metadata;
	
	/** Component Category, as of yet undefined.  TODO */
	private Category category;
	
	/** True if this component has our standard JMX hooks */
	private boolean jmxEnabled;
	
	/** True if this is a camel component */
	private boolean camelApp;
	
	/** Time of last alert triggered */
	private long lastAlertTime;
	
	/** Number of alerts triggered
	 *  TODO: per session?  total history?
	 *  TODO: Needs to tie in with metrics kept for SOACore 
	 */
	private int alertCount;
	
	/** Simple state representing being triggered or not 
	 *  TODO: will eventually be an Enum with several states 
	 **/
	private boolean onAlert;
	
	/** If true, it means the component is expected to be running, and responding to heartbeats */
	private boolean live;
	
	/** Last time of attempted restart of component */
	private long lastRestartTime;
	
	// TODO: Add a standardized (abstract?) class that holds values and behaviors particular to this node
	//	 	 so that mach knows on what conditions to trigger alerts
	
	
	/**
	 * Single argument constructor.  Mostly added for ease of use with db4o native queries where
	 * you only fill in the fields you want to search on.
	 * 
	 * NOTE:  A NodeComponent initialized with this constructor won't pass registration, since it's
	 * 	      missing the topic and node fields.  Unless, of course, you add them after instantiation, but
	 * 		  before registering with the @ComponentRegistry
	 * 
	 * @param name
	 */
	public NodeComponent(final String name) {
		this.name = name;
	}
	
	
	/**
	 * NodeComponent constructor
	 * 
	 * @param name Name of component
	 * @param topic Topic to send heartbeats to
	 * @param node Server node the component is running on
	 * @param isJmxEnabled True if component supports JMX, False otherwise
	 * @param isCamelApp True if component is a Camel app, False otherwise
	 */
	public NodeComponent(final String name, final String topic, final String node, 
			final boolean isJmxEnabled, final boolean isCamelApp) {
				
		this.name = name;
		this.topic = topic;
		this.node = node;
		this.jmxEnabled = isJmxEnabled;
		this.camelApp = isCamelApp;
		
		alertCount = 0;
		lastAlertTime = 0;
	}
	
	
	/**
	 * Called when an alert was triggered for this component,
	 * setting the timestamp of the last alert, and setting the
	 * component's onAlert property to true. 
	 * 
	 * @param timestamp
	 */
	public void alertTriggered(final long timestamp) {
		alertCount++;
		lastAlertTime = timestamp;
		onAlert = true;
	}
	
	
	/**
	 * Clears the alert counter, last alert time, and sets
	 * the onAlert flag to false
	 */
	public void clearAlertHistory() {
		alertCount = 0;
		lastAlertTime = -1;
		onAlert = false;
	}

	
	/**
	 * Util method for getting the compound id
	 * 
	 * @return
	 */
	public String getCid() {
		return node.concat("-").concat(name);
	}
	
	/**
	 * Overridden toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("NodeComponent:\n");
		sb.append("\tname: " + name);
		sb.append("\n\ttopic: " + topic);
		sb.append("\n\tpath: " + path);
		sb.append("\n\tnode: " + node);
		sb.append("\n\tmetadata: " + metadata);
		sb.append("\n\tcategory: " + category.toString());
		sb.append("\n\tjmxEnabled: " + jmxEnabled);
		sb.append("\n\tcamelApp: " + camelApp);
		sb.append("\n\tlastAlertTime: " + lastAlertTime);
		sb.append("\n\talertCount: " + alertCount);
		sb.append("\n\tonAlert: " + onAlert);
		sb.append("\n\tlive: " + live);
		
		return sb.toString();
	}
	
/* TODO: implement a proper equals() 
	public boolean equals() {
		boolean matches = false;
		
		
		return matches;
	}
*/
	
	
	//===========================================
	// Getters and Setters
	//===========================================
	
		
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}


	public String getTopic() {
		return topic;
	}
	
	public void setTopic(final String topic) {
		this.topic = topic;
	}


	public String getPath() {
		return path;
	}
	
	public void setPath(final String path) {
		this.path = path;
	}

	public String getNode() {
		return node;
	}
	
	public void setNode(final String node) {
		this.node = node;
	}

	public String getMetadata() {
		return metadata;
	}
	
	public void setMetadata(final String metadata) {
		this.metadata = metadata;
	}
	
	
	public Category getCategory() {
		return category;
	}
	
	public void setCategory(final Category category) {
		this.category = category;
	}

	
	public boolean isJmxEnabled() {
		return jmxEnabled;
	}

	public void setJmxEnabled(final boolean isJmxEnabled) {
		this.jmxEnabled = isJmxEnabled;
	}


	public boolean isCamelApp() {
		return camelApp;
	}

	public void setCamelApp(final boolean isCamelApp) {
		this.camelApp = isCamelApp;
	}
	
	public long getLastAlertTime() {
		return lastAlertTime;
	}
	
	public void setLastAlertTime(final long lastAlertTime) {
		this.lastAlertTime = lastAlertTime;
	}
	
	public int getAlertCount() {
		return alertCount;
	}
	
	public void setAlertCount(final int alertCount) {
		this.alertCount = alertCount;
	}
	
	public void resetAlertCount() {
		alertCount = 0;
	}

	public boolean isOnAlert() {
		return onAlert;
	}

	public void setOnAlert(final boolean onAlert) {
		this.onAlert = onAlert;
	}

	public boolean isLive() {
		return live;
	}
	
	public void setLive(final boolean live) {
		this.live = live;
	}

	public long getLastRestartTime() {
		return lastRestartTime;
	}

	public void setLastRestartTime(long lastRestartTime) {
		this.lastRestartTime = lastRestartTime;
	}
}
