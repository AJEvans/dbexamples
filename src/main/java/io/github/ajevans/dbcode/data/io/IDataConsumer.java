/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.io;


import java.util.ArrayList;

import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.utilities.IReportingListener;




/**
* Interface for classes that store data.
* <p>
* Note that classes must have a default constructor <em>only</em>, for building 
* into automatic loading systems.
* <p>
* Each store (e.g. a dataset or directory) may have a set of record stores 
* (e.g. tables or files) each containing records (e.g. rows).
* <p>
* Note that exceptions should generally be re-thrown from implementations 
* where not dealt with, allowing users of implementations to deal with the 
* exceptions as part of a broader system. 
* Exception messages should therefore be user friendly.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public interface IDataConsumer {

    
    
    
    //--------MAJOR METHODS-----------------------------------------------------    
    
    
    

    /**
    * Sets up the data store.
    * <p>
    * If a store path and/or record store names haven't been set using the 
    * <code>setSource</code> / <code>setRecordStoreNames</code> methods, this 
    * method should deal with this.  
    * <p>
    * It should then create the relevant store and record stores. It may or may 
    * not wish to check whether they already exist.
    *
    * @param     dataset             Dataset to store - note that this need 
    *                                not be filled with records as long as it 
    *                                has metadata / field data.
    * @throws    DataException       If issues arise.
    */
    public void initialise(IDataset dataset) throws DataException;
    



    /**
    * Method should load a whole dataset into current store. 
    * <p>
    * May or may not wipe prior data. Implementers may 
    * like to call <code>disconnectStore</code> at the 
    * end of processing to disconnect and garbage collect.
    * 
    * @param      dataset          Array of data.
    * @throws     DataException    If there is an issue.
    */
    public void bulkLoad(IDataset dataset) throws DataException;

    
    
    
    /**
    * Appends set of records to current store.
    * <p>
    * This should be used to push small numbers of records to the consumer 
    * in low memory conditions. Implementers are encouraged to limit 
    * links to these objects outside of the method (e.g. immediately writing 
    * them to a database) so the supplier can garbage collect them.
    * 
    * @param      records            ArrayList of data records.
    * @throws     DataException      If there is an issue.
    */
    public void load(ArrayList<IRecord> records) throws DataException;
    
    
    
    
    //--------ACCESSOR / MUTATOR METHODS----------------------------------------    



    
    /**
    * Should set store location to use. 
    * <p>
    * If this isn't set via this method, it should be drawn from defaults 
    * or the dataset in <code>initialise</code>.
    * 
    * @param      store            Store to connect to.
    * @throws     DataException    If there is an issue.
    */
    public void setStore(String store) throws DataException;
    
        
    
    
    /**
    * Should set the record store names for the store. 
    * <p>
    * Record stores within a store will be, e.g. tables within a database or 
    * files within a directory. 
    * <p>
    * If these aren't set via this method, they should be drawn from defaults 
    * or the dataset in <code>initialise</code>. 
    * <p>
    * If set here directly (for example, the user enters the name/s), it is the 
    * implementers responsibility to make sure the number of record stores named 
    * and number supplied in the dataset to <code>initialise</code> match up or 
    * <code>initialise</code> should throw an exception.
    * 
    * @param      recordStoreNames    Names of record stores to connect to.
    * @throws     DataException       Only is there is an issue.
    * @see        #initialise(IDataset dataset)
    */    
    public void setRecordStoreNames(ArrayList<String> recordStoreNames)  
                                                            throws DataException;
    
    
    
    
    /**
    * For objects wishing to get progress reports on data reading.
    *
    * @param     reportingListener    Object wishing to gain reports.
    * @see       io.github.ajevans.dbcode.utilities.IReportingListener
    */
    public void addReportingListener(IReportingListener reportingListener);
    
    

    
    //--------UTILITY METHODS---------------------------------------------------
    
    
    
    
    /**
    * Should connect to current store.
    *
    * @throws     DataException    If there is an issue.
    */
    public void connectStore() throws DataException;
    
    
    
    
    /**
    * Should disconnect from current store and, if present, record stores.
    * <p>
    * Implementations should use this as an opportunity to force a 
    * garbage collection.
    *
    * @throws     DataException    If there is an issue.
    */
    public void disconnectStore() throws DataException;
    

    
            
    //--------DECRECATED METHODS------------------------------------------------
    
    
    
    
    /**
    * Method should load a whole dataset into current store. 
    * <p>
    * May or may not wipe prior data. 
    *
    * 
    * @param     dataset    Array of data.
    * @throws     DataException    If there is an issue.
    * @see #load(ArrayList<IRecord> records)
    * @deprecated
    * This would usually be a synonym for <code>bulkLoad</code> but is 
    * demanded as some stores may have need for alternative loading routes 
    * dependent on e.g. file size (though see 
    * <code>load(ArrayList&lt;IRecord&gt; records)</code>).
    */
    @Deprecated
    public void load(IDataset dataset) throws DataException;
    
    
    
    
}