/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbconsumers;


import io.github.ajevans.dbcode.data.io.DataException;




/**
* Generic exception for the flat file connectors in this package.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class FlatFileCreationException extends DataException {
    
    
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
    public FlatFileCreationException() {
        
        message = "Issue with flat file.";
        
    }
    
    
    
    
    /**
    * Constructor for setting message.
    *
    * @param     message        Message associated with issue.
    */
    public FlatFileCreationException(String message) {
        
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