/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;




/**
* Generic table class containing one or more <code>IRecord</code> objects, 
* usually but not by necessity, <code>Rows</code>. 
* <p>
* As all <code>IRecord</code> objects are held as <code>IRecord</code>, they'll 
* need casting on access.
* <p>
* Maintains field names and types for the records.
* <p>
* Has its own metadata. 
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
* @todo    As records can be used with any IRecordHolder, it probably makes 
*          sense for them to directly link to fields as an independent object 
*          and class at some point.
*/
public class Table implements IRecordHolder {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------    
    
    
    
    
    /**
    * Parent container for the table.
    */
    private IDataset parentDataset = null;


    /**
    * Metadata for the table. 
    */
    private IMetadata tableMetadata = new Metadata();
    
    
    /**
    * Field names for the values in the IRecord objects.
    */
    private ArrayList<String> fieldNames = new ArrayList<>();
    
    
    /**
    * Field types for the values in the IRecord objects.
    */
    private ArrayList<Class> fieldTypes = new ArrayList<>();
    
    
    /**
    * Missing data flags/values for the IRecord objects. 
    * Each value at a specific index (column) in all the IRecord objects 
    * can have a separate specific flag.
    */
    private ArrayList fieldMissingDataFlags = new ArrayList();
    
    
    /**
    * The <code>IRecord</code> objects.
    */
    private ArrayList<IRecord> records = new ArrayList<>();
    
    


    //--------MAJOR METHODS-----------------------------------------------------    




    /**
    * Generic constructor made private to stop floating tables without 
    * a parent dataset. 
    * <p>
    * We need to be able to navigate towards the root 
    * dataset for metadata etc.
    */
    public Table(){}
    
    
    
    
    /**
    * Constructor for table with a parent dataset.
    * <p>
    * This allows us to retrieve parent metadata if needed, as 
    * well as engage in any leaf-to-root tree navigation. 
    *
    * @param     parentDataset    Parent Dataset.
    */
    public Table(IDataset parentDataset) {
        this.parentDataset = parentDataset;
    }
    
    


    //--------ACCESSOR / MUTATOR METHODS----------------------------------------    

    
    
    
    /**
    * Returns the dataset this table is part of.
    *
    * @return     IDataset    Parent dataset.
    */
    public IDataset getParentDataset() {
        return parentDataset;
    }
    
    
    
    
    /**
    * Sets the metadata for this table.
    *
    * @param    tableMetadata    The metadata object for this table.
    */
    public void setMetadata(IMetadata tableMetadata) {
        this.tableMetadata = tableMetadata;
    }
    
    
    
    
    /**
    * Gets the metadata for this table.
    *
    * @return    IMetadata    The metadata object for this table.
    */
    public IMetadata getMetadata() {
        return (Metadata)tableMetadata;
    }
    
    
    
    
    /**
    * Sets the field names for this table.
    *
    * @param    fieldNames    The field names for this table.
    */
    public void setFieldNames(ArrayList<String> fieldNames) {
        this.fieldNames = fieldNames;
    }
        
    
    
    
    /**
    * Gets the field names for this table.
    *
    * @return    ArrayList    The field names for this table.
    */
    public ArrayList<String> getFieldNames() {
        return  fieldNames;
    }
    
    
    
    
    /**
    * Sets the field types for this table.
    *
    * @param    fieldTypes    The field types for this table.
    */
    public void setFieldTypes(ArrayList<Class> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }
    
    
    
    
    /**
    * Gets the field types for this table.
    *
    * @return    ArrayList    The field types for this table.
    */
    public ArrayList<Class> getFieldTypes() {
        return  fieldTypes;
    }
    
    
    
    
    /**
    * Sets the field missing data flags for this table.
    * <p>
    * Each value at a specific index (column) in all the IRecord objects 
    * can have a separate specific flag.
    *
    * @param    fieldMissingDataFlags    The field missing data flags for this table.
    */
    public void setFieldMissingDataFlags(ArrayList fieldMissingDataFlags) {
        this.fieldMissingDataFlags = fieldMissingDataFlags;
    }
    
    
    
    
    /**
    * Gets the field missing data flags for this table.
    *
    * @return    ArrayList    The field missing data flags for this table.
    */
    public ArrayList getFieldMissingDataFlags() {
        return  fieldMissingDataFlags;
    }
    
    
    
    
    /**
    * Sets a single <code>IRecord</code> in the table at index position.
    *
    * @param     index        Location of IRecords.
    * @param     record        Records to add.
    */
    public void setRecord(int index, IRecord record) {
        records.set(index, record);
    }
    
    
    
    
    /**
    * Add a single <code>IRecord</code> to the end of the container.
    *
    * @param     record        Record to add.
    */
    public void addRecord(IRecord record) {
        records.add(record);
    }    
    
    
    
    
    /**
    * Add a set of <code>IRecord</code> objects to the end of the container.
    *
    * @param     records        Records to add.
    */
    public void addRecords(ArrayList<IRecord> records) {
        
        for (IRecord record: records) {
            this.records.add(record);
        }

    }
    
    

    
    /**
    * Get a single <code>IRecord</code> at an index.
    *
    * @param     index        Location of record.
    * @return     IRecord        Record requested.
    */
    public IRecord getRecord(int index) {
        return records.get(index);
    }

    

    
    /**
    * Get all <code>IRecords</code>.
    *
    * @return     ArrayList    All Rows/Records.
    */
    public ArrayList<IRecord> getRecords() {
        return records;
    }
    
    
    
    
}