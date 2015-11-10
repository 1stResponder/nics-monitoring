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

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.mit.ll.nics.mach.messages.MachMessageType;

public final class Util {
	
	private static Util instance = new Util();
		
	public static final boolean DEBUG = true;
	public static final String VERSION = "1";
	public static final String TITLE = "Mach";
	
	
	private Util() {
	}
	
	
	public static synchronized Util getInstance() {
		return instance;
	}
	

	/**
	 * Takes in a string containing one or more topics, separated by a comma, 
	 * and returns a List of the individual topics.
	 * 
	 * @param topics A comma delimited String of topics
	 * @return a List of the topics
	 */
	public static List<String> getTopicsFromProperty(String topics) {
		
		if(topics == null || topics.equals(""))
			return null;
		
		List<String> result = new ArrayList<String>();
				
		if(!topics.contains(",")) {
			result.add(topics.trim());
			return result;
		}
		
		String[] arrTopics = topics.split(",");
		
		for(int i = 0; i < arrTopics.length; i++) {
			result.add(arrTopics[i].trim());
		}
		
		return result;
	}
	
	/**
	 * Parses a long from a string 
	 * 
	 * TODO: This functionality might already exist in java's property reader
	 * 
	 * @param val String containing number
	 * @param defaultValue The long value to use if the parse fails
	 * @return The value you passed in as a long
	 */
	public static long parseLongFromString(String val, long defaultValue) {
		long ret;		
		try{
			ret = Long.parseLong(val, 10);
		}catch(NumberFormatException nfe){
			ret = defaultValue;
		}
		return ret;
	}
	
	/**
	 * Parses an int from a string 
	 * 
	 * TODO: This functionality might already exist in java's property reader
	 * 
	 * @param val String containing number
	 * @param defaultValue The int value to use if the parse fails
	 * @return The value you passed in as a int
	 */
	public static int parseIntFromString(String val, int defaultValue) {
		int ret;
		try{
			ret = Integer.parseInt(val, 10);
		}catch(NumberFormatException nfe){
			ret = defaultValue;
		}
		return ret;
	}
	
	
	/**
	 * 
	 * 
	 * TODO: do I even need this? Since enums have a valueOf(string) method
	 * 
	 * @param messageType
	 * @return
	 */
	public static MachMessageType getMachMessageType(String messageType) {
		
		// TODO:
		return null;
	}
	
	
	/**
	 * Returns the Timestamp.toString() of the current time
	 * 
	 * @return
	 */
	public static String timestampNow() {
		return new Timestamp(System.currentTimeMillis()).toString();
	}
	
	/**
	 * Builds a human readable string, saying how long it was between the two times
	 * passed in
	 * 
	 * 
	 * @param previous Previous/Older time
	 * @param latest Latest/Recent time
	 * @return A string with the hours/mins/seconds, as appropriate, between the two times
	 */
	public static String timeBetweenAlertAndAck(long previous, long latest) {
		DecimalFormat df = new DecimalFormat("#.##");
		
		long diff = latest - previous;
		
		
		
		return df.format((diff / (60000))) + " minutes";
	}
}
