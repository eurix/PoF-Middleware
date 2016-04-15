package eu.forgetit.middleware.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.objectdb.o._NoResultException;

@Entity 
@NamedQueries({
	@NamedQuery(name="PreservationEntity.findAll",query="SELECT p FROM PreservationEntity p"),
	@NamedQuery(name="PreservationEntity.findByPofId",query="SELECT p FROM PreservationEntity p WHERE p.pofId = :pofId"),
	@NamedQuery(name="PreservationEntity.findByCmisServerId",query="SELECT p FROM PreservationEntity p WHERE p.cmisServerId = :cmisServerId"),
	@NamedQuery(name="PreservationEntity.findByCmisId",query="SELECT p FROM PreservationEntity p WHERE p.cmisServerId = :cmisServerId AND p.cmisId = :cmisId")
})
public abstract class PreservationEntity extends PersistableEntity {
	
	private static Logger logger = LoggerFactory.getLogger(PreservationEntity.class);
	
	public enum PreservationEntityQueryType{
		FIND_ALL("PreservationEntity.findAll"), 
		FIND_BY_POF_ID("PreservationEntity.findByPofId"),
		FIND_BY_CMIS_SERVER_ID("PreservationEntity.findByCmisServerId"),
		FIND_BY_CMIS_ID("PreservationEntity.findByCmisId");
		
		private String value;

	    private PreservationEntityQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	

	/*
	 * POF_ID - PoF unique identifier: internal Object DB generator (from parent class)
	 * CMIS_ID - CMIS identifier (not unique): User Application ID (CMIS ID) created on a CMIS Server
	 * CMIS_SERVER_ID - CMIS server unique identifier: used by Collector to locate resource with a given cmisId
	 * REPOSITORY_ID - Digital Repository identifier (not unique). For DSpace: ID is the internal DB identifier
	 * STORAGE_ID - Preservation-aware Storage System unique identifier. For Storlet Engine: used by Swift Client
	 */
	private String cmisServerId;
	private String cmisId;
	private String repositoryId;
	private String storageId;
	
	private String PV;
	private String context;	
	
	private String author;
    private String organization;
    private String title;	
    private String description; 
    private String type;
    private String activeSystem; // Active System (user application) which created the item
    private boolean isPreserved; //if already in the Preservation System
	

	public String getCmisServerId() {
		return cmisServerId;
	}

	public void setCmisServerId(String cmisServerId) {
		this.cmisServerId = cmisServerId;
	}

	public String getCmisId() {
		return cmisId;
	}

	public void setCmisId(String cmisId) {
		this.cmisId = cmisId;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getStorageId() {
		return storageId;
	}

	public void setStorageId(String storageId) {
		this.storageId = storageId;
	}


	public String getPV() {
		return PV;
	}
				
	public void setPV(String PV){
		this.PV = PV;
	}
		
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}
					
	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription(){
		return description;
	}
			
	public void setDescription(String description){
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getActiveSystem() {
		return activeSystem;
	}

	public void setActiveSystem(String activeSystem) {
		this.activeSystem = activeSystem;
	}

	public boolean isPreserved() {
		return isPreserved;
	}

	public void setPreserved(boolean isPreserved) {
		this.isPreserved = isPreserved;
	}
	
	
	public static PreservationEntity findByCmisId(String cmisServerId, String cmisId){

		PreservationEntity preservationEntity = null;

		Map<String,Object> params = new HashMap<>();
		params.put("cmisServerId",cmisServerId);
		params.put("cmisId", cmisId);
		
		List<PreservationEntity> preservationEntities = DataManager.getInstance().getEntities(PreservationEntityQueryType.FIND_BY_CMIS_ID.toString(), params, PreservationEntity.class);
		
		if(preservationEntities==null || preservationEntities.size() == 0) return null;
		
		if(preservationEntities.size() == 1) return preservationEntities.get(0);
		
		if(preservationEntities.size() > 1) {
			
			logger.debug("More than one Preservation Entity found... returning the most recent one.");
			
			long version = 0;
			
			for (PreservationEntity entity : preservationEntities) {
				
				if(entity.getVersion() > version){
					
					version = entity.getVersion();
					preservationEntity = entity;
					
				}
				
			}
			
		}
		
		return preservationEntity;
		
	}
	
	
	public static List<PreservationEntity> findByCmisServerId(String cmisServerId){
		
		List<PreservationEntity> preservationEntities = new ArrayList<>();
		
		Map<String,Object> params = new HashMap<>();
		params.put("cmisServerId",cmisServerId);
		
		preservationEntities = DataManager.getInstance().getEntities(PreservationEntityQueryType.FIND_BY_CMIS_SERVER_ID.toString(), "cmisServerId", cmisServerId, PreservationEntity.class);
		
		return preservationEntities;
		
	}
	
	public static PreservationEntity findByPofId(long pofId){
		
		PreservationEntity preservationEntity = null;
		
		try{
		
			preservationEntity = DataManager.getInstance().getEntity(PreservationEntityQueryType.FIND_BY_POF_ID.toString(), pofId, PreservationEntity.class);
	
		} catch(_NoResultException e){
			
			return null;
			
		}
		
		return preservationEntity;
			
	}
	
	public JsonObject toSimpleJSON(){
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		job.add("type", this.getType() != null ? this.getType() : "");
		job.add("cmisServerId", this.getCmisServerId() != null ? this.getCmisServerId() : "");
		job.add("cmisId", this.getCmisId() != null ? this.getCmisId() : "" );
		job.add("PV", this.getPV() != null ? this.getPV() : "" );
		job.add("pofId", this.getPofId());
		job.add("lastUpdate", this.getLastUpdate());
		job.add("preserved", this.isPreserved());
		job.add("version", this.getVersion());
		

		return job.build();
				
	}
	

}
