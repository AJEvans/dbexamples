/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbconsumers;


import io.github.ajevans.dbcode.data.io.DataException;




/**
* Generic exception for the database connectors in this package.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class DBCreationException extends DataException {
    
    
    /**
    * Message explaining issue.
    */
    private String message = "";
    
    
    
    
    /**
    * Generic constructor. 
    * <p>
    * Includes generic message "Issue with database."
    *
    */
    public DBCreationException() {
        
        message = "Issue with database.";
        
    }
    
    
    
    
    /**
    * Constructor for setting message.
    *
    * @param     message        Message associated with issue.
    */
    public DBCreationException(String message) {
        
        this.message = message;
        
    }    
    
    
    
    
    /**
    * Accessor for message.
    *
    * @return     Message associated with issue.
    */
    public String getMessage() {
        
        return message;
        
    }
    
    
    
    
}