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

package com.att.nsa.dmaapMMAgent;

import static org.junit.Assert.*;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.att.nsa.dmaapMMAgent.dao.ListMirrorMaker;
import com.att.nsa.dmaapMMAgent.dao.MirrorMaker;

@RunWith(PowerMockRunner.class)
public class TestMirrorMakerAgent {
	MirrorMakerAgent mirrorMakerAgent = new MirrorMakerAgent();
	ListMirrorMaker listMirrorMaker = new ListMirrorMaker();
	MirrorMaker mirrorMaker = new MirrorMaker();
	MirrorMaker mirrorMaker2 = new MirrorMaker();
	ArrayList<MirrorMaker> listsMirrorMaker = new ArrayList<MirrorMaker>();

	@Test
	public void testcheckStartup() {
		String currentDirectory = System.getProperty("user.dir");
		String MMAGENTHOME = currentDirectory + "/src/test/resources/";
		String parameters[] = {"-encrypt", "test"};
		String args[] = null;
		
		System.setProperty("MMAGENTHOME", MMAGENTHOME);
		
		mirrorMakerAgent.main(args);
		
	}
	
	@Test
	public void testCreateMirrorMaker() {
		mirrorMaker.setConsumer("consumer");
		mirrorMaker.setName("MirrorMaker1");
		mirrorMaker.setProducer("producer");
		mirrorMaker.setStatus("200");
		mirrorMaker.setWhitelist("whitelist");
		
		mirrorMaker2.setConsumer("consumer");
		mirrorMaker2.setName("MirrorMaker2");
		mirrorMaker2.setProducer("producer");
		mirrorMaker2.setStatus("200");
		mirrorMaker2.setWhitelist("whitelist");
		
		listsMirrorMaker.add(mirrorMaker2);
		listMirrorMaker.setListMirrorMaker(listsMirrorMaker);
		
		mirrorMakerAgent.mirrorMakers = listMirrorMaker;
		
		mirrorMakerAgent.createMirrorMaker(mirrorMaker);
		
		assertEquals(2, mirrorMakerAgent.mirrorMakers.getListMirrorMaker().size());
	}

}
