/**
 * Condensator.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * Contributors: Vassilios Solachidis <vsol@iti.gr>, Olga Papadopoulou <olgapapa@iti.gr>
 * 
 * This file is part of ForgetIT Preserve-or-Forget (PoF) Middleware.
 * 
 * Copyright (C) 2013-2016 ForgetIT Consortium - www.forgetit-project.eu
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.forgetit.middleware.component;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.component.Scheduler.TaskStatus;
import eu.forgetit.middleware.remote.CondensatorServiceConsumer;
import eu.forgetit.middleware.utils.MessageTools;

public class Condensator {
	
	private static Logger logger = LoggerFactory.getLogger(Condensator.class);
	
	@BeanInject
	private Scheduler scheduler;
		
	private CondensatorServiceConsumer service = null; 
	
	private String sipMetadataDirPath = null;
	
	private String imageAnalysisResult = null;
	
	
	private Condensator(){
		
		service = new CondensatorServiceConsumer();
			
	}
	
	
	/* 
	 * Clustering of image collections using time, geolocation and visual information
	 */
	public void imageClustering_bkp(Exchange exchange){

		logger.debug("New message retrieved");
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = headers.getInt("taskId");
		scheduler.updateTask(taskId, TaskStatus.RUNNING, "IMAGE CLUSTERING", null);
		
		MessageTools.setHeaders(exchange, headers);
		
		JsonObject jsonBody = MessageTools.getBody(exchange); 
		
		if(jsonBody!=null){

			
			try{
			
				String jsonNofImagesElement = jsonBody.getString("numOfImages");
				String minCLusteringImages = headers.getString("minClusteringImages");
			
				int nofImages = 0;
				int minNofImages = 0;
			
				if(jsonNofImagesElement!=null) nofImages = Integer.parseInt(jsonNofImagesElement);

				if(minCLusteringImages!=null)
					minNofImages = Integer.parseInt(minCLusteringImages);
				else
					minNofImages = 0;
				
				String jsonImageAnalysisResult = jsonBody.getString("imageAnalysis-all");
				if(jsonImageAnalysisResult!=null) imageAnalysisResult = jsonImageAnalysisResult;
				logger.debug("Retrieved Image Analysis Result: "+imageAnalysisResult);
						
				String jsonMetadataDir = jsonBody.getString("sipMetadataDir");
				if(jsonMetadataDir!=null) sipMetadataDirPath = jsonMetadataDir;
				logger.debug("Retrieved SIP Metadata Directory: "+sipMetadataDirPath);
			
				if(nofImages>=minNofImages){
				
					logger.debug("Executing Image Collection Clustering");
				
					String response = service.request(imageAnalysisResult);
					
					logger.debug("Clustering result:\n"+response);

					File resultFile = new File(sipMetadataDirPath,"clustering.xml");
					
					FileUtils.writeStringToFile(resultFile, response);
						
					JsonObjectBuilder job = Json.createObjectBuilder();
					
					job.add("clustering", resultFile.getAbsolutePath());
					
					for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
				        job.add(entry.getKey(), entry.getValue());
				    }
					
					exchange.getIn().setBody(jsonBody.toString());
				
				} else {
					
					logger.debug("Found only "+nofImages+" images, below threshold ("+minCLusteringImages+")... skipping.");
				}
				
			}catch(NumberFormatException | IOException e){
				
				e.printStackTrace();
			}
				
		} else {
			
			JsonObjectBuilder job = Json.createObjectBuilder()
					.add("taskStatus", TaskStatus.FAILED.toString());

		    for (Entry<String, JsonValue> entry : headers.entrySet()) {
		        job.add(entry.getKey(), entry.getValue());
		    }
	
			MessageTools.setHeaders(exchange,headers);
		
		}		
		
	}
	
	
	public void imageClustering(Exchange exchange){
		
		logger.debug("New message retrieved");
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
		}
		
		if(jsonBody!=null){
					
				String xmlPath = jsonBody.getString("extractorOutput");
				logger.debug("Retrieved XML of image collection Path: "+xmlPath);				
				job.add("extractorOutput", xmlPath);		
				
				if(xmlPath!=null&&!xmlPath.isEmpty()){				
																				
					String response = service.request(xmlPath);					
					logger.debug("Image clustering result:\n"+response);					
					job.add("result", response);
					
				} else {					
					logger.debug("Unable to process XML results, wrong request");
					job.add("result", "Unable to process XML results, wrong request");
				}
				
				exchange.getOut().setBody(job.build().toString());
				exchange.getOut().setHeaders(exchange.getIn().getHeaders());
				
			}	
	}
	
	
	public void videoClustering(Exchange exchange){

		System.out.println("New message retrieved");
		
		logger.debug("New message retrieved");
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
		}
		
	
		if(jsonBody!=null){
				
				String videoXMLPath = jsonBody.getString("video_xmls");
				System.out.println("Retrieved Video XMLs Path: "+videoXMLPath);
			
				//String method = jsonBody.getString("method");
				//System.out.println("Retrieved VAM: "+method);
			
				
				// ONE video per call
				
				if(videoXMLPath!=null&&!videoXMLPath.isEmpty()){
				
					System.out.println("Executing Video Clustering Method: ");
																
					String response_temp = service.video_clustering_request(videoXMLPath);
					
					System.out.println("Clustering method result:\n"+response_temp);
					// callid is returned at the response 
					String callid;
					String[] callid_tmp = response_temp.split("::");
					callid = callid_tmp[1].trim();
					String response = service.video_clustering_result(callid);

					logger.debug("Video Clustering result:\n"+response);
					
					job.add("result", response);
					
				} else {
					
					System.out.println("Unable to process video xmls, wrong request");
					job.add("result","Unable to process video, wrong request");
					
				}
				
				exchange.getOut().setBody(job.build().toString());
				exchange.getOut().setHeaders(exchange.getIn().getHeaders());
				
			}
	
	}
	
	
}
