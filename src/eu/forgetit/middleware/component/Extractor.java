/**
 * Extractor.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * Contributors: Vassilios Solachidis <vsol@iti.gr>, Olga Papadopoulou <olgapapa@iti.gr>
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

package eu.forgetit.middleware.component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import eu.forgetit.middleware.ConfigurationManager;
import eu.forgetit.middleware.component.Scheduler.TaskStatus;
import eu.forgetit.middleware.persistence.Collection;
import eu.forgetit.middleware.remote.ExtractorServiceConsumer;
import eu.forgetit.middleware.utils.MessageTools;

public class Extractor {
	
	private static Logger logger = LoggerFactory.getLogger(Extractor.class);
	
	@BeanInject
	private Scheduler scheduler;
	
	private String taskStep = null;
	
	public enum MethodType{
		ALL("all"), 
		IMG_CONCEPT_DETECTION("concept"), 
		IMG_QUALITY_ASSESSMENT("quality"), 
		IMG_NEAR_DUPLICATE_DETECTION("duplicate"), 
		IMG_FACE_DETECTION("faceDetection"),
		IMG_FACE_CLUSTERING("faceClustering"),
		IMG_AESTHETIC_QUALITY("aesthetic"),
		VD_CONCEPT_DETECTION("concept"), 
		VD_AESTHETIC_QUALITY("aesthetic"),
		VD_FACE_DETECTION("fdetection"),
		VD_FACE_CLUSTERING("fclustering"),
		VD_SHOT_SCENE("shot-scene");		
		
		private String value;

	    private MethodType(String value) {
	        this.value = value;
	    }

	    @Override
	    public String toString() {
	        return value;
	    }
	    
	}
		
	private ExtractorServiceConsumer service = null; 
	
	private String sipContentDirPath = null;
	private String vamMethod = null;
	private int numOfVideos;
	
	public Extractor(){
		
		service = new ExtractorServiceConsumer();
			
	}

	
	public void executeImageAnalysis(Exchange exchange){
		
		taskStep = "EXTRACTOR_IMAGE_ANALYSIS";

		logger.debug("New message retrieved for "+taskStep);
		
		JsonObjectBuilder job = Json.createObjectBuilder();

		JsonObject headers = MessageTools.getHeaders(exchange);
		
		long taskId = Long.parseLong(headers.getString("taskId"));
		scheduler.updateTask(taskId, TaskStatus.RUNNING, taskStep, null);
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  	
		
		if(jsonBody!=null){			
			
			String cmisServerId = jsonBody.getString("cmisServerId");
			JsonArray jsonEntities = jsonBody.getJsonArray("entities");
			
			job.add("cmisServerId", cmisServerId);
			job.add("entities", jsonEntities);
			
			for (JsonValue jsonValue : jsonEntities) {
				
				JsonObject jsonObject = (JsonObject)jsonValue;
				
				String type = jsonObject.getString("type");
				
				if(type.equals(Collection.class.getName())) continue;
				
				long pofId = jsonObject.getInt("pofId");
			
				try{
			
					String collectorStorageFolder = ConfigurationManager.getConfiguration().getString("collector.storage.folder");

					Path sipPath = Paths.get(collectorStorageFolder+File.separator+pofId);
					
					Path metadataPath = Paths.get(sipPath.toString(),"metadata");

					Path contentPath = Paths.get(sipPath.toString(),"content");
					
					Path imageAnalysisPath = Paths.get(metadataPath.toString(),"imageAnalysis.xml");
					
					String imagePaths = getImagePaths(contentPath, pofId);
		
					if(imagePaths!=null&&!imagePaths.isEmpty()){
					
						String response = service.img_request(imagePaths,"all",null);
						
						FileUtils.writeStringToFile(imageAnalysisPath.toFile(), response);
					
						logger.debug("Image Analysis completed for "+imagePaths);
					} 
									
				} catch (IOException e) {
				
					e.printStackTrace();
				}
				
			}
				
			exchange.getOut().setBody(job.build().toString());
			exchange.getOut().setHeaders(exchange.getIn().getHeaders());
		
		
		} else {
			
			scheduler.updateTask(taskId, TaskStatus.FAILED, taskStep, null);
			
			job.add("Message", "Task "+taskId+" failed");
			
			scheduler.sendMessage("activemq:queue:ERROR.QUEUE", exchange.getIn().getHeaders(), job.build());
		}
	
	}
	
	private synchronized String getImagePaths(Path contentPath, long pofId) throws IOException{
				
		StringBuilder imagePaths = new StringBuilder();
		
		String pofPublicDir = ConfigurationManager.getConfiguration().getString("pofmiddleware.pub.dir");
		String pofPublicURL = ConfigurationManager.getConfiguration().getString("pofmiddleware.pub.url");
		
		String publicURL = pofPublicURL+File.separator+pofId;
		
		Path publicPath = Paths.get(pofPublicDir,String.valueOf(pofId));
		
		FileUtils.forceMkdir(publicPath.toFile());
				
		List<String> imageList = getFilteredImageList(contentPath.toString());
		
		int i=0;
		
		for(String imageFilePath : imageList){
				
			File imageFile = new File(imageFilePath);
			
			String uniqueImageFileName = "img-"+UUID.randomUUID().toString()+"."+FilenameUtils.getExtension(imageFilePath);
			
			File outputFile = new File(publicPath.toFile(),uniqueImageFileName);
			
			FileUtils.copyFile(imageFile, outputFile);
			
			String imagePath = publicURL+File.separator+outputFile.getName();
				
			if(i==0){
					
				imagePaths.append(imagePath);
				
			} else {
					
				imagePaths.append("~"+imagePath);
				
			}
				
			i++;
				
		}				
		
		return imagePaths.toString();
	
	}
	
	
	public void testVideoAnalysis(Exchange exchange){

		logger.debug("New message retrieved");
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  	
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
		}

		if(jsonBody!=null){
			
				String videoPath = jsonBody.getString("videoPath");
				logger.debug("Retrieved Video Path: "+videoPath);
				
				job.add("videoPath", videoPath);
			
				String method = jsonBody.getString("method");
				logger.debug("Retrieved VAM: "+method);
				
				job.add("method", method);
			
				// ONE video per call
				
				if(videoPath!=null&&!videoPath.isEmpty()){
				
					logger.debug("Executing Video Analysis Method: "+method);
					
					String result = service.video_request(videoPath, method);
					
							//System.out.println("VAM method "+method+" result:\n"+result);
					
					//  Video analysis service response
					String response = service.video_results(videoPath);
					
					logger.debug("VAM method "+method+" result:\n"+response);
					
					job.add("result", response);

					
				} else {
					
					logger.debug("Unable to process video, wrong request");
					
					job.add("result","Unable to process video, wrong request");
					
				}
				
				exchange.getOut().setBody(job.build().toString());
				exchange.getOut().setHeaders(exchange.getIn().getHeaders());
				
			}
		
	}
	
	
	public void testZeedAnalysis(Exchange exchange){

		logger.debug("New message retrieved");
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
		}


		if(jsonBody!=null){
				
				String diary = jsonBody.getString("diary");
				logger.debug("Retrieved Video Path: "+diary);
				
				job.add("diary", diary);
			
				String imagePath = jsonBody.getString("imgPath");
				logger.debug("Retrieved VAM: "+imagePath);
				
				job.add("imgPath", imagePath);
			
				
				// ONE video per call
				
				if(diary!=null&&!diary.isEmpty()){
					if (imagePath!=null&&!imagePath.isEmpty()){
				
					logger.debug("Executing Zero Example Event Detection Method for image collection: " + imagePath);
																
					String response = service.zeed_request(diary, imagePath);
					
					logger.debug("Zeed method for text file: "+imagePath+" result:\n"+response);
					
					}else{
					
						logger.debug("Unable to process image collection, wrong request");
					
					}
					
				} else {
					
					logger.debug("Unable to process textual information, wrong request");
					
				}
				
			}
	
	}
	
	
	
	
	

	
	

	
	
	
	private synchronized String getVideoPath(JsonObject jsonBody){
		
		long pofId = jsonBody.getInt("pofId");
		logger.debug("Retrieved PoF ID: "+pofId);
	
		String jsonContentDir = jsonBody.getString("sipContentDir");
		if(jsonContentDir!=null) sipContentDirPath = jsonContentDir;
		logger.debug("Retrieved SIP Content Directory: "+sipContentDirPath);
		
		String rootPublicDir = ConfigurationManager.getConfiguration().getString("pofmiddleware.pub.dir");
		File publicDir = new File(rootPublicDir,String.valueOf(pofId));
		logger.debug("Publication Dir: "+publicDir.getAbsolutePath());
		if(!publicDir.exists())publicDir.mkdirs();
		
		String rootPublicURL = ConfigurationManager.getConfiguration().getString("pofmiddleware.pub.url");
		String publicURL = rootPublicURL+File.separator+pofId;
		logger.debug("Publication URL: "+publicURL);

		List<String> videoList = getFilteredVideoList(sipContentDirPath);
		
		numOfVideos = videoList.size();

		logger.debug("Video List for Analysis: "+videoList);

		StringBuilder videoPaths = new StringBuilder();
		
		int i=0;
		
		for(String videoFilePath : videoList){				
			File videoFile = new File(videoFilePath);			
			String uniqueImageFileName = "vd-"+UUID.randomUUID().toString()+"."+FilenameUtils.getExtension(videoFilePath);			
			File outputFile = new File(publicDir,uniqueImageFileName);
			try{
			
				FileUtils.copyFile(videoFile, outputFile);
			
				String videoPath = publicURL+File.separator+outputFile.getName();
				
				logger.debug("videoPath: "+videoList);
				
				if(i==0)
					videoPaths.append(videoPath);
				else
					// Store only the first video .. one video per call
					videoPaths.append("+"+videoPath);
				
				i++;
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}				
		
		logger.debug("videoPaths: "+videoPaths);
		
		return videoPaths.toString();
}
	
		

	
	
	private List<String> getFilteredVideoList(String contentDir){
		
		List<String> filteredImages = new ArrayList<>();
	
		String[] contentFiles = new File(contentDir).list();
	
		if(contentFiles==null||contentFiles.length==0) return filteredImages;
	
		List<String> formatList = Arrays.asList(ConfigurationManager.getConfiguration().getStringArray("extractor.video.analysis.formats"));
		
		logger.debug("Accepted image formats:"+formatList);
	
		String format = null;
	
		Tika tika = new Tika();
		
		for (String file : contentFiles) {
	
			format = tika.detect(file);
			
			logger.debug("Found file in content dir: "+file+" ("+format+")");
		
			if(formatList.contains(format)) {
			
				filteredImages.add(contentDir+"/"+file);
				logger.debug("New image added to list: "+contentDir+"/"+file);
			
			} 
				
		}
	
		return filteredImages;
	
	}
	
	
	private List<String> getFilteredImageList(String contentDir){
	
		List<String> filteredImages = new ArrayList<>();
	
		String[] contentFiles = new File(contentDir).list();
	
		if(contentFiles==null||contentFiles.length==0) return filteredImages;
	
		List<String> formatList = Arrays.asList(ConfigurationManager.getConfiguration().getStringArray("extractor.image.analysis.formats"));
		
		logger.debug("Accepted image formats:"+formatList);
	
		String format = null;
	
		Tika tika = new Tika();
		
		for (String file : contentFiles) {
	
			format = tika.detect(file);
			
			logger.debug("Found file in content dir: "+file+" ("+format+")");
		
			if(formatList.contains(format)) {
			
				filteredImages.add(contentDir+"/"+file);
				logger.debug("New image added to list: "+contentDir+"/"+file);
			
			} 
				
		}
	
		return filteredImages;
	
	}
	
	
	private String parseUserID(String inputFile){
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setNamespaceAware(true);
		DocumentBuilder builder = null;
		try {
		    builder = builderFactory.newDocumentBuilder();
		    
		    Document document = builder.parse(new File(inputFile)); 
		
            XPathFactory xpathFactory = XPathFactory.newInstance();
 
            XPath xpath = xpathFactory.newXPath();
 
            XPathExpression expr = xpath.compile("/Image_analysis_methods/@userID");
            
            String userID = (String)expr.evaluate(document,XPathConstants.STRING);
		    
            System.out.println("userID: "+userID);
            
            return userID;
            
		} catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
		    e.printStackTrace();  
		}
		
		return null;
		
	}	
	
	
	public void videoNDDAnalysis(Exchange exchange){
		
		logger.debug("New message retrieved");
		
		JsonObject jsonBody = MessageTools.getBody(exchange);  		
		
		JsonObjectBuilder job = Json.createObjectBuilder();
		
		for (Entry<String, JsonValue> entry : jsonBody.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
		}

		if(jsonBody!=null){
			
				//String videoPaths = getVideoPath(jsonBody);
				
				String videoPath = jsonBody.getString("videoPath");
				logger.debug("Retrieved Video Path: "+videoPath);
				
				job.add("videoPath", videoPath);
			
				//String method = jsonBody.getString("method");
				//System.out.println("Retrieved VAM: "+method);
			
				
				// ONE video per call
				
				if(videoPath!=null&&!videoPath.isEmpty()){
				
					logger.debug("Executing Video Analysis Method: Near Duplicate Detection");
																
					String response_tmp = service.video_ndd_request(videoPath);
					
					logger.debug("Video NDD Analysis method result:\n"+response_tmp);
					
					// call id is returned at the response 
					String callid;
					String[] callid_tmp = response_tmp.split("::");
					callid = callid_tmp[1].trim();
					String response = service.video_ndd_result(callid);
					
					logger.debug("Video NDD result:\n"+response);
					
					job.add("result", response);

					
				} else {
					
					logger.debug("Unable to process video, wrong request");
					
					job.add("result","Unable to process video, wrong request");
					
				}
				
				exchange.getOut().setBody(job.build().toString());
				exchange.getOut().setHeaders(exchange.getIn().getHeaders());
				
			}	
	
	}



//	@Override
//	public void processMessage(MapMessage mapMessage) throws JMSException {
//
////		String logMessage = "Processor "+this.getClass().getSimpleName()+": received new message from queue "+mapMessage.getString(ConfKey.QUEUE);
////		logger.debug(logMessage);
////
////		String contentDir = mapMessage.getString(ConfKey.CONTENT_DIR);
////		String metadataDir = mapMessage.getString(ConfKey.METADATA_DIR);
////		String imageAnalysisFlag = mapMessage.getString(ConfKey.IMAGE_ANALYSIS_FLAG);
////		String globalID = mapMessage.getString(ConfKey.GLOBAL_ID);
////				
////		String outputImageAnalysis = null;	
////		MapMessage newMessage = getMutableMapMessage(mapMessage);
////		
////		List<String> images = getImageList(contentDir);
////		
////		if(images==null||images.size()==0) {
////			
////			logger.debug("No suitable images found in content folder... skipping");
////			
////		} else {
////		
////			StringBuilder imagesString = new StringBuilder();
////		
////			if(images.size()==1){
////				
////				imagesString.append(publishImage(images.get(0),globalID));
////				
////			}else{
////			
////				for (String image : images) {
////			
////					imagesString.append(publishImage(image, globalID)+"~");
////			
////				}
////			}
////		
////			if(imageAnalysisFlag==null||imageAnalysisFlag.isEmpty()){
////			
////				logger.debug("No image analysis request... skipping");
////						
////			} else if (imageAnalysisFlag.equals(ConfKey.IMAGE_ANALYSIS)){
////			
////				String extractorMessage = "ImageAnalysis for content ID "+globalID;
////				logger.debug("Image List: "+imagesString.toString());
////				logger.debug(extractorMessage);
////				
////				outputImageAnalysis = new Extractor().imageAnalysis(imagesString.toString());
////			
////				logMessage = "Image Analysis Result:\n"+outputImageAnalysis;
////				logger.debug(logMessage);
////				
////				File imageAnalysisResults = new File(metadataDir,ConfKey.IMAGE_ANALYSIS_RESULT_FILE);
////				try {
////					FileUtils.writeStringToFile(imageAnalysisResults, outputImageAnalysis);
////					//MessageLogger.send("ImageAnalysis Result: "+imageAnalysisResults.getAbsolutePath());
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
////				
////				
////				newMessage.setString(ConfKey.IMAGE_ANALYSIS_RESULT,outputImageAnalysis);
////			
////			}       			
////			
////		}
////		
//		
//	}
//	

	
	
}
	
	

