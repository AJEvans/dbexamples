/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.*;




/**
* Dataset containing one or more <code>IRecordHolder</code> objects, usually but 
* not by necessity, <code>Table</code> objects. 
* <p>
* As all <code>IRecordHolder</code> objects are held as 
* <code>IRecordHolder</code>, they'll need casting on access.
* <p>
* Has its own metadata, as well as any <code>IRecordHolder</code> objects 
* having their own.
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class TabulatedDataset implements IDataset {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------    
    
    
    

    /**
    * Metadata for the tabulated dataset. 
    */
    private IMetadata datasetMetadata = new Metadata();



    
    /**
    * The <code>IRecordHolder</code> objects.
    */
    private ArrayList<IRecordHolder> recordHolders = new ArrayList<>();
    
    
    
    
    /**
    * Total records held - or to be held - in this dataset. 
    * Used for progress monitoring only.
    */
    private int estimatedRecordCount;
    
    
    
    
    //--------MAJOR METHODS-----------------------------------------------------    
    


    
    /**
    * Generic constructor.
    */
    public TabulatedDataset() {}
    
    


    //--------ACCESSOR / MUTATOR METHODS----------------------------------------    

    
    
    
    /**
    * Sets the total records held - or to be held - in this dataset.
    * <p>
    * For data streaming this may be a calculation of the estimated number of 
    * records to be held rather than the actual records, hence the name. 
    *
    * @param    estimatedRecordCount    Number of records or estimate.
    */
    public void setEstimatedRecordCount(int estimatedRecordCount) {
        this.estimatedRecordCount = estimatedRecordCount;
    }
    
    
    
    
    /**
    * Gets the total records held - or to be held - in this dataset.
    * <p>
    * For data streaming this may be a calculation of the estimated number of 
    * records to be held rather than the actual records, hence the name - it 
    * should only be used for processing progress estimations, not drawing on 
    * data. For the latter, use 
    * <code>getRecordHolders().getRecords().get(i).size()</code>.
    *
    * @return    int        Number of records or estimate.
    */
    public int getEstimatedRecordCount(){
        return estimatedRecordCount;
    }
    
    
    
    
    /**
    * Sets the metadata for this dataset.
    *
    * @param    datasetMetadata        The metadata object for this dataset.
    */
    public void setMetadata(IMetadata datasetMetadata) {
        this.datasetMetadata = datasetMetadata;
    }
    
    
    
    
    /**
    * Gets the metadata for this dataset.
    *
    * @return    IMetadata    The metadata object for this dataset.
    */
    public IMetadata getMetadata() {
        return datasetMetadata;
    }
    
    
    
    /**
    * Set a single <code>IRecordHolder</code> at an index.
    *
    * @param     index                Location to add RecordHolder.
    * @param     recordHolder        RecordHolder to add.
    */
    public void setRecordHolder(int index, IRecordHolder recordHolder) {
        recordHolders.set(index, recordHolder);
    }
        
    
    
    
    /**
    * Add a single <code>IRecordHolder</code> to the end of the container.
    *
    * @param     table    RecordHolder to add.
    */
    public void addRecordHolder(IRecordHolder table) {
        recordHolders.add(table);
    }
    



    /**
    * Add a collection of <code>IRecordHolder</code> objects to the end of 
    * the container.
    *
    * @param     recordHolders        Record holders to add.
    */
    public void addRecordHolders(ArrayList<IRecordHolder> recordHolders) {
        
        for (IRecordHolder recordHolder: recordHolders) {
            this.recordHolders.add(recordHolder);
        }

    }
    
    
    
    
    /**
    * Get a single <code>IRecordHolder</code> at an index.
    *
    * @param      index                Location of RecordHolder.
    * @return     IRecordHolder        IRecordHolder requested.
    */
    public IRecordHolder getRecordHolder(int index) {
        return recordHolders.get(index);
    }
    
    
    
    /**
    * Get all <code>IRecordHolder</code> objects.
    *
    * @return     ArrayList    IRecordHolder collection requested.
    */
    public ArrayList<IRecordHolder> getRecordHolders() {
        return recordHolders;
    }

    
    

}
    
    
    