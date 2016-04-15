/**
 * CmisRepositoryManager.java
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

package eu.forgetit.middleware.cmis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.commons.io.FileUtils;

import eu.forgetit.middleware.persistence.PreservationEntity;

public abstract class CmisRepositoryManager {
	
	protected Session session = null;
	protected String cmisServerId = null;
	
	public final Session getSession(){
		return this.session;
	}
	
	public final void setSession(Session session){
		this.session = session;
	}

	public final String getCmisServerId() {
		return cmisServerId;
	}

	public final void setCmisServerId(String cmisServerId) {
		this.cmisServerId = cmisServerId;
	}
	
	
	public abstract List<PreservationEntity> getObjectInformation(String cmisId); 
	
	public abstract void fetchContentAndMetadata(String cmisId, Path destPath);
	
	public final Path getObjectStream(String cmisId, Path destPath, String fileName) throws IOException{
		
		Path streamPath = null;
		
		Document document = (Document)session.getObject(cmisId);	
			
		ContentStream contentStream = document.getContentStream();
			
		if(contentStream != null){
							
			if(fileName == null) fileName = contentStream.getFileName();
				
			streamPath = Paths.get(destPath.toString(),fileName);
					
			File streamFile = new File(streamPath.toString());
				
			InputStream inputStream = contentStream.getStream();
				
			FileUtils.copyInputStreamToFile(inputStream, streamFile);
				
		}
			
		return streamPath;
			
	};
	
	public final String getObjectProperty(String cmisId, String property) throws IOException{
		
		Document cmisObject = (Document)session.getObject(cmisId);	
		
		return cmisObject.getPropertyValue(property);
		
	};
	
	
	public final String getObjectType(String cmisId){
		
		CmisObject cmisObject = session.getObject(cmisId);	
		
		return cmisObject.getType().getId();
		
	}

}
