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

public abstract class MachMessage {
	
	/** Name of the component */
	protected String name;
	
	/** The node (server) the component is running on */
	protected String node;
	
	/** The time the original message was created/sent */
	protected String timestamp;
	
	/** The version of the MachMessage format */
	public static String version = "0.0.1";
	
	/**
	 * The concrete type of the message, currently: 
	 * <p><ul>
	 * 	<li>HeartbeatMessage</li>	
	 * 	<li>RegisterMessage</li>
	 *  <li>UnregisterMessage</li>
	 * </ul></p> 
	 */
	public abstract Object getMessageType();	
	
	
	/**
	 * MachMessage constructor
	 * 
	 * @param name Name of component
	 * @param node Server node
	 * @param timestamp If null, will use current timestamp
	 * @param version If null, defaults to static version in this class
	 */
	public MachMessage(String name, String node, String timestamp, String version) {
		this.name = name;
		this.node = node;
		
		if(timestamp == null) {
			this.timestamp = ""+ System.currentTimeMillis();
		} else {
			this.timestamp = timestamp;
		}		
		
		if(version != null) {
			this.version = version;
		}
	}
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	// TODO: implement toString and equals?
}
