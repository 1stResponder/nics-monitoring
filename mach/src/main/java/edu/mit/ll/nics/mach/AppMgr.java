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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * Class for interacting with the appMgr script on the nodes
 * 
 * @author jpullen
 * 
 * TODO: Implement
 *
 */
public class AppMgr {
	
	/** Logger */
	private static final Logger log = Logger.getLogger(AppMgr.class);
	
	/** Local copy of location of appMgr */
	private static final String appMgr = Props.appMgrPath;
	
	/** Enumeration of commands */
	public static enum CMD {
		START,
		STOP,
		RESTART,
		STATUS
	};
	
	/** Enumeration of system commands */
	public static enum SYSCMD {
		PSAUXGREP,
		PS
	}
		
	/** Enumeration of component states */
	public static enum Response {
		// TODO: make these more self-explanatory
		RUNNING,
		NOT_RUNNING,
		UNKNOWN_APP_ID,
		FAILED_TO_EXECUTE,
		STARTED,
		START_ATTEMPTED,
		STOPPED,
		FAILURE, // TODO; rename to EXCEPTION?
		ABORTED,
		UNABLE_TO_PROCESS,
		KILL_PID_NOT_FOUND,
		KILL_KILLED,
		KILL_ARGUMENT_ERROR,
		KILL_UNKNOWN_RESULT,
		
		UNKNOWN 
	}
		
	
	/**
	 * Private no-arg constructor to stop instantiation
	 */
	private AppMgr() {
		PropertyConfigurator.configure(Props.log4jProperties);
	}
		
	
	/**
	 * Calls: appMgr componentName start background
	 * 
	 * @param component The name of the appMgr registered component to start
	 * @param params 
	 * 
	 * @return
	 */
	public static Response start(String component, String[] params) {		
		// TODO: appMgr already handles running instances, but could decide
		// to do a status first to ensure it's not already running so we don't
		// try to run it again... but no harm done if we tell it to run when it already is, 
		//	since appMgr checks.
		
		/* HOWEVER, starting gives little to no info, just a nohup comment...
		 * so may want to actually return the status Response after executing the
		 * start, so we can say whether or not it's running
		 */
		
		return execCmd(CMD.START, component, params);
	}
	
	
	/**
	 * Calls: appMgr componentName status
	 * 
	 * @param component Name of the component to retrieve status on
	 * 
	 * @return
	 */
	public static Response status(String component) {
		return execCmd(CMD.STATUS, component, null);
	}
	
	
	/**
	 * Calls: appMgr componentName stop
	 * 
	 * @param component Name of the component to stop
	 * @return
	 */
	public static Response stop(String component) {
		return execCmd(CMD.STOP, component, null);
	}
	
	
	/**
	 * Calls: appMgr componentName restart
	 * 
	 * <p>
	 * TODO: currently (20111021), appMgr's restart call doesn't do anything if the component
	 * 		 says it's running via the status command.  It'll only start it up if it's not running.  So
	 * 		for now need to call stop, then start, ourselves
	 * </p>
	 * 
	 * @param component Name of the component to restart
	 * @return
	 */
	public static Response restart(String component, String params[]) {
		
		Response resp = Response.UNKNOWN;
		
		try{		
			Response stop = execCmd(CMD.STOP, component, null);						
			Thread.sleep(5000);
			Response start = execCmd(CMD.START, component, null);
			Thread.sleep(5000);
			Response status = execCmd(CMD.STATUS, component, null);
						
			resp = status;
			
		}catch(InterruptedException e) {
			log.error("AppMgr.restart(): Interrupted exception sleeping between stop/start/status commands");
		}
		
		
		if(resp == Response.UNKNOWN) {
			resp = execCmd(CMD.STATUS, component, null);
		}
		
		return resp;
		
		// TODO: hack until we can confirm appMgr restarts properly
		//return execCmd(CMD.RESTART, component, params);
	}
	
	
	/**
	 * <p>Utility method for executing the appMgr script, and processing
	 * the results.</p>
	 * 
	 * <p>NOTE: Currently the params parameter is ignored </p>
	 * 
	 * @param component Name of the component the command is being executed for
	 * @param params Array of parameters to pass along to the command
	 * 
	 * @return the response (@AppMgr.Response) processed from appMgr's output
	 * 		   	<p> 
	 * 				Response.ABORTED if the specified CMD isn't handled by this method<br/>
	 * 				Response.FAILURE if appMgr's response couldn't be interpreted
	 * 			</p>
	 */
	private static Response execCmd(CMD cmd, String component, String[] params) {
		
		// TODO: if necessary, check if params is set for more advanced commands, but
		//		 for the most part, mach will just use a few basic commands, and will
		//		 always start and restart in background
		
		try{
			Process cmdProc;
			//String exec = appMgr + " ";
			String exec = ""; //"./appMgr ";
			List<String> cmds = new ArrayList<String>();
			
			cmds.add("./appMgr");
			cmds.add(component);
			
			switch(cmd) {
			
				case RESTART:
					exec += component + " restart background";
					cmds.add("restart");
					cmds.add("background");
					break;
				case START:
					exec += component + " start background";
					cmds.add("start");
					cmds.add("background");
					break;
					
				case STOP:
					exec += component + " stop";
					cmds.add("stop");
					break;
					
				case STATUS:
					exec += component + " status";
					cmds.add("status");
					break;
													
				default:
					log.info("Not executing command... unknown cmd: " + cmd);
					return Response.ABORTED;
			}
			
			//String[] cmds = {"/bin/bash", "-c", "/home/dmit/lddrsDeploy/appMgr " + exec};
			
			StringBuilder sbprintcmds = new StringBuilder();
			for(String pcmd : cmds) {
				sbprintcmds.append(pcmd + " ");
			}
			log.debug("!!! Command to be executed: " + sbprintcmds.toString());
			
			
			// TODO:  Possibly break up the exec call to get access to the runtime, so we
			//		  can execute multiple commands
									
			log.info("APPMGR:executing " + cmd.name() + "...");
			
			//cmdProc = Runtime.getRuntime().exec(cmds.toArray(new String[0]), null, new File("/home/dmit/lddrsDeploy"));
			cmdProc = Runtime.getRuntime().exec(cmds.toArray(new String[0]), null, new File(Props.appMgrPath));
			
			
			int exitCode = -1;
			
			try{				
				//exitCode  = cmdProc.exitValue();
				exitCode = cmdProc.waitFor();
				log.info("APPMGR: exit value: '" + exitCode + "'\n");
			} catch(Exception e) {
				System.out.println("Exception trying to get exit value: " + e.getMessage());
			}
			
			
			if(cmd == CMD.START || cmd == CMD.RESTART) {
				// TODO: Assume started... can't check stream if we're reading the app's output
				return Response.START_ATTEMPTED;
			
			} else {

				//log.debug("TESTING: not doing a waitFor() on the process...");
							
				
				// This buffer processing is taking over my main thread forever... we don't want to 
				// keep reading this stream forever
				 
				log.info("APPMGR:processing result...");
							
				String[] body = getBuffer(cmdProc.getInputStream());
				String eBody[] = getBuffer(cmdProc.getErrorStream());
				String sbodyString = "";			
				for(String bodyline : body) {
					sbodyString += bodyline;
				}
				String sebody = "";
				for(String ebodyline : eBody) {
					sebody += ebodyline;
				}
				
				// If it's an unknown app id, don't let it any further
				if(body.length == 1){
					if(body[0].contains("Unknown application")) {			
						return Response.UNKNOWN_APP_ID;
					}				
				}else{
					log.info("APPMGR:Unable to process results from appMgr, returning FAILURE");
					log.info("RESULTS: \n'" + sbodyString + "'");
					log.info("ERRORS: \n'" + sebody + "'");
					return Response.FAILURE;
				}
				
				return processOutput(cmd, body);
			}			

		}catch(IOException ioe) {
			log.error("IOException while executing appMgr script: ", ioe);
			
			return Response.FAILED_TO_EXECUTE;
		}catch(Exception e) {
			log.error("Unhandled exception while executing appMgr script: " + 
					e.getClass().getCanonicalName(), e);
			
			e.printStackTrace();
			
			return Response.FAILURE;
		}
		
		
	}
	
	
	/**
	 * Takes an InputStream and puts the contents into a String
	 * TODO: must be a quick and easy way to do this
	 * 
	 * @param is InputStream from Process results
	 * @return A String[] containing lines of results, if any
	 */
	private static String[] getBuffer(InputStream is) {
		
		BufferedReader reader;
		String contents;
		String line;
		List<String> resultList = new ArrayList<String>();
		
		try{
			reader = new BufferedReader(
					new InputStreamReader(is));
			
			contents = "";
			
			while((line = reader.readLine()) != null) {
				//contents += line;
				resultList.add(line);
			}					
			
		}catch(Exception e) {
			log.error("Unhandled exception while reading buffer from appMgr execution", e);
			contents = null;
		}
		
		//return contents;
		return resultList.toArray(new String[resultList.size()]);
	}
	
	
	/**
	 * Parses the appMgr output, and assigns an AppMgr.Response value according to
	 * the contents
	 * 
	 * @param cmd The command that was executed
	 * @param output The output from stdout
	 * 
	 * @return The processed Response
	 */
	private static Response processOutput(CMD cmd, String[] output) {
		
		Response retval = Response.FAILURE;
		String soutput = "";
		if(output.length == 1) {
			soutput = output[0];
		} else {
			for(String s : output) {
				soutput += s + "\n";
			}
		}
		
		switch(cmd) {
		
			case RESTART:
				// Internally just calls stop and start
				// Currently won't stop and restart, so it'll just return
				// status if the app is currently running.  If it's not running,
				// it'll start it, but this is a bug
				
				break;
			case START:
																
				if(soutput.contains("already running")) {
					// "xxx is already running (pid:####)"
					
					log.info("component is already running");
					
					retval = Response.RUNNING;
				}else if(soutput.contains("nohup:")) {
					// "nohup: redirecting stderr to stdout"
					
					// Assumed to be running, it was at least executed, but internal problems
					// could mean it's not really still running
					
					log.info("component was started");
					retval = Response.RUNNING;
				} else if(soutput.isEmpty()) {
					log.info("Empty output, assuming started OK... bad assumption.  Confirm with STATUS");
					retval = Response.RUNNING;
				}else{
					
					log.info("unknown output: '" + soutput + "', assumed failed");
					retval = Response.FAILED_TO_EXECUTE;
				}			
				
				break;
				
			case STOP:
				if(soutput.contains("was not running")) {
					retval = Response.NOT_RUNNING;
				}else{
					// If it was running, and was told to stop, there is no
					// output from appMgr
					retval = Response.STOPPED;
				}
				
				break;
				
			case STATUS:
				
				if(soutput.contains("NOT")) {
					retval = Response.NOT_RUNNING;
				} else {
					retval = Response.RUNNING;
				}
				
				break;
				
			default:
				log.error("UNSUPPORTED cmd: " + cmd.name());
				retval = Response.UNABLE_TO_PROCESS;
			
		}
		
		log.info("Processed appMgr output: " + retval.name());
		
		return retval;
	}

	
	/**
	 * 
	 * @param component
	 * @return
	 */
	public static Response ps(String component) {
		return execSystemCommand(component, SYSCMD.PS);
	}
	
	
	/**
	 * 
	 * @param component
	 * @return
	 */
	public static Response psauxgrep(String component) {
		return execSystemCommand(component, SYSCMD.PSAUXGREP);
	}
	
	
	/**
	 * 
	 * TODO: Doesn't necessarily return a Response... may want data
	 * TODO: !!! Deprecated.. delete?
	 * 
	 * @param param
	 * @param which
	 * @return
	 */
	private static Response execSystemCommand(String param, SYSCMD which) {
		Response returnResponse = Response.UNKNOWN;
		// TODO: For calls like ps aux | grep "appname", or psinfo <pid>... detail a way for
		// getting known results.. maybe should just have separate functions for ps calls, so
		// we know how to handle the results
		Process cmdProc = null;
		String cmd = "";
		
		switch(which) {
		
			case PSAUXGREP:				
				cmd = "ps aux | grep \"" + param + "\"";
				log.debug("Executing PSAUXGREP: " + cmd);
				break;
			
			case PS:
				cmd = "ps " + param;
				log.debug("Executing PS: " + cmd);
				break;
				
			default:
				
		}
		
		String[] cmds = {"/bin/bash", "-c", cmd};
		
		try{
			log.info("APPMGR:executing " + cmd + "...");
			//cmdProc = Runtime.getRuntime().exec(cmds);
			cmdProc.waitFor(); // TODO: test, and be careful, as this blocks this thread until the proc responds
			log.info("APPMGR:processing result...");
						
			String[] body = getBuffer(cmdProc.getInputStream());
			int resultLines = body.length;		
			
			switch(which) {
				case PSAUXGREP:
					log.debug("PSAUXGREP");
					if(resultLines <= 1) { // just our own grep, or a failure to execute?  0 lines
						log.debug("Our own grep result: " + body[0]);
					} else {
						for (String result : body) {
							// TODO: maybe split on tab character?  Or pass in something other than aux
							if(result.contains(param)) { 
								// TODO: Will want to match on more than app name, especially in
								// cases where the same app is used multiple times with a different config
								// script...
								result = result.replaceAll("\\s{2,4}", ",");
								
								log.debug("After space replace: " + result);
								
								String[] fields = result.split(",");
								if(fields.length > 0) {
									log.debug("USER: " + fields[0]);
									log.debug("PID: " + fields[1]);
									log.debug("Command: " + fields[fields.length-1]);
									returnResponse = Response.RUNNING;
									// user 'should' be dmit... want to verify the .xml or .properties
									// file used to execute this process...
								}								
							} else {
								log.debug("resultline: " + result);
							}
						}
					}
					
					returnResponse = Response.NOT_RUNNING;
					
					break;
					
				case PS:
					log.debug("PS");
					
					break;
					
				default:
					
			}
			
			
			// TODO: Can get deeper into appMgr errors with this, if need be
			String eBody[] = getBuffer(cmdProc.getErrorStream());
			log.debug("\nERRORS:\n");
			for(String e : eBody) {
				log.debug(e);
			}
		
		//} catch(IOException ioe) {
		//	log.error("IOE Exception in execSystemCommand: " + ioe.getMessage());
		} catch(Exception e) {
			log.error("Unhandled Exception in execSystemCommand: " + e.getMessage());
		}
		
		return returnResponse;
	}
	
	
	/**
	 * Does a ps aux | grep on the specified pattern, and returns a PID if found 
	 * 
	 * @param grepPattern The search pattern to send to grep.  The pattern is placed inside quotes, so
	 * 		  there can be spaces
	 * 
	 * @return The PID as a String if found, otherwise, null
	 */
	public static String getPid(String grepPattern) {
		String pid = null;
		
		String[] cmds = {"/bin/bash", "-c", "ps aux | grep \"" + grepPattern + "\""};
		Process cmdProc;
		try{
			
			cmdProc = Runtime.getRuntime().exec(cmds);
			cmdProc.waitFor(); // TODO: test, and be careful, as this blocks this thread until the proc responds
									
			String[] body = getBuffer(cmdProc.getInputStream());
			int resultLines = body.length;
			
			// TODO: If there are too many results, probably should go around killing the
			// process.... there will usually be 3, assuming there's a match.
			// 1. the actual running app your're looking for
			// 2. the /bin/bash ps command
			// 3. the piped grep command
			// All three will match... look into better way of narrowing down.
			
			log.debug("getPid");
			if(resultLines <= 1) { // just our own grep, or a failure to execute?  0 lines
				log.debug("Our own grep result?: " + body[0]);
			} else {
				for (String result : body) {
					// TODO: maybe split on tab character?  Or pass in something other than aux
					if(result.contains(grepPattern) && !result.contains("grep")) { 
						// TODO: Will want to match on more than app name, especially in
						// cases where the same app is used multiple times with a different config
						// script...
						log.debug("result matches: " + result);
						
						result = result.replaceAll("\\s{2,4}", ",");
						
						log.debug("After space replace: " + result);
						
						String[] fields = result.split(",");
						if(fields.length > 0) {
							log.debug("USER: " + fields[0]);
							log.debug("PID: " + fields[1]);
							log.debug("Command: " + fields[fields.length-1]);
							
							log.debug("Setting pid to: " + fields[1]);
							pid = fields[1];
							
							break;
							// user 'should' be dmit... want to verify the .xml or .properties
							// file used to execute this process...
						}								
					} else {
						log.debug("Ignoring result: " + result);
					}
				}
			}			
			
			
			// TODO: Can get deeper into appMgr errors with this, if need be
			String eBody[] = getBuffer(cmdProc.getErrorStream());
			log.debug("\nERRORS:\n");
			for(String e : eBody) {
				log.debug(e);
			}
		
		} catch(IOException ioe) {
			log.error("IOE Exception in execSystemCommand: " + ioe.getMessage());
		} catch(Exception e) {
			log.error("Unhandled Exception in execSystemCommand: " + e.getMessage());
		}
		
		log.debug("Returning pid: " + pid);
		return pid;
	}
	
	
	/**
	 * Kills the specified process id/app.  If the given pidorpattern is a number,
	 * it's treated as a PID.  If the pidorpattern is not a number, it's treated
	 * ass an appname/grep pattern, and getPid() is called to retrieve the matching
	 * pid.  The function then attempts to kill the pid.
	 * 
	 * @param pidorpattern a Process ID, or the appname/grep pattern to find the PID
	 * @param dash9 True if you want to use -9 in the kill command, False if not.
	 * @return A Response result: KILL_KILLED, KILL_PID_NOT_FOUND, KILL_ARGUMENT_ERROR, or
	 * 			KILL_UNKNOWN_RESULT
	 */
	public static Response kill(String pidorpattern, boolean dash9) {
		 
		Response killed = Response.KILL_UNKNOWN_RESULT;		
		boolean isPid = false;
		
		// TODO: Could be a PID that is longer than an int... but I believe
		//		 that'd result in a PID being out of range anyway.  Double check this.
		try {
			int intPid = Integer.parseInt(pidorpattern);
			isPid = true;
		} catch(NumberFormatException nfe) {
			log.debug("pid wasn't a number, so treating as an appname");
		}
		
		if(!isPid) {
			pidorpattern = getPid(pidorpattern);
			if(pidorpattern == null) {
				log.debug("PID/APP not found");
				return Response.KILL_PID_NOT_FOUND;
			}
		}
		
		Process proc;
		
		try {
			String command = "kill " + ((dash9) ? "-9 " : "") + pidorpattern;
			log.debug("Executing: " + command);
			proc = Runtime.getRuntime().exec(command);
			
			byte[] contents = new byte[proc.getInputStream().available()];
			proc.getInputStream().read(contents);
			int exitCode = proc.waitFor();
			//int exitCode = proc.exitValue();
			
			log.debug("kill exit code: " + exitCode);
			
			String result = new String(contents);
			
			log.debug("kill result: " + result);
			
			if(exitCode == 0) { //result.equals("")) {
				log.debug("Exit code 0, kill results, if any: '" + result + "'");
				killed = Response.KILL_KILLED;
			} else if(result.contains("arguments must be process or job IDs")) {
				// Failure, pid parameter wasn't a pid/job id
				log.debug("Argument wasn't a Process or Job ID");
				killed = Response.KILL_ARGUMENT_ERROR;
			} else if(result.contains("No such process")) {
				log.debug("No such process: " + pidorpattern);
				killed = Response.KILL_PID_NOT_FOUND;
			} else {
				log.debug("Unknown response from kill command, unsure of result: " + result);
				killed = Response.KILL_UNKNOWN_RESULT;
			}
			
		} catch(Exception e) {
			log.error("Unhandled exception while attemping to kill process with id(" + pidorpattern +"): " +
					e.getMessage(), e);
		}
		
		return killed;
	}
	
	
	/**
	 * Main entry point for stand-alone testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("APPMGR\n\n");
		String help = "\nInvalid parameters!\n\n" + 
				"\tappMgr [component] [start|stop|status|kill]\n\n";

		if(args.length == 2) {
			if(args[1].equals("start")) {
				System.out.println(AppMgr.start(args[0], null));
			}else if(args[1].equals("stop")) {
				System.out.println(AppMgr.stop(args[0]));
			}else if(args[1].equals("status")) {
				System.out.println(AppMgr.status(args[0]));
			} else if(args[1].equals("psauxgrep")) {
				System.out.println(AppMgr.psauxgrep(args[0]));
			} else if(args[1].equals("ps")) {
				System.out.println(AppMgr.ps(args[0]));
			} else if(args[1].equals("kill")) {
				System.out.println(AppMgr.kill(args[0], true));
			} else {
				System.out.println(help);
			}

		} else {
			System.out.println(help);
		}

	}


	/**
	 * Probes the status of the component, and based on that
	 * initiates corrective measures like restarts/process
	 * kills, etc.
	 * 
	 * @param component
	 */
	public static void restartComponent(String component) {
		final String TAG = "restartComponent: ";
		
		log.debug(TAG + "entered for " + component);
				
		Response status = status(component);
		
		//switch(status) {
		//	case RUNNING:
				StringBuilder sb = new StringBuilder();
				sb.append("Steps taken to restart service, and available results:\n");
				
				//log.debug(TAG + "\tRUNNING");
				// Assumes running, but has problems... so..
				// STOP
				Response stop = stop(component);
				log.debug(TAG + "\t\tstop() result: " + stop.toString());
				sb.append("\nstop\t\t\t" + stop.toString());
				// TODO: Check for running pid no matter if the stop was successful, because it may have
				//		 stopped the starting script, but not the app itself
								
				// Check known pid, and grep app name
				String pidToKill = getPid(component);
				if(pidToKill == null) {
					// Couldn't find the app running... so it's probably officially dead
					sb.append("\ngetPid:\t\t\tNo pid found");
				} else {
					// App is still running, so kill it
					Response killResponse = kill(pidToKill, true /* use -9 */);
					sb.append("\nkill:\t\t\t" + killResponse.toString());
					log.debug("Response to kill attempt: " + killResponse.toString());
				}
				
				// start app
				Response startResponse = start(component, null);
				log.debug("Response to start attempt: " + startResponse.toString());
				sb.append("\nstart:\t\t\t" + startResponse.toString());
		//		break;
				
				AlertManager.getInstance().sendAlertEmail(component, sb.toString(), true);
				
				// NOTE: Relying on caller to set the component back to live!
				
		//	case NOT_RUNNING:
				/* Double check process not still running
				 * 	kill if running
				 * start app
				 */
		//		break;
				
		//	case UNKNOWN_APP_ID:
				
				// TODO: Send alert informing that the given appId for this component
				//  	 is wrong
				
		//		break;
				
		//	case ABORTED: // Wasn't actually executed, unknown command was issued
		//	case FAILED_TO_EXECUTE:
		//	case FAILURE:
		//	case UNKNOWN:
		//	case UNABLE_TO_PROCESS:
				
		//		break;
			
		//	default:
				
		//}
		
	}
	
	
	/**
	 * Utility method for putting String array values as lines
	 * into a single String with line breaks
	 * 
	 * TODO: delete this or use Arrays.toString... nothing is calling this
	 * 
	 * @param strArr
	 * @return
	 */
	private static String strArrToString(String[] strArr) {
		String stringValue = null;
		if(strArr == null) {
			return stringValue;
		}
		
		
		StringBuilder sb = new StringBuilder();
		
		for(String line : strArr) {
			sb.append(line + "\n");
		}
		stringValue = sb.toString();
		
		
		return stringValue;
	}

}
