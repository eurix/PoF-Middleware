package eu.forgetit.middleware.persistence;

import javax.jdo.annotations.Column;
import javax.json.JsonObject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public abstract class PersistableEntity {
	
	@Id 
	@GeneratedValue
	@Column(name = "ID")
	private long pofId;
	
	private long version;
	
	private long creationDate;
	private long lastUpdate;

	public long getPofId() {
		return pofId;
	}
	
	public long getVersion(){
		return version;
	}

	public void setVersion(long version) {
		
		this.version = version;
	
	}
	
	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public abstract JsonObject toJSON();
	
}
