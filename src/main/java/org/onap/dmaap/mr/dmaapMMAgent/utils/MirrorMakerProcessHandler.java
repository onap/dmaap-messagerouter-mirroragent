/*******************************************************************************
 *  ============LICENSE_START=======================================================
 *  org.onap.dmaap
 *  ================================================================================
 *  Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ============LICENSE_END=========================================================
 *
 *  ECOMP is a trademark and service mark of AT&T Intellectual Property.
 *  
 *******************************************************************************/
package org.onap.dmaap.mr.dmaapMMAgent.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class MirrorMakerProcessHandler {
	static final Logger logger = Logger.getLogger(MirrorMakerProcessHandler.class);
	static String mmagenthome = System.getProperty("MMAGENTHOME");

	public static boolean checkMirrorMakerProcess(String agentname, boolean enablelogCheck, String grepLog) throws Exception {
		String line,linelog;
		try {
			Runtime rt = Runtime.getRuntime();
			Process mmprocess = null;

			if (System.getProperty("os.name").contains("Windows")) {
				String args = "";
				args = "wmic.exe process where \"commandline like '%agentname=" + agentname
						+ "~%' and caption='java.exe'\"";
				mmprocess = rt.exec(args);
			} else {
				//String args[] = { "/bin/sh", "-c", "ps -ef |grep java |grep agentname=" + agentname + "~" };
				
				String args[] = { "/bin/sh", "-c", "ps -ef | grep `ps -ef |grep agentname=" + agentname + "~ | egrep -v 'grep|java' | awk '{print $2}' `| egrep -v '/bin/sh|grep' "};
				logger.info("CheckMM process->"+args[2]);
				mmprocess = rt.exec(args);
			}

			InputStream is = mmprocess.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			 
			while ((line = br.readLine()) != null) {
				System.out.println(line);
		//		if (line.contains("agentname=" + agentname) && line.contains("/bin/sh -c") == false) {
					
					//If enablelogCheck Check MirrorMaker log for errors and restart mirrormaker
					if(enablelogCheck) {
						logger.info("Check if MM log contains any errors");
						String args2[];
						args2 = new String[] { "/bin/sh", "-c", "grep -i ERROR "+ mmagenthome + "/logs/" + agentname + "_MMaker.log"};
						if(null!=grepLog && !grepLog.isEmpty()) 
						{
							 args2 = new String[]{ "/bin/sh", "-c", grepLog +" " +  mmagenthome + "/logs/" + agentname + "_MMaker.log"};
						}
						logger.info("Grep log args-- "+args2[2]);				
						mmprocess = rt.exec(args2);
						InputStream islog = mmprocess.getInputStream();
						InputStreamReader isrlog = new InputStreamReader(islog);
						BufferedReader brlog = new BufferedReader(isrlog);
							
							while ((linelog = brlog.readLine()) != null) 
							{
								logger.info("Error from MM log--"+linelog);
									
										if (linelog.toLowerCase().contains("ERROR".toLowerCase()) ||
												linelog.toLowerCase().contains("Issue".toLowerCase())	) 
										{
											logger.info("MM log contains error Stop MM and restart");
											stopMirrorMaker(agentname);
											isrlog.close();
											brlog.close();
											return false;
										} 
										
										
							}
							isrlog.close();
							brlog.close();
					}
						
						return true;					
			//	}
			}
		} catch (Exception e) {
			logger.error("Error occured in MirrorMakerProcessHandler.checkMirrorMakerProcess"+e);
		}
		return false;
	}

	public static void stopMirrorMaker(String agentname) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process killprocess = null;

			if (System.getProperty("os.name").contains("Windows")) {
				String args = "wmic.exe process where \"commandline like '%agentname=" + agentname
						+ "~%' and caption='java.exe'\" call terminate";
				killprocess = rt.exec(args);
			} else {
				//String args[] = { "/bin/sh", "-c",
					//	"kill -9 $(ps -ef |grep java |grep agentname=" + agentname + "~| awk '{print $2}')" };
				
				//String args[] = { "/bin/sh", "-c",
				//		"kill -9 `ps -ef |grep agentname=" + agentname + "~| egrep -v 'grep|java' | awk '{print $2}'` | egrep -v '/bin/sh|grep'"};
				String args[] = { "/bin/sh", "-c",
						"for i in `ps -ef |grep agentname="+ agentname + "~ | egrep -v 'grep|java' | awk '{print $2}'`;do kill -9 `ps -eaf | grep $i | egrep -v '/bin/sh|grep' | awk '{print $2}'` ;done"};
				logger.info ("Stop MM ->"+args[2]);				
				// args = "kill $(ps -ef |grep java |grep agentname=" +
				// agentname + "~| awk '{print $2}')";
				killprocess = rt.exec(args);
			}

			InputStream is = killprocess.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
			}

			logger.info("Mirror Maker " + agentname + " Stopped");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void startMirrorMaker(String mmagenthome, String kafkaHome, String agentName, String consumerConfig,
			String producerConfig, int numStreams,  String whitelist) {
		try {
			Runtime rt = Runtime.getRuntime();

			if (System.getProperty("os.name").contains("Windows")) {
				String args = kafkaHome + "/bin/windows/kafka-run-class.bat -Dagentname=" + agentName
						+ "~ kafka.tools.MirrorMaker --consumer.config " + consumerConfig + " --producer.config "
						+ producerConfig +" --num.streams " + numStreams + "  --abort.on.send.failure true" +" --whitelist '" + whitelist + "' > " + mmagenthome + "/logs/" + agentName
						+ "_MMaker.log";
				final Process process = rt.exec(args);
				new Thread() {
					@Override
					public void run() {
						try {
							InputStream is = process.getInputStream();
							InputStreamReader isr = new InputStreamReader(is);
							BufferedReader br = new BufferedReader(isr);
							String line;
							while ((line = br.readLine()) != null) {
								// System.out.println(line);
							}
						} catch (Exception anExc) {
							anExc.printStackTrace();
						}
					}
				}.start();
			} else {
				String args[] = { "/bin/sh", "-c",
						kafkaHome + "/bin/kafka-run-class.sh -Dagentname=" + agentName
								+ "~ kafka.tools.MirrorMaker --consumer.config " + consumerConfig
								+ " --producer.config " + producerConfig + " --num.streams " + numStreams + "  --abort.on.send.failure true" + " --whitelist '" + whitelist + "' >"
								+ mmagenthome + "/logs/" + agentName + "_MMaker.log 2>&1" };
				final Process process = rt.exec(args);
				new Thread() {
					public void run() {
						try {
							InputStream is = process.getInputStream();
							InputStreamReader isr = new InputStreamReader(is);
							BufferedReader br = new BufferedReader(isr);
							String line;
							while ((line = br.readLine()) != null) {
								// System.out.println(line);
							}
						} catch (Exception anExc) {
							anExc.printStackTrace();
						}
					}
				}.start();
			}

			logger.info("Mirror Maker " + agentName + " Started" + " WhiteListing:" + whitelist);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
