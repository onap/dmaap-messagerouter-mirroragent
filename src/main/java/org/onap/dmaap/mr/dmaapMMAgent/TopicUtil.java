/*******************************************************************************
 *  ============LICENSE_START=======================================================
 *  org.onap.dmaap
 *  ================================================================================
 *  Copyright Â© 2017 AT&T Intellectual Property. All rights reserved.
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class TopicUtil {

	static final Logger logger = Logger.getLogger(TopicUtil.class);

	public String publishTopic(String topicURL, String topicname, String mechid, String password, String message) {
		try {
			String requestURL = topicURL + "/events/" + topicname;
			String authString = mechid + ":" + password;
			String authStringEnc = Base64.encode(authString.getBytes());
			URL url = new URL(requestURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", Integer.toString(message.length()));
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.write(message.getBytes());

			InputStream content = (InputStream) connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(content));
			String line;
			String response = "";
			while ((line = in.readLine()) != null) {
				response = response + line;
			}
			return response;

		} catch (Exception e) {
			logger.error(" Exception Occered " + e);
			return "ERROR:" + e.getLocalizedMessage();
		}
	}

	public String subscribeTopic(String topicURL, String topicname, String timeout, String mechid, String password) {
		String response = "";
		try {
			String requestURL = topicURL + "/events/" + topicname + "/mirrormakeragent/1?timeout=" + timeout
					+ "&limit=1";
			String authString = mechid + ":" + password;
			String authStringEnc = Base64.encode(authString.getBytes());
			URL url = new URL(requestURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			connection.setRequestProperty("Content-Type", "application/json");
			InputStream content = (InputStream) connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(content));
			String line;

			while ((line = in.readLine()) != null) {
				response = response + line;
			}
			Gson g = new Gson();
			// get message as JSON String Array
			String[] topicMessage = g.fromJson(response, String[].class);
			if (topicMessage.length != 0) {
				return topicMessage[0];
			}
		} catch (Exception e) {
			logger.error(" Exception Occered " + e);
			return "ERROR:" + e.getMessage() + " Server Response is:" + response;
		}
		return null;
	}

}
