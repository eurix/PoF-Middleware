/**
 * Archiver.java
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.component.Scheduler.TaskStatus;
import eu.forgetit.middleware.persistence.CmisRepository;
import eu.forgetit.middleware.persistence.Collection;
import eu.forgetit.middleware.persistence.DataManager;
import eu.forgetit.middleware.persistence.Item;
import eu.forgetit.middleware.persistence.PreservationEntity;
import eu.forgetit.middleware.remote.DigitalRepositoryServiceConsumer;
import eu.forgetit.middleware.utils.MessageTools;

public class Archiver {
	
	private static Logger logger = LoggerFactory.getLogger(Archiver.class);
	
	@BeanInject
	private Scheduler scheduler;
	
	private String taskStep = null;
	private DigitalRepositoryServiceConsumer digitalRepoService = null;
	
	public Archiver(){
				
		digitalRepoService = new DigitalRepositoryServiceConsumer();
		
	}
	
	
	/*
	 * INGEST
	 */
	
	public void ingestSIP(Exchange exchange){
		
		taskStep = "ARCHIVER_INGEST_SIP";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
		
		JsonObject jsonBody = MessageTools.getBody(exchange); 
		
		if(jsonBody!=null){
			
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
			
			JsonArray ja = ingestEntities(jsonEntities);
				
			job.add("entities", ja);
				
			exchange.getOut().setBody(job.build().toString());
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
									
								
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
		
		}		
				
	}
	
	
	public void bulkIngestSIP(Exchange exchange){
		
		taskStep = "ARCHIVER_INGEST_SIP";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
		
		JsonObject jsonBody = MessageTools.getBody(exchange); 
		
		if(jsonBody!=null){
			
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
			
			JsonArray ja = ingestEntities(jsonEntities);
				
			job.add("entities", ja);
				
			exchange.getOut().setBody(job.build().toString());
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
									
								
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
		
		}		
				
	}
	
	public void updateRepository(Exchange exchange){
		
		taskStep = "ARCHIVER_UPDATE_REPOSITORY_TIMESTAMP";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
				
		if(jsonBody!=null) {
			
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
		
			job.add("entities", jsonEntities);
			
			String cmisServerId = null;
			
			for (JsonValue jsonValue : jsonEntities) {
				
				JsonObject jsonObject = (JsonObject)jsonValue;
				
				cmisServerId = jsonObject.getString("cmisServerId");
				
				if(cmisServerId != null) continue;
				
			}

			if(cmisServerId != null) {
			
				logger.debug("Updating CMIS Repository "+cmisServerId);
			
				job.add("cmisServerId", cmisServerId);
			
				CmisRepository cmisRepository = CmisRepository.findById(cmisServerId);
			
				if(cmisRepository != null){
				
					cmisRepository.setLastUpdate(System.currentTimeMillis());
				
					DataManager.getInstance().storeEntity(cmisRepository);
				
					job.add("lastUpdate", cmisRepository.getLastUpdate());
							
					logger.debug("CMIS Repository "+cmisServerId+" last update: "+cmisRepository.getLastUpdate());
																		
				}

			} else {
				
				logger.debug("No CMIS Repository updated");
				
			}
				
			JsonObject messageJsonObject = job.build();
			
			scheduler.updateTask(taskId, TaskStatus.COMPLETED, taskStep, messageJsonObject);
			
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
			exchange.getOut().setBody(messageJsonObject.toString());  
				
		} else {
		
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed: unable to parse message body");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());

		    
		}
	}				
		
	
	private JsonArray ingestEntities(JsonArray jsonArray){
		
		JsonArrayBuilder jab = Json.createArrayBuilder();
		JsonArrayBuilder collectionsJab = Json.createArrayBuilder();
		
		String collectorStorageFolder = ConfigurationManager.getConfiguration().getString("collector.storage.folder");
		
		Path sipPath = null;
		
		// Ingest Collections
		
		logger.debug("Start ingesting Collections");
		
		for (JsonValue jsonValue : jsonArray) {
						
			JsonObject jsonObject = (JsonObject)jsonValue;
			
			if(jsonObject.getString("type").equals(Collection.class.getSimpleName())) {
				
				logger.debug("Looping on JsonArray: "+jsonObject.getString("cmisId"));
				
				long pofId = jsonObject.getInt("pofId");
				String cmisId = jsonObject.getString("cmisId");
				
				sipPath = Paths.get(collectorStorageFolder,String.valueOf(pofId));

				logger.debug("Executing SIP Ingest for POF ID: "+pofId+" CMIS ID: "+cmisId+" Path: "+sipPath);
				
				String collectionId = digitalRepoService.ingestCollection(pofId);
				
				logger.debug("Ingested Collection "+pofId+" into Digital Repository: "+collectionId);
				
				Collection collection = DataManager.getInstance().getEntity(pofId, Collection.class);
				collection.setRepositoryId(collectionId);
				collection.setLastUpdate(System.currentTimeMillis());
				collection.setPreserved(true);
				DataManager.getInstance().updateEntity(collection);
				
				jab.add(collection.toJSON());
				collectionsJab.add(collection.toJSON());
				
			}
			
		}
		
		
		// Ingest Items
	
		logger.debug("Start ingesting Items");
		
		JsonArray collectionsJsonArray = collectionsJab.build();
		 
		String itemId = null;
		
		for (JsonValue jsonValue : jsonArray) {
			
			JsonObject jsonObject = (JsonObject)jsonValue;
			
			logger.debug("Looping on JsonArray: "+jsonObject.getString("cmisId"));
			
			if(jsonObject.getString("type").equals(Item.class.getSimpleName())) {
				
				long pofId = jsonObject.getInt("pofId");
				String cmisId = jsonObject.getString("cmisId");
				
				sipPath = Paths.get(collectorStorageFolder,String.valueOf(pofId));

				logger.debug("Executing SIP Ingest for POF ID: "+pofId+" CMIS ID: "+cmisId+" Path: "+sipPath);
				
				String collectionId = getCollectionId(pofId, collectionsJsonArray);

				if(collectionId == null){
				
					logger.debug("No associated Collection found in SIP for Item "+cmisId);
	
					// Create new Single Item Collection
					
					Collection coll = new Collection();
					String cmisServerId = jsonObject.getString("cmisServerId");
					coll.setActiveSystem(cmisServerId);
					coll.setCmisId("");
					coll.setCreationDate(System.currentTimeMillis());
					coll.setDescription("Single Item Collection");
					coll.setTitle("Single Item Collection");
					coll.setPV(jsonObject.getString("PV"));
					coll.setTitle("Single Item Collection");
					coll.setType(Collection.class.getSimpleName());
					coll.setVersion(1);
					coll.setAuthor(cmisServerId);
					
					DataManager.getInstance().storeEntity(coll);
					
					collectionId = digitalRepoService.ingestCollection(coll.getPofId());
					
					logger.debug("Ingested Collection "+coll.getPofId()+" into Digital Repository: "+collectionId);
					
					coll.setRepositoryId(collectionId);
					coll.setLastUpdate(System.currentTimeMillis());
					coll.setPreserved(true);
					DataManager.getInstance().updateEntity(coll);
					
					logger.debug("Created Single Item Collection for Item "+cmisId);
					
				}
				
				itemId = digitalRepoService.ingestItem(pofId,collectionId);
				
				logger.debug("Ingested Item "+pofId+" into Digital Repository with ID: "+itemId+" within Collection: "+collectionId);
				
				Item item = DataManager.getInstance().getEntity(pofId, Item.class);
				item.setRepositoryId(itemId);
				item.setLastUpdate(System.currentTimeMillis());
				item.setPreserved(true);
				DataManager.getInstance().updateEntity(item);
				
				jab.add(item.toJSON());
				
			}
			
		}
	
		return jab.build();
	}
		
	

	private String getCollectionId(long itemPofId, JsonArray collectionsJsonArray){
		
		logger.debug("Searching for Collection ID in JsonArray "+collectionsJsonArray);
		
		/*
		 * JsonArray with One Collection
		 * 
		 [
		   {"type":"Collection","pofId":172,"cmisServerId":"stainer_stainer","cmisId":"pimo:1421765397015:5","repositoryId":"","storageId":"",
		   "PV":"ash","context":"pimo:1421765397015:5:context","title":"Visit Edinburgh 2013","description":"Visit Edinburgh 2013", "status":"Not Preserved",
		   "lastUpdate":1456336675105,"creationDate":1456336675105,"activeSystem":"stainer_stainer","author":"Peter Stainer", "version":1,
		   "items":[{"pofId":173},{"pofId":174}],
		   "collections":[]}
		  ]
		  */
		
		String collectionId = null;
		JsonObject collectionJsonObject = null;
		JsonObject itemJsonObject = null;
		
		for(JsonValue jsonValue : collectionsJsonArray){
			
			collectionJsonObject = (JsonObject)jsonValue;
				
			JsonArray itemsJsonArray = collectionJsonObject.getJsonArray("items");
				
			for(JsonValue itemJsonValue : itemsJsonArray){
					
				itemJsonObject = (JsonObject)itemJsonValue;
					
				long pofId = itemJsonObject.getInt("pofId");
					
				if(pofId == itemPofId){
						
					collectionId = collectionJsonObject.getString("repositoryId");
					
					break; //FIXME: this only works for a single collection in the Array containing the Item, but should be ok...
								
				}
							
			}
		}
			
		logger.debug("Item "+itemPofId+" found in JSON Array Collection "+collectionId);
		
		return collectionId;
		
	}
	
	/*
	 * ACCESS
	 */
	
	
	public void reactivateAIP(Exchange exchange){
		
		taskStep = "ARCHIVER_REACTIVATE_AIP";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
		
		JsonObject jsonBody = MessageTools.getBody(exchange); 
		
		if(jsonBody!=null){
			
			String cmisId = jsonBody.getString("cmisId");
			String cmisServerId = jsonBody.getString("cmisServerId");
			
			PreservationEntity preservationEntity = PreservationEntity.findByCmisId(cmisServerId, cmisId);
			
			String repositoryId = preservationEntity.getRepositoryId();
			
			job.add("repositoryId", repositoryId);
			
			String type = preservationEntity.getType();
			
			logger.debug("Re-activating "+type+" "+repositoryId);
			
			String exportDir = ConfigurationManager.getConfiguration().getString("archiver.dip.folder");
			
			Path dipPath = Paths.get(exportDir,cmisServerId,cmisId);

			try {

				if(type.equals("Collection")){
									
					digitalRepoService.exportCollection(repositoryId, dipPath);
				
				} else if(type.equals("Item")){
					
					digitalRepoService.exportItem(repositoryId, dipPath);
				}
				
			} catch (IOException e) {


			}
			
			
			job.add("dipPath", dipPath.toString());
				
			exchange.getOut().setBody(job.build().toString());
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
								
								
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed: unable to parse message body");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
		
		}		
				
	}
	
		

}



//String response = archiverService.ingestSIP(sipFilePath);
//
//logger.debug("SIP Ingest result:\n"+response);
//
//String repositoryId = parseResponse(response);
//
//logger.debug("Returned repositoryId: "+repositoryId);
//
//Random rnd = new Random();
//String storageId = (String)ConfigurationManager.getConfiguration().getProperty("cloud.storage.tenant.id")+":"+String.valueOf(rnd.nextInt(1000));

//public void reactivateAIP(Exchange exchange){
	
//	logger.debug("New message retrieved");
//	
//	JsonObject headers = MessageTools.getHeaders(exchange);
//	
//	long taskId = headers.getInt("taskId");
//	scheduler.updateTask(taskId, TaskStatus.RUNNING, "AIP RETRIEVAL", null);
//					
//	JsonObject jsonBody = MessageTools.getBody(exchange);  		
//
//	if(jsonBody!=null){
//
//			String jsonCmisId = jsonBody.getString("cmisId");
//			if(jsonCmisId!=null) cmisId = jsonCmisId;
//			logger.debug("Retrieved CMIS ID: "+cmisId);
//			
//			String jsonCmisServerId = jsonBody.getString("cmisServerId");
//			if(jsonCmisServerId!=null) cmisServerId = jsonCmisServerId;
//			logger.debug("Retrieved CMIS Server ID: "+cmisServerId);
//			
//			String pofId = idManager.getPofId(cmisServerId,cmisId);
//			String storageId = idManager.getStorageId(pofId);
//			String repositoryId = idManager.getRepositoryId(pofId);	
//			
//			logger.debug("Executing AIP Re-activation");
//			
//			String tmpDir = (String)ConfigurationManager.getConfiguration().getProperty("pofmiddleware.tmp.dir");
//			
//			String[] storageIds = storageId.split(":");
//	
//			String container = storageIds[0];
//			String aipFileName = storageIds[1];
//			
//			File aipFile = new File(tmpDir,aipFileName);
//			
//			logger.debug("Retrieving AIP "+aipFileName+" from container "+container);
//					
//			try {
//				cloudStorageService.downloadFile(container, aipFileName, aipFile);
//			} catch (IOException e) {
//				
//				e.printStackTrace();
//			}
//			
//			logger.debug("AIP downloaded to file "+aipFile.getAbsolutePath());
//			
//			JsonObjectBuilder job = Json.createObjectBuilder();
//			
//			job.add("aipTarFilePath", aipFile.getAbsolutePath());
//			
//			String jsonMetadata = archiverService.retrieveMetadata(repositoryId);
//				
//			job.add("aipMetadata",jsonMetadata);
//			
//			logger.debug("Retrieved AIP metadata");
//			
//			 for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
//			        job.add(entry.getKey(), entry.getValue());
//			    }
//			
//			exchange.getIn().setBody(job.build());
//			
//			scheduler.updateTask(taskId, TaskStatus.COMPLETED, "EXPORTED_AIP", jsonBody);
//											
//	} else {
//		
//		JsonObjectBuilder job = Json.createObjectBuilder()
//				.add("taskStatus", TaskStatus.FAILED.toString());
//
//	    for (Entry<String, JsonValue> entry : headers.entrySet()) {
//	        job.add(entry.getKey(), entry.getValue());
//	    }
//
//		MessageTools.setHeaders(exchange,headers);
//	
//	}		
//	
//	MessageTools.setHeaders(exchange, headers);
//	
//}


/*
public synchronized void storeAIP(Exchange exchange){

logger.debug("New message retrieved");

JsonObject headers = MessageTools.getHeaders(exchange);

long taskId = headers.getInt("taskId");
scheduler.updateTask(taskId, TaskStatus.RUNNING, "PACKAGE EXPORT", null);
				
JsonObject jsonBody = MessageTools.getBody(exchange);  		

if(jsonBody!=null){

		String jsonPofId = jsonBody.getString("pofId");
		if(jsonPofId!=null) pofId = jsonPofId;
		logger.debug("Retrieved PoF ID: "+pofId);
		
		String jsonAipTarFilePath = jsonBody.getString("aipTarFilePath");
		if(jsonAipTarFilePath!=null) aipTarFilePath = jsonAipTarFilePath;
		logger.debug("Retrieved AIP TAR FILE: "+aipTarFilePath);
		
		
		String container = null;
		
		// Decide Swift container based on resource types
		
		String jsonNofImagesElement = jsonBody.getString("numOfImages");
		String jsonNofDocumentsElement = jsonBody.getString("numOfDocuments");
		
		int nofImages = 0;
		int nofDocuments = 0;
	
		if(jsonNofImagesElement!=null) nofImages = Integer.parseInt(jsonNofImagesElement);
		if(jsonNofDocumentsElement!=null) nofDocuments = Integer.parseInt(jsonNofDocumentsElement);
		
		if(nofImages>0&&nofDocuments==0) 
			container = (String)ConfigurationManager.getConfiguration().getProperty("cloud.storage.images.container");
		else if(nofImages==0&&nofDocuments>0)
			container = (String)ConfigurationManager.getConfiguration().getProperty("cloud.storage.documents.container");
		else
			container = (String)ConfigurationManager.getConfiguration().getProperty("cloud.storage.mixed.container");
							
		logger.debug("Executing AIP Storage");
		
		cloudStorageService.createContainer(container, true);
		
		String storageId = null;
		
		try {
			
			if(nofImages==0&&nofDocuments==1){
				
				logger.debug("Storing AIP and triggering Text Metadata Extraction for PoF ID "+pofId);
				
				cloudStorageService.uploadTextAIP(container, "package-"+pofId+".tar", aipTarFilePath);
				
			}else{
				
				cloudStorageService.uploadFile(container, "package-"+pofId+".tar", aipTarFilePath);
			
			}
			
			storageId = container+":"+"package-"+pofId+".tar";
		
		} catch (IOException e) {
		
			e.printStackTrace();
		
		}
							
		logger.debug("Stored AIP to cloud storage for Storage Id: "+storageId);

		JsonObjectBuilder job = Json.createObjectBuilder();
		
		job.add("storageId", storageId);
		
		for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
	    }

		idManager.updateStorageId(pofId, storageId);
		
		exchange.getIn().setBody(job.build());
		
		scheduler.updateTask(taskId, TaskStatus.COMPLETED, "PRESERVED_AIP_TO_CLOUD", job.build());
						
		MessageTools.setHeaders(exchange, headers);
										
} else {
	
	JsonObjectBuilder job = Json.createObjectBuilder()
			.add("taskStatus", TaskStatus.FAILED.toString());

    for (Entry<String, JsonValue> entry : headers.entrySet()) {
        job.add(entry.getKey(), entry.getValue());
    }

	MessageTools.setHeaders(exchange,headers);

}		


}


private static String parseResponse(String jsonResponse){


/* example:
 * 
 * {"sip-size":"3704208","mime-type":"application/zip","HTTP-status-code":"200","aipUri":"http://archive/xmlui/handle/600826/4",
 * "uploaded-file-path":"/opt/forgetit/archive/tmpstore/file-7184714651446982833.zip","HTTP-media-type":"multipart",
 * "content-type":"application/octet-stream","aipId":"600826/4","info":"Using default DSpace user",
 * "input-file":"eae978c9-8428-48a6-87a5-7937816ef8d6.zip"}
 */
/*
String repositoryId = null;

JsonObject jsonObject = Json.createReader(new StringReader("jsonResponse")).readObject();

if(jsonObject!=null){
	String jsonRepositoryId = jsonObject.getString("aipId");
	if(jsonRepositoryId!=null) repositoryId = jsonRepositoryId;
	logger.debug("Retrieved Repository ID: "+repositoryId);
}

return repositoryId;
}
	
	*
	*
	*
	*		String zipFileName = ConfigurationManager.getConfiguration().getString("server.publication.dir")+File.separator+globalID+".zip";
//		
//		MiddlewareTools.zipDirectory(new File(resourceDir), zipFileName);
//		
//		return zipFileName;
//			
//	}
	*
	*
	*/