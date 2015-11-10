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


public class MainTest {

	Logger log = Logger.getLogger(MainTest.class);
	
	public MainTest() {
		PropertyConfigurator.configure("log4j.properties");
	}
	
	public void test() {
		log.debug("TESTING");
		
		testParsePing();
	}
	
	
	public void testParsePing() {
		
		String time = "" + System.currentTimeMillis();
		
		String msg = "{\"mach\":{\"type\":\"heartbeat\"," +
		 		"\"name\":\"someComponent\"," +
		 		"\"node\":\"localhost\"," +
		 		"\"time\":\""+ time +"\"," +
		 		"\"body\":{\"type\":\"ping\"}," +
		 		"\"version\":\"1\"}}";
		
		HeartbeatMessage machMessage = null;
		
		try {
		
			machMessage = (HeartbeatMessage) MsgParser.parse(msg);
		
		} catch(Exception e) {
			log.debug("Exception while parsing mach message: " + e.getMessage(), e);
		}
		
		//Assert.assertNotNull(machMessage);
		
		if(machMessage != null) {
			
			assert("someComponent".equals(machMessage.getName()));
			
			/*
			Assert.assertEquals(machMessage.getName(), "someComponent");
			Assert.assertEquals(machMessage.getNode(), "localhost");
			Assert.assertEquals(machMessage.getMessageType(), "heartbeat");
			Assert.assertEquals(machMessage.getTimestamp(), time);
			Assert.assertEquals(machMessage.getVersion(), "1");
			
			Assert.assertEquals(machMessage.getType(), "ping");
			*/
		}
				
	}
	
	public static void main(String[] args) {
		MainTest test = new MainTest();
		
		test.test();
		
		
	}
}
