package eu.forgetit.middleware.persistence;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import com.objectdb.o._NoResultException;

@Entity
@NamedQueries({
	@NamedQuery(name="Situation.findAll", query="SELECT s FROM Situation s"),
	@NamedQuery(name="Situation.findByPofId",query="SELECT s FROM Situation s WHERE s.pofId = :pofId"),
	@NamedQuery(name="Situation.findByCollectionPofId", query="SELECT s FROM Situation s JOIN s.collections c WHERE c.pofId = :pofId")
})
public class Situation extends PreservationEntity{
	
	public enum SituationQueryType{
		FIND_ALL("Situation.findAll"), 
		FIND_BY_POF_ID("Situation.findByPofId"),
		FIND_BY_COLLECTION_POF_ID("Situation.findByCollectionPofId");
		
		private String value;

	    private SituationQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	@OneToMany(targetEntity=Collection.class, fetch=FetchType.LAZY)
	private SituationProfile profile;
	
	private Boolean profileAdded = false;
	
	
	//add a new profile (+mandatory attributes) to the index
	public void addProfile(String title, String ownerId){
		
		if (!this.profileAdded){
					
			//create new profile
			SituationProfile profile = new SituationProfile();
					
			//add mandatory attributes
			profile.setTitle(title);
			profile.setOwnerId(ownerId);
					
			this.setProfile(profile);
					
			this.setProfileAdded(true);
			
		}
	}
	
	
	public void addStringAttribute(String attribute, String value){
		//check for all attributes
		switch (attribute) {
		
		case "title":  		this.getProfile().setTitle(value);
							break;
		
        case "type":  		this.getProfile().setType(value);
                 			break;
 						
        case "description": this.getProfile().setDescription(value);
        					break;
        					
        case "memoryCues":  this.getProfile().setMemoryCues(value);
							break;
							
        case "furtherEntities": this.getProfile().setFurtherEntities(value);
								break;
        					
        case "persons": 	this.getProfile().setPersons(value);
        
        case "collectionId": 	this.getProfile().setCollectionId(value);
        
        case "ownerId": 	this.getProfile().setOwnerId(value);
        
        case "serverId": 	this.getProfile().setCmisServerId(value);
							
        default: System.out.println("invalid field or attribute");
                 break;
		}
	}
	
	
	public void addLocation(List<String> location1){
		profile.setLocation1(location1);
	}
	
	
	public void addDate(String attribute, String date){
		if (attribute.equals("startDate")){
			profile.setStartDate(date);
		} else{
			if (attribute.equals("endDate")){
				profile.setEndDate(date);
			}
		}
	}
	
	
	public String createSituationId(){
		String id = UUID.randomUUID().toString();
		return id;
	}
	
	
	public static List<Situation> findAll(){
		
		return DataManager.getInstance().getEntities(SituationQueryType.FIND_ALL.toString(), Situation.class);
		
	}
	
	public static Situation findByPofId(long pofId){
		
		Situation situation = null;
		
		try{
		
			situation = DataManager.getInstance().getEntity(SituationQueryType.FIND_BY_POF_ID.toString(), pofId, Situation.class);
	
		} catch(_NoResultException e){
			
			return null;
			
		}
		
		return situation;
				
	}

	public static List<Situation> findByCollectionPofId(long pofId){
	
		return DataManager.getInstance().getEntities(SituationQueryType.FIND_BY_COLLECTION_POF_ID.toString(), "pofId", pofId, Situation.class);
	
	}
	

	public SituationProfile getProfile() {
		return profile;
	}


	public void setProfile(SituationProfile profile) {
		this.profile = profile;
	}


	public Boolean getProfileAdded() {
		return profileAdded;
	}


	public void setProfileAdded(Boolean profileAdded) {
		this.profileAdded = profileAdded;
	}


	@Override
	public JsonObject toJSON() {
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		job.add("type", profile.getType()!=null ? profile.getType() : "");
		job.add("pofId", getPofId());
		job.add("PV", profile.getPV()!=null ? profile.getPV() : "");
		job.add("title", profile.getTitle()!=null ? profile.getTitle() : "");		
		job.add("description", profile.getDescription() != null ? profile.getDescription() : "");
		job.add("memoryCues", profile.getMemoryCues() != null ? profile.getMemoryCues() : "");
		job.add("lastUpdate", getLastUpdate());
		job.add("creationDate", getCreationDate());
		
		//TODO add more fields
	
		JsonObject jsonObject = job.build();
		
		return jsonObject;
	}
	
	
	public String toString(){
		String string = 
				"Title: " + profile.getTitle() + "\n" 
				+ "Type: " + profile.getType() + "\n"
				+ "Description: " + profile.getDescription() + "\n"
				+ "MemoryCues: " + profile.getMemoryCues() + "\n"
				+ "Persons: " + profile.getPersons() + "\n";				
		
				//TODO: add other attributes
		
		return string;
	}

	
}
