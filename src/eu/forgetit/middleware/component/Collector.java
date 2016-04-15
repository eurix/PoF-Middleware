/**
 * Collector.java
 * Author: Francesco Gallo (gallo@eurix.it)
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.persistence.CmisRepository;
import eu.forgetit.middleware.persistence.DataManager;
import eu.forgetit.middleware.persistence.PersistableEntity;
import eu.forgetit.middleware.persistence.PreservationEntity;
import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.cmis.CmisRepositoryManagerFactory;
import eu.forgetit.middleware.cmis.PIMORepositoryManager;
import eu.forgetit.middleware.cmis.CmisRepositoryManager;
import eu.forgetit.middleware.component.Scheduler.TaskStatus;
import eu.forgetit.middleware.utils.MessageTools;

public class Collector {

	private static Logger logger = LoggerFactory.getLogger(Collector.class);
	
	@BeanInject
	private Forgettor forgettor;
	
	@BeanInject
	private Scheduler scheduler;
	
	private String taskStep = null;
	
	public void parseResources(Exchange exchange){
		
		taskStep = "COLLECTOR_PARSE_RESOURCES";
		
		logger.debug("New message retrieved for "+taskStep);
			
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
			
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
			
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
			
		String cmisId = null;
		String cmisServerId = null;
			
		if(jsonBody!=null) {
				
			cmisId = jsonBody.getString("cmisId");
			cmisServerId = jsonBody.getString("cmisServerId");
				
			job.add("cmisServerId", cmisServerId);
				
			CmisRepositoryManager cmisRepoManager = CmisRepositoryManagerFactory.buildManager(cmisServerId);	
				
			if(cmisRepoManager!=null){
					
				List<PreservationEntity> entities = cmisRepoManager.getObjectInformation(cmisId);
					
				logger.debug("Preservation Entities: "+entities);
					
				JsonArrayBuilder jab = Json.createArrayBuilder();
											
				for(PersistableEntity entity : entities){
				 
					DataManager.getInstance().storeEntity(entity);
						
					logger.debug("Persisted entity "+entity.getPofId()+ " (" + entity.getClass().getSimpleName()+ ")");
						
					jab.add(entity.toJSON());
							
				}
					
				JsonArray jsonArray = jab.build();
					
				logger.debug("Preservation Entities JSON Array: "+jsonArray);
					
				job.add("entities", jsonArray);
				
				exchange.getOut().setHeaders(exchange.getIn().getHeaders());
				exchange.getOut().setBody(job.build().toString());  
															
			} else {
				
				logger.error("Unable to find CMIS Repository "+cmisServerId);
			}
				
			
					
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
				
		}
	}				

		
	public void fetchResources(Exchange exchange){
		
		taskStep = "COLLECTOR_FETCH_RESOURCES";
			
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
			
		JsonObject headers = MessageTools.getHeaders(exchange);
			
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
			
		JsonObject jsonBody = MessageTools.getBody(exchange);
			
		String collectorStorageFolder = ConfigurationManager.getConfiguration().getString("collector.storage.folder");
			
		String cmisServerId = null;
		
		if(jsonBody!=null) {
				
			cmisServerId = jsonBody.getString("cmisServerId");
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
				
			job.add("cmisServerId", cmisServerId);
			job.add("entities", jsonEntities);
				
			CmisRepositoryManager cmisRepoManager = CmisRepositoryManagerFactory.buildManager(cmisServerId);	
				
			if(cmisRepoManager!=null){
					
				for (JsonValue jsonValue : jsonEntities) {
						
					JsonObject jsonObject = (JsonObject)jsonValue;
						
					long pofId = jsonObject.getInt("pofId");
					String cmisId = jsonObject.getString("cmisId");
					
					try {
						    
						Path contentPath = Files.createDirectories(Paths.get(collectorStorageFolder+File.separator+pofId));
						    
						cmisRepoManager.fetchContentAndMetadata(cmisId, contentPath);
						    
						logger.debug("Copied CMIS Object "+cmisId+" to folder "+contentPath.toString());
						      
					} catch (IOException e) {
						    
						logger.error(e.getMessage());
							
					} 
						
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
	
	
	public void parseBulkRequest(Exchange exchange){
		
		taskStep = "COLLECTOR_PARSE_BULK_REQUEST";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
			
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
			
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
			
		String cmisServerId = null;
			
		if(jsonBody!=null) {
			
			//read array of update items
			//[{"pvCategory":"silver","pvLastModifiedTimestamp":1445010909673,"resourceLastModifiedTimestamp":1445009909673,"resourceUri":"pimo:bla"}, ...]
			
			cmisServerId = jsonBody.getString("cmisServerId");
			job.add("cmisServerId", cmisServerId);

			PIMORepositoryManager cmisRepoManager = (PIMORepositoryManager)CmisRepositoryManagerFactory.buildManager(cmisServerId);
			
			if(cmisRepoManager != null){
				
				CmisRepository cmisRepository = CmisRepository.findById(cmisServerId);
				
				long cmisRepoLastUpdate = cmisRepository.getLastUpdate();
				
				JsonArray bulkJsonArray = jsonBody.getJsonArray("entities");
			
				JsonObject jsonObject = null;
			
				int i=0;
				
				String cmisId = null;
			
				JsonArrayBuilder jab = Json.createArrayBuilder();
				
				for (JsonValue jsonValue : bulkJsonArray) {
				
					i++;
				
					jsonObject = (JsonObject)jsonValue;
				
					logger.debug("Bulk Request - resource "+i+": "+jsonObject.toString());
					
					long resourceLastUpdate = jsonObject.getInt("resourceLastModifiedTimestamp");
					
					if(resourceLastUpdate < cmisRepoLastUpdate) continue;
					
					cmisId = jsonObject.getString("resourceUri");
					
					List<PreservationEntity> entities = cmisRepoManager.getObjectInformation(cmisId);
					
					
					logger.debug("Preservation Entities: "+entities.size());
					
					for (PreservationEntity entity : entities) {
						
						DataManager.getInstance().storeEntity(entity);
						
						logger.debug("Persisted entity "+entity.getPofId()+ " (" + entity.getClass().getSimpleName()+ ")");
						
						jab.add(entity.toJSON());
						
					}
					
				}
				
				JsonArray jsonArray = jab.build();
				
				logger.debug("Bulk Preservation Entities JSON Array: "+jsonArray);
					
				job.add("entities", jsonArray);
		
			}
			
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
			exchange.getOut().setBody(job.build().toString());  
			
					
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
			
		}
			
	}		
	
	
	
	public void fetchBulkResources(Exchange exchange){
		
		taskStep = "COLLECTOR_FETCH_RESOURCES";
			
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
			
		JsonObject headers = MessageTools.getHeaders(exchange);
			
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
			
		JsonObject jsonBody = MessageTools.getBody(exchange);
			
		String collectorStorageFolder = ConfigurationManager.getConfiguration().getString("collector.storage.folder");
			
		String cmisServerId = null;
		
		if(jsonBody!=null) {
				
			cmisServerId = jsonBody.getString("cmisServerId");
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
				
			job.add("cmisServerId", cmisServerId);
			job.add("entities", jsonEntities);
				
			PIMORepositoryManager cmisRepoManager = (PIMORepositoryManager)CmisRepositoryManagerFactory.buildManager(cmisServerId);	
				
			if(cmisRepoManager!=null){
					
				for (JsonValue jsonValue : jsonEntities) {
						
					JsonObject jsonObject = (JsonObject)jsonValue;
						
					long pofId = jsonObject.getInt("pofId");
					String cmisId = jsonObject.getString("cmisId");
					
					try {
						    
						Path contentPath = Files.createDirectories(Paths.get(collectorStorageFolder+File.separator+pofId));
						    
						cmisRepoManager.fetchContentAndMetadata(cmisId, contentPath);
						    
						logger.debug("Copied CMIS Object "+cmisId+" to folder "+contentPath.toString());
						      
					} catch (IOException e) {
						    
						logger.error(e.getMessage());
							
					} 
						
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
	
	
	

}



/*
 * 	private String uploadToCMIS(String inputFilePath){
		
	
		Folder root = session.getRootFolder();

		File inputFile = new File(inputFilePath);
		
		Tika tika = new Tika();
		String mimeType = null;
		InputStream fileInputStream = null; 
		try {
			mimeType = tika.detect(inputFile);
			fileInputStream = new FileInputStream(inputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
		properties.put(PropertyIds.NAME, inputFile.getName());
		properties.put(PropertyIds.OBJECT_ID, "pof:"+System.currentTimeMillis());
		
		ContentStream contentStream = new ContentStreamImpl(inputFilePath, BigInteger.valueOf(inputFile.length()), mimeType, fileInputStream );
		
		ItemIterable<CmisObject> children = root.getChildren();

		for (CmisObject o : children) {
		  
			if(o.getName().equals(inputFile.getName())){
				
				logger.debug("Found existing CMIS Document in Middleware CMIS Server: "+o.getName());
				o.delete();
				logger.debug("CMIS document deleted!");
				break;
				
			}
			
		}
				
		Document newDoc = root.createDocument(properties, contentStream, VersioningState.NONE);
		
		String objectId = newDoc.getId();
		
		System.out.println("New document: "+objectId+" "+newDoc.getContentUrl());
		
		return objectId;
				
	}
	
		public void restore(Exchange exchange){
		
		logger.debug("New message retrieved");
		
		JsonObject headers = MessageTools.getHeaders(exchange);

		long taskId = headers.getInt("taskId");
		scheduler.updateTask(taskId, TaskStatus.RUNNING, "RESOURCE RETRIEVAL", null);
		
		MessageTools.setHeaders(exchange, headers);
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
		
		if(jsonBody!=null) {
		
			String jsonCmisId = jsonBody.getString("cmisId");
			if(jsonCmisId!=null) cmisId = jsonCmisId;
			logger.debug("Retrieved CMIS ID: "+cmisId);
			
			String jsonCmisServer = jsonBody.getString("cmisServerId");
			if(jsonCmisServer!=null) cmisServerId = jsonCmisServer;
			logger.debug("Retrieved CMIS Server ID: "+cmisServerId);
			
			String jsonAipTarFilePath = jsonBody.getString("aipTarFilePath");
			if(jsonAipTarFilePath!=null) aipTarFilePath = jsonAipTarFilePath;
			logger.debug("Retrieved AIP TAR FILE PATH: "+aipTarFilePath);
			
			String objectId = uploadToCMIS(aipTarFilePath);
			
			logger.debug("Restored Resource Object ID: "+objectId);
			
			JsonObjectBuilder job = Json.createObjectBuilder();
			
			job.add("source-cmisId",cmisId);
			job.add("source-cmisServerId",cmisServerId);
			
			job.add("cmisId", objectId);
			job.add("cmisServerId", "pofrepo");
			
			String atomPubUrl = (String)ConfigurationManager.getConfiguration().getProperty("cmis.server.atom.binding");
			job.add("atom-pub-url",atomPubUrl);
			
			String mwCmisUsername = (String)ConfigurationManager.getConfiguration().getProperty("cmis.server.username");
			String mwCmisPassword = (String)ConfigurationManager.getConfiguration().getProperty("cmis.server.password");
			
			job.add("cmis-repo-username",mwCmisUsername);
			job.add("cmis-repo-password", mwCmisPassword);
			
			exchange.getIn().setBody(job.build());
			
			scheduler.updateTask(taskId, TaskStatus.COMPLETED, "RESTORED_AIP", jsonBody);

			
		} else {
			
			JsonObjectBuilder job = Json.createObjectBuilder()
					.add("taskStatus", TaskStatus.FAILED.toString());

		    for (Entry<String, JsonValue> entry : headers.entrySet()) {
		        job.add(entry.getKey(), entry.getValue());
		    }
	
			MessageTools.setHeaders(exchange,headers);
		
		}
				
		MessageTools.setHeaders(exchange, headers);
		
	}
	
	
	
*/
