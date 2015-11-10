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
package edu.mit.ll.nics.mach.messages.test;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.mit.ll.nics.mach.messages.HeartbeatMessage;
import edu.mit.ll.nics.mach.messages.MachMessage;
import edu.mit.ll.nics.mach.messages.MachMessageType;
import edu.mit.ll.nics.mach.messages.MsgParser;


/**
 * Tests the various message processing utilities of this package
 * 
 * @author jpullen
 *
 * TODO: Create a base message test that switches between messageType, and can be re-used...
 *  	 just not sure how that works within TestNG
 *
 */
public class MachMessagesTest {
	
	/** The name of the component to use in messages */
	private static final String NAME = "someComponent";
	
	/** The node to use in messages */
	private static final String NODE = "localhost";
	
	/** The version of the message format... should be the same set in MachMessage */
	private static final String VERSION = MachMessage.version;

	/** Logger */
	private static Logger log = Logger.getLogger(MachMessagesTest.class);
	
	/** Base message string with some placeholder text to be replaced */
	private String msgBASE;
	
	/** Mock register message */
	private String msgRegister;
	
	/** Mock unregister message */
	private String msgUnregister;
	
	/** Mock heartbeat ping message */
	private String msgHBPing;
	
	/** Mock heartbeat request message */
	private String msgHBRequest;
	
	/** mock heartbeat response message */
	private String msgHBResponse;
	
	/** Holds the current time (long) in a string */
	private String time;
	
	
	@BeforeClass
	public void beforeMachMessagesTests() {
		// configure log4j
		BasicConfigurator.configure();
		//PropertyConfigurator.configure("log4j.properties");
	}
	
	
	/**
	 * Sets up strings for testing the parser with
	 */
	@BeforeClass
	public void testParseMessageTypes() {
				
		time = "" + System.currentTimeMillis();
		
		msgBASE = "{\"mach\":{\"type\":\"TYPE\"," +
		 		"\"name\":\""+ NAME +"\"," +
		 		"\"node\":\""+ NODE +"\"," +
		 		"\"time\":\""+ time +"\"," +
		 		"\"body\":{BODY}," +
		 		"\"version\":\""+VERSION+"\"}}";
		
		
		// REGISTER
		msgRegister = msgBASE.replace("TYPE", "register");
		msgRegister = msgRegister.replace("BODY", 
				"" // TODO:
			);
		
		
		// UNREGISTER
		// TODO: Make unregister message to test
		
		// HEARTBEAT ping
		msgHBPing = msgBASE.replace("TYPE", "heartbeat");
		msgHBPing = msgHBPing.replace("BODY", 
				"\"type\":\"ping\""
			);
		
		// HEARTBEAT request
		msgHBRequest = msgHBPing.replace("ping", "request");
		
		// HEARTBEAT response
		msgHBResponse = msgHBPing.replace("request", "response");
	}

	
	@Test(description="Tests the parsing of heartbeat ping messages")
	public void testParsePing() {
				
		HeartbeatMessage machMessage = null;
		
		try {
		
			machMessage = (HeartbeatMessage) MsgParser.parse(msgHBPing);
			log.debug("Successfully created a machMessage from json string");
		
		} catch(Exception e) {
			log.error("Exception while creating mach message: " + e.getMessage(), e);
		}
		
		log.debug("Asserting the object is not null...");
		Assert.assertNotNull(machMessage);
		
		if(machMessage != null) {
			log.debug("Object was not null, so continuing with assertions on fields...");
			
			Assert.assertEquals(machMessage.getName(), "someComponent");
			Assert.assertEquals(machMessage.getNode(), "localhost");
			Assert.assertEquals(machMessage.getMessageType(), MachMessageType.HEARTBEAT);
			Assert.assertEquals(machMessage.getTimestamp(), time);
			Assert.assertEquals(machMessage.getVersion(), VERSION);
			
			Assert.assertEquals(machMessage.getType(), "ping");
			
			log.debug("Done.");
		}
				
	}

}
