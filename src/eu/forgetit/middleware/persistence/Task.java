package eu.forgetit.middleware.persistence;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import eu.forgetit.middleware.component.Scheduler.TaskStatus;
import eu.forgetit.middleware.component.Scheduler.TaskType;
import eu.forgetit.middleware.utils.JsonTools;

@Entity 
@NamedQueries({
	@NamedQuery(name="Task.findAll",query="SELECT task FROM Task task"),
    @NamedQuery(name="Task.findByPofId",query="SELECT task FROM Task task WHERE task.pofId = :pofId")
})
public class Task extends PersistableEntity{
	
	public enum TaskQueryType{
		FIND_ALL("Task.findAll"), 
		FIND_BY_POF_ID("Task.findByPofId");
		
		private String value;

	    private TaskQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	
	public enum TaskPropertyType{
		ID("taskId"), 
		STATUS("taskStatus"),
		TYPE("taskType"),
		BODY("taskBody"),
		CREATION_DATE("taskCreationDate"),
		LAST_UPDATE("taskLastUpdate"),
		LAST_STEP("taskLastStep");
		
		private String value;

	    private TaskPropertyType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}

	
	
	private TaskStatus status = null;
	private TaskType type = null;
	private byte[] body = null;
	private String lastStep = null;

	
	public TaskStatus getStatus() {
		
		return status;
	
	}

	public void setStatus(TaskStatus status) {
		
		this.status = status;
	
	}

	public TaskType getType() {
		
		return type;
	
	}

	public void setType(TaskType type) {
		
		this.type = type;
	
	}

	public JsonObject getBody() {
		
			try {
				return JsonTools.deserialize(body);
			} catch (IOException e) {
				e.printStackTrace();
				return Json.createObjectBuilder().build();
			}
	
	}

	public void setBody(JsonObject body) {
		try {
			this.body = JsonTools.serialize(body);
		} catch (IOException e) {
			e.printStackTrace();
			this.body = new byte[0];
		}
	}

	public String getLastStep() {
		return lastStep;
	}

	public void setLastStep(String lastStep) {
		this.lastStep = lastStep;
	}
	
	
	@Override
	public JsonObject toJSON() {
			
		JsonObject jsonObject = Json.createObjectBuilder()
				.add(TaskPropertyType.ID.toString(), getPofId())
		        .add(TaskPropertyType.STATUS.toString(), status.toString())
		        .add(TaskPropertyType.TYPE.toString(), type.toString())
		        .add(TaskPropertyType.BODY.toString(), body.toString())
		        .add(TaskPropertyType.CREATION_DATE.toString(), getCreationDate())
		        .add(TaskPropertyType.LAST_UPDATE.toString(), getLastUpdate())
		        .add(TaskPropertyType.LAST_STEP.toString(), getLastStep()).build();
			
			return jsonObject;
			
		}
	
}