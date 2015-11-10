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

public class AlertMessage extends MachMessage {

	/** The human readable message for placing in an email */
	private String messsageBody;
	
	/** The AlertType of this AlertMessage */
	private AlertType alertType;
	
	/** enum containing alert types */
	public static enum AlertType {
		NO_DATA_THRESHOLD_EXCEEDED,
		NO_MACH_HEARTBEAT_EXCEEDED,
		UNDEFINED
	};
	
	
	/**
	 * Full constructor calls super constructor with name, node, timestamp, and
	 * version.  Sets messageBody and alertType on this class.
	 * 
	 */
	public AlertMessage(String name, String node, String timestamp,
			String version, String messageBody, AlertType alertType) {
		
		super(name, node, timestamp, version);
		
		this.messsageBody = messageBody;
		this.alertType = alertType;
	}

	@Override
	public Object getMessageType() {		
		return MachMessageType.ALERT;
	}

	
	public final String getMesssageBody() {
		return messsageBody;
	}

	public final void setMesssageBody(String messsageBody) {
		this.messsageBody = messsageBody;
	}

	public final AlertType getAlertType() {
		return alertType;
	}

	public final void setAlertType(AlertType alertType) {
		this.alertType = alertType;
	}

}
