/**
 * FileshareRepositoryManager.java
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

package eu.forgetit.middleware.cmis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.persistence.Collection;
import eu.forgetit.middleware.persistence.Item;
import eu.forgetit.middleware.persistence.PreservationEntity;

public class FileshareRepositoryManager extends CmisRepositoryManager {
	
	private static Logger logger = LoggerFactory.getLogger(FileshareRepositoryManager.class);

	public void getObjectContent(String cmisId, Path destPath) {
					
		Document pimoObject = (Document)session.getObject(cmisId);
		
		String fileName = pimoObject.getContentStream().getFileName();
		
		File contentFile = new File(destPath.toFile(),fileName);
		
		try {
			FileUtils.copyInputStreamToFile(pimoObject.getContentStream().getStream(), contentFile);
			
			logger.debug("Copied CMIS Object "+cmisId+" to file "+contentFile.getAbsolutePath());
		
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}
	
	@Override
	public List<PreservationEntity> getObjectInformation(String cmisId) {

		List<PreservationEntity> entities = new ArrayList<>(); 
			
		CmisObject cmisObject = session.getObject(cmisId);
				
		logger.debug("CmisObject Type Id: "+cmisObject.getType().getId());
		
		if(cmisObject.getType().getId().equals("cmis:folder")){
			
			Folder cmisFolder = (Folder)cmisObject;
				
			logger.debug("Fetching information from FILESHARE CMIS for collection: "+cmisObject.getId());
			
			Collection coll = new Collection();
			coll.setCmisServerId(cmisServerId);
			coll.setCmisId(cmisId);
			coll.setCreationDate(System.currentTimeMillis());
			coll.setLastUpdate(System.currentTimeMillis());
			coll.setPV("gold");
			coll.setContext("Context not available");
			coll.setActiveSystem(cmisServerId);
			coll.setAuthor(cmisFolder.getPropertyValue("cmis:createdBy"));
			coll.setDescription(cmisFolder.getPropertyValue("cmis:description"));
			coll.setPreserved(false);
			coll.setTitle(cmisFolder.getPropertyValue("cmis:name"));
			coll.setType(coll.getClass().getSimpleName());
			coll.setVersion(1);
			
			entities.add(coll);
				        				
			int collCounter = 0;
			int presCounter = 0;
				 
			Item item = null;
			Document resource = null;
			
			ItemIterable<CmisObject> children = cmisFolder.getChildren();
			
			for(CmisObject child : children){
					
				if(child.getBaseTypeId().equals("cmis:folder")) continue;
				
				resource = (Document)child;
									
				collCounter++;
							
				item = new Item();
				item.setCmisServerId(cmisServerId);
				item.setCmisId(resource.getId());
				item.setCreationDate(System.currentTimeMillis());
				item.setLastUpdate(System.currentTimeMillis());
				item.setPV("gold");
				item.setContext("Context not available");
				item.setActiveSystem(cmisServerId);
				item.setAuthor(resource.getPropertyValue("cmis:createdBy"));
				item.setDescription(resource.getPropertyValue("cmis:description"));
				item.setPreserved(false);
				item.setTitle(resource.getPropertyValue("cmis:name"));
				item.setType(item.getClass().getSimpleName());
				item.setVersion(1);
				
				coll.getItems().add(item);
					
				entities.add(item);
					
				presCounter++;
										
			}
				
			logger.debug("Number of collection items to be preserved: "+presCounter+" out of "+collCounter);
				
		} else if(cmisObject.getType().getId().equals("cmis:document")) {
			
			logger.debug("Fetching information from FILESHARE CMIS for item: "+cmisObject.getId());
			
			Item item = new Item();
			item.setCmisServerId(cmisServerId);
			item.setCmisId(cmisId);
			item.setCreationDate(System.currentTimeMillis());
			item.setLastUpdate(System.currentTimeMillis());
			item.setPV("gold");
			item.setContext("Context not available");
			item.setActiveSystem(cmisServerId);
			item.setAuthor(cmisObject.getPropertyValue("cmis:createdBy"));
			item.setDescription(cmisObject.getPropertyValue("cmis:description"));
			item.setPreserved(false);
			item.setTitle(cmisObject.getPropertyValue("cmis:name"));
			item.setType(item.getClass().getSimpleName());
			item.setVersion(1);
				
			entities.add(item);
			
			Collection coll = new Collection();
			coll.setVersion(1);
								
			coll.setCmisServerId(cmisServerId);
			coll.setCreationDate(System.currentTimeMillis());
			coll.setLastUpdate(System.currentTimeMillis());
			coll.setPV(item.getPV());
			coll.setContext("Context not available");
			coll.setActiveSystem(cmisServerId);
			coll.setAuthor(item.getAuthor());
			coll.setDescription(item.getDescription());
			coll.setPreserved(false);
			coll.setTitle("Single Item Collection: "+ item.getTitle());
			coll.setType(coll.getClass().getSimpleName());
											
			entities.add(coll);
						
		} else {
			
			// Sanity check!
			
			logger.debug("Unable to fecth information from FILESHARE CMIS for item: "+ cmisObject.getId());
			
		}
			
		return entities;
			
	}

	@Override
	public void fetchContentAndMetadata(String cmisId, Path destPath) {
		
		Path contentPath = Paths.get(destPath.toString(),"content");
		Path metadataPath = Paths.get(destPath.toString(),"metadata");
		
		try {
				
			Files.createDirectories(contentPath);
			Files.createDirectories(metadataPath);
					
			String contentType = getObjectType(cmisId);
					
			if(contentType.equals("cmis:document")) {
			
				Path contentFilePath = super.getObjectStream(cmisId, contentPath, null);
				
				logger.debug("Copied CMIS Object "+cmisId+" to file "+contentFilePath);
				
			}
		
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}

}
