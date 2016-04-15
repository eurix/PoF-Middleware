package eu.forgetit.middleware.persistence;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	@NamedQuery(name="CmisRepository.findAll",query="SELECT c FROM CmisRepository c"),
	@NamedQuery(name="CmisRepository.findByPofId",query="SELECT c FROM CmisRepository c WHERE c.pofId = :pofId"),
	@NamedQuery(name="CmisRepository.findById",query="SELECT c FROM CmisRepository c WHERE c.cmisServerId = :cmisServerId")
})
public class CmisRepository extends PersistableEntity{
	
	public enum CmisRepoQueryType{
		FIND_ALL("CmisRepository.findAll"), 
		FIND_BY_POF_ID("CmisRepository.findByPofId"),
		FIND_BY_ID("CmisRepository.findById");
		
		private String value;

	    private CmisRepoQueryType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	public enum CmisServerType{
		ALFRESCO("ALFRESCO"), 
		CHEMISTRY_FILESHARE("CHEMISTRY_FILESHARE"),
		CHEMISTRY_PIMO("CHEMISTRY_PIMO");
		
		private String value;

	    private CmisServerType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
	
	private @Id String cmisServerId;
	private String cmisServerURL;
	private String cmisServerBindingType;
	private String cmisServerUsername;
	private String cmisServerPassword;
	private String cmisRepositoryId;
	private CmisServerType cmisRepositoryType;
	
	public static List<CmisRepository> findAll(){
		
		return DataManager.getInstance().getEntities(CmisRepoQueryType.FIND_ALL.toString(), CmisRepository.class);
		
	}
	
	public static CmisRepository findByPofId(long pofId){
		
		return DataManager.getInstance().getEntity(CmisRepoQueryType.FIND_BY_POF_ID.toString(), pofId, CmisRepository.class);
		
		
	}
	
	public static CmisRepository findById(String cmisServerId){
		
		List<CmisRepository> cmisRepositories = DataManager.getInstance().getEntities(CmisRepoQueryType.FIND_BY_ID.toString(), "cmisServerId", cmisServerId, CmisRepository.class);
		
		if(cmisRepositories == null || cmisRepositories.size() != 1) return null;
		
		return cmisRepositories.get(0);
		
	}
	
	
	public String getCmisServerId() {
		return cmisServerId;
	}
	public void setCmisServerId(String cmisServerId) {
		this.cmisServerId = cmisServerId;
	}
	public String getCmisServerURL() {
		return cmisServerURL;
	}
	public void setCmisServerURL(String cmisServerURL) {
		this.cmisServerURL = cmisServerURL;
	}
	public String getCmisServerBindingType() {
		return cmisServerBindingType;
	}
	public void setCmisServerBindingType(String cmisServerBindingType) {
		this.cmisServerBindingType = cmisServerBindingType;
	}
	public String getCmisServerUsername() {
		return cmisServerUsername;
	}
	public void setCmisServerUsername(String cmisServerUsername) {
		this.cmisServerUsername = cmisServerUsername;
	}
	public String getCmisServerPassword() {
		return cmisServerPassword;
	}
	public void setCmisServerPassword(String cmisServerPassword) {
		this.cmisServerPassword = cmisServerPassword;
	}
	public String getCmisRepositoryId() {
		return cmisRepositoryId;
	}
	public void setCmisRepositoryId(String cmisRepositoryId) {
		this.cmisRepositoryId = cmisRepositoryId;
	}
	public CmisServerType getCmisRepositoryType() {
		return cmisRepositoryType;
	}
	public void setCmisRepositoryType(CmisServerType cmisRepositoryType) {
		this.cmisRepositoryType = cmisRepositoryType;
	}
	
	@Override
	public JsonObject toJSON() {
		
		JsonObject jsonObject = Json.createObjectBuilder()
				.add("cmisServerId",cmisServerId)
				.add("cmisServerURL", cmisServerURL)
				.add("cmisServerBindingType", cmisServerBindingType)
				.add("cmisServerUsername", cmisServerUsername)
				.add("cmisServerPassword", cmisServerPassword)
				.add("cmisRepositoryId", cmisRepositoryId)
				.add("cmisRepositoryType", cmisRepositoryType.value).build();
		
		return jsonObject;
		
	}
	

}

