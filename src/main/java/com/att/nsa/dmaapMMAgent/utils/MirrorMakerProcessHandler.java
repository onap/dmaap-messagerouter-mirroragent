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

package com.att.nsa.dmaapMMAgent.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.att.nsa.dmaapMMAgent.MirrorMakerAgent;


public class MirrorMakerProcessHandler {
	static final Logger logger = Logger.getLogger(MirrorMakerProcessHandler.class);

	public static boolean checkMirrorMakerProcess(String agentname) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process mmprocess = null;

			if (System.getProperty("os.name").contains("Windows")) {
				String args = "";
				args = "wmic.exe process where \"commandline like '%agentname=" + agentname
						+ "~%' and caption='java.exe'\"";
				mmprocess = rt.exec(args);
			} else {
				String args[] = { "/bin/sh", "-c", "ps -ef |grep java |grep agentname=" + agentname + "~" };
				mmprocess = rt.exec(args);
			}

			InputStream is = mmprocess.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				if (line.contains("agentname=" + agentname) && line.contains("/bin/sh -c") == false) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
				String args[] = { "/bin/sh", "-c",
						"kill -9 $(ps -ef |grep java |grep agentname=" + agentname + "~| awk '{print $2}')" };
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
			String producerConfig, String whitelist) {
		try {
			Runtime rt = Runtime.getRuntime();

			if (System.getProperty("os.name").contains("Windows")) {
				String args = kafkaHome + "/bin/windows/kafka-run-class.bat -Dagentname=" + agentName
						+ "~ kafka.tools.MirrorMaker --consumer.config " + consumerConfig + " --producer.config "
						+ producerConfig + " --whitelist '" + whitelist + "' > " + mmagenthome + "/logs/" + agentName
						+ "_MMaker.log";
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
			} else {
				String args[] = { "/bin/sh", "-c",
						kafkaHome + "/bin/kafka-run-class.sh -Dagentname=" + agentName
								+ "~ kafka.tools.MirrorMaker --consumer.config " + consumerConfig
								+ " --producer.config " + producerConfig + " --whitelist '" + whitelist + "' >"
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
