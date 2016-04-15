/**
 * CmisRepositoryManagerFactory.java
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;

import eu.forgetit.middleware.persistence.CmisRepository;
import eu.forgetit.middleware.persistence.DataManager;
import eu.forgetit.middleware.persistence.CmisRepository.CmisRepoQueryType;
import eu.forgetit.middleware.persistence.CmisRepository.CmisServerType;

public class CmisRepositoryManagerFactory {
		
	public static CmisRepositoryManager buildManager(String cmisServerId){
		
		CmisRepositoryManager cmisRepoManager = null;
		
		List<CmisRepository> cmisRepoList = DataManager.getInstance().getEntities(CmisRepoQueryType.FIND_BY_ID.toString(), "cmisServerId", cmisServerId, CmisRepository.class);
		
		if(cmisRepoList!=null&&cmisRepoList.size()==1) {
			
			CmisRepository cmisRepo = cmisRepoList.get(0);
			
			if(cmisRepo!=null) {
				
				cmisRepoManager = buildManager(cmisRepo);
				
			}
			
		}
		
		return cmisRepoManager;
		
		
	}
	
	
	public static CmisRepositoryManager buildManager(CmisRepository cmisRepository){
				
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put(SessionParameter.USER, cmisRepository.getCmisServerUsername());
		parameter.put(SessionParameter.PASSWORD, cmisRepository.getCmisServerPassword());
		parameter.put(SessionParameter.REPOSITORY_ID, cmisRepository.getCmisRepositoryId());
		
		if(cmisRepository.getCmisServerBindingType().equalsIgnoreCase(BindingType.ATOMPUB.value())){
			
			parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
			parameter.put(SessionParameter.ATOMPUB_URL, cmisRepository.getCmisServerURL());
			
		} else {
			
			parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
			parameter.put(SessionParameter.BROWSER_URL, cmisRepository.getCmisServerURL());
		}
		
		CmisRepositoryManager cmisRepoManager = null;
		
		if(cmisRepository.getCmisRepositoryType().equals(CmisServerType.CHEMISTRY_PIMO)){

			cmisRepoManager = new PIMORepositoryManager();
				
		} else if(cmisRepository.getCmisRepositoryType().equals(CmisServerType.ALFRESCO)){

			cmisRepoManager = new TYPO3RepositoryManager();
			
		} else if(cmisRepository.getCmisRepositoryType().equals(CmisServerType.CHEMISTRY_FILESHARE)){
			
			cmisRepoManager = new FileshareRepositoryManager();
			
		} else {

			parameter.put(SessionParameter.BROWSER_URL, cmisRepository.getCmisServerURL());
			parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
			
			cmisRepoManager = new FileshareRepositoryManager();
			
		}
		
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Session session = sessionFactory.createSession(parameter);			

		cmisRepoManager.setSession(session);
		
		cmisRepoManager.setCmisServerId(cmisRepository.getCmisServerId());
		
		return cmisRepoManager;
		
	}
	
}
