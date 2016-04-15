/**
 * Contextualizer.java
 * Author: Mark A. Greenwood (m.a.greenwood@sheffield.ac.uk)
 * Contributors: Francesco Gallo (gallo@eurix.it)
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

import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.component.Scheduler.TaskStatus;
import eu.forgetit.middleware.persistence.Collection;
import eu.forgetit.middleware.remote.ContextualizerServiceConsumer;
import eu.forgetit.middleware.utils.MessageTools;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contextualizer {
	
	private static Logger logger = LoggerFactory.getLogger(Contextualizer.class);
	
	@BeanInject
	private Scheduler scheduler;
	
	private ContextualizerServiceConsumer service;
	
	private String[] context = null;

	private String taskStep = null;
	
	public Contextualizer() {
		
		service = new ContextualizerServiceConsumer();
		
	}

	
	public void executeTextContextualization(Exchange exchange){
		
		taskStep = "CONTEXTUALIZER_CONTEXTUALIZE_DOCUMENTS";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
			
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
		
		String cmisServerId = null;
		
		if(jsonBody!=null) {
				
			cmisServerId = jsonBody.getString("cmisServerId");
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
				
			job.add("cmisServerId", cmisServerId);
			job.add("entities", jsonEntities);
			
			for (JsonValue jsonValue : jsonEntities) {
				
				JsonObject jsonObject = (JsonObject)jsonValue;
					
				String type = jsonObject.getString("type");
				
				if(type.equals(Collection.class.getName())) continue;
				
				long pofId = jsonObject.getInt("pofId");
	
				try{
					
					String collectorStorageFolder = ConfigurationManager.getConfiguration().getString("collector.storage.folder");

					Path sipPath = Paths.get(collectorStorageFolder+File.separator+pofId);
					
					Path metadataPath = Paths.get(sipPath.toString(),"metadata");

					Path contentPath = Paths.get(sipPath.toString(),"content");
					
					Path contextAnalysisPath = Paths.get(metadataPath.toString(),"worldContext.json");
					
					logger.debug("Looking for text documents in folder: "+contentPath);
				
					List<File> documentList = getFilteredDocumentList(contentPath);
					
					logger.debug("Document List for Contextualization: "+documentList);
			
					if(documentList!=null&&!documentList.isEmpty()){
						
						File[] documents = documentList.stream().toArray(File[]::new); 
						
						context = service.contextualize(documents);
						
						logger.debug("World Context:\n");
						
						for (String contextEntry : context) {
							
							logger.debug(contextEntry);
						}
						
						StringBuilder contextResult = new StringBuilder();
						
						for (int i = 0; i < context.length; i++) {
							
							Map<String,String> jsonMap = new HashMap<>();
							jsonMap.put("filename", documents[i].getName());
							jsonMap.put("context", context[i]);

							contextResult.append(jsonMap.toString());
							
						}
						
						FileUtils.writeStringToFile(contextAnalysisPath.toFile(), contextResult.toString());
					
						logger.debug("Document Contextualization completed for "+documentList);
					
					} 
					
				} catch (IOException | ResourceInstantiationException | ExecutionException e) {
					
					e.printStackTrace();
					
				}
				
			}
	
			exchange.getOut().setBody(job.build().toString());
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
			
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
			
		}
	
	}
		
	

	
	private List<File> getFilteredDocumentList(Path contentPath){
			
		List<File> filteredDocuments = new ArrayList<>();
		
		File contentDir = new File(contentPath.toString());
		
		String[] contentFiles = contentDir.list();
		
		if(contentFiles==null||contentFiles.length==0) return filteredDocuments;
		
		List<String> formatList = Arrays.asList(ConfigurationManager.getConfiguration().getStringArray("contextualizer.context.analysis.formats"));
			
		logger.debug("Accepted document formats:"+formatList);
		
		String format = null;
		
		Tika tika = new Tika();
			
		for (String file : contentFiles) {
		
			format = tika.detect(file);
				
			logger.debug("Found file in content dir: "+file+" ("+format+")");
			
			if(formatList.contains(format)) {
				
				filteredDocuments.add(new File(contentDir,file));
				logger.debug("New document added to list: "+contentDir+"/"+file);
				
			} 
			
		}
	
		return filteredDocuments;
		
	}
	
			
	public void testImageContextualization(Exchange exchange) throws ExecutionException{

		System.out.println("New message retrieved");
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  		

		if(jsonBody!=null){
				
				String userimg = jsonBody.getString("userimg");
				System.out.println("Retrieved User image collection: "+userimg);
			
				String event = jsonBody.getString("event");
				System.out.println("Retrieved event name: "+event);
				
				String location = jsonBody.getString("location");
				System.out.println("Retrieved location of the event: "+location);
				
				String year = jsonBody.getString("year");
				System.out.println("Retrieved year of the event: "+year);
			
				
				// ONE video per call
				
				if(userimg!=null&&!userimg.isEmpty()){
					if (event!=null&&!event.isEmpty()&&location!=null&&!location.isEmpty()&&year!=null&&!year.isEmpty()){
				
					System.out.println("Executing image contextualization for user image collection: " + userimg);
																
					String response = service.imgcontextual_request(userimg, event, location, year);
					
					System.out.println("Image contextualization method for event: "+event+" result:\n"+response);
					}else{
						System.out.println("Unable to process textual information, wrong request");
					}					
				} else {
					
					System.out.println("Unable to process image collection, wrong request");
					
				}
				
			}
	
	}
		
	
		

		/*
		 * 
		 * OLD STUFF: CHECK AND REMOVE
		 * 
		 * File[] files = getFileList(contentDir);

    if(files == null || files.length == 0) {

      logger
        .debug("No suitable files for contextualization found in content folder... skipping");

      QueueManager.getInstance().sendMessage(mapMessage, getOutputQueue());

    } else {

      if(contextAnalysisFlag == null || contextAnalysisFlag.isEmpty()) {

        logger.debug("No context analysis request... skipping");

        QueueManager.getInstance().sendMessage(mapMessage, getOutputQueue());

      } else if(contextAnalysisFlag.equals(ConfKey.TEXT_CONTEXTUALIZATION)) {

        logger.debug("Processing request for text contextualization");

        ContextualizerServiceConsumer contextualizer = new ContextualizerServiceConsumer();


        File contextResults =
          new File(metadataDir, ConfKey.CONTEXTUALIZATION_MENTIONS_FILE);

        try {
          String result = contextualizer.contextualize(files);
          PrintWriter out = new PrintWriter(contextResults);
          out.println(result);
          out.flush();
          out.close();
        } catch(ResourceInstantiationException | ExecutionException
            | IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        MapMessage newMessage = getMutableMapMessage(mapMessage);
        newMessage.setString(ConfKey.CONTEXTUALIZATION_MENTIONS,
          contextResults.getAbsolutePath());

        newMessage.setString(ConfKey.QUEUE, getOutputQueue());

        QueueManager.getInstance().sendMessage(newMessage, getOutputQueue());

      }

    }
		 */
		
		
  }




  
