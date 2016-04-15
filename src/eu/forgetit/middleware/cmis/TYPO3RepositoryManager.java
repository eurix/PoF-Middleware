/**
 * TYPO3RepositoryManager.java
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.component.forgettor.Typo3Item;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.persistence.Collection;
import eu.forgetit.middleware.persistence.PreservationEntity;

public class TYPO3RepositoryManager extends CmisRepositoryManager {
	
	private static Logger logger = LoggerFactory.getLogger(TYPO3RepositoryManager.class);

	@Override
	public List<PreservationEntity> getObjectInformation(String cmisId) {
		
		List<PreservationEntity> entities = new ArrayList<>(); 
		
		CmisObject cmisObject = session.getObject(cmisId);
				
		logger.debug("CmisObject Type Id: "+cmisObject.getType().getId());
		
		
		Collection parentColl = Collection.findByCmisId(cmisServerId, "root");
		
		if(parentColl == null ) {

			parentColl = new Collection();
			parentColl.setCmisId(cmisServerId);
			parentColl.setCreationDate(System.currentTimeMillis());
			parentColl.setPV("gold");
			parentColl.setContext("Context not available");
			parentColl.setActiveSystem(cmisServerId);
			parentColl.setDescription("TYPO3 Fish Shop Root Collection");
			parentColl.setTitle("TYPO3 Fish Shop Root Collection");
			parentColl.setAuthor("Fish Shop");
			parentColl.setType(parentColl.getClass().getSimpleName());
			parentColl.setVersion(1);
			parentColl.setLastUpdate(System.currentTimeMillis());
			parentColl.setPreserved(false);
			
		} else {
		
			long version = parentColl.getVersion();
			parentColl.setVersion(version+1);
			
			parentColl.setLastUpdate(System.currentTimeMillis());
			parentColl.setPreserved(false);
		
		}
			
		entities.add(parentColl);
					
			
		if(cmisObject.getType().getId().equals("F:dkd:typo3:pages")) {
			
			Folder typo3Folder = (Folder)cmisObject;
			
			ItemIterable<CmisObject> typo3Children = typo3Folder.getChildren();
			
			for (CmisObject typo3Child : typo3Children) {
				
				if(typo3Child.getType().getId().equals("D:dkd:typo3:tt_content")) {
					
					Document typo3Page = (Document)typo3Child;
				
					logger.debug("Fetching information from ALFRESCO CMIS for D:dkd:typo3:tt_content: "+typo3Page.getId());
				
					Typo3Item item = new Typo3Item();
					item.setCmisServerId(cmisServerId);
					item.setCmisId(cmisId);
					item.setCreationDate(System.currentTimeMillis());
					item.setLastUpdate(System.currentTimeMillis());
					item.setPV("gold");
					item.setContext("Context not available");
					item.setActiveSystem(cmisServerId);
					item.setAuthor(typo3Page.getPropertyValue("cmis:createdBy"));	
					item.setDescription(typo3Folder.getPropertyValue("cmis:path"));
								
					item.setPreserved(false);
					item.setTitle(typo3Page.getPropertyValue("dkd:typo3:general:fullType"));
					item.setType(item.getClass().getSimpleName());
					item.setVersion(1);
		
					if(!parentColl.getItems().contains(item)) parentColl.getItems().add(item);
					
					entities.add(item);

				}
				
			}
			
		} else if(cmisObject.getType().getId().equals("D:dkd:typo3:tt_content")) {
			
			Document typo3Page = (Document)cmisObject;
			
			logger.debug("Fetching information from ALFRESCO CMIS for D:dkd:typo3:tt_content: "+typo3Page.getId());
			
			Typo3Item item = new Typo3Item();
			item.setCmisServerId(cmisServerId);
			item.setCmisId(cmisId);
			item.setCreationDate(System.currentTimeMillis());
			item.setLastUpdate(System.currentTimeMillis());
			item.setPV("gold");
			item.setContext("Context not available");
			item.setActiveSystem(cmisServerId);
			item.setAuthor(typo3Page.getPropertyValue("cmis:createdBy"));
			
			List<Folder> parentFolders = typo3Page.getParents();
			
			if(parentFolders != null && parentFolders.size() > 0) {
				
				item.setDescription(parentFolders.get(0).getPropertyValue("cmis:path"));
				
			} else {
				
				item.setDescription(typo3Page.getPropertyValue("cmis:name"));
			}
			
			item.setPreserved(false);
			item.setTitle(typo3Page.getPropertyValue("dkd:typo3:general:fullType"));
			item.setType(item.getClass().getSimpleName());
			item.setVersion(1);
			
			if(!parentColl.getItems().contains(item)) parentColl.getItems().add(item);
				
			entities.add(item);
			
			
		} else {
			
			// Sanity check!
			
			logger.debug("Unable to fecth information from FILESHARE CMIS for item: "+ cmisObject.getId());
			
		}
			
		return entities;
		
	}
	
	public JsonArray getUserLog(String cmisId, Path destDir){
		
		JsonArray userLogJsonArray = null;
		JsonObject jsonObject = null;
		
		try {
						
			Path userLogPath = super.getObjectStream(cmisId, destDir, "typo3-user-log-"+cmisId+".json");
			
			logger.debug("User log saved to file: "+userLogPath);
			
			JsonReader jsonReader = Json.createReader(new FileInputStream(userLogPath.toFile()));
			
			userLogJsonArray = jsonReader.readArray();
			
			for (JsonValue jsonValue : userLogJsonArray) {
				
				jsonObject = (JsonObject)jsonValue;
				
				logger.debug("User log entry: "+jsonObject);
				
			}
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
				
	
		return userLogJsonArray;
		
	}

	/**
	 * Get the CMIS object
	 */
	public Document getDocument(String cmisId) {
		return (Document)session.getObject(cmisId);
	}

	/**
	 * Update the incoming links file
	 */
	public void updateIncomingLinksFile() {
		Path graphFile = Paths.get(ConfigurationManager.getConfiguration().getString("forgettor.fishshop.graphfile"));
		Folder root = session.getRootFolder();

		// TODO: write the recursive function to implement the incoming links here in the real setting
		// 14 April: DKD people broke the tie between ids of objects in old db and in the CMIS, so I have no way
		// but to set the default values for the objects
	}

	@Override
	public void fetchContentAndMetadata(String cmisId, Path destPath) {
		// TODO Auto-generated method stub
	}
}
