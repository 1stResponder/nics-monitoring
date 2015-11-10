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

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.rabbitmq.client.ShutdownSignalException;

import edu.mit.ll.nics.common.rabbitmq.client.RabbitConsumer;
import edu.mit.ll.nics.common.rabbitmq.client.RabbitProducer;
import edu.mit.ll.nics.mach.Props;
import edu.mit.ll.nics.mach.RegistrationEvent;
import edu.mit.ll.nics.mach.RegistrationEvents;


/**
 * Manager class for handling messaging and processing of messages
 * 
 * @author jpullen
 * 
 * TODO: - Will eventually contain a new Rabbit client class that itself
 * 		   publishes notifications of messages with the Observer pattern
 * 
 * 		- Could use a list of MessageBusClient objects to manage multiple
 * 		  buses
 *
 */
public final class MachMessageManager extends Observable implements Observer {

	private boolean initialized = false;
	
	/** Logger */
	private static final Logger LOG = Logger.getLogger(MachMessageManager.class);
	
	/** Single instance of MachMessagemanger */
	private static final MachMessageManager INSTANCE = new MachMessageManager(); 
	
	/** Rabbit consumer */
	public RabbitConsumer consumer;
	
	/** Rabbit producer */
	public RabbitProducer producer;
	
	/** Timer for executing CheckMessagesTask */
	private Timer timer;
	
	
	/**
	 * Private constructor 
	 */
	private MachMessageManager() {
		super();
		
		PropertyConfigurator.configure(Props.log4jProperties);
		
		initialized = init();
		
		initTimerTask();
	}
	
	
	/**
	 * Accessor for the single instance of this class
	 * 
	 * @return
	 */
	public static MachMessageManager getInstance() {
		return INSTANCE;
	}
	
	/**
	 * 
	 * TODO: can't allow this to be called from the outside, but at the same time, if
	 * initialiazing the rabbit clients fails, mach needs to fail entirely
	 * 
	 * @return
	 */
	private boolean init() {
		
		boolean pro = initProducer();
		boolean con = initConsumer();
		
		return pro || con;
	}
	
	
	/**
	 * Tells whether or not the bus clients were initialized w/o error.
	 * 
	 * TODO: sort of a hack to get feedback on if the MachMessageManager initialized w/o
	 * exposing the call to get initialized.  Any calls to this class that use the clients
	 * will attempt to to re-initialize the clients, but we don't want anyone calling that since
	 * this is a singleton class.
	 * 
	 * @return
	 */
	public boolean isInitialized() {
		return initialized;
	}
	
		
	/**
	 * Initializes the producer
	 * 
	 * TODO: make sure this is only called privately?  Or check for the producer already
	 * 		 being initialized.
	 * 
	 * @return
	 */
	private synchronized boolean initProducer() {
				
		try{

			// Init producer
			producer = new RabbitProducer(Props.rabbitUsername, Props.rabbitPassword, 
					Props.rabbitHost, Props.rabbitPort);

		}catch(ShutdownSignalException sse) {
			LOG.error("ShutdownSignalException attempting to initialize rabbit clients:", sse);
			return false;
			
		}catch(Exception e){				

			LOG.error("Unhandled exception attempting to initialize rabbit clients", e);				
			return false;
		}			

		return true;
	}
	
	
	/**
	 * Initializes the rabbit consumer with settings specified in the properties file
	 * 
	 * @return True if successfully initialized, False otherwise
	 */
	private synchronized boolean initConsumer() {
		try{
			// Init consumer
			consumer = new RabbitConsumer(Props.rabbitUsername, Props.rabbitPassword, 
					Props.rabbitHost, Props.rabbitPort, null, null, Props.initialTopics); //, false);
			
			// TODO: Once the consumer implements observable, we'll observe here
			//consumer.addObserver(this);
			
		}catch(ShutdownSignalException sse) {
			LOG.error("ShutdownSignalException attempting to initialize rabbit clients:", sse);
			return false;
			
		}catch(Exception e){				

			LOG.error("Unhandled exception attempting to initialize rabbit clients", e);				
			return false;
		}			

		return true;
	}
	
	/**
	 * Initializes the CheckMessagesTask timer
	 */
	public void initTimerTask() {
		
		// Init timer as daemon thread
		timer = new Timer(true);		
				
		timer.scheduleAtFixedRate(new CheckMessagesTask(),
				5, // Delay of 5 ms
				10 * 1000);	// Executes on interval of 10 seconds	
		
		// TODO: make this rate configurable via a property, but this is meant to be done away
		//		 with once the bus consumer implements the observerable pattern
	}
	
	
	/**
	 * Exposed method for polling for latest messages from rabbit consumer.
	 * 
	 * TODO:  This should go away when we move to the observer pattern fully, and may
	 * 		  not even need to exist, as the component wanting the messages will be 
	 * 	 	  notified automatically.
	 * 
	 * TODO: make thread safe
	 * 
	 * @return A list of the latest messages the consumer has received
	 */
	public synchronized List<String> getLatestMessages() {
		
		if(consumer == null) {
			initConsumer();
		}
		
		return consumer.getLatestMessages();
	}
	
	
	/**
	 * Exposed method for sending messages on the rabbit bus...
	 * 
	 * TODO:  	This needs to be set up so it can be sent to all bus clients, not just
	 * 		  	rabbit
	 * 
	 * TODO:  	need to check for producer being null, or not initialized
	 * TODO: 	send msg to event log as well, or maybe have an EventLogger observe this
	 * 			class for sent messages
	 */
	public synchronized boolean sendMessage(String topic, String msg) {
		boolean retval = false;
				
		if(producer == null)
			initProducer();
		
		retval = producer.sendMessage(topic, msg);		
		
		doNotify(EVENT.SENT_MESSAGE, msg);
		
		return retval;
	}
	
	
	/**
	 * Public utility method for adding observers
	 * 
	 * @param observer Observer object
	 */
	public void observe(Observer observer) {
		addObserver(observer);
		LOG.info("Added observer: " + observer.getClass().getName());
	}	
	
	/**
	 * update() method to be called by Observable this class is observing.
	 * 
	 * TODO: implement... will eventually be observing the consumer, so we won't
	 * 		have to call getMessages() on it
	 */
	@Override
	public void update(Observable observable, Object object) {
		
		LOG.info("Received notification from " + observable.getClass().getName() 
				+ ": " + object.getClass().getName());
		
		if(object instanceof String) {
			LOG.debug("object: " + object);
		}
		
		
		/* TODO:
		if( observable instanceof BUSCONSUMER) {
			// object should be the String of the message
		}
		*/
		
	}
	
	
	/**
	 * Utility method for notifying observers of an event
	 * 
	 * @param object
	 */
	private void doNotify(EVENT event, Object object) {
		
		switch(event) {
			case SENT_MESSAGE:
				// TODO: not notifying them of messages sent, until there's a need to
				
				break;
				
			case RECEIVED_MESSAGE:
				LOG.info("Notifying observers of " + event);
				setChanged();
				notifyObservers(object);
				clearChanged();
				
				break;
				
			default:
				
		}		
	}
		
	
	public static enum EVENT {
		SENT_MESSAGE,
		RECEIVED_MESSAGE
	};
	
	
	/**
	 * Called by the CheckMessagesTask.
	 * 
	 * This checks for messages, and then sends out a notification for
	 * each message as a means to send messages w/o needing to poll for them.
	 * Will eventually work off update() when a bus consumer sends an update
	 * 
	 */
	private void checkMessages() {
		List<String> messages = getLatestMessages();

		//LOG.debug("\n*** TASK: CHECKING FOR MESSAGES ***\n");
		
		if(messages != null && !messages.isEmpty()) {
			
			for (String msg : messages) {
				doNotify(EVENT.RECEIVED_MESSAGE, msg);
			}
		}
	}	
	
	
	/**
	 * Performs clean up and shutdown of any threads/processes/connections, etc
	 */
	public void shutdown() {
		
		// Stop the checkMessagesTask timer
		if(timer != null) {
			timer.cancel();
		}
		
		// Kill the producer
		// TODO:
		
		// Kill the consumer
		// TODO:
	}
	
	
	/**
	 * TimerTask for checking messages on an interval
	 * 
	 * @author jpullen
	 *
	 */
	class CheckMessagesTask extends TimerTask {
		
		@Override
		public void run() {
			
			checkMessages();
		}
		
	}
}
