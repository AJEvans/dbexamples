/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;




/**
* Interface for a container for one or more indexed records implementing 
* <code>IRecord</code>.
* <p>
* Record holders' directly equivalent sources might include, e.g. a file of 
* data or a database table. The are contained in <code>IDataset</code> objects 
* (e.g. representing directories or databases).
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
* @see IRecord
*/
public interface IRecordHolder {
    
    
    
    
    /**
    * Should return the dataset this <code>RecordHolder</code> is part of, 
    * or null if none.
    *
    * @return     IDataset    Parent data set or null.
    */
    public IDataset getParentDataset();
    
    
    
    
    /**
    * Should set the metadata for this <code>RecordHolder</code>.
    *
    * @param    metadata    The metadata object for this <code>RecordHolder</code>.
    */
    public void setMetadata(IMetadata metadata);
    
    
    
    
    /**
    * Should get the metadata for this <code>RecordHolder</code>.
    *
    * @return    IMetadata    The metadata object for this <code>RecordHolder</code>.
    */
    public IMetadata getMetadata();
    
    
    
    
    /**
    * Should set the field names for this <code>RecordHolder</code>.
    *
    * @param    fieldNames    The field names for this <code>RecordHolder</code>.
    */
    public void setFieldNames(ArrayList<String> fieldNames);
        
    
    
    
    /**
    * Should get the field names for this <code>RecordHolder</code>.
    *
    * @return    ArrayList    The field names for this <code>RecordHolder</code>.
    */
    public ArrayList<String> getFieldNames();
    
    
    
    
    /**
    * Should set the field types for this <code>RecordHolder</code>.
    *
    * @param    fieldTypes    The field types for this <code>RecordHolder</code>.
    */
    public void setFieldTypes(ArrayList<Class> fieldTypes);    
    
    
    
    
    /**
    * Should get the field types for this <code>RecordHolder</code>.
    *
    * @return    ArrayList    The field types for this <code>RecordHolder</code>.
    */
    public ArrayList<Class> getFieldTypes();
    
    

    
    /**
    * Should set a single <code>IRecord</code> in the holder at index position.
    *
    * @param     index     Location of Record.
    * @param     record    Record to add.
    */
    public void setRecord(int index, IRecord record);
    
    
    
    
    /**
    * Should add a single <code>IRecord</code> to the end of the holder.
    *
    * @param     record    Record to add.
    */
    public void addRecord(IRecord record);
    
    
    
    
    /**
    * Should add a collection of <code>IRecord</code> objects to the end 
    * of the container.
    *
    * @param     records        Records to add.
    */
    public void addRecords(ArrayList<IRecord> records);
    
    
    
    
    /**
    * Should get a single <code>IRecord</code> at an index.
    *
    * @param     index    Location of record.
    * @return     IRecord    Record requested.
    */
    public IRecord getRecord(int index);
    
    
    
    
    /**
    * Should get all <code>IRecord</code> objects.
    *
    * @return     ArrayList    All records.
    */
    public ArrayList<IRecord> getRecords();
    
    

    
}