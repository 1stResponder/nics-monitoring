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
package edu.mit.ll.nics.mach.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.mit.ll.nics.common.alert.EmailAlert;
import edu.mit.ll.nics.common.alert.EmailAlertFactory;

/**
 * Tool for pinging endpoints(hosts/IPs and webpages) and sending alerts if they don't respond
 * 
 * @author jpullen
 *
 */
public class HostPingAlert implements Processor {

	/** Logger */
	private static Logger log = Logger.getLogger(HostPingAlert.class);
	
	/** 
	 * The id of the route driving this in the xml file
	 * <p>Default: processPingsRoute</p> 
	 */
	private String routeName = "processPingsRoute";
	
	/**
	 * The log4j properties file to use
	 * <p>Default: log4j.properties</p>
	 */
	private String log4jProperties = "config/log4j.properties";
	
	/** A comma delimited list of email addresses to send to */
	private String subscribers;
	
	/** 
	 * The email address shown as the sender of email alerts
	 * 
	 * <p>Default: host-ping-alert@ll.mit.edu</p>
	 */
	private String fromEmail = "host-ping-alert@ll.mit.edu";
	
	/** Comma delimited list of hostnames/IPs to ping */
	private String hosts;
	
	/** Comma delimited list of web page urls to hit */
	private String pages;
	
	/** 
	 * URL/Page pattern to search for in response
	 * 
	 *  <p>Default: "" <empty string>, disabled
	 */
	private String pagePattern = "";
	
	/** Map of endpoints to last successful ping time */
	private Map<String, Long> endpointResponseTimes = new ConcurrentHashMap<String, Long>();
	
	/** Map of endpoints to last time alert was sent for it */
	private Map<String, Long> addressAlertTimes = new ConcurrentHashMap<String, Long>();
		
	/** 
	 * emailConsumer endpoint
	 * <p>Default: local rabbit with LDDRS.alerts.email routingKey</p>
	 */
	private String emailConsumer = "rabbitmq://localhost:5672?amqExchange=amq.topic&amqExchangeType=topic&requestedHeartbeat=0&routingKey=LDDRS.alert.email&noAck=false&user=guest&password=guest&msgPersistent=false&msgContentType=text";
	
	/** 
	 * The amount of time (in ms) for the isReachable call is given before it's decided
	 * the host isn't reachable
	 * <p>Default: 10000</p>
	 */
	private int timeout = 10000;
	
	/** 
	 * The max number of hops allowed for packets to take in ping
	 * <p>Default: 5</p>
	 */
	private int maxhops = 5;
	
	/**
	 * Time (in minutes) between reminder e-mails once initial unreachable occurs
	 * <p>Default: 60</p>
	 */
	private int minsBetweenReminders = 60;
	
	/** Reusable EmailAlertFactory */
	private EmailAlertFactory emailAlertFactory;
	
	/** List of addresses to process... populated from the 'hosts' property */	
	private List<InetAddress> addresses;
	
	/** List of URLS to process, populated by the 'pages' property */
	private List<URL> urls;

	/** Specifies whether or not to log the HTML response in the debug level log 
	 * <p>Default: false</p> 
	 */
	private boolean showResponseInDebug = false;
	
	/** The Pattern object built from the pagePattern property.  Will be null if no pagePatter was set. */
	private Pattern urlPattern;
	
	/** Specifies whether or not the urlPattern object has been initialized */
	private boolean isPatternInitialized;
	
	/** 
	 * Specifies whether or not to use a proxy
	 * <p>Default: false</p>  
	 */
	private boolean proxyEnabled = false;
	
	/** Proxy host */
	private String proxyHost;
	
	/** Proxy port */
	private String proxyPort;
	
	
	/**
	 * Constructor
	 */
	public HostPingAlert() {
	}

	/**
	 * Init method specified in the bean definition in the Spring xml configuration file.
	 * This is called AFTER all properties have been set by the Spring context
	 */
	@SuppressWarnings("unused")
	private void init() {
		PropertyConfigurator.configure(log4jProperties);
		
		if(proxyEnabled) {
			
			if((proxyHost != null && !proxyHost.isEmpty()) &&
			   (proxyPort != null && !proxyPort.isEmpty())) {
			
				System.setProperty("http.proxyHost", proxyHost);
				System.setProperty("http.proxyPort", proxyPort);
			} else {
				log.warn("Proxy was enabled, but invalid host and port values were provided:\nproxyHost: " + 
						proxyHost +	"\nproxyPort: " + proxyPort + "\nNOT using proxy");
			}
		}
	}
	
	/**
	 * Initializes InetAddress objects for any hosts/IPs given in the 'hosts' property, and
	 * populates the local addresses map
	 */
	private void initInetAddresses() {
		
		if(hosts == null || hosts.isEmpty()) {
			log.info("Hosts property was empty, so not initializing any host endpoints to monitor");
			return;
		}
		
		String[] strHosts = hosts.split(",");
		
		if(strHosts == null || strHosts.length == 0) {
			log.info("Invalid hosts property... didn't contain comma delimited list of hosts/ips to ping: " + hosts);
		}
		
		log.info("Initializing hosts list: '" + hosts + "'");
		
		addresses = new ArrayList<InetAddress>();
		
		InetAddress addy = null;
		for(String url : strHosts) {
			try {
				addy = InetAddress.getByName(url);
				addresses.add(addy);
			} catch(UnknownHostException uhe) {
				log.error("Unknown host: " + url + ".  Could not resolve host name to an IP. Not tracking this host.");
			} catch(Exception e) {
				log.error("Caught unhandled exception while resolving host/ip. Not tracking this host: " 
						+ url + ".: " + e.getMessage(),e);
			}
		}
	}
	
	
	/**
	 * Initializes a list of URLs from the 'pages' property
	 */
	private void initUrls() {
		urls = new ArrayList<URL>();
		String[] strUrls = pages.split(",");
		
		if(strUrls == null || strUrls.length == 0) {
			log.info("Invalid pages property... didn't contain comma delimited list of URLs to ping: " + pages);
			log.info("Terminating...");
			System.exit(1);
		}
		
		URL url = null;
		
		for(String strUrl : strUrls) {
			try {
				url = new URL(strUrl);
				urls.add(url);
			} catch (MalformedURLException e) {
				log.error("malformed url, not adding to tracking: " + strUrl, e);
			} catch (Exception e) {
				log.error("Caught unhandled exception ("+ e.getMessage() +") while initializing URL object for url: '" + strUrl + 
						"'.  Not tracking URL: " + strUrl, e);
			}
		}
	}
	
	
	/**
	 * Initializes the urlPattern for use in checking responses.  If
	 * @pagePattern is not set, then urlPattern will be null
	 */
	private void initPattern() {
		if(pagePattern != null && !pagePattern.isEmpty()) {
			urlPattern = Pattern.compile(pagePattern);
		} else {
			urlPattern = null;
		}
		
		isPatternInitialized = true;
	}
	
	
	@Override
	public void process(final Exchange exchange) throws Exception {
		// TODO: possibly move to constructor if the pagePattern variable is set by then... don't want
		// to call this on every process call
		if(!isPatternInitialized) {
			initPattern();
		}
		
		printLastPings();
				
		if(addresses == null) {
			initInetAddresses();
		}
		
		if(urls == null) {
			initUrls();
		}
				
		pollAddresses();
		pollPages();
	}
	
	
	/**
	 * Prints the url/host name, and its last successful response time, if available
	 */
	private void printLastPings() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nLast successful pings:\n");
		
		if(endpointResponseTimes.isEmpty()) {
			log.info("No successful pings yet...");
		} else {
			for(Entry<String, Long> entry : endpointResponseTimes.entrySet()) {
				sb.append(entry.getKey() + " : " + new Timestamp(entry.getValue()).toString() + "\n");
			}
			
			sb.append("\n");
			log.info(sb.toString());
		}
	}
	
	
	/**
	 * Polls all specified addresses, and checks if they're reachable, triggering
	 * an alerts to be queued if needed
	 */
	private void pollAddresses() {
		
		if(addresses == null) {
			log.debug("No addresses to poll... returning");
			return;
		}
		
		for(final InetAddress addy : addresses) {
			
			Runnable pollThread = new Runnable(){
				@Override
				public void run() {
					hostReachable(addy);
				}
			};
			pollThread.run();			
		}
	}
	
	
	/**
	 * Checks to see if the given address is reachable.  If not, an alert is queued.  If it is
	 * reachable, a timestamp is recorded for a successful pinging of the address.
	 * 
	 * @param address
	 */
	private void hostReachable(InetAddress address) {
		
		try{			
			if(!address.isReachable(null /*null is any interface*/, 
					maxhops, timeout /*timeout*/)) {
				
				sendAlert(address.getHostAddress(), EndpointType.HOST, "failed to respond to PING");
			} else {
				log.debug("HOST RESPONDED: " + address.getHostAddress());
				updateTimestamp(address.getHostAddress(), EndpointType.HOST);
			}
			
		} catch (Exception e){
			log.error("Caught unhandled exception pinging host: " + e.getMessage(), e);  
		}
	}
	
	
	/**
	 * Polls all specified URLs, checking to see if the URL is reachable, triggering
	 * alerts, and successfully pinged timestamps to be updated.
	 */
	private void pollPages() {
				
		if(urls == null) {
			log.debug("No URLs to poll... returning");
			return;
		}
		
		for(final URL url : urls) {
			
			Runnable pollThread = new Runnable(){
				@Override
				public void run() {
					urlReachable(url);
				}
			};
			pollThread.run();			
		}
	}
	
	
	/**
	 * Attempts to reach the URL.  If the URL is unreachable, an alert is triggered.
	 * If the URL is reached successfully, a timestamp is added for this endpoint
	 * 
	 * @param url The URL to check status on
	 */
	private void urlReachable(URL url) {
		
		// TODO:  Refactor to limit calls to sendAlert to one place, and set the
		//			parameters up beforehand
		HttpURLConnection conn = null;
		try{
			conn = (HttpURLConnection)url.openConnection();
			conn.setConnectTimeout(timeout);
			conn.setInstanceFollowRedirects(true); // No effect since it defaults to true?
			
			String response = null;
			response = parseStream(conn.getInputStream());
			
			log.debug(url.toString() + ": response code: " + conn.getResponseCode());
			
			if(showResponseInDebug) {
				log.debug("\nCONTENT\n=================\n" + response + "\n===================\n\n");
			}
			
			if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				
				boolean matches = matchesPattern(response);
				
				if(matches) {
					updateTimestamp(url.toString(), EndpointType.URL);
				} else {
					sendAlert(url.toString(), EndpointType.URL, 
							"got a successful response, but page didn't contain the specified pattern(" + pagePattern + ")");
				}
			
			} else if(conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
				// Hit a redirect, and needs to manually re-request from the new url?
				log.debug("Unfollowable redirect (due to protocol change?).");
				sendAlert(url.toString(), EndpointType.URL, 
						"couldn't follow redirect: \n\n" + response + "\n\n");
				
			} else if(conn.getResponseCode() == HttpURLConnection.HTTP_UNAVAILABLE) {
				// TODO: Doesn't actually show up here... an IOException is thrown first, which mentions the 503/unavailable
				log.debug("reported service unavailable");
				sendAlert(url.toString(), EndpointType.URL, "reported service unavailable");
				
			} else {
				if(showResponseInDebug) {
					log.debug("\nCONTENT\n=================\n" + response + "\n===================\n\n");
				}				
				sendAlert(url.toString(), EndpointType.URL, "received response other than 200: " + 
						conn.getResponseCode() + "\n\n" + response + "\n\n");
			}
		} catch (MalformedURLException e) {
			log.error("Malformed URL: " + url);
		} catch (SocketTimeoutException ste) {
			log.error("Socket timed out connecting to: " + url);
			
			try {
				sendAlert(url.toString(), EndpointType.URL, "timed out");
			} catch (Exception e) {
				log.error("Caught unhandled exception trying to send alert due to a socket timeout for URL: " + url, e);
			}
	    } catch (IOException ioe) {
	    	log.error("Caught IO Exception while attempting to reach URL: " + url);
	    		    	
	    	String message = "";
	    	if(ioe.getMessage().contains("503")) {
	    		message = "reported service unavailable\n\n";
	    	} else {
	    		message = "exception reading response, state unknown:\n\n";
	    	}
	    	
	    	try {
				sendAlert(url.toString(), EndpointType.URL, message +
						ioe.getMessage());
			} catch (Exception e) {
				log.error("Caught unhandled exception trying to send alert for URL: " + url, e);
			}
	    } catch(Exception e) {
	    	log.error("Caught unhanlded exception attempting to reach URL: " + url, e);
	    	try {
				sendAlert(url.toString(), EndpointType.URL, 
					"Caught unhandled exception attempting to reach host: " + e.getMessage());
			} catch (Exception ee) {
				log.error("Caught unhandled exception trying to send alert for URL: " + url, ee);
			}
	    }
		
		 // TODO: put final call to sendAlert here in a try catch
		try {
			//sendAlert(,,);
		} catch(Exception e) {
			
		}
		
		if(conn != null){
			log.info("Disconnecting.....");
			conn.disconnect();
		}
	}
	
	
	/**
	 * Utility method for checking a response for a specified pattern.  If the pattern isn't
	 * set, this function returns true, since there's nothing to match, letting the validity
	 * of the response be inferred from the actual response code.
	 * 
	 * @param response The content of the response from the HTTP url request
	 * @return true if pattern is found, or if no pattern was set, false otherwise
	 */
	private boolean matchesPattern(String response) {
		boolean matches = false;
		
		if(urlPattern != null) {
			if(response != null && !response.isEmpty()) {				
				//Pattern pattern = Pattern.compile(pagePattern);
				Matcher matcher = urlPattern.matcher(response);
				if(matcher.find()) {
					log.debug("Matched: " + response.substring(matcher.start(), matcher.end()));
					matches = true;
				} else {
					log.debug("Pattern not found!");
				}
			}
		} else {
			// TODO: Bad form, but limits us to only making one check and call to add timestamp
			matches = true;
		}
		
		return matches;
	}
	
	
	/**
	 * Single access point for updating an addressTimestamp.  If the endpoint was "on alert", then
	 * an email is sent out saying that we've heard from the endpoint again.
	 * 
	 * @param endpoint
	 * @param type
	 */
	private synchronized void updateTimestamp(String endpoint, EndpointType type) {
		
		log.info("Adding/updating timestamp for " + type.toString() + " endpoint: " + endpoint);
		
		long lastAlert = -1;
		long lastResponse = -1; // TODO: Handle case were there has been no response and no alerts yet, using this value
		
		if(addressAlertTimes.containsKey(endpoint)) {
			lastAlert = addressAlertTimes.get(endpoint);
		}
		
		if(endpointResponseTimes.containsKey(endpoint)) {
			lastResponse = endpointResponseTimes.get(endpoint);
		}
		
		if(addressAlertTimes.containsKey(endpoint) ) { 
						
			// If the last alert time for this endpoint was more recent than the last
			// successful ping, then send an email saying we heard from the endpoint again
			if( lastAlert == -1  || (addressAlertTimes.get(endpoint) > endpointResponseTimes.get(endpoint)) ) {

				// send alert
				try {
					
					long elapsed = System.currentTimeMillis() - addressAlertTimes.get(endpoint);
										
					long hours = elapsed / (1000*60*60);
					long mins = (elapsed % (1000*60*60)) / (1000*60);
					long seconds = ((elapsed % (1000*60*60)) % (1000*60)) / 1000;
					
					sendAlert(endpoint, type, 
							"\n\nAn endpoint that was previously unable to be reached has successfully been pinged." +
							"\n\nTime elapsed since alert: " + hours + "h" + mins + "m" + seconds + "s", true /* override */);
				} catch (Exception e) {
					log.error("There was an unhandled exception while trying to send an email after hearing from a " + 
							"previously down endpoint: " + e.getMessage(), e);
				}
			} else {
				log.debug("last alert time("+addressAlertTimes.get(endpoint)+
						") < last timestamp("+endpointResponseTimes.get(endpoint)+")\n\nNOT sending got response after alert email!");
			}
		} else {
			log.debug("Endpoint " + endpoint + " hadn't previously alerted, and hasn't yet received a response timestamp");
		}
		
		endpointResponseTimes.put(endpoint, System.currentTimeMillis());
	}
	
	
	/**
	 * Parses the response from the URL into a String.
	 * 
	 * @param is The input stream from the HttpUrlConnection 
	 * @return The content of the response if processed successfully, null otherwise
	 */
	public String parseStream(InputStream is) {
		String content = null;
		StringBuffer result = new StringBuffer();
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String inputLine;
			while ((inputLine = in.readLine()) != null){
				result.append(inputLine);
			}
			in.close();
			
			content = result.toString();
		}catch(Exception e){
			e.printStackTrace();
		}
				
		return content;		
	}
	
	
	/**
	 * Utility method for calling sendAlert, and defaulting to false on the override argument
	 * 
	 * @see sendAlert
	 */
	public synchronized void sendAlert(String host, EndpointType type, String message) {
		try {
			sendAlert(host, type, message, false);
		} catch(Exception e) {
			log.error("Unhandled exception while calling sendAlert with default override parameter", e);
		}
	}
	
	
	/**
	 * Send email alert
	 * 
	 * @param host
	 * @param type
	 * @param message
	 * @param override
	 * @throws Exception
	 */
	public synchronized void sendAlert(String host, EndpointType type, String message, boolean override) throws Exception {
		
		log.info("Send alert requested for host: " + host);
		
		long lastHostAlert = -1; 
		
		long now = System.currentTimeMillis();
		
		String alertMessage = "";
		
		if(addressAlertTimes.containsKey(host)) {			
			lastHostAlert = addressAlertTimes.get(host);
			log.debug("Last time endpoint '" + host + "' was on alert: " + lastHostAlert);
		} else {
			log.debug("Host: " + host + " going on alert for first time");
			addressAlertTimes.put(host, now);
		}
		
		long timeSinceLastAlert = now - lastHostAlert;
		long reminderThreshold = minsBetweenReminders*60*1000;
				
		if(lastHostAlert == -1) {
			log.debug("First time alert, bypassing reminder threshold");
		} else if( (timeSinceLastAlert < reminderThreshold) && !override ) {			
				log.debug("Not sending a reminder alert until threshold is reached (Time since last alert: " 
						+ timeSinceLastAlert + ", MS until next reminder will be sent: " 
						+ (reminderThreshold - timeSinceLastAlert));
				return;			
		} else {
			// Going to send reminder
			// Not a reminder when override is true, since it'll be an email saying the endpoint is back up
			if(!override) {
				alertMessage = "REMINDER! Endpoint still on alert!\n\n";
			}
		}
		
		try {
			
			long hostTime = 0;
			if(endpointResponseTimes.containsKey(host)) {
				hostTime = endpointResponseTimes.get(host);
			} else {
				// Shouldn't happen?  Timestamps are being looped by host, so host should be there
				log.debug("Expected host: '" + host + "' to be in endpointResponseTimes, but wasn't!");
			}
			
			switch(type) {
				case HOST:					
				
					alertMessage += "ALERT!\n\nHost (" + host + ") failed to respond to a PING at " +
							new Timestamp(System.currentTimeMillis()).toString() + "." + 
							"\n\nTime host was last successfully pinged: " + 
							((hostTime > 0) ? new Timestamp(hostTime).toString() : "NEVER")
							+ ".\n\n\n- HostPingAlert";
					break;
				case URL:
					
					alertMessage += "ALERT!\n\nURL '" + host + "' " + message + 
						"\n\nTime URL was last successfully reached: " + 
						((hostTime > 0) ? new Timestamp(hostTime).toString() : "NEVER")	+ 
						"\n\n\n- HostPingAlert";
					
					break;
				
				default:
				
			}
			
			
			EmailAlert emailAlert = emailAlertFactory.createEmailAlert(emailConsumer, subscribers, 
					fromEmail, shortenUrlForSubject(host));			
			
			log.info("Sending email alert for endpoint: " + host);
			addressAlertTimes.put(host, now);
			emailAlert.sendString(alertMessage, true /*force sending*/);
			
		} catch (Exception e) {
			throw e;
		}
	}
	
	
	/**
	 * Shortens the endpoint URL to be just the base url, no paths or protocol
	 *  
	 * @param url The URL to be shortened to the base URL
	 * @return The base URL
	 */
	public String shortenUrlForSubject(String url) {
						
		String subject = url.replace("http://", "");
		
		if(subject.contains("/")) {
			String[] betweenSlashes = subject.split("/");
			if(betweenSlashes != null && betweenSlashes.length > 0) {
				subject = betweenSlashes[0];
			}
		}
				
		return subject;
	}
	
	
	/** An enumeration for the endpoints supported, and to distinguish one from the other */
	public enum EndpointType {
		HOST,
		URL
	}	
	
	
	// Getters and Setters
	
	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	
	public String getLog4jProperties() {
		return log4jProperties;
	}

	public void setLog4jProperties(String log4jProperties) {
		this.log4jProperties = log4jProperties;
	}	
		
	
	public final String getSubscribers() {
		return subscribers;
	}

	public final void setSubscribers(String subscribers) {
		this.subscribers = subscribers;
	}
	
	
	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	
	public final String getHosts() {
		return hosts;
	}

	public final void setHosts(String hosts) {
		this.hosts = hosts;
	}
	
	
	public String getPages() {
		return pages;
	}

	public void setPages(String pages) {
		this.pages = pages;
	}

	
	public String getPagePattern() {
		return pagePattern;
	}

	public void setPagePattern(String pagePattern) {
		this.pagePattern = pagePattern;
	}

	
	public final String getEmailConsumer() {
		return emailConsumer;
	}

	public final void setEmailConsumer(String emailConsumer) {
		this.emailConsumer = emailConsumer;
	}

	
	public final int getTimeout() {
		return timeout;
	}

	public final void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	
	public final int getMaxhops() {
		return maxhops;
	}

	public final void setMaxhops(int maxhops) {
		this.maxhops = maxhops;
	}

	
	public final int getMinsBetweenReminders() {
		return minsBetweenReminders;
	}

	public final void setMinsBetweenReminders(int minsBetweenReminders) {
		this.minsBetweenReminders = minsBetweenReminders;
	}

	
	public boolean isShowResponseInDebug() {
		return showResponseInDebug;
	}

	public void setShowResponseInDebug(boolean showResponseInDebug) {
		this.showResponseInDebug = showResponseInDebug;
	}


	public final boolean isProxyEnabled() {
		return proxyEnabled;
	}

	public final void setProxyEnabled(boolean proxyEnabled) {
		this.proxyEnabled = proxyEnabled;
	}


	public final String getProxyHost() {
		return proxyHost;
	}

	public final void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}


	public final String getProxyPort() {
		return proxyPort;
	}

	public final void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public final EmailAlertFactory getEmailAlertFactory() {
		return this.emailAlertFactory;
	}

	public final void setEmailAlertFactory(EmailAlertFactory emailAlertFactory) {
		this.emailAlertFactory = emailAlertFactory;
	}

}
