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
 * 
 * 
 * @author jpullen
 *
 */
public class HeartbeatMessage extends MachMessage {				

	private final MachMessageType messageType = MachMessageType.HEARTBEAT;
		
	private String body;
	
	
	/** 
	 * Should be one of two values:
	 * - request
	 * - response
	 * 
	 * TODO: Make an enum?
	 */
	private String type;
		
	/**
	 * response info from the component
	 *  TODO: make a class, with more fields?
	 */
	private String message;
	
	/**
	 * The ID (name and node) of the component that the heartbeat is 
	 * intended for.
	 */
	private String componentID;

		
	/**
	 * Full argument constructor
	 * 
	 * @param name
	 * @param node
	 * @param timestamp
	 * @param body
	 * @param type
	 * @param message
	 * @param version
	 * @param componentID
	 */
	public HeartbeatMessage(String name, String node, String timestamp,
			String body, String type, String message, String version,
			String componentID) {
		
		super(name, node, timestamp, version);
		
		this.body = body;
		this.type = type;
		this.message = message;
		this.componentID = componentID;
	}

	
	@Override
	public Object getMessageType() {		
		return messageType;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getComponentID() {
		return this.componentID;
	}

	public void setComponentID(String id) {
		this.componentID = id;
	}
	
	/*
	@Override
	public String toString() {
		
	}
	*/

}
