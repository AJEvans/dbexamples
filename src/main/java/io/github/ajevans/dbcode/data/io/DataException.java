/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.io;




/**
* Generic superclass for data related exceptions.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class DataException extends Exception {
    
    
    
    
    /**
    * Message outlining issue.
    */
    private String message = "";
    
    
    
    
    /**
    * Generic constructor. Includes
    * generic message.
    */
    public DataException() {
        
        message = "There has been a data issue.";
    
    }
    
    
    
    
    /**
    * Constructor for setting message.
    *
    * @param     message        Message associated with issue.
    */
    public DataException(String message) {
        
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