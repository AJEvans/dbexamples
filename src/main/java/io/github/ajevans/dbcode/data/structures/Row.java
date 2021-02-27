/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;




/**
* Generic row class containing one or more value objects.
* <p>
* While this might usually be used within a <code>Table</code>, it will work 
* with any <code>IRecordHolder</code>. Has its own metadata in which it sets 
* a row version number to track edits for, e.g. rollback.
* Rows should refer to their container for field information.
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
* @todo It might be nice to have free-floating rows with access to their 
*       own fields, but maybe that's another IRecord type; for now any 
*       IRecordHolder needs to contain the field info.
* @todo Originally this class extended ArrayList, but probably makes sense to 
*       wrap an ArrayList instead: allows for latter addition of fields etc. on 
*       a consistent basis.
*/
public class Row implements IRecord {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------    
    
    
    
    
    /**
    * Metadata for the row. Includes row version number.
    */
    private Metadata metadata = new Metadata();
    
    
    /**
    * Storage for values.
    */
    private ArrayList values = new ArrayList();
    
    
    /**
    * Parent container for the row.
    */
    private IRecordHolder parentRecordHolder = null;
    
    


    //--------MAJOR METHODS-----------------------------------------------------    




    /**
    * Generic constructor made private to stop floating rows 
    * without a parent container.
    *
    */
    private Row() {}
    
    
    
    
    /**
    * Constructor for rows with a parent container.
    * <p>
    * This allows us to retrieve parent metadata if needed, as 
    * well as engage in any leaf-to-root tree navigation.
    * <p>
    * Metadata version number starts at 1.
    * <p>
    * Also allows the row to gain its field info.
    * 
    *
    * @param     parentRecordHolder    Parent container.
    */
    public Row(IRecordHolder parentRecordHolder) {
        
        metadata.setVersion("1");
        this.parentRecordHolder = parentRecordHolder;
        
    }
    
    


    //--------ACCESSOR / MUTATOR METHODS----------------------------------------    

    
    
    
    /**
    * Returns the record holder this row is part of.
    *
    * @return     IRecordHolder    Parent record holder.
    */
    public IRecordHolder getParentRecordHolder() {
        return parentRecordHolder;
    }
    
    
    
    
    /**
    * Increment the row version number in metadata.
    */
    public void incrementVersion() {
        
        String versionString = metadata.getVersion();
        int versionInt = Integer.parseInt(versionString);
        versionInt++;
        versionString = new Integer(versionInt).toString();
        metadata.setVersion(versionString);
        
    }
    
    
    
        
    /**
    * Decrement the row version number in metadata.
    */
    public void decrementVersion() {
        
        String versionString = metadata.getVersion();
        int versionInt = Integer.parseInt(versionString);
        versionInt--;
        versionString = new Integer(versionInt).toString();
        metadata.setVersion(versionString);
        
    }
    
    
    
    
    /**
    * Get the row version number.
    *
    * @return     int        Row version number.
    */
    public int getVersion() {
        
        String versionString = metadata.getVersion();
        return(Integer.parseInt(versionString));
        
    }
    
    
    
    
    /**
    * Set a single value.
    *
    * @param     index    Location of value.
    * @param     value    Object representing value.
    */
    public void setValue(int index, Object value) {
        
            values.set(index, value);
            
    }
    
    


    /**
    * Add a single value to end of row.
    *
    * @param     value    Object representing value.
    */
    public void addValue(Object value) {
        
            values.add(value);
            
    }    
    



    /**
    * Get a single value at an index.
    *
    * @param     index     Location of value.
    * @return    Object    Requested value as an object.
    */
    public Object getValue(int index) {
        
            return values.get(index);
            
    }
    
    
    
        
    /**
    * Get all values.
    *
    * @return    ArrayList    ArrayList of values.
    */
    public ArrayList getValues() {
        return values;
    }
        
    


    //--------UTILITY METHODS---------------------------------------------------    

    
    
    
    /**
    * For printing rows.
    * <p>
    * Mainly added to print calender objects reasonably.
    *
    * @return    String    Row as String of values.
    * @todo      Add additional object types as required.
    */
    public String toString() {
        
        ArrayList<String> fieldNames = parentRecordHolder.getFieldNames();
    
        // Make CSV list of values.
        
        String rowString = "";
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        
        for (int i = 0; i < values.size(); i++) {
            
            String classString = values.get(i).getClass().toString();
            if (classString.contains("GregorianCalendar")) {
                try {
                    Date date = ((GregorianCalendar)values.get(i)).getTime();
                    rowString = rowString + fieldNames.get(i) + " = " + formatter.format(date) + ", ";
                } catch (RuntimeException re) {
                    rowString = rowString + fieldNames.get(i) + " = " + values.get(i).toString() + ", ";
                }
            } else {
                rowString = rowString + fieldNames.get(i) + " = " +  values.get(i) + ", " ;
            }
            
        }
        
        // Remove final comma.
        rowString = rowString.substring(0, rowString.length() - 2);

        return rowString;
    
    }
    
    
    
    
}
    
    