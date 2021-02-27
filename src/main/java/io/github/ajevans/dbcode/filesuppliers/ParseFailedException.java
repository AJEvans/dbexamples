/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.filesuppliers;


import io.github.ajevans.dbcode.data.io.DataException;




/**
* Generic exception for the parsers in this package.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class ParseFailedException extends DataException {
    
    
    /**
    * Message explaining issue.
    */
    private String message = "";
    
    
    
    
    /**
    * Generic constructor. 
    * <p>
    * Includes generic message "Issue with Parsing".
    */
    public ParseFailedException() {
        
        message = "Issue with Parsing";
    
    }
    
    
    
    
    /**
    * Constructor for setting message.
    *
    * @param     message        Message associated with issue.
    */
    public ParseFailedException(String message) {
        
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