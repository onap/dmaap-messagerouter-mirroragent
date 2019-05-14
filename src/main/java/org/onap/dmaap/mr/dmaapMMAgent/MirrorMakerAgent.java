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
package org.onap.dmaap.mr.dmaapMMAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jasypt.util.text.BasicTextEncryptor;
import org.json.JSONObject;
import org.onap.dmaap.mr.dmaapMMAgent.dao.CreateMirrorMaker;
import org.onap.dmaap.mr.dmaapMMAgent.dao.DeleteMirrorMaker;
import org.onap.dmaap.mr.dmaapMMAgent.dao.ListMirrorMaker;
import org.onap.dmaap.mr.dmaapMMAgent.dao.MirrorMaker;
import org.onap.dmaap.mr.dmaapMMAgent.dao.UpdateMirrorMaker;
import org.onap.dmaap.mr.dmaapMMAgent.dao.UpdateWhiteList;
import org.onap.dmaap.mr.dmaapMMAgent.utils.MirrorMakerProcessHandler;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class MirrorMakerAgent {
	static final Logger logger = Logger.getLogger(MirrorMakerAgent.class);
	Properties mirrorMakerProperties = new Properties();
	ListMirrorMaker mirrorMakers = null;
	String mmagenthome = "/opt";
	String kafkahome = "";
	String topicURL = "";
	String topicname = "";
	String mechid = "";
	String password = "";
	String grepLog = "";
	public boolean exitLoop = false;
	TopicUtil topicUtil = new TopicUtil();
	private static String secret = "utdfpWlgyDQ2ZB8SLVRtmN834I1JcT9J";

	public static void main(String[] args) {
		if (args != null && args.length == 2) {
			if (args[0].equals("-encrypt")) {
				BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
				textEncryptor.setPassword(secret);
				String plainText = textEncryptor.encrypt(args[1]);
				System.out.println("Encrypted Password is :" + plainText);
				return;
			}
		} else if (args != null && args.length > 0) {
			System.out.println(
					"Usage: ./mmagent to run with the configuration \n -encrypt <password> to Encrypt Password for config ");
			return;
		}
		MirrorMakerAgent agent = new MirrorMakerAgent();
		if (agent.checkStartup()) {
			logger.info("mmagent started, loading properties");
			try {
				agent.checkAgentProcess();
			} catch (Exception e) {
			    logger.error("exception occured in checkAgentProcess ", e);
			}
			agent.readAgentTopic();
		} else {
			System.out.println(
					"ERROR: mmagent startup unsuccessful, please make sure the mmagenthome /etc/mmagent.config is set and mechid have the rights to the topic");
		}
	}

	private boolean checkStartup() {
		FileInputStream input = null;
		try {
			input = new FileInputStream(mmagenthome + "/etc/mmagent.config");
			logger.info("mmagenthome is set :" + mmagenthome + " loading properties at /etc/mmagent.config");
		} catch (IOException ex) {
			logger.error(mmagenthome + "/etc/mmagent.config not found.", ex);
			return false;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("exception occured in checkStartup "+e);
				}
			}
		}
		loadProperties();
		input = null;
		try {
			input = new FileInputStream(kafkahome + "/bin/kafka-run-class.sh");
			logger.info("kafkahome is set :" + kafkahome);
		} catch (IOException ex) {
			logger.error(kafkahome + "/bin/kafka-run-class.sh not found.  Make sure kafka home is set correctly", ex);
			return false;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.error("exception occured in checkStartup "+e);
				}
			}
		}
		String response = topicUtil.publishTopic(topicURL, topicname, mechid, password, "{\"test\":\"test\"}");
		if (response.startsWith("ERROR:")) {
			logger.error("Problem publishing to topic, please verify the config " + this.topicname + " MR URL is:"
					+ this.topicURL + " Error is:  " + response);
			return false;
		}
		logger.info("Published to Topic :" + this.topicname + " Successfully");
		response = topicUtil.subscribeTopic(topicURL, topicname, "1", mechid, password);
		if (response != null && response.startsWith("ERROR:")) {
			logger.error("Problem subscribing to topic, please verify the config " + this.topicname + " MR URL is:"
					+ this.topicURL + " Error is:  " + response);
			return false;
		}
		logger.info("Subscribed to Topic :" + this.topicname + " Successfully");
		return true;
	}

	private void checkPropertiesFile(MirrorMaker mm, String propName, boolean refresh) {
		InputStream input = null;
		OutputStream out = null;
		try {
			if (refresh) {
				throw new IOException();
			}
			input = new FileInputStream(mmagenthome + "/etc/" + mm.name + propName + ".properties");
		} catch (IOException ex) {
			try {
				input = new FileInputStream(mmagenthome + "/etc/" + propName + ".properties");
				Properties prop = new Properties();
				prop.load(input);
				if (propName.equals("consumer")) {
					prop.setProperty("group.id", mm.name);

					prop.setProperty("bootstrap.servers", mm.consumer);
					prop.setProperty("client.id", mm.name + "MM_consumer");
				} else {
					prop.setProperty("bootstrap.servers", mm.producer);
					prop.setProperty("client.id", mm.name + "MM_producer");

				}
				out = new FileOutputStream(mmagenthome + "/etc/" + mm.name + propName + ".properties");
				prop.store(out, "");

			} catch (Exception e) {
				logger.error("exception occured in checkPropertiesFile "+e);
			}
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				    logger.error("exception occured in checkPropertiesFile ", e);
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logger.error("exception occured in checkPropertiesFile "+e);
				}
			}
		}
	}

	private void checkAgentProcess() throws Exception {
		logger.info("Checking MirrorMaker Process");
		if (mirrorMakers != null) {
			int mirrorMakersCount = mirrorMakers.getListMirrorMaker().size();
			for (int i = 0; i < mirrorMakersCount; i++) {
				MirrorMaker mm = mirrorMakers.getListMirrorMaker().get(i);
				if (MirrorMakerProcessHandler.checkMirrorMakerProcess(mm.name, mm.enablelogCheck,
						this.grepLog) == false) {
					checkPropertiesFile(mm, "consumer", false);
					checkPropertiesFile(mm, "producer", false);

					if (mm.whitelist != null && !mm.whitelist.equals("")) {
						logger.info(
								"MirrorMaker " + mm.name + " is not running, restarting.  Check Logs for more Details");
						MirrorMakerProcessHandler.startMirrorMaker(this.mmagenthome, this.kafkahome, mm.name,
								mmagenthome + "/etc/" + mm.name + "consumer.properties",
								mmagenthome + "/etc/" + mm.name + "producer.properties", mm.numStreams, mm.whitelist);
						mm.setStatus("RESTARTING");

					} else {
						logger.info("MirrorMaker " + mm.name + " is STOPPED");
						mm.setStatus("STOPPED");
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					mirrorMakers.getListMirrorMaker().set(i, mm);
				} else {
					logger.info("MirrorMaker " + mm.name + " is running");
					mm.setStatus("RUNNING");
					mirrorMakers.getListMirrorMaker().set(i, mm);
				}
			}
		}
		// Gson g = new Gson();
		// System.out.println(g.toJson(mirrorMakers));
	}

	public void readAgentTopic() {
		try {
			int connectionattempt = 0;
			while (true) {
				logger.info("--------------------------------");
				logger.info("Waiting for Messages for 60 secs");
				String topicMessage = topicUtil.subscribeTopic(topicURL, topicname, "60000", mechid, password);
				Gson g = new Gson();
				LinkedTreeMap<?, ?> object = null;
				if (topicMessage != null) {
					try {
						// Check and parse if String object returned by consumer
						// API
						// else use the jsonObject
						if (topicMessage.startsWith("\"")) {
							topicMessage = g.fromJson(topicMessage.toString(), String.class);
						}
						object = g.fromJson(topicMessage, LinkedTreeMap.class);

						// Cast the 1st item (since limit=1 and see the type of
						// object
						readAgent(object, topicMessage);
					} catch (Exception ex) {
						connectionattempt++;
						if (connectionattempt > 5) {
							logger.info("Can't connect to the topic, mmagent shutting down , " + topicMessage);
							return;
						}
						logger.info("Can't connect to the topic, " + topicMessage + " Retrying " + connectionattempt
								+ " of 5 times in 1 minute" + " Error:" + ex.getLocalizedMessage());
						Thread.sleep(60000);
					}
				} else {
					// Check all MirrorMaker every min
					connectionattempt = 0;
					checkAgentProcess();
				}
				if (exitLoop) {
					break;
				}

			}
		} catch (Exception e) {
		    logger.error("exception occured in readAgentTopic ", e);
		}

	}

	public void createMirrorMaker(MirrorMaker newMirrorMaker) {
		boolean exists = false;
		if (mirrorMakers != null) {
			int mirrorMakersCount = mirrorMakers.getListMirrorMaker().size();
			for (int i = 0; i < mirrorMakersCount; i++) {
				MirrorMaker mm = mirrorMakers.getListMirrorMaker().get(i);
				if (mm.name.equals(newMirrorMaker.name)) {
					exists = true;
					logger.info("MirrorMaker already exist for:" + newMirrorMaker.name);
					return;
				}
			}
		}
		logger.info("Adding new MirrorMaker:" + newMirrorMaker.name);
		if (exists == false && mirrorMakers != null) {
			mirrorMakers.getListMirrorMaker().add(newMirrorMaker);
		} else if (exists == false && mirrorMakers == null) {
			mirrorMakers = new ListMirrorMaker();
			ArrayList<MirrorMaker> list = mirrorMakers.getListMirrorMaker();
			list = new ArrayList<MirrorMaker>();
			list.add(newMirrorMaker);
			mirrorMakers.setListMirrorMaker(list);
		}
		checkPropertiesFile(newMirrorMaker, "consumer", true);
		checkPropertiesFile(newMirrorMaker, "producer", true);

		Gson g = new Gson();
		mirrorMakerProperties.setProperty("mirrormakers", g.toJson(this.mirrorMakers));
		OutputStream out = null;
		try {
			out = new FileOutputStream(mmagenthome + "/etc/mmagent.config");
			mirrorMakerProperties.store(out, "");
		} catch (IOException ex) {
		    logger.error("exception occured in createMirrorMaker ", ex);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				    logger.error("exception occured in createMirrorMaker ", e);
				}
			}
		}
	}

	private void updateMirrorMaker(MirrorMaker newMirrorMaker) {
		boolean exists = false;
		if (mirrorMakers != null) {
			int mirrorMakersCount = mirrorMakers.getListMirrorMaker().size();
			for (int i = 0; i < mirrorMakersCount; i++) {
				MirrorMaker mm = mirrorMakers.getListMirrorMaker().get(i);
				if (mm.name.equals(newMirrorMaker.name)) {
					exists = true;
					if (null != newMirrorMaker.getConsumer()) {
						mm.setConsumer(newMirrorMaker.getConsumer());
					}
					if (null != newMirrorMaker.getProducer()) {
						mm.setProducer(newMirrorMaker.getProducer());
					}
					if (newMirrorMaker.getNumStreams() >= 1) {
						mm.setNumStreams(newMirrorMaker.getNumStreams());
					}

					mm.setEnablelogCheck(newMirrorMaker.enablelogCheck);

					mirrorMakers.getListMirrorMaker().set(i, mm);
					newMirrorMaker = mm;
					logger.info("Updating MirrorMaker:" + newMirrorMaker.name);
				}
			}
		}
		if (exists) {
			checkPropertiesFile(newMirrorMaker, "consumer", true);
			checkPropertiesFile(newMirrorMaker, "producer", true);

			Gson g = new Gson();
			mirrorMakerProperties.setProperty("mirrormakers", g.toJson(this.mirrorMakers));
			OutputStream out = null;
			try {
				out = new FileOutputStream(mmagenthome + "/etc/mmagent.config");
				mirrorMakerProperties.store(out, "");
				MirrorMakerProcessHandler.stopMirrorMaker(newMirrorMaker.name);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			} catch (IOException ex) {
			    logger.error("exception occured in updateMirrorMaker ", ex);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
					    logger.error("exception occured in updateMirrorMaker ", e);
					}
				}
			}
		} else {
			logger.info("MirrorMaker Not found for:" + newMirrorMaker.name);
		}
	}

	private void updateWhiteList(MirrorMaker newMirrorMaker) {
		boolean exists = false;
		if (mirrorMakers != null) {
			int mirrorMakersCount = mirrorMakers.getListMirrorMaker().size();
			for (int i = 0; i < mirrorMakersCount; i++) {
				MirrorMaker mm = mirrorMakers.getListMirrorMaker().get(i);
				if (mm.name.equals(newMirrorMaker.name)) {
					exists = true;
					mm.setWhitelist(newMirrorMaker.whitelist);
					mirrorMakers.getListMirrorMaker().set(i, mm);
					logger.info("Updating MirrorMaker WhiteList:" + newMirrorMaker.name + " WhiteList:"
							+ newMirrorMaker.whitelist);
				}
			}
		}
		if (exists) {
			Gson g = new Gson();
			mirrorMakerProperties.setProperty("mirrormakers", g.toJson(this.mirrorMakers));
			OutputStream out = null;
			try {
				out = new FileOutputStream(mmagenthome + "/etc/mmagent.config");
				mirrorMakerProperties.store(out, "");
				MirrorMakerProcessHandler.stopMirrorMaker(newMirrorMaker.name);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			} catch (IOException ex) {
			    logger.error("exception occured in updateWhiteList ", ex);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
					    logger.error("exception occured in updateWhiteList ", e);
					}
				}
			}
		} else {
			logger.info("MirrorMaker Not found for:" + newMirrorMaker.name);
		}
	}

	private void deleteMirrorMaker(MirrorMaker newMirrorMaker) {
		boolean exists = false;
		if (mirrorMakers != null) {
			int mirrorMakersCount = mirrorMakers.getListMirrorMaker().size();
			for (int i = 0; i < mirrorMakersCount; i++) {
				MirrorMaker mm = mirrorMakers.getListMirrorMaker().get(i);
				if (mm.name.equals(newMirrorMaker.name)) {
					exists = true;
					mirrorMakers.getListMirrorMaker().remove(i);
					logger.info("Removing MirrorMaker:" + newMirrorMaker.name);
					i = mirrorMakersCount;
				}
			}
		}
		if (exists) {
			try {
				String path = mmagenthome + "/etc/" + newMirrorMaker.name + "consumer" + ".properties";
				File file = new File(path);
				file.delete();
			} catch (Exception ex) {
			    logger.error("exception occured in deleteMirrorMaker ", ex);
			}
			try {
				String path = mmagenthome + "/etc/" + newMirrorMaker.name + "producer" + ".properties";
				File file = new File(path);
				file.delete();
			} catch (Exception ex) {
			    logger.error("exception occured in deleteMirrorMaker ", ex);
			}
			Gson g = new Gson();
			mirrorMakerProperties.setProperty("mirrormakers", g.toJson(this.mirrorMakers));
			OutputStream out = null;
			try {
				out = new FileOutputStream(mmagenthome + "/etc/mmagent.config");
				mirrorMakerProperties.store(out, "");
				MirrorMakerProcessHandler.stopMirrorMaker(newMirrorMaker.name);
			} catch (IOException ex) {
			    logger.error("exception occured in deleteMirrorMaker ", ex);
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
					    logger.error("exception occured in deleteMirrorMaker ", e);
					}
				}
			}
		} else {
			logger.info("MirrorMaker Not found for:" + newMirrorMaker.name);
		}
	}

	private void loadProperties() {
		InputStream input = null;
		try {

			input = new FileInputStream(mmagenthome + "/etc/mmagent.config");
			mirrorMakerProperties.load(input);
			Gson g = new Gson();
			if (mirrorMakerProperties.getProperty("mirrormakers") == null) {
				this.mirrorMakers = new ListMirrorMaker();
				ArrayList<MirrorMaker> list = this.mirrorMakers.getListMirrorMaker();
				list = new ArrayList<>();
				this.mirrorMakers.setListMirrorMaker(list);
			} else {
				this.mirrorMakers = g.fromJson(mirrorMakerProperties.getProperty("mirrormakers"),
						ListMirrorMaker.class);
			}

			this.kafkahome = mirrorMakerProperties.getProperty("kafkahome");
			this.topicURL = mirrorMakerProperties.getProperty("topicURL");
			this.topicname = mirrorMakerProperties.getProperty("topicname");
			this.mechid = mirrorMakerProperties.getProperty("mechid");
			this.grepLog = mirrorMakerProperties.getProperty("grepLog");

			BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
			textEncryptor.setPassword(secret);
			this.password = textEncryptor.decrypt(mirrorMakerProperties.getProperty("password"));
		} catch (Exception ex) {
		    logger.error("exception occured in loadProperties ", ex);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				    logger.error("exception occured in loadProperties ", e);
				}
			}
		}

	}

	public void readAgent(LinkedTreeMap<?, ?> object, String topicMessage) throws Exception{

		Gson g = new Gson();

		if (object.get("createMirrorMaker") != null) {
			logger.info("Received createMirrorMaker request from topic");
			CreateMirrorMaker m = g.fromJson(topicMessage, CreateMirrorMaker.class);
			createMirrorMaker(m.getCreateMirrorMaker());
			checkAgentProcess();
			mirrorMakers.setMessageID(m.getMessageID());
			topicUtil.publishTopic(topicURL, topicname, mechid, password, g.toJson(mirrorMakers));
			mirrorMakers.setMessageID("");
		} else if (object.get("updateMirrorMaker") != null) {
			logger.info("Received updateMirrorMaker request from topic");
			UpdateMirrorMaker m = g.fromJson(topicMessage, UpdateMirrorMaker.class);
			JSONObject json = new JSONObject(topicMessage);
			JSONObject json2 = (JSONObject) json.get("updateMirrorMaker");
			if (!json2.has("numStreams")) {
				m.getUpdateMirrorMaker().setNumStreams(0);
			}
			updateMirrorMaker(m.getUpdateMirrorMaker());
			checkAgentProcess();
			mirrorMakers.setMessageID(m.getMessageID());
			topicUtil.publishTopic(topicURL, topicname, mechid, password, g.toJson(mirrorMakers));
			mirrorMakers.setMessageID("");
		} else if (object.get("deleteMirrorMaker") != null) {
			logger.info("Received deleteMirrorMaker request from topic");
			DeleteMirrorMaker m = g.fromJson(topicMessage, DeleteMirrorMaker.class);
			deleteMirrorMaker(m.getDeleteMirrorMaker());
			checkAgentProcess();
			mirrorMakers.setMessageID(m.getMessageID());
			topicUtil.publishTopic(topicURL, topicname, mechid, password, g.toJson(mirrorMakers));
			mirrorMakers.setMessageID("");
		} else if (object.get("listAllMirrorMaker") != null) {
			logger.info("Received listALLMirrorMaker request from topic");
			checkAgentProcess();
			mirrorMakers.setMessageID((String) object.get("messageID"));
			topicUtil.publishTopic(topicURL, topicname, mechid, password, g.toJson(mirrorMakers));
			mirrorMakers.setMessageID("");
		} else if (object.get("updateWhiteList") != null) {
			logger.info("Received updateWhiteList request from topic");
			UpdateWhiteList m = g.fromJson(topicMessage, UpdateWhiteList.class);
			updateWhiteList(m.getUpdateWhiteList());
			checkAgentProcess();
			mirrorMakers.setMessageID(m.getMessageID());
			topicUtil.publishTopic(topicURL, topicname, mechid, password, g.toJson(mirrorMakers));
			mirrorMakers.setMessageID("");
		} else if (object.get("listMirrorMaker") != null) {
			logger.info("Received listMirrorMaker from topic, skipping messages");
		} else {
			logger.info("Received unknown request from topic");
		}

	}
}
