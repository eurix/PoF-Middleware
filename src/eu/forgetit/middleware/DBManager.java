/**
 * DBManager.java
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

package eu.forgetit.middleware;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBManager {

		private static Logger logger = LoggerFactory.getLogger(DBManager.class);	
		
	    private static Connection conn = null;
	    
	    private static DBManager dbManager = null;
	    
	    public static DBManager getInstance(){
	    	
	    	if(dbManager==null) dbManager = new DBManager();
	    	
	    	return dbManager;
	    	
	    }
	    
	    private DBManager(){
	    	
	    	initDB();
	    		    	
	    }
	    
	    private synchronized void initDB(){
	    	
	    	String dbHome = ConfigurationManager.getConfiguration().getString("db.home.dir");
	    	System.setProperty("derby.system.home", dbHome);    	
	    	
	    	String dbName = ConfigurationManager.getConfiguration().getString("db.name");
	    	String protocol = ConfigurationManager.getConfiguration().getString("db.protocol");

	    	try {
            	
            	Class.forName(ConfigurationManager.getConfiguration().getString("db.driver"));
            	
            	String dbUrl = protocol+dbName+";create=true";
            	
				conn = DriverManager.getConnection(dbUrl);
				
	            conn.setAutoCommit(false);

	            Statement s = conn.createStatement();
	            s.execute("CREATE TABLE logtable(date TIMESTAMP, message LONG VARCHAR)");
	            
          
            } catch (SQLException e){
            	
            	if(e.getSQLState().equals("X0Y32")) {
            	
            		logger.debug("Table logtable already exists");
            
            	} else {
            
            		e.printStackTrace();	
            	
            	} 
            
            } catch (ClassNotFoundException e) {
				
 
				e.printStackTrace();
			}
	    
	    }
	    	
	    
	    public synchronized void addMessage(String message){
	    	
	    	PreparedStatement insertMessageStatement = null;
			try {
				insertMessageStatement = conn.prepareStatement("insert into logtable values (?,?)");
				Timestamp ts = new Timestamp(System.currentTimeMillis());
				insertMessageStatement.setTimestamp(1, ts);
				insertMessageStatement.setString(2, message);
		    	insertMessageStatement.executeUpdate();
		    	conn.commit();
			} catch (SQLException e) {
				
				e.printStackTrace();
			}
	    	
	    
	    }

	    
	    public List<String> getMessages(int n){
	    	
	    	List<String> messages = new ArrayList<String>();
	    	
	    	Statement s;
			try {
				s = conn.createStatement();
				ResultSet rs = s.executeQuery("SELECT date,message FROM logtable ORDER BY date DESC FETCH FIRST "+n+" ROWS ONLY");
		    	
				String logMessage = null;
				
		    	while(rs.next()){
		    		
		    		logMessage = rs.getTimestamp(1)+": "+rs.getString(2);
		    		messages.add(logMessage);
		    		
		    	}
		    	
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    		    	
	    	return messages;
	    	
	    }
	    
	    public void closeConnection(){
	    	
	    	String dbProtocol = ConfigurationManager.getConfiguration().getString("db.protocol");
	    	
	    	try {
				DriverManager.getConnection(dbProtocol+";shutdown=true");
	    	}catch (SQLException se){
                 if (( (se.getErrorCode() == 50000)
                         && ("XJ015".equals(se.getSQLState()) ))) {
                     
                     logger.debug("Derby shut down normally");
                     // Note that for single database shutdown, the expected
                     // SQL state is "08006", and the error code is 45000.
                 } else {
                     // if the error code or SQLState is different, we have
                     // an unexpected exception (shutdown failed)
                     logger.debug("Derby did not shut down normally");
                     se.printStackTrace();
                 }
             }
         }

}
	            
