/**
 * CtxAwareManager.java
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

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.persistence.DataManager;
import eu.forgetit.middleware.persistence.PreservationBrokerContract;

public class CtxAwarePresManager {
	
	private static Logger logger = LoggerFactory.getLogger(CtxAwarePresManager.class);
	
	public PreservationBrokerContract createPreservationContract(JsonObject jsonObject){
		
		try{
		
			String cmisServerId = jsonObject.getString("cmisServerId");
			
			PreservationBrokerContract preservationContract = PreservationBrokerContract.findById(cmisServerId);
		
			if(preservationContract == null) {
			
				preservationContract = PreservationBrokerContract.fromJSON(jsonObject);
		
			} else {
		
				preservationContract.setLastUpdate(System.currentTimeMillis());
				long version = preservationContract.getVersion();
				preservationContract.setVersion(version++);
		
				preservationContract.setGoldLevel(jsonObject.getString("gold"));
				preservationContract.setSilverLevel(jsonObject.getString("silver"));
				preservationContract.setBronzeLevel(jsonObject.getString("bronze"));
				preservationContract.setWoodLevel(jsonObject.getString("wood"));
				preservationContract.setAshLevel(jsonObject.getString("ash"));
		
				preservationContract.setContactEMail(jsonObject.getString("contactEMail"));
				preservationContract.setContactName(jsonObject.getString("contactName"));
				
			}
		
			DataManager.getInstance().storeEntity(preservationContract);
			
			return preservationContract;
		
		} catch (RuntimeException e){
			
			logger.error(e.getMessage());
			
			return null;
		}
		
	}
	
	public List<String> getPreservationCategories(String cmisServerId){
		
		List<String> preservationCategories = new ArrayList<>();
		
		PreservationBrokerContract preservationContract = PreservationBrokerContract.findById(cmisServerId);
		
		if(preservationContract != null){
			
			if(!preservationContract.getAshLevel().equalsIgnoreCase("none")) preservationCategories.add("ash");
			if(!preservationContract.getWoodLevel().equalsIgnoreCase("none")) preservationCategories.add("wood");
			if(!preservationContract.getBronzeLevel().equalsIgnoreCase("none")) preservationCategories.add("bronze");
			if(!preservationContract.getSilverLevel().equalsIgnoreCase("none")) preservationCategories.add("silver");
			if(!preservationContract.getGoldLevel().equalsIgnoreCase("none")) preservationCategories.add("gold");
		
		} else {

			// If the contract is missing, the default is preserving gold, silver, bronze
			
			preservationCategories.add("gold");
			preservationCategories.add("silver");
			preservationCategories.add("bronze");
			
		}
		
		logger.debug("Preservation Categories for "+cmisServerId+": "+preservationCategories);
		
		return preservationCategories;
		
	}
	

}
