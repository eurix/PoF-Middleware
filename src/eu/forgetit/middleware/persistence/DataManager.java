/**
 * DataManager.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * 
 * This file is part of ForgetIT Preserve-or-Forget (PoF) Middleware.
 * 
 * Copyright (C) 2013-2015 ForgetIT Consortium - www.forgetit-project.eu
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

package eu.forgetit.middleware.persistence;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.persistence.PersistableEntity;

public class DataManager {
	
	private static Logger logger = LoggerFactory.getLogger(DataManager.class);
	
	private static DataManager dataManager = null;
	
	private static String sourceDB = null;
	private static EntityManagerFactory emf = null;
    private static EntityManager em = null;
	
	public static DataManager getInstance(){
		
		if(dataManager==null) dataManager = new DataManager();
		
		return dataManager;
		
	}
	
	private DataManager(){
		
		sourceDB = (String) ConfigurationManager.getConfiguration().getProperty("persistence.db.file");
		emf = Persistence.createEntityManagerFactory(sourceDB);
		em = emf.createEntityManager();
		
	}
	
	// CRUD Methods
	
	/*
	 * CREATE
	 */
			
	public synchronized void storeEntity(PersistableEntity entity){
		
		em.getTransaction().begin();
		em.persist(entity);
		em.getTransaction().commit();
		
		logger.debug("Stored "+entity.getClass().getSimpleName()+" "+entity.getPofId());
		
	}
	
	/*
	 * READ
	 */
	
	
	public synchronized <T extends PersistableEntity> List<T> getEntities(String namedQuery, Class<T> entityClass){
		
		 TypedQuery<T> query = em.createNamedQuery(namedQuery, entityClass);
		 
		 return query.getResultList();
		
	}
	
	public synchronized <T extends PersistableEntity> List<T> getEntities(String namedQuery, String paramName, Object paramValue, Class<T> entityClass){
		
		 TypedQuery<T> query = em.createNamedQuery(namedQuery, entityClass);
		 query.setParameter(paramName, paramValue);
		 
		 return query.getResultList();
		
	}
	
	public synchronized <T extends PersistableEntity> List<T> getEntities(String namedQuery, Map<String,Object> params, Class<T> entityClass){
		
		TypedQuery<T> query = em.createNamedQuery(namedQuery, entityClass);
		
		for (String paramName : params.keySet()) {
			
			query.setParameter(paramName, params.get(paramName));
		    		
		}
				 
		 return query.getResultList();
		
	}
	
	public synchronized <T extends PersistableEntity> T getEntity(String namedQuery, long pofId, Class<T> entityClass){
		
		 TypedQuery<T> query = em.createNamedQuery(namedQuery, entityClass);
		 query.setParameter("pofId", pofId);
		 
		 return query.getSingleResult();
		
	}
	

	public synchronized <T extends PersistableEntity> T getEntity(long id, Class<T> entity){
		
		return em.find(entity,id);
		
	}
	
	
	/*
	 *  UPDATE 
	 */


	public synchronized void updateEntity(PersistableEntity persistableEntity){
		
		// using transparent update
		
		em.getTransaction().begin();
		persistableEntity.setLastUpdate(System.currentTimeMillis());
		em.getTransaction().commit();
		
	}
	
	/*
	 *  DELETE
	 */
	
	public synchronized void delete(PersistableEntity persistableEntity){
		
			em.getTransaction().begin();
			em.remove(persistableEntity);
			em.getTransaction().commit();
			
	}
	
	
	// DB METHODS
	
	public void closeConnection()
	{
		em.close();
		emf.close();
		
	}

	public void cleanDB(){
	
		closeConnection();
		File dbFile = new File(sourceDB);
		if(dbFile.exists()) dbFile.delete();
		logger.debug("Cleaned ObjectDB file at: "+sourceDB);
	
	}
	
	

}
