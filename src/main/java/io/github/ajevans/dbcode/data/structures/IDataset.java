/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;




/**
* Interface for classes that will contain one or more indexed objects 
* implementing <code>IRecordHolder</code>.
* <p>
* Dataset's directly equivalent sources might include, e.g. directories where 
* the contained files are the record holders to be, or a database holding tables. 
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public interface IDataset {
        
    
    
    
    /**
    * Should set the metadata for this <code>IDataset</code>.
    *
    * @param    metadata    The metadata object for this <code>IDataset</code>.
    */
    public void setMetadata(IMetadata metadata);
    
    
    
    
    /**
    * Should get the metadata for this <code>IDataset</code>.
    *
    * @return    IMetadata    The metadata object for this <code>IDataset</code>.
    */
    public IMetadata getMetadata();
    
    
    
    
    /**
    * Should set the total records held - or to be held - in this dataset.
    * <p>
    * For data streaming this may be a calculation of the estimated number of 
    * records to be held rather than the actual records, hence the name. 
    *
    * @param    estimatedRecordCount    Number of records or estimate.
    */
    public void setEstimatedRecordCount(int estimatedRecordCount);
    
    
    
    
    /**
    * Should get the total records held - or to be held - in this dataset.
    * <p>
    * For data streaming this may be a calculation of the estimated number of 
    * records to be held rather than the actual records, hence the name - it 
    * should only be used for processing progress estimations, not drawing on 
    * data. For the latter, use 
    * <code>getRecordHolders().getRecords().get(i).size()</code>.
    *
    * @return    int    Number of records or estimate.
    */
    public int getEstimatedRecordCount();
    
    
    

    /**
    * Should set a single <code>RecordHolder</code> at an index.
    *
    * @param     index                Location to add RecordHolder.
    * @param     recordHolder        RecordHolder to add.
    */
    public void setRecordHolder(int index, IRecordHolder recordHolder);
    
    
    
    
    /**
    * Should add a single <code>RecordHolder</code> to the end of the container.
    *
    * @param     recordHolder    RecordHolder to add.
    */
    public void addRecordHolder(IRecordHolder recordHolder);
    
    
    
    
    /**
    * Should add a collection of <code>IRecordHolder</code> objects to the end of 
    * the container.
    *
    * @param     recordHolders        Record holders to add.
    */
    public void addRecordHolders(ArrayList<IRecordHolder> recordHolders);




    /**
    * Should get a single <code>RecordHolder</code> at an index.
    *
    * @param     index                Location of RecordHolder.
    * @return     IRecordHolder    RecordHolder requested.
    */
    public IRecordHolder getRecordHolder(int index);
    
    
    
    
    /**
    * Should get all <code>RecordHolder</code> objects.
    *
    * @return     ArrayList     All RecordHolder as a collection.
    */
    public ArrayList<IRecordHolder> getRecordHolders();
    
    

    
}