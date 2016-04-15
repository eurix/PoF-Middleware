/**
 * PIMORepositoryManager.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * Contributor: Andreas Lauer (andreas.lauer@dfki.uni-kl.de)
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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.persistence.Item;
import eu.forgetit.middleware.persistence.PreservationEntity;
import eu.forgetit.middleware.component.CtxAwarePresManager;
import eu.forgetit.middleware.persistence.Collection;

public class PIMORepositoryManager extends CmisRepositoryManager {
	
	private static Logger logger = LoggerFactory.getLogger(PIMORepositoryManager.class);
	
	@Override
	public List<PreservationEntity> getObjectInformation(String cmisId) {

		List<PreservationEntity> entities = new ArrayList<>(); 
			
		Document pimoObject = (Document)session.getObject(cmisId);
		
		CtxAwarePresManager CaPM = new CtxAwarePresManager();
		List<String> preservationCategories = CaPM.getPreservationCategories(cmisServerId);
						
		if(pimoObject.getType().getId().equals("forgetit:collection")){
			
			Collection coll = new Collection();
					
			long cVersion = getVersion(cmisServerId, pimoObject.getId());

			coll.setVersion(cVersion);
								
			logger.debug("Fetching CMIS Info for Collection: "+pimoObject.getId());
			
			coll.setCmisServerId(cmisServerId);
			coll.setCmisId(cmisId);
			coll.setCreationDate(System.currentTimeMillis());
			coll.setLastUpdate(System.currentTimeMillis());
			coll.setPV(pimoObject.getPropertyValue("pimo:pvCategory"));
			String collectionContextCmisId = pimoObject.getPropertyValue("pimo:context");
			coll.setContext(getLocalContext(collectionContextCmisId));
			coll.setActiveSystem(cmisServerId);
			coll.setAuthor(pimoObject.getPropertyValue("cmis:createdBy"));
			coll.setDescription(pimoObject.getPropertyValue("cmis:description"));
			coll.setPreserved(false);
			coll.setTitle(pimoObject.getPropertyValue("cmis:name"));
			coll.setType(coll.getClass().getSimpleName());
			
			entities.add(coll);
				        				
			int collCounter = 0;
			int presCounter = 0;
				 
			Item item = null;
			Document resource = null;
			
			for(Relationship r: pimoObject.getRelationships()){
					
				if(!r.getId().equals("forgetit:containedIn")) continue;
						
				collCounter++;
				
				resource = (Document)r.getSource();
					
				String PV = (String)resource.getPropertyValue("pimo:pvCategory");
					
				if(preservationCategories.contains(PV)){
					
					item = new Item();
					
					long iVersion = getVersion(cmisServerId, resource.getId());

					item.setVersion(iVersion);
					
					item.setCmisServerId(cmisServerId);
					item.setCmisId(resource.getId());
					item.setCreationDate(System.currentTimeMillis());
					item.setLastUpdate(System.currentTimeMillis());
					item.setPV(PV);
					String itemContextCmisId = resource.getPropertyValue("pimo:context");
					item.setContext(getLocalContext(itemContextCmisId));
					item.setActiveSystem(cmisServerId);
					item.setAuthor(pimoObject.getPropertyValue("cmis:createdBy"));
					item.setDescription(pimoObject.getPropertyValue("cmis:description"));
					item.setPreserved(false);
					item.setTitle(pimoObject.getPropertyValue("cmis:name"));
					item.setType(item.getClass().getSimpleName());
					
					String subject = resource.getPropertyValue("pimo:tags");
					item.setSubject(subject);
				
					coll.getItems().add(item);
					
					entities.add(item);
					
					presCounter++;
					
				} else {
					
					logger.debug("PV for Item "+cmisId+" is: "+PV+"... skipping.");
					
				}
					
			}
				
			logger.debug("Number of collection items to be preserved: "+presCounter+" out of "+collCounter);
				
		} else if(pimoObject.getType().getId().equals("pimo:document")) {
			
			logger.debug("Fetching CMIS Info for Item: "+pimoObject.getId());
			
			Item item = null;
			
			String PV = (String)pimoObject.getPropertyValue("pimo:pvCategory");
			
			if(preservationCategories.contains(PV)){
				
				item = new Item();
				
				long iVersion = getVersion(cmisServerId, pimoObject.getId());
				
				item.setVersion(iVersion);
								
				item.setCmisServerId(cmisServerId);
				item.setCmisId(cmisId);
				item.setCreationDate(System.currentTimeMillis());
				item.setLastUpdate(System.currentTimeMillis());
				item.setPV(PV);

				String contextCmisId = pimoObject.getPropertyValue("pimo:context");
				item.setContext(getLocalContext(contextCmisId));
				
				item.setActiveSystem(cmisServerId);
				item.setAuthor(pimoObject.getPropertyValue("cmis:createdBy"));
				item.setDescription(pimoObject.getPropertyValue("cmis:description"));
				item.setPreserved(false);
				item.setTitle(pimoObject.getPropertyValue("cmis:name"));
				item.setType(item.getClass().getSimpleName());
				
				String subject = pimoObject.getPropertyValue("pimo:tags");
				item.setSubject(subject);
				
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
				
				logger.debug("PV for Item "+cmisId+" is: "+PV+"... skipping.");
				
			}
			
		} else {
			
			// Sanity check!
			
			logger.error("Unable to fecth information from PIMO CMIS for item: "+ pimoObject.getId());
			
		}
			
		return entities;
	
	}
	
	
	private String getLocalContext(String contextCmisId) {
		
		String localContext = "";
		
		Document localContextObject = (Document)session.getObject(contextCmisId);
		
		if(localContextObject != null) {
		
			StringWriter writer = new StringWriter();
			
			try {
			
				IOUtils.copy(localContextObject.getContentStream().getStream(), writer);
				localContext = writer.toString();
			
			} catch (IOException e) {
			
				e.printStackTrace();
			
			}
		 
		}
		
		return localContext;
		
	}
	
	
	private long getVersion(String cmisServerId, String cmisId){
		
		long version = 1;
		
		PreservationEntity entity = PreservationEntity.findByCmisId(cmisServerId, cmisId);
		
		if(entity != null) version = entity.getVersion() + 1;
			
		return version;

	}

	
	public void fetchContentAndMetadata(String cmisId, Path destPath) {

		Path metadataPath = Paths.get(destPath.toString(),"metadata");
		
		Path contentPath = Paths.get(destPath.toString(),"content");
		
		
		try {
				
			String localContextCmisId = super.getObjectProperty(cmisId, "pimo:context");
			
			super.getObjectStream(localContextCmisId, metadataPath, "localContext.ttl");
					
			String contentType = getObjectType(cmisId);
					
			if(contentType.equals("pimo:document")) {
			
				Path contentFilePath = super.getObjectStream(cmisId, contentPath, null);
				
				logger.debug("Copied CMIS Object "+cmisId+" to file "+contentFilePath);
				
			}
		
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}
		
	
}
		
