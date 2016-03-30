/**
 * Copyright (c) 2008-2016, Massachusetts Institute of Technology (MIT)
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
package edu.mit.ll.nics.mach.ping.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.mit.ll.nics.mach.ping.HostPingAlert;

public class HostPingAlertTest {

	@Test
	public void TestShortenURL() {
		HostPingAlert hostPingAlert = new HostPingAlert();
		
		String testUrls[] = new String[]{
			"http://sandbox.nics.ll.mit.edu/blah/test.seam",
			"http://nics.ll.mit.edu/sadisplay/login.seam",
			"http://129.55.46.92",
			"http://129.55.46.82/",
			"129.55.46.201",
			"northeast.ll.mit.edu/sadisplay/",
			"dev.northeast.ll.mit.edu/sadisplay/login.seam"
		};
		
		String expectedUrls[] = new String[]{
				"sandbox.nics.ll.mit.edu",
				"nics.ll.mit.edu",
				"129.55.46.92",
				"129.55.46.82",
				"129.55.46.201",
				"northeast.ll.mit.edu",
				"dev.northeast.ll.mit.edu"
			};
		
		String result = "";
		
		for(int i = 0; i < testUrls.length; i++) {
		
			result = hostPingAlert.shortenUrlForSubject(testUrls[i]);			
			Assert.assertEquals(result, expectedUrls[i]);
		}	
		
		/*
			result = hostPingAlert.shortenUrlForSubject(testUrls[1]);			
			Assert.assertEquals(result, "nics.ll.mit.edu");
			
			result = hostPingAlert.shortenUrlForSubject(testUrls[2]);			
			Assert.assertEquals(result, "129.55.46.92");
			
			result = hostPingAlert.shortenUrlForSubject(testUrls[3]);			
			Assert.assertEquals(result, "129.55.46.82");
			
			result = hostPingAlert.shortenUrlForSubject(testUrls[4]);			
			Assert.assertEquals(result, "129.55.46.201");
			
			result = hostPingAlert.shortenUrlForSubject(testUrls[5]);			
			Assert.assertEquals(result, "northeast.ll.mit.edu");
			
			result = hostPingAlert.shortenUrlForSubject(testUrls[6]);			
			Assert.assertEquals(result, "dev.northeast.ll.mit.edu");
			
		}*/
	}
	
}
