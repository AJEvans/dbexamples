/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;




/**
* Interface for records containing one or more indexed values.
* <p>
* A record's directly equivalent source could be a row in a database or line in 
* a data file. They are contained in <code>IRecordHolder</code> objects (e.g. 
* representing tables or files).
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public interface IRecord {
    
    
    
    
    /**
    * Should return the record holder this record is 
    * part of, or null if none.
    *
    * @return     IRecordHolder    Parent record holder or null.
    */
    public IRecordHolder getParentRecordHolder();
        


    
    /**
    * Should set a single value.
    *
    * @param     index    Location of value.
    * @param    value    Object representing value.
    */
    public void setValue(int index, Object value);
    
    
        
    
    /**
    * Should add a single data value to end of record.
    *
    * @param     data    Object representing value.
    */
    public void addValue(Object data);
    
    
    
    
    /**
    * Should get a single value at an index.
    *
    * @param     index    Location of value.
    * @return    Object    Requested value as an object.
    */
    public Object getValue(int index);


    
    
    /**
    * Should get all values.
    *
    * @return    ArrayList    ArrayList of values.
    */
    public ArrayList getValues();

        
    
    
}