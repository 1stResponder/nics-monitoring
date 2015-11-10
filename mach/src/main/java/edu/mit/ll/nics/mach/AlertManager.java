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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.mit.ll.nics.common.alert.EmailAlert;
import edu.mit.ll.nics.common.alert.EmailAlertFactory;
import edu.mit.ll.nics.common.email.EmailFactory;

/**
 * 
 * @author jpullen
 *
 */
public final class AlertManager {
	
	/** Logger instance */
	private static final Logger LOG = Logger.getLogger(AlertManager.class);
	
	/** Singleton instance of this class */
	private static AlertManager instance = new AlertManager();
	
	/** Map of email alert objects, indexed by component??? TODO: */
	private Map<String, EmailAlert> emailAlerts = new HashMap<String, EmailAlert>();
	
	private EmailAlertFactory emailAlertFactory;
	private CamelContext alertContext;
	
	/**
	 * private constructor to block instantiation
	 * 
	 */
	private AlertManager() {	
		PropertyConfigurator.configure(Props.log4jProperties);
		alertContext = new DefaultCamelContext();
		emailAlertFactory = new EmailAlertFactory(new EmailFactory(alertContext));
		try {
			alertContext.start();
		}
		catch(Exception e) {
			LOG.error("Error instantiating AlertManager", e);
		}
		emailAlertFactory.init();
	}
	
	
	/**
	 * 
	 * 
	 * @return Singleton instance of the AlertManager class
	 */
	public static AlertManager getInstance() {		
		return instance;
	}

	
	/**
	 * Registers an alert with a particular component. 
	 *  
	 * @param component Name of the component, for use as a key into the hash
	 * @param subjectClass The string that shows up in the subject of the email preceding the hostname
	 * @param to Recipients to send alert to
	 * @param from Who the email is from
	 * @param timeout The time the alert will wait before sending another alert.  If less than 1, the value
	 * 			      from the property file is used, or the default if that's not set.  Otherwise, whatever
	 * 				  is passed in will be used.
	 */
	public void addEmailAlert(final String component, final String subjectClass, 
			final String to, final String from, final int timeout) {
					
		// TODO: Allow passing in null values for from and to, so that the default from the
		// properties file is used.
		
		final EmailAlert alert = emailAlertFactory.createEmailAlert(Props.rabbitEmailEndpoint, to, from, subjectClass);				
		alert.setTimeout( (timeout > 0) ? timeout : Props.emailAlertTimeout );
				
		addEmailAlert(component, alert);
	}
	
	
	/**
	 * Registers an alert with a particular component.
	 * 
	 * @see addEmailAlert
	 * 
	 * @param component name of component alert is associated with
	 * @param alert Fully initialized EmailAlert object
	 */
	public void addEmailAlert(final String component, final EmailAlert alert) {
		
		synchronized(AlertManager.class) { // TODO: proper usage?
			emailAlerts.put(component, alert);
		}
		
		LOG.debug("EmailAlert added for component: " + component);
	}
	
	
	/**
	 * Remove the email alert associated with the specified component
	 * 
	 * @param component Name of the component 
	 */
	public void removeEmailAlert(final String component) {
		if(emailAlerts == null) {
			LOG.debug("EmailAlert hash has not been initialized, nothing to remove.");
			return;
		}
		
		if(emailAlerts.containsKey(component)) {
			synchronized(AlertManager.class) {
				emailAlerts.remove(component);
			}
			LOG.debug("Removed EmailAlert for component: " + component);
		}else {
			LOG.debug("Specified component did not have an EmailAlert to remove: "
					+ component);
		}
	}
	
	
	/**
	 * Utility method to clear entire emailAlerts hashmap
	 */
	public void clearEmailAlerts() {
		if(emailAlerts == null) return;		
		
		synchronized(AlertManager.class) {
			emailAlerts.clear();
		}
	}
	
	
	/**
	 * Utility method for checking if an alert exists for specified component
	 * 
	 * @param component Name of component
	 * 
	 * @return True if component exists in alert hashmap, False otherwise
	 */
	public boolean containsAlertFor(final String component) {
		return emailAlerts.containsKey(component);
	}
	
	
	/**
	 * Sends an alert email for the specified component
	 * 
	 * @param component Name of component to send email about
	 * @param info Extra info on component state
	 * @return True if alert for component exists and email was sent, False otherwise
	 * 
	 */
	public void sendAlertEmail(final String component, final String info, final boolean force) {
		
		/* TODO: Provide different headers... not all emails will have this body, or
		 * just let each call set the body.
		 */
		
		final StringBuilder msg = new StringBuilder();
		msg.append("Alert for component: '" + component + "'\n\n");		
		msg.append(info + "\n");
		msg.append("\n\n- Mach (Heartbeat Manager)");
	
		
		if(!emailAlerts.containsKey(component)) {
			
			addEmailAlert(component, component, Props.emailTo, Props.emailFrom,
					Props.emailAlertTimeout);
		}
		
		LOG.debug("*** Sending email: ***\n" + msg.toString() + 
				"\n***********************************\n");
				
		emailAlerts.get(component).sendString(msg.toString(), force);
	}

	public void close() {
		this.emailAlertFactory.close();
		try {
			this.alertContext.stop();
		}
		catch (Exception e) {
			LOG.error("Error stopping alert CamelContext", e);
		}
	}


	
	// Getters and Setters
	public EmailAlertFactory getEmailAlertFactory() {
		return this.emailAlertFactory;
	}
	
}
