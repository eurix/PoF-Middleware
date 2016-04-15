/**
 * Scheduler.java
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.persistence.DataManager;
import eu.forgetit.middleware.persistence.Task;
import eu.forgetit.middleware.persistence.Task.TaskPropertyType;
import eu.forgetit.middleware.persistence.Task.TaskQueryType;
import eu.forgetit.middleware.utils.MessageTools;

public class Scheduler {
	
	private static Logger logger = LoggerFactory.getLogger(Scheduler.class);
	
	public enum TaskType{
		TEST("TEST"),
		MANUAL_PRESERVATION("MANUAL_PRESERVATION"),
		AUTO_PRESERVATION("AUTOMATIC_PRESERVATION"),
		USER_LOGS_ANALYSIS("USER_LOGS_ANALYSIS"),
		REACTIVATION("REACTIVATION");
		
		private String value;

	    private TaskType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	public enum TaskStatus{
		STARTED("STARTED"),
		RUNNING("RUNNING"),
		COMPLETED("COMPLETED"), 
		FAILED("FAILED");
		
		private String value;

	    private TaskStatus(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	}
	
	private static ProducerTemplate producer = null;
	private static String waitTime;
	
	private String taskStep = null;

	public Scheduler(){
				
		// FIXME: use bean injection
		CamelContext context = new DefaultCamelContext();
		producer = context.createProducerTemplate();
		
		waitTime = (String)ConfigurationManager.getConfiguration().getProperty("scheduler.task.waiting.time");
		if(waitTime==null) waitTime = "1000";
		
	}
	
	public synchronized long createTask(TaskType taskType, JsonObject params){
		
		Task task = new Task();
		task.setStatus(TaskStatus.STARTED);		
		task.setType(taskType);
		
		JsonObject jsonBody = Json.createObjectBuilder().add(TaskPropertyType.BODY.toString(), "No result is available for this task").build();
		task.setBody(jsonBody);
		
		long startTime = System.currentTimeMillis();
		task.setCreationDate(startTime);
		task.setLastUpdate(startTime);
		
		DataManager.getInstance().storeEntity(task);
		
		long taskId = task.getPofId();
		
		Map<String,Object> headers = new HashMap<String, Object>();
		headers.put(TaskPropertyType.ID.toString(), taskId);
		headers.put(TaskPropertyType.TYPE.toString(), taskType.toString());
		headers.put(TaskPropertyType.STATUS.toString(), TaskStatus.STARTED);
		headers.put(TaskPropertyType.CREATION_DATE.toString(), task.getCreationDate());
		
		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		
		for(String paramKey : params.keySet()){
			jsonObjectBuilder.add(paramKey, params.get(paramKey));
		}
		
		JsonObject jsonObject = jsonObjectBuilder.build();
		
		logger.debug("Sending JsonObject: "+jsonObject.toString());
					
		producer.sendBodyAndHeaders("activemq:queue:SCHEDULER.QUEUE",jsonObject.toString(),headers);
		producer.sendBodyAndHeaders("activemq:queue:LOG.QUEUE",jsonObject.toString(),headers);
		
		return taskId;
	
	}
	
	public synchronized void updateTask(long taskId, TaskStatus taskStatus, String taskStep, JsonObject result){
		
		Task task = DataManager.getInstance().getEntity(TaskQueryType.FIND_BY_POF_ID.toString(), taskId, Task.class);
		
		if(task!=null){
		
			if(taskStatus!=null) task.setStatus(taskStatus);
			if(taskStep!=null) task.setLastStep(taskStep);
			if(result!=null) task.setBody(result);
			
			task.setLastUpdate(System.currentTimeMillis());
			
			DataManager.getInstance().updateEntity(task);

		}
				
	}
	
	public synchronized void closeTask(Exchange exchange){
		
		taskStep = "SCHEDULER_CLOSE_TASK";
		
		logger.debug("New message retrieved for "+taskStep);
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		JsonObject jsonBody = MessageTools.getBody(exchange);
		
		if(headers!=null){
		
			long taskId = Long.valueOf(headers.getString("taskId"));
			logger.debug("Retrieved Task ID: "+taskId);
			
			Task task = DataManager.getInstance().getEntity(TaskQueryType.FIND_BY_POF_ID.toString(), taskId, Task.class);
			
			if(task!=null){
				task.setStatus(TaskStatus.COMPLETED);
				task.setLastUpdate(System.currentTimeMillis());
				task.setBody(jsonBody);
				DataManager.getInstance().updateEntity(task);
			}
		}
	}

	
	public void waitForTask(long taskId) {
	
		while(!(getTaskStatus(taskId)==TaskStatus.COMPLETED)){
		
			logger.debug("Waiting for task completion: "+taskId);
		
			try {
				Thread.sleep(Long.parseLong(waitTime));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		}
	
		logger.debug("Task completed: "+taskId);
	
	}

	public TaskStatus getTaskStatus(long taskId){
		
		Task task = DataManager.getInstance().getEntity(TaskQueryType.FIND_BY_POF_ID.toString(), taskId, Task.class);
		
		if(task!=null)
			return task.getStatus();
		else
			return null;
		
	}
	
	public JsonObject getTaskResult(long taskId){
		
		Task task = DataManager.getInstance().getEntity(TaskQueryType.FIND_BY_POF_ID.toString(), taskId, Task.class);
		
		if(task!=null){
			logger.debug("Retrieving task result");
			return task.getBody();
		}else{ 
			return null;
		}
	}
	
		
	public List<Task> getAllTasks() {

		return DataManager.getInstance().getEntities(TaskQueryType.FIND_ALL.toString(), Task.class);
	
	}
	
	
	public void sendMessage(String queue, JsonObject body){
		
		
		producer.sendBody(queue,body);
		
	}
	
	public void sendMessage(String queue, Map<String,Object> headers, JsonObject body){
		
		
		producer.sendBodyAndHeaders(queue,body.toString(),headers);
		
	}
	
}


