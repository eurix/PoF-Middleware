package eu.forgetit.middleware.persistence;

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

@Entity
public class SituationProfile extends PreservationEntity{
	
	@ManyToMany(targetEntity=Collection.class, fetch=FetchType.LAZY)
	private List<String> attributes;
	
	private String title;
	private String type;
	private String persons;
	private String furtherEntities;
	private String description;
	private String memoryCues;
	private List<String> location1; //contains name of the location and meta data (lat, lon etc)
	private String startDate;
	private String endDate;
	
	private String id; //?
	private String ownerId;
	private String collectionId;
	private String pv;
	private String serverId;
	
	
	public SituationProfile(){
		
		setCreationDate(System.currentTimeMillis());
		
		this.attributes = new ArrayList<>();
		attributes.add("title");
		attributes.add("type");
		attributes.add("startDate");
		attributes.add("endDate");
		attributes.add("location1");
		attributes.add("persons");
		attributes.add("memoryCues");
		attributes.add("furtherEntities");
		attributes.add("description");
		attributes.add("pv");
		attributes.add("collectionId");
		attributes.add("ownerId");
		attributes.add("serverId");
	}
	
	

	public List<String> getAttributes() {
		return attributes;
	}



	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}



	public String getTitle() {
		return title;
	}



	public void setTitle(String title) {
		this.title = title;
	}



	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}



	public String getPersons() {
		return persons;
	}



	public void setPersons(String persons) {
		this.persons = persons;
	}



	public String getFurtherEntities() {
		return furtherEntities;
	}



	public void setFurtherEntities(String furtherEntities) {
		this.furtherEntities = furtherEntities;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public String getMemoryCues() {
		return memoryCues;
	}



	public void setMemoryCues(String memoryCues) {
		this.memoryCues = memoryCues;
	}



	public List<String> getLocation1() {
		return location1;
	}



	public void setLocation1(List<String> location1) {
		this.location1 = location1;
	}



	public String getStartDate() {
		return startDate;
	}



	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}



	public String getEndDate() {
		return endDate;
	}



	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}



	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}



	public String getOwnerId() {
		return ownerId;
	}



	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}



	public String getCollectionId() {
		return collectionId;
	}



	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}



	public String getPv() {
		return pv;
	}



	public void setPv(String pv) {
		this.pv = pv;
	}


	@Override
	public JsonObject toJSON() {

		JsonObject jsonObject = Json.createObjectBuilder()
				.add("title", getTitle())
				.add("type",  getType())
				.build();
		
		return jsonObject;
	}
	
}