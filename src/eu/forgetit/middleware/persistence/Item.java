package eu.forgetit.middleware.persistence;

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
	@NamedQuery(name="Item.findAll",query="SELECT i FROM Item i"),
	@NamedQuery(name="Item.findByPofId",query="SELECT i FROM Item i WHERE i.pofId = :pofId"),
	@NamedQuery(name="Item.findByCmisId",query="SELECT i FROM Item i WHERE i.cmisServerId = :cmisServerId AND i.cmisId = :cmisId")
})
public class Item extends PreservationEntity implements Comparable<Item> {
	
	private static Logger logger = LoggerFactory.getLogger(Item.class);
	
	public enum ItemQueryType{
		FIND_ALL("Item.findAll"),
		FIND_BY_POF_ID("Item.findByPofId"),
		FIND_BY_CMIS_ID("Item.findByCmisId");
		
		private String value;

	    private ItemQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	private String fileName; //filename 

	private String relationshipId;
	private String relationshipTarget;
	private String subject;
	
	
	public Item(){
		
		setCreationDate(System.currentTimeMillis());
		setPreserved(false);

	}
	
	public static List<Item> findAll(){
		
		return DataManager.getInstance().getEntities(ItemQueryType.FIND_ALL.toString(), Item.class);
		
	}
	
	public static Item findByPofId(long pofId){
		
		Item item = null;
		
		try{
		
			item = DataManager.getInstance().getEntity(ItemQueryType.FIND_BY_POF_ID.toString(), pofId, Item.class);
	
		} catch(_NoResultException e){
			
			return null;
			
		}
		
		return item;
				
	}
	
	public static Item findByCmisId(String cmisServerId, String cmisId){
		
		Item item = null;

		Map<String,Object> params = new HashMap<>();
		params.put("cmisServerId",cmisServerId);
		params.put("cmisId", cmisId);
		
		List<Item> items = DataManager.getInstance().getEntities(ItemQueryType.FIND_BY_CMIS_ID.toString(), params, Item.class);
		
		if(items==null || items.size() == 0) return null;
		
		if(items.size() == 1) return items.get(0);
		
		if(items.size() > 1) {
			
			logger.debug("More than one Item found... returning the most recent one.");
			
			long version = 0;
			
			for (Item i : items) {
				
				if(i.getVersion() > version){
					
					version = i.getVersion();
					item = i;
					
				}
				
			}
			
		}
		
		return item;
		
	}
	

	// File Name
	
	public String getFileName(){
		return fileName;
	}
	
	public void setFileName(String fileName){
		this.fileName = fileName;
	}
	
	
	
	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public int compareTo(Item item) {
		return String.valueOf(getPofId()).compareTo(String.valueOf(item.getPofId()));
	}


	// Relationship: e.g. containedIn:cmisServerId:cmisId
	
	public String getRelationshipId() {
		return relationshipId;
	}

	public void setRelationshipId(String relationshipId) {
		this.relationshipId = relationshipId;
	}

	public String getRelationshipTarget() {
		return relationshipTarget;
	}

	public void setRelationshipTarget(String relationshipTarget) {
		this.relationshipTarget = relationshipTarget;
	}
		
	
	@Override
	public JsonObject toJSON() {
		
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		
		jsonBuilder.add("type", getType()!=null ? getType() : "");
		jsonBuilder.add("pofId", getPofId());
		jsonBuilder.add("cmisServerId", getCmisServerId()!=null ? getCmisServerId() : "");
		jsonBuilder.add("cmisId", getCmisId()!=null ? getCmisId() : "");
		jsonBuilder.add("repositoryId", getRepositoryId()!=null ? getRepositoryId() : "");
		jsonBuilder.add("storageId", getStorageId()!=null ? getStorageId() : "");
		jsonBuilder.add("PV",getPV()!=null ? getPV() : "");
		jsonBuilder.add("context", getContext()!=null ? getContext() : "");
		jsonBuilder.add("title", getTitle()!=null ? getTitle() : "");
		jsonBuilder.add("description", getDescription()!=null ? getDescription() : "");
		jsonBuilder.add("status", isPreserved() ? "Preserved" : "Not Preserved");
		jsonBuilder.add("lastUpdate", getLastUpdate());
		jsonBuilder.add("creationDate", getCreationDate());
        jsonBuilder.add("activeSystem", getActiveSystem()!=null ? getActiveSystem() : "");		
        jsonBuilder.add("author", getAuthor()!=null ? getAuthor() : "");
        jsonBuilder.add("version", getVersion());
        jsonBuilder.add("preserved", isPreserved());
        jsonBuilder.add("subject", getSubject()!=null ? getSubject() : "");
		
		return jsonBuilder.build();
		
	}

	public static Item fromJSON(JsonObject jsonObject) {
		
		Item item = new Item();
		
		item.setCmisId(jsonObject.getString("cmisId"));
		item.setCmisServerId(jsonObject.getString("cmisServerId"));
		//FIXME: to be completed
		
		return item;
		
	}


	
	
}
