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
 * Represents a change in the registry, for sending to
 * any classes observing the registry.
 * 
 * @author jpullen
 *
 */
public class RegistrationEvent {
	
	
	private RegistrationEvents event;
	private String componentId;
	private String topic; // TODO: will probably always be the root (LDDRS.mach) then the componentName...
						  //       so don't really need this?  May be useful, though
		
	/**
	 * RegistrationEvent constructor
	 * 
	 * @param event Type of @Event that occurred
	 */
	public RegistrationEvent(final RegistrationEvents event, final String componentId) {
		this.event = event;
		this.componentId = componentId;
	}	
	
	public RegistrationEvents getEvent() {
		return event;
	}
	
	public void setEvent(final RegistrationEvents event) {
		this.event = event;
	}
	
	public String getComponentId() {
		return componentId;
	}
	
	public void setComponentId(final String componentId) {
		this.componentId = componentId;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public void setTopic(final String topic) {
		this.topic = topic;
	}

}
