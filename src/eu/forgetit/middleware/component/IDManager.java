/**
 * IDManager.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * Contributors: Kaweh Djafari Naini (naini@l3s.de)
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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import eu.forgetit.middleware.persistence.DataManager;
import eu.forgetit.middleware.persistence.Item;
import eu.forgetit.middleware.persistence.PersistableEntity;
import eu.forgetit.middleware.persistence.PreservationEntity;

public class IDManager {
	
		
	public JsonObject getIdMapping(long pofId){
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		PreservationEntity entity =	DataManager.getInstance().getEntity(pofId, PreservationEntity.class);
		
		if(entity != null){
			
			job.add("pofId", entity.getPofId());
			job.add("cmisId", entity.getCmisId());
			job.add("cmisServerId", entity.getCmisServerId());
			job.add("repositoryId", entity.getRepositoryId());
			job.add("storageId", entity.getStorageId());
			
		}
		
		return job.build();
		
	}
			
	public <T extends PersistableEntity> long getItemPofId(String cmisServerId, String cmisId){
		
		Item item = Item.findByCmisId(cmisServerId, cmisId);
		
		if(item==null) return 0;
		
		return item.getPofId();
		
	}
	

	public String getRepositoryId(String pofId){
		
		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);
		return entity.getRepositoryId();
	}
	
	public String getStorageId(String pofId){
		
		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);
		return entity.getStorageId();
	}
	
	
	public boolean getPreservationStatus(String pofId){
		
		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);
		
		//return idMapping.isPreserved();
		
		return entity.isPreserved();
	}
	
	public synchronized void updateCmisId(String pofId, String cmisId, String cmisServerId){
	
		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);

		entity.setCmisId(cmisId);
	
		entity.setCmisServerId(cmisServerId);
			
		DataManager.getInstance().updateEntity(entity);
	
	}

	public synchronized void updateRepositoryId(String pofId, String repositoryId){
		
		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);

		entity.setRepositoryId(repositoryId);
	
		DataManager.getInstance().updateEntity(entity);
	
	}
	
	public synchronized void updateStorageId(String pofId, String storageId){
		
		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);

		entity.setStorageId(storageId);
	
		DataManager.getInstance().updateEntity(entity);
	
	}
		
	public synchronized void updatePreservationStatus(String pofId, boolean isPreserved) {

		PreservationEntity entity = (PreservationEntity)DataManager.getInstance().getEntity(Long.valueOf(pofId),PreservationEntity.class);

		entity.setPreserved(isPreserved);
	
		DataManager.getInstance().updateEntity(entity);
		
	}

		
}

