/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;




/**
* Minimal interface for metadata objects.
* <p>
* Minimal to give as much scope for implementations to represent different 
* metadata standards as possible. 
* <p>
* Implementors should consider that terms may be inserted into storage  
* so may like to avoid categories that are also storage technology 
* keywords, e.g. if using with databases, avoid metadata "schema" which 
* will clash with SQL "SCHEMA". 
* 
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public interface IMetadata {
    
    
    /**
    * Should set metadata standard or equivalent description.
    *
    * @param    standard    Metadata standard or other description.
    */
    public void setStandard(String standard);
    
    
    
    
    /**
    * Gets metadata standard or equivalent description.
    *
    * @return    String    Metadata standard or other description.
    */
    public String getStandard();
    
    
    
    
    /**
    * Should get the metadata as an array of <code>ArrayLists</code>.
    * <ul>
    * <li>array[0] = The category names as Strings.</li>
    * <li>array[1] = The data types of the categories as Class</li>
    * <li>array[2] = The category values as objects.</li>
    * </ul>
    * Having this mechanism for getting the values at the interface 
    * level allows interoperability (for getting values) 
    * while allowing implementations to dictate the metadata categories 
    * (and note there is no setAll so category names are immutable).
    *
    * @return    ArrayList[]    Array of metadata.
    */
    public ArrayList[] getAll();
        
        
    
    
}