package eu.forgetit.middleware.persistence;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@NamedQuery(name="PreservationBrokerContract.findById",query="SELECT pbc FROM PreservationBrokerContract pbc WHERE pbc.cmisServerId = :cmisServerId")
public class PreservationBrokerContract extends PersistableEntity {
	
	private static Logger logger = LoggerFactory.getLogger(PreservationBrokerContract.class); 
	
	public enum PbcQueryType{
		FIND_BY_ID("PreservationBrokerContract.findById");
		
		private String value;

	    private PbcQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	private String goldLevel;
	private String silverLevel;
	private String bronzeLevel;
	private String woodLevel;
	private String ashLevel;
			
	private @Id String cmisServerId;
	private String contactName;
	private String contactEMail;
	
	
	public String getCmisServerId() {
		return cmisServerId;
	}

	public void setCmisServerId(String cmisServerId) {
		this.cmisServerId = cmisServerId;
	}

	public String getGoldLevel() {
		return goldLevel;
	}

	public void setGoldLevel(String goldLevel) {
		this.goldLevel = goldLevel;
	}

	public String getSilverLevel() {
		return silverLevel;
	}

	public void setSilverLevel(String silverLevel) {
		this.silverLevel = silverLevel;
	}

	public String getBronzeLevel() {
		return bronzeLevel;
	}

	public void setBronzeLevel(String bronzeLevel) {
		this.bronzeLevel = bronzeLevel;
	}

	public String getWoodLevel() {
		return woodLevel;
	}

	public void setWoodLevel(String woodLevel) {
		this.woodLevel = woodLevel;
	}

	public String getAshLevel() {
		return ashLevel;
	}

	public void setAshLevel(String ashLevel) {
		this.ashLevel = ashLevel;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactEMail() {
		return contactEMail;
	}

	public void setContactEMail(String contactEMail) {
		this.contactEMail = contactEMail;
	}


	@Override
	public JsonObject toJSON() {
		
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		
		jsonBuilder.add("cmisServerId", getCmisServerId()!=null ? getCmisServerId() : "");
		
		jsonBuilder.add("gold", getGoldLevel() != null ? getGoldLevel() : "");
		jsonBuilder.add("silver", getSilverLevel() != null ? getSilverLevel() : "");
		jsonBuilder.add("bronze", getBronzeLevel() != null ? getBronzeLevel() : "");
		jsonBuilder.add("wood", getWoodLevel() != null ? getWoodLevel() : "");
		jsonBuilder.add("ash", getAshLevel() != null ? getAshLevel() : "");
		
		jsonBuilder.add("contactName", getContactName()!=null ? getContactName() : "");
		jsonBuilder.add("contactEMail", getContactEMail()!=null ? getContactEMail() : "");
		
		return jsonBuilder.build();
		
	}

	public static PreservationBrokerContract fromJSON(JsonObject jsonObject) {
		
		PreservationBrokerContract contract = new PreservationBrokerContract();
		
		contract.setCreationDate(System.currentTimeMillis());
		contract.setLastUpdate(System.currentTimeMillis());
		contract.setVersion(1);
		
		try {
		
			contract.setCmisServerId(jsonObject.getString("cmisServerId"));
				
			contract.setGoldLevel(jsonObject.getString("gold"));
			contract.setSilverLevel(jsonObject.getString("silver"));
			contract.setBronzeLevel(jsonObject.getString("bronze"));
			contract.setWoodLevel(jsonObject.getString("wood"));
			contract.setAshLevel(jsonObject.getString("ash"));
		
			contract.setContactEMail(jsonObject.getString("contactEMail"));
			contract.setContactName(jsonObject.getString("contactName"));
			
		} catch(RuntimeException e){
		
			logger.error(e.getMessage());
			
			return null;
			
		}
		
		return contract;
		
	}
	
	
	public static PreservationBrokerContract findById(String cmisServerId){
		
		List<PreservationBrokerContract> preservationContracts = DataManager.getInstance().getEntities(PbcQueryType.FIND_BY_ID.toString(), "cmisServerId", cmisServerId, PreservationBrokerContract.class);
		
		if(preservationContracts == null || preservationContracts.size() == 0) return null;
		
		return preservationContracts.get(0);
		
	}
	
	

}
