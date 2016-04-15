/**
 * MessageLogger.java
 * Author: Francesco Gallo (gallo@eurix.it)
 * Contributor: Andreas Lauer (andreas.lauer@dfki.uni-kl.de)
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

package eu.forgetit.middleware;

import javax.json.JsonObject;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.forgetit.middleware.utils.MessageTools;

public class MessageLogger {
	
	private static Logger logger = LoggerFactory.getLogger(MessageLogger.class);
		
	public void processMessage(Exchange exchange){
		
		logger.debug("New message retrieved from LOG.QUEUE");
		
		JsonObject headers = MessageTools.getHeaders(exchange);
		
		if(headers!=null) {
			DBManager.getInstance().addMessage(headers.toString());
			logger.debug(headers.toString());
		}
		
		if (exchange == null) {
			return;
		}
		
		JsonObject body = MessageTools.getBody(exchange);
		
		if(body!=null) {
			String msg = body.toString();
			if (msg.length() > 1000) {
				msg = msg.substring(0, 1000 ) + " ... truncated ...";
			}
			
			DBManager.getInstance().addMessage(msg);
			logger.debug(msg);
		}
		
	}
			
}
