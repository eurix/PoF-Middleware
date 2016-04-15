package eu.forgetit.middleware.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.objectdb.o._NoResultException;

@Entity 
@NamedQueries({
	@NamedQuery(name="Collection.findAll",query="SELECT c FROM Collection c"),
	@NamedQuery(name="Collection.findByPofId",query="SELECT c FROM Collection c WHERE c.pofId = :pofId"),
	@NamedQuery(name="Collection.findByCmisId",query="SELECT c FROM Collection c WHERE c.cmisServerId = :cmisServerId AND c.cmisId = :cmisId"),
	@NamedQuery(name="Collection.findByItemPofId",query="SELECT c FROM Collection c JOIN c.items i WHERE i.pofId = :pofId"),
	@NamedQuery(name="Collection.findBySubCollectionPofId",query="SELECT c FROM Collection c JOIN c.subCollections sc WHERE sc.pofId = :pofId")
})
public class Collection extends PreservationEntity {
	
	private static Logger logger = LoggerFactory.getLogger(Collection.class);
	
	public enum CollectionQueryType{
		FIND_ALL("Collection.findAll"), 
		FIND_BY_POF_ID("Collection.findByPofId"),
		FIND_BY_CMIS_ID("Collection.findByCmisId"),
		FIND_BY_ITEM_POF_ID("Collection.findByItemPofId"),
		FIND_BY_SUB_COLLECTION_POF_ID("Collection.findBySubCollectionPofId");
		
		private String value;

	    private CollectionQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	@OneToMany(targetEntity=Item.class, fetch=FetchType.LAZY, cascade=CascadeType.PERSIST)
	private List<Item> items;
	
	@OneToMany(targetEntity=Collection.class, fetch=FetchType.LAZY, cascade=CascadeType.PERSIST)
	private List<Collection> collections;
	
	
	public Collection(){

		setCreationDate(System.currentTimeMillis());
		this.items = new ArrayList<>();
		this.collections = new ArrayList<>();
		this.setPreserved(false);

	}
	
	
	public static List<Collection> findAll(){
		
		return DataManager.getInstance().getEntities(CollectionQueryType.FIND_ALL.toString(), Collection.class);
		
	}
	
	public static Collection findByPofId(long pofId){
		
		Collection collection = null;
		
		try{
		
			collection = DataManager.getInstance().getEntity(CollectionQueryType.FIND_BY_POF_ID.toString(), pofId, Collection.class);
			
		} catch(_NoResultException e){
			
			return null;
			
		}
		
		return collection;
		
	}
	
	public static Collection findByCmisId(String cmisServerId, String cmisId){

		Collection collection = null;
		
		Map<String,Object> params = new HashMap<>();
		params.put("cmisServerId",cmisServerId);
		params.put("cmisId", cmisId);
		
		List<Collection> collections = DataManager.getInstance().getEntities(CollectionQueryType.FIND_BY_CMIS_ID.toString(), params, Collection.class);
		
		if(collections==null || collections.size() == 0) return null;
		
		if(collections.size() == 1) return collections.get(0);
		
		if(collections.size() > 1) {
					
			logger.debug("More than one Collection found... returning the most recent one.");
			
			long version = 0;
			
			for (Collection coll : collections) {
				
				if(coll.getVersion() > version){
					
					version = coll.getVersion();
					collection = coll;
					
				}
				
			}
			
		}
		
		return collection;
		
	}

	public static List<Collection> findByItemPofId(long pofId){
	
	return DataManager.getInstance().getEntities(CollectionQueryType.FIND_BY_ITEM_POF_ID.toString(), "pofId", pofId, Collection.class);
	
	
}
	
	public static List<Collection> findBySubCollectionPofId(long pofId){
		
		return DataManager.getInstance().getEntities(CollectionQueryType.FIND_BY_SUB_COLLECTION_POF_ID.toString(), "pofId", pofId, Collection.class);
		
		
	}
	
	
	// Parent and Child Entities (Sub-Collections and Items)
	
	public List<Item> getItems() {
		return items;
	}

	public List<Collection> getCollections() {
		return collections;
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
		
		JsonArrayBuilder jsonItemArrayBuilder = Json.createArrayBuilder();
		JsonArrayBuilder jsonCollectionArrayBuilder = Json.createArrayBuilder();
		
		for(Item item : items){
			
			JsonObjectBuilder jsonItemBuilder = Json.createObjectBuilder();
			jsonItemBuilder.add("pofId", item.getPofId());
			jsonItemArrayBuilder.add(jsonItemBuilder);
			
		}
			
		for(Collection collection : collections){
			
			JsonObjectBuilder jsonCollectionBuilder = Json.createObjectBuilder();
			jsonCollectionBuilder.add("pofId", collection.getPofId());
			jsonCollectionArrayBuilder.add(jsonCollectionBuilder);
			
		}	
		
		jsonBuilder.add("items", jsonItemArrayBuilder);
		jsonBuilder.add("collections", jsonCollectionArrayBuilder);
		
		return 		jsonBuilder.build();
		
	}

}
