/*
* Copyright 2009 Accenture. All rights reserved.
*
* Accenture licenses this file to you under the Apache License, 
* Version 2.0 (the "License"); you may not use this file except in 
* compliance with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* @author Huan Liu (huanliu AT cs.stanford.edu)
*/ 
package com.acnlabs.CloudMapReduce.examples;

import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.acnlabs.CloudMapReduce.Global;
import com.acnlabs.CloudMapReduce.S3FileSystem;
import com.acnlabs.CloudMapReduce.SimpleQueue;
import com.acnlabs.CloudMapReduce.application.MapReduceApp;
import com.acnlabs.CloudMapReduce.mapreduce.MapReduce;
import com.acnlabs.CloudMapReduce.mapreduce.Mapper;
import com.acnlabs.CloudMapReduce.mapreduce.OutputCollector;
import com.acnlabs.CloudMapReduce.mapreduce.Reducer;
import com.acnlabs.CloudMapReduce.performance.PerformanceTracker;
import com.acnlabs.CloudMapReduce.util.*;
import com.amazonaws.sdb.AmazonSimpleDB;
import com.amazonaws.sdb.AmazonSimpleDBClient;
import com.amazonaws.sdb.AmazonSimpleDBException;
import com.amazonaws.sdb.model.GetAttributesRequest;
import com.amazonaws.sdb.model.GetAttributesResponse;
import com.amazonaws.sdb.model.PutAttributesRequest;
import com.amazonaws.sdb.model.PutAttributesResponse;
import com.amazonaws.sdb.model.ReplaceableAttribute;
import com.amazonaws.sdb.model.UpdateCondition;

public class SimpleDBTest extends MapReduceApp {
	
	static final String domain = "consistencytest";
	static private boolean consistentRead = false;
	static private int numThreads;

	static public class SDBWriteRunnable implements Runnable {
		private AmazonSimpleDB service;
		private PerformanceTracker perf;
	    private Random generator;
		
		public SDBWriteRunnable(AmazonSimpleDB service, PerformanceTracker perf, Random generator) {
			this.service = service;
			this.perf = perf;
			this.generator = generator;
		}

		public void run() {
	    	try {
	 			ReplaceableAttribute[] attributes = new ReplaceableAttribute[1];
	 			
	 			while (true) { 
	 				int i = generator.nextInt(1000);
	 				attributes[0] = new ReplaceableAttribute("attribute", Integer.toString(i), true);
	 				UpdateCondition condition = new UpdateCondition("nonexist", null, false);
				
	 				try {
//		    			PutAttributesRequest request = new PutAttributesRequest().withItemName("item1" + i).withAttribute(attributes).withExpected(condition);
	 					PutAttributesRequest request = new PutAttributesRequest().withItemName("item" + i).withAttribute(attributes);
	 					long startTime = perf.getStartTime();
	 					PutAttributesResponse response = service.putAttributes(request.withDomainName(domain));
	 			    	perf.stopTimer("UpdateAttribute", startTime);
	 				} catch (AmazonSimpleDBException ex) {
	 					System.out.println("Caught Exception: " + ex.getMessage());
	 					System.out.println("Response Status Code: " + ex.getStatusCode());
	 					System.out.println("Error Code: " + ex.getErrorCode());
	 					System.out.println("Error Type: " + ex.getErrorType());
	 					System.out.println("Request ID: " + ex.getRequestId());
	 					System.out.print("XML: " + ex.getXML());
	 				}
	 				
	 				Thread.sleep(1000);
	 			}
	        }
	        catch (Exception ex) {
	    		System.out.println(ex.getMessage());
	        }
		}
	}
	
	static public class SDBRunnable implements Runnable {
		private AmazonSimpleDB service;
		private PerformanceTracker perf;
	    private Random generator;
	    
		public SDBRunnable(AmazonSimpleDB service, PerformanceTracker perf, Random generator) {
			this.service = service;
			this.perf = perf;
			this.generator = generator;
		}

		public void run() {
	    	try {
				try {
					GetAttributesRequest request = new GetAttributesRequest();
					long startTime = perf.getStartTime();
					GetAttributesResponse response = service.getAttributes(request.withDomainName(domain).withItemName("item"+generator.nextInt(1000)).withAttributeName("attribute").withConsistentRead(consistentRead));
			    	perf.stopTimer("SingleAttribute", startTime);
			    	
	/*	            if (response.isSetGetAttributesResult()) {
		                GetAttributesResult  getAttributesResult = response.getGetAttributesResult();
		                java.util.List<Attribute> attributeList = getAttributesResult.getAttribute();
		                System.out.println("Attributes");
		                for (Attribute attribute : attributeList) {
		                    if (attribute.isSetName()) {
		                        System.out.println("                Name    " + attribute.getName());
		                    }
		                    if (attribute.isSetValue()) {
		                        System.out.println("          Value          " + attribute.getValue());
		                    }
		                }
		            } 
	*/
				} catch (AmazonSimpleDBException ex) {
		            
		            System.out.println("Caught Exception: " + ex.getMessage());
		            System.out.println("Response Status Code: " + ex.getStatusCode());
		            System.out.println("Error Code: " + ex.getErrorCode());
		            System.out.println("Error Type: " + ex.getErrorType());
		            System.out.println("Request ID: " + ex.getRequestId());
		            System.out.print("XML: " + ex.getXML());
		        }
	        }
	        catch (Exception ex) {
	    		System.out.println(ex.getMessage());
	        }
		}
	}

	public static class Map implements Mapper {
		
		public Map() {
		}
		
		@Override
		public void map(String key, String value, OutputCollector output, PerformanceTracker perf)
				throws Exception {
			
			WorkerThreadQueue workers = new WorkerThreadQueue(numThreads, "mapthreads");

			Random generator = new Random();
			
	        /************************************************************************
	         * Access Key ID and Secret Acess Key ID, obtained from:
	         * http://aws.amazon.com
	         ***********************************************************************/
	        String accessKeyId = "119ACF5RYDD83Q130J82";
	        String secretAccessKey = "VhoxkgiqqAaaCHU3GIdRyweL/IOWnIl4IyECKYwU";

	        /************************************************************************
	         * Instantiate Http Client Implementation of Amazon Simple DB 
	         ***********************************************************************/
	        AmazonSimpleDB service = new AmazonSimpleDBClient(accessKeyId, secretAccessKey);
	        
	        /************************************************************************
	         * Uncomment to try advanced configuration options. Available options are:
	         *
	         *  - Signature Version
	         *  - Proxy Host and Proxy Port
	         *  - Service URL
	         *  - User Agent String to be sent to Amazon Simple DB   service
	         *
	         ***********************************************************************/
	        // AmazonSimpleDBConfig config = new AmazonSimpleDBConfig();
	        // config.setSignatureVersion("0");
	        // AmazonSimpleDB service = new AmazonSimpleDBClient(accessKeyId, secretAccessKey, config);
	 
	        /************************************************************************
	         * Uncomment to try out Mock Service that simulates Amazon Simple DB 
	         * responses without calling Amazon Simple DB  service.
	         *
	         * Responses are loaded from local XML files. You can tweak XML files to
	         * experiment with various outputs during development
	         *
	         * XML files available under com/amazonaws/sdb/mock tree
	         *
	         ***********************************************************************/
	        // AmazonSimpleDB service = new AmazonSimpleDBMock();

			long startTime = perf.getStartTime();
			for ( int i=0 ; i<100000 ; i++ ) {
				workers.push(new SDBRunnable(service, perf, generator));
			}

			workers.waitForFinish();
	    	int totalTime = (int) (System.currentTimeMillis() - startTime);
	    	perf.stopTimer("Overall", startTime);

			output.collect("Overall", Integer.toString(totalTime));

			perf.report();
		}
	}
	
	public static class Reduce implements Reducer<String> {
		
		@Override
		public String start(String key, OutputCollector output)
				throws IOException {
			return "";
		}
		
		@Override
		public void next(String key, String value, String state,
				OutputCollector output, PerformanceTracker perf) throws Exception {
			output.collect(key, value);
		}
		
		@Override
		public void complete(String key, String state, OutputCollector output)
				throws IOException {
		}
		
		public long getSize(String state) throws IOException {
			return state.length();
		}
	}
	
	@Override
	protected void run(String jobID, int numReducers,
			int numSetupNodes, SimpleQueue inputQueue, SimpleQueue outputQueue, int numReduceQReadBuffer)
			throws IOException {
		
		MapReduce mapreduce = new MapReduce(jobID, dbManager, queueManager, inputQueue, outputQueue);
		  Map map = new Map();
		  Reduce reduce = new Reduce();

		// invoke Map Reduce with input SQS name and output SQS name
		if ( Global.enableCombiner )
			mapreduce.mapreduce(map, reduce, numReducers, numSetupNodes, reduce);
		else
			mapreduce.mapreduce(map, reduce, numReducers, numSetupNodes, null);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// could parse the arguments here first, that is needed by our Map
		if ( Integer.parseInt(args[args.length-1]) == 1 )
			consistentRead = true;
		numThreads = Integer.parseInt(args[args.length-2]);
		new SimpleDBTest().runMain(args);
		System.exit(0);
	}

}
