/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.io;


import java.io.File;
import java.util.ArrayList;

import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.utilities.IReportingListener;




/**
* Interface for classes that supply data.
* <p>
* Note that classes must have a default constructor <em>only</em>, for building 
* into automatic loading systems.
* <p>
* Each source (e.g. a directory) may have a set of record holders 
* (e.g. files) each containing records (e.g. rows).
* <p>
* Note that exceptions should generally be re-thrown from implementations 
* where not dealt with, allowing users of implementations to deal with the 
* exceptions as part of a broader system. 
* Exception messages should therefore be user friendly.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public interface IDataSupplier {

    
    
    
    //--------MAJOR METHODS-----------------------------------------------------    

    
    
    
    /**
    * Should set up the data supplier ready to read data.
    * <p>
    * Should create the relevant internal data structures in 
    * preparation for reading the data, including reading in and parsing 
    * file headers. 
    * <p>
    * Note that this method is kept separate from <code>setSource</code> 
    * to enable piped data processing implementations where a series of 
    * supplies and consumers set up prior to activation.
    * <p>
    * If a source path and record holder names haven't been set using the 
    * <code>setSource</code> / <code>setRecordHolderNames</code> methods, this 
    * method should throw a <code>DataException</code>. 
    *
    * @throws    DataException    If issues arise.
    */
    public void initialise() throws DataException;

    

    
    /**
    * Should get the dataset, which may or may not be empty. 
    * <p>
    * This method allows for a data pull directly by data consumers where data 
    * definitely fits in memory, but also allows access to an empty dataset 
    * set up by an <code>initialise</code> call so callers can interrogate it for 
    * prospective data fields and metadata. At minimum, therefore, 
    * implementations should demand <code>initialise</code> called first.
    * <p>
    * Implementations should require a call to <code>readData</code> first to 
    * load the data into the supplier if the caller needs the data. 
    * <p>
    * Primitives should be boxed.
    * 
    * @return    IDataset        IDataset of data or null if no data.
    * @see #pushData()
    */
    public IDataset getDataset();




    /**
    * Should fill the dataset with data. 
    * <p>
    * Implementations should require a call to <code>initialise</code> first to 
    * set up the internal data structures necessary - this allows callers to 
    * <code>getDataset</code> to gain a dataset containing just metadata and 
    * field information in preparation for any data arriving.
    *
    * @throws     DataException    If there is is an issue - for example, 
    *                                the data needs to be sent piecemeal.
    */
    public void readData()  throws DataException;
        
        
    
    
    /**
    * This method should start the data supplier pushing collections of records 
    * to one or more registered consumers. 
    * <p>
    * This observer push pathway is for situations where memory is low / data 
    * is large. <code>IDataConsumers</code> should be able to register for push 
    * data using <code>addDataListener</code>. Implementers should call 
    * consumer's <code>load(ArrayList&lt;IRecord&gt; records)</code> method when 
    * records are available. 
    * <p>
    * As the active class in the observer pattern, implementers of this 
    * method may like to force a garbage collection to free up 
    * resources after the push.
    *
    * @throws     DataException        If there is is an issue - for example, the 
    *                                  data can only be supplied in one go.
    *
    * @see IDataConsumer#load(ArrayList<IRecord> records)
    * @see #addDataListener(IDataConsumer consumer)
    */
    public void pushData() throws DataException;

    
    

    //--------ACCESSOR / MUTATOR METHODS----------------------------------------    



    
    /**
    * Should set up the data source path as a File object.
    * <p>
    * If not called, <code>initialise</code> should generate a default path 
    * when called.
    *
    * @param    source              Source file to read.
    * @throws   DataException       If issues arise.
    * @todo     For a piping system, add version for streaming.
    */
    public void setSource(File source) throws DataException;




    /**
    * Should gets the source that will be read. 
    * <p>
    * This will be, e.g. a directory. Filenames should be supplied through 
    * <code>setRecordStoreNames</code> to allow multiple files to be read.
    *
    * @return    source                Source file to read.
    */
    public File getSource();
    
    
    
    
    /**
    * Should set the record holder names (e.g. filenames) for the store 
    * (e.g. directory). 
    * <p>
    * Record holders within a source will be, e.g. tables within a database or 
    * files within a directory. 
    * <p>
    * If these names aren't set via this method, they should be drawn from 
    * defaults or the dataset in <code>initialise</code>. 
    * <p>
    * If set here directly (for example, the user enters the name/s), it is the 
    * implementers responsibility to make sure the number of record holders named 
    * and number supplied in the source to <code>initialise</code> match up or 
    * <code>initialise</code> should throw an exception.
    * 
    * @param    recordHolderNames    Names of record holders to connect to.
    * @throws   DataException        When issues arise.
    * @see      #initialise()
    */    
    public void setRecordHolderNames(ArrayList<String> recordHolderNames) 
                                                        throws DataException;
    


    
    /**
    * Should gets the names of source record holders to read.
    *
    * @return    recordHolderNames        ArrayList of names.
    */
    public ArrayList<String> getRecordHolderNames();



        
    /**
    * This method should register a consumer with a supplier for push data.
    *
    * @param     consumer    Consumer to push the data to.
    * @see         IDataConsumer#load(ArrayList<IRecord> records)
    * @see        pushData()
    */
    public void addDataListener(IDataConsumer consumer);
    
    
    
    
    /**
    * For objects wishing to get progress reports on data reading.
    *
    * @param     reportingListener    Object wishing to gain reports.
    * @see       io.github.ajevans.dbcode.utilities.IReportingListener
    */
    public void addReportingListener(IReportingListener reportingListener);




    //--------UTILITY METHODS---------------------------------------------------
    
    
    
    
    /**
    * Should connect to a record holder (e.g. file) in the 
    * current source (directory).
    *
    * @param     index            Index of record holder to connect to in 
    *                             collection set using 
    *                             <code>setRecordHolderNames</code>.
    * @throws    DataException    If there is an issue.
    */
    public void connectSource(int index) throws DataException;
    
    
    
    
    /**
    * Should disconnect from current source and any files.
    * <p>
    * Implementations should use this as an opportunity to force a 
    * garbage collection.
    *
    * @throws     DataException    If there is an issue.
    */
    public void disconnectSource() throws DataException;
    
    
    
    
}