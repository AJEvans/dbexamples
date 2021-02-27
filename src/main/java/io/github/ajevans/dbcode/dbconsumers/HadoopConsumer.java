/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbconsumers;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.MapFile.Writer;
import org.apache.hadoop.io.MapFile.Reader;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.data.structures.IRecordHolder;
import io.github.ajevans.dbcode.data.structures.IMetadata;
import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.IReportingListener;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Class for writing out files to a Hadoop Distributed File System (HDFS).
* <p>
* Works with an <code>IDataset</code> (e.g. database) containing one or more 
* <code>IRecordHolders</code> (e.g. data tables) containing 
* <code>IRecords</code> (e.g. rows) of values. It writes these to a 
* store (in this case a directory) as record stores (files). It will also 
* write out an associated set of metadata files.
* <p>
* <code>IReportingListener</code> objects may be registered with objects of 
* this class to receive suitable progress reporting and messaging. In general, 
* exceptions not dealt with internally are re-thrown for calling objects to 
* deal with. Messages are user-friendly. 
* <p>
* If developers have a specific directory and set of filenames 
* they'd like to use, they should first call <code>setStore</code> 
* and <code>setRecordStoreNames</code>. If these are missing when 
* <code>initialise</code> is finally called, <code>initialise</code> 
* will build a directory in the user's space (called after 
* <code>defaultFileDirectory</code> in <code>application.properties</code>) 
* within which will be a directory named after the dataset or 
* <code>DEFAULT</code> and files named after the record holders or 
* <code>DEFAULTx</code> where <code>x</code> is an integer.
* <p>
* Either way, all developers should call <code>initialise</code> with 
* a dataset that at least has the fields and fieldtypes in it. This will 
* build the file structures with headers and set up progress monitoring. 
* <p>
* They can then get data written to the files by either calling 
* <code>bulkLoad</code> with a complete dataset object or 
* <code>load</code> with a collection of records. 
* <p>
* If you don't want file headers written, calling <code>connectStore</code> will 
* check the directories exist and are ready, but won't set up the files or 
* progress monitoring.
* <p>
* Note that because instance variables will hold a wide variety of information 
* on pervious writes, it is essential that for each new set of files / dataset 
* <strong>a new instance of this class is used</strong>.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class HadoopConsumer implements IDataConsumer {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------
    
    
    
    
    /**
    * Debugging flag, set by System variable passed in <code>-Ddebug=true</code> 
    * rather than setting here / with accessor.
    */
    private boolean debug = false;
    
    
    /**
    * Path to directory used for storage.
    */
    private String store = null;
        
    
    /**
    * Filenames used.
    */
    private ArrayList<String> recordStoreNames = null;
    
    
    /**
    * Listeners interested in updates on progress.
    */
    private ArrayList<IReportingListener> reportingListeners = new ArrayList<>();
    
    
    /**
    * Default directory to create within store.
    */
    private final String DEF_DB_DIR = "dbexamples-databases";    
    
    
    /**
    * Constant for aggresive sanitisation.
    */
    private final int SANITISE_DIRPATH = 0;
    
    
    /**
    * Constant for database object name sanitisation.
    */
    private final int SANITISE_FILENAME = 1;    
    
    
    /**
    * Constant for weak sanitisation to remove a few problematic chars.
    */
    private final int SANITISE_DATA = 2;
    
    
    /**
    * Constant proveLoaded.
    */
    private final boolean METADATA_TABLE = true;
    
    
    /**
    * Constant proveLoaded.
    */
    private final boolean NORMAL_TABLE = false;
    

    /**
    * Used for progress monitoring.
    */
    private int progress = 0; 
    
        
    /**
    * HDFS.
    */
    FileSystem fileSystem = null;
    
    
    /**
    * Configuration.
    */
    Configuration conf = null;
    
    
    
    
    //--------MAJOR METHODS-----------------------------------------------------
    
    
    
    
    /**
    * Default constructor.
    */
    public HadoopConsumer() {
        
        debug = DebugMode.getDebugMode();
        gapFillLocalisedGUIText();
                
    }
        
    

    
    /**
    * Sets up the data store (directory and files).
    * <p>
    * If a store (directory) path and/or record store (file) names haven't been 
    * set using the <code>setSource</code> / <code>setRecordStoreNames</code> 
    * methods, this method deals with this. It pulls the titles from the dataset 
    * passed in for directory name and from the dataset record holders for file 
    * names. The default location for the directory is the user's home 
    * directory where an containing directory (see class docs) will be constructed 
    * first. 
    * All names and paths are sanitised.
    * <p>
    * It then creates the relevant files with appropriate headers.
    *
    * @param     dataset            Dataset to store - note that this need 
    *                               not be filled with records as long as it 
    *                               has recordHolders, metadata, and field data.
    * @throws    DBCreationException      If issues arise.
    */
    public void initialise(IDataset dataset) throws DBCreationException {
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtCreatingFileStruct")
            );
            
            
        String datasetTitle = findTitle(dataset.getMetadata());
        
        // Check and build directory path. If missing, this will be the dataset name.
                
        String fileDirectory = PropertiesSingleton.getInstance().getProperty(
                                    "defaultDatabaseDirectory", DEF_DB_DIR
                                );        
                
        if (store == null) {
            
            setStore(System.getProperty("user.home") + 
                         File.separator + 
                         fileDirectory + 
                         File.separator + 
                         datasetTitle + 
                         File.separator
                     );
            
        } else {
            // NB note we don't add separator to the store here 
            // as store will have already got this going through setStore.
            setStore(store + 
                    fileDirectory + 
                    File.separator + 
                    datasetTitle + 
                    File.separator);
            
        }
        
        // Check and build file names. Recordholders are the individual data 
        // tables (of which a dataset can have multiple). These will be written 
        // as a file each. If filenames not set already, get them from the 
        // record holder metadata or DEFAULT + int if missing.
        

        if (recordStoreNames == null) {

            ArrayList<IRecordHolder> recordHolders = dataset.getRecordHolders();
            ArrayList<String> tempRecordHolderNames = new ArrayList<>(recordHolders.size());
            
            String name = "";
            
            for (int i = 0; i < recordHolders.size(); i++) {
            
                name = findTitle(recordHolders.get(i).getMetadata());
                if (name.equals("DEFAULT")) {
                    name = name + i;
                }
                tempRecordHolderNames.add(name);
                
            }
            
            setRecordStoreNames(tempRecordHolderNames);
            
        } else {
            
            if (recordStoreNames.size() != dataset.getRecordHolders().size()) {
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtHoldersVsNamesIssue")
                    );
            }
            
        }

        // Check the directories exist and call file system. 
        
        try {
            connectStore();
        } catch (DBCreationException dbce) {
           if (debug) dbce.printStackTrace();
           throw dbce;
        }

        
        // Build a metadata file for the dataset.
        
        try {
            buildMetadataFile(dataset.getMetadata(), datasetTitle + "META");
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }

        // Build the files along with field names and types to 
        // store in the files. Build a metadata file for each record holder.

        for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
            
            try {
                buildFile(dataset, i);
                buildMetadataFile(
                        dataset.getRecordHolder(i).getMetadata(), 
                        recordStoreNames.get(i) + "META");
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }
            
            reportMessage(
                    PropertiesSingleton.getInstance().getProperty("txtGeneratedFile") +
                    recordStoreNames.get(i)
                );
           
        }    

        reportMessage(
                    PropertiesSingleton.getInstance().getProperty("txtInDirectory") + 
                    store
                );
         
    }    
      
        
        
        
    /**
    * Sets up the relevant file. 
    *
    * @param     dataset     The dataset containing the file to be built.
    *                        Record holders can be empty of data at this point, but 
    *                        must contain field information and metadata.
    * @param     index       The index of the file to build in 
    *                        <code>recordStoreNames</code>. 
    * @throws    DBCreationException      If issues arise.
    */
    private void buildFile(IDataset dataset, int index) 
                                                  throws DBCreationException {    
        
        
        // Make files and write out file headers.
        
        ArrayList<String> fieldNames = null;
        ArrayList<Class> fieldTypes = null;
        String tableName = null;
        
            
        Path path = new Path(store + recordStoreNames.get(index));
        Boolean result = null;
        

        FSDataOutputStream out = null;
        try {
            out = fileSystem.create(path); 
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileWriteIssue") + 
                    path 
                );
        }

        
        
        // Write out the headers.

        fieldNames = dataset.getRecordHolder(index).getFieldNames();
        fieldTypes = dataset.getRecordHolder(index).getFieldTypes();
        
        if ((fieldNames == null) || (fieldTypes == null)) {
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileHeaderIssue") + 
                    path
                );
        }
        
        if (fieldNames.size() != fieldTypes.size()) {
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFieldNamesIssue") + 
                    path
                );
        }
        
        String fields = "";
        
        for (String fieldName : fieldNames) {
            fields = fields + fieldName + ",";
        }
        
        fields = fields.substring(0, fields.length() - 1); // Remove last comma.
        
        try {
            out.writeBytes(fields);
            out.writeBytes(System.lineSeparator()); 
            out.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileHeaderWriteIssue") + 
                    path
                );
        }
         
    }
    
    
    
    
    /**
    * Sets up a metadata MapFile for each record holder file. 
    * <p>
    * MapFiles are, despite their name, a directory of interrelated files, 
    * so this is what is produced. Note that the MapFile data will be 
    * sorted alphabetically by metadata category, as is usual with these 
    * files for fast searches.
    *
    * @param    metadata             The metadata to be written to the metadata 
    *                                file.
    * @param    metadataFileName     The name of the metadata file.
    * @throws   DBCreationException  If issues arise.
    */
    private void buildMetadataFile(IMetadata metadata, String metadataFileName) 
                                                throws DBCreationException {
        
        
        ArrayList[] metadataArrays = metadata.getAll();
        
        ArrayList<String> fieldNames = metadataArrays[0];
        ArrayList<Class> fieldTypes = metadataArrays[1];
        ArrayList<String> fieldDefs = new ArrayList<>(fieldTypes.size());
        ArrayList fieldValues = metadataArrays[2];
        

        String path = store + metadataFileName;

        Text key = new Text();
        Text value = new Text();
        MapFile.Writer writer = null;
        
        // Open the MapFile for writing.
        
        try {
            writer = new MapFile.Writer(conf, fileSystem, path, key.getClass(), value.getClass());
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtHadoopWriteIssue") + 
                    path
                );
        }
       
        // First we need to extract the metadata from the dataset and 
        // convert it to a writable format. In the case of a MapFile this 
        // also needs to be sorted, so we store it in a hashtable and 
        // sort this with a TreeMap.
        
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        
        String fieldValue = "";
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        // Run through the records converting the Java types to 
        // suitable text.

        String fieldName= null;
        
        String txtMissingMetadata = 
            PropertiesSingleton.getInstance().getProperty("txtMissingMetadata");    
            
        for (int i = 0; i < fieldNames.size(); i++) {

            // Just incase, but most default to "".
            if (fieldValues.get(i) == null) continue;
            
            fieldName = fieldNames.get(i);

            String classString = fieldTypes.get(i).getName(); 
            
            try {
                fieldValue = switch(classString) {
                    
                    case "java.lang.String" -> (String)fieldValues.get(i); 
                    
                    case "java.lang.Integer" -> fieldValues.get(i).toString();
                    
                    case "java.util.GregorianCalendar" -> sdf.format(
                                ((GregorianCalendar)fieldValues.get(i)).getTime()
                                );
                    
                    case "java.math.BigDecimal" -> fieldValues.get(i).toString();
                    
                    default -> fieldValues.get(i).toString();
                    
                };
            } catch (Exception e) {
                if (debug) e.printStackTrace();
                throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtMetaReadFileIssue") + 
                    path
                );
            }

            // For multi-line lines, replace the linebreaks with pipe symbols.
            fieldValue = fieldValue.replaceAll("\\r\\n|\\r|\\n", " | " );
            if (fieldValue.equals("")) fieldValue = txtMissingMetadata;
         
            hashtable.put(fieldName, fieldValue);
            
        }
         
        // Sort the keys-value pairs.
        
        TreeMap<String, String> treeMap = new TreeMap<String, String>(hashtable); 

        // And now write to the file. 

        for (java.util.Map.Entry<String,String> entry : treeMap.entrySet()) {
 
            String treeMapKey = entry.getKey();
            String treeMapValue = entry.getValue();

            try {
                key.set(treeMapKey);
                value.set(treeMapValue);
                writer.append(key, value);
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtMetaWriteFileIssue") + 
                    path
                );
            }

        }

        reportMessage("Generated metadata file: " + metadataFileName);
        

        try {
            writer.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtCloseFileIssue") + 
                    path
                );
        }
        
       // Report first and last lines.  
       proveLoaded(metadataFileName, METADATA_TABLE);

    }
    
    

    
    /**
    * This method bulk-loads a whole dataset into one or more files.
    * <p>
    * This method calls <code>disconnectStore</code> when done to clean up.
    *
    * @param      dataset                  The dataset to load.
    * @throws     DBCreationException      If there's an issue.
    */
    public void bulkLoad(IDataset dataset) throws DBCreationException {
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtLoading")
            );
        
        // Check the directories exist and call file system. 
        // Should do if initialise called, but isn't always.
        try {
            connectStore();
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
        
        IRecordHolder dataTable = null;
        ArrayList<IRecord> records = null;
        
        // A given dataset may have several record holders (data tables).
        
        try { 
            for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
            
                dataTable = dataset.getRecordHolders().get(i);
                records = dataTable.getRecords();
                
                // Write the current record holder / data table to the relevant file.        
                storeRecords(records);
                
                proveLoaded(recordStoreNames.get(i), NORMAL_TABLE);
                
            }
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
        
                
        // Zero progress.
        reportProgress(0, 1);
        
        // Disconnect and garbage collect.
        try {
            disconnectStore();
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
        
                
    }
    
  
  
  
    /**
    * Adds multiple records to current store.
    * <p>
    * This little at the moment than call <code>storeRecords</code> with 
    * the records, but we keep it as a separate method as it acts as a gateway 
    * for data in the supplier/consumer push model and we may want to add 
    * functionality to the gateway in the future.
    * 
    * @param      records                 ArrayList of records.
    * @throws     DBCreationException     Only if there is an issue.
    */
    public void load(ArrayList<IRecord> records) throws DBCreationException {
        
        IRecordHolder table = records.get(0).getParentRecordHolder();        
        IDataset dataset = table.getParentDataset();
        int totalEstimate = dataset.getEstimatedRecordCount();
        
        try {
            storeRecords(records);
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
        
        // We do a garbage collection here to free up resources .
        Runtime.getRuntime().gc();

        // If all data with us, zero progress.
        if (progress >= totalEstimate) {
            reportProgress(0, 1);
            // Disconnect and garbage collect.
            try {
                disconnectStore();
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }
            // Not needed, but just to prove it is there for this application use.
            for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
                proveLoaded(recordStoreNames.get(i), NORMAL_TABLE);
            }
            
        }

    }    




    /**
    * Add a set of records to the relevant record store (file). 
    *
    * These are standard Hadoop flat files. Dates are US MM/dd/yyyy format.
    *
    * @param     records                    An ArrayList of the IRecords to add.
    * @throws    DBCreationException        Only if there is an issue.
    */
    private void storeRecords(ArrayList<IRecord> records) throws DBCreationException {
        
        // Get the field types for casting objects to strings.
        
        ArrayList<Class> fieldTypes = 
                        records.get(0).getParentRecordHolder().getFieldTypes();

        // Find out which table of data in the datset we are dealing with,
        // then retrieve its name so we can write to the correct file.
        
        IRecordHolder table = records.get(0).getParentRecordHolder();
        IDataset dataset = table.getParentDataset();
        int tableIndex = dataset.getRecordHolders().indexOf​(table);
        String tableName = recordStoreNames.get(tableIndex);

        // Prepare to write to the file.
        
        Path path = new Path(store + tableName);

        // Appending doesn't seem to work for non-native Hadoop classes,
        // at least on Windows, where there's known file access bugs,
        // so unfortunately we need to copy the header ourselves (and, 
        // indeed, the whole contents for push mode).
        
        ArrayList<String> headerAndData = new ArrayList<>();
        String oldLine = null;
        try {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(fileSystem.open(path)));
            while ((oldLine = buffer.readLine()) != null){
                headerAndData.add(oldLine);  
            }
            buffer.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtRecordWriteIssue") + 
                    path 
                );
        }
        
        // We can now recreate the file and write out the header.
        
        FSDataOutputStream out = null;
        try {
            out = fileSystem.create(path); 
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtRecordWriteIssue") + 
                    path 
                );
        }

        try {
            for (String oldLineOut: headerAndData) {
                out.writeBytes(oldLineOut);
                out.writeBytes(System.lineSeparator());
            }
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtRecordWriteIssue") + 
                    path
                );
        }

        
        // Loop through the record, getting values, 
        // converting to Strings and writing to file.
        
        String stringToWrite = "";
        
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // ISO 8601 
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy"); // US format
        
        for (int j = 0; j < records.size(); j++) {

            ArrayList values = records.get(j).getValues();
        
            stringToWrite = "";
            
            // Build up the string to write, casting where need be 
            // based on the value's data type.
        
            for (int k = 0; k < values.size(); k++) {
            
                try {
                    
                    String classString = fieldTypes.get(k).getName();

                    stringToWrite = switch(classString) {
                        case "java.lang.String" -> stringToWrite + 
                                                   sanitise((String)values.get(k), 
                                                   SANITISE_DATA) + 
                                                   ",";
                                                   
                        case "java.lang.Integer" -> stringToWrite + 
                                                    values.get(k).toString() + 
                                                    ",";
                                                    
                        case "java.util.GregorianCalendar" -> stringToWrite + 
                                                    sdf.format(
                                                    ((GregorianCalendar)values.get(k)).getTime()
                                                    ) + ",";
                                                    
                        case "java.math.BigDecimal" -> stringToWrite + 
                                                    values.get(k).toString() + 
                                                    ",";
                                                    
                        default -> stringToWrite + values.get(k) + ",";
                    };
                    
                    
                } catch (RuntimeException re) {
                    if (debug) re.printStackTrace();
                     throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtConvertIssue") + 
                            tableName + " -> " + (String)values.get(k)
                        );                                
                }    
            }
                
           
            
            // Write string to file.
            
            // Remove last comma.
            stringToWrite = stringToWrite.substring(0, stringToWrite.length() - 1); 

            
            try {
                out.writeBytes(stringToWrite);
                out.writeBytes(System.lineSeparator());
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtRecordWriteIssue") + 
                        path
                    );
            }
            
            // Progress monitoring.
            progress++;
            reportProgress(progress, dataset);
            
        }

       
        
        try {
            out.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtCloseFileIssue") + 
                    path
                );
        }
        
        
    }

    
    
    
    //--------ACCESSOR / MUTATOR METHODS----------------------------------------
        
    
    

    /**
    * Sets the location for the store / directory to write to. 
    * <p>
    * If not set the parameters can be set from the dataset 
    * via the <code>initialise</code> method.
    * <p>
    * Sanitises input with SANITISE_DIRPATH.
    * 
    * @param    store                       File path of directory to write to. 
    * @throws   DBCreationException         Not used in this implementation.
    * @see      #initialise(IDataset dataset)
    */    
    public void setStore(String store) throws DBCreationException {
        
        if (store == null || store.equals("")) {
            this.store = null;
            return;
        }
        
        // Store is directory to write files to.
        
        if (!store.endsWith(File.separator)) {
            store = store + File.separator;
        }
        
        this.store = sanitise(store, SANITISE_DIRPATH);
        
    }
    
    
    
    
    /**
    * Gets the location for the store / database. 
    * <p>
    * For testing.
    *
    * @return   String      Store location.
    */
    public String getStore() {
        return store;
    }
    
    
    
    
    /**
    * Sets the record store (file) names for the store (directory). 
    * <p>
    * If not set the parameters can be set from the dataset 
    * via the <code>initialise</code> method. 
    * <p>
    * Sanitises input with SANITISE_FILENAME.
    * <p>
    * If set here directly (for example, the user 
    * enters the name/s) it's the implementers responsibility to make sure 
    * the number of record stores named and number supplied in the dataset 
    * to <code>initialise</code> match up or <code>initialise</code>
    * will throw an exception.
    * 
    * @param    recordStoreNames           Names of record stores (files) to make.
    * @throws   DBCreationException  Not used in this implementation.
    * @see      #initialise(IDataset dataset)
    */    
    public void setRecordStoreNames(ArrayList<String> recordStoreNames) 
                                              throws DBCreationException {

        // recordStoreNames    is the filenames to use for the datafiles. 
        
        if (recordStoreNames == null) {
            this.recordStoreNames = null;
            return;
        }
        
        ArrayList<String> temp = new ArrayList<>(recordStoreNames.size());
        
        recordStoreNames.forEach(recordHolderName -> {
                temp.add(sanitise(recordHolderName, SANITISE_FILENAME)); 
        });
        
        this.recordStoreNames = temp;

    }

    
    
    
    /**
    * Gets the record store names. 
    * <p>
    * For testing.
    *
    * @return   ArrayList   Record store names.  
    */
    public ArrayList<String> getRecordStoreNames() {
        return recordStoreNames;
    }


   
   
    /**
    * For objects wishing to get progress reports on data reading.
    *
    * @param     reportingListener    Object wishing to gain reports.
    * @see       io.github.ajevans.dbcode.utilities.IReportingListener
    */
    public void addReportingListener(IReportingListener reportingListener){ 
        reportingListeners.add(reportingListener);
    }
    
    
     
     
    //--------UTILITY METHODS---------------------------------------------------
    
    
    
    
    /**
    * Finds the title category in an unknown IMetadata object. 
    * <p>
    * If it doesn't exist, defaults to "DEFAULT".
    *
    * @param     metadata      Metadata object of unknown schema.
    * @return    String        Title, if found.
    */
    protected String findTitle(IMetadata metadata) {
        // "protected" for tests.
        
        int count = 0;
        String defaultName = "DEFAULT";
        
        if (metadata == null) {
            return defaultName;
        }
        
        // Scan through all the metadata catagories looking for 'title'.
        // arrayLists[0] == categories
        // arrayLists[2] == values
        ArrayList[] arrayLists = metadata.getAll();
        String name = null;
        for (int i = 0; i < arrayLists[0].size(); i++) {
            if (((String)arrayLists[0].get(i)).equals("title")) {
                name = (String)arrayLists[2].get(i);
                break;
            }
        }
        if (name == null) return defaultName;
        return name;
        
    }
    
    
    
    
    /**
    * Creates data directory and directory it is in if missing, and calls 
    * HDFS.
    *
    * @throws     DBCreationException    If there's an issue.
    */
    public void connectStore() throws DBCreationException {
                
        try {
            File directory = new File(store);

            if(!directory.getParentFile().exists()){

                 directory.getParentFile().mkdir();
                
            }
            
            if(!directory.exists()){

                 directory.mkdir();
                
            }
            
            conf = new Configuration();
            conf.setBoolean("dfs.support.append", true);
          
            try {
                fileSystem = FileSystem.get(conf);
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtHadoopFSIssue") + 
                    store
                );
            }
            
            
        } catch (SecurityException se) {
             if (debug) se.printStackTrace();
             throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtDirWriteIssue") + 
                    store
                );
        }

    }
    
    
    
    
    /**
    * Disconnects from current store / database and garbage collects.
    *
    * @throws     DBCreationException     Not thrown in this implementation.
    */
    public void disconnectStore() throws DBCreationException {
        try {
            fileSystem.close();
        } catch (IOException ioe) {
            // Unlikely to be an issue if the database exists, 
            // and associated conditions around this are caught before.
            if (debug) ioe.printStackTrace();
        }
        Runtime.getRuntime().gc();
    }
        
    
        
        
    /**
    * Sanitise Strings.
    * <p>
    * Directory path sanitises removes characters that are illegal in windows filenames, 
    * but leaves slashes etc. If these are likely, remove these using filename 
    * sanitisation on component parts.<br> 
    * Filename sanitisation removes all  
    * <a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file">illegal 
    * windows filename characters</a> and slashes for POSIX systems. It also 
    * adds <code>"Data-"</code> to  
    * <a href="https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file">illegal 
    * windows filenames</a><br> and removes trailing periods and spaces.<br>
    * Data sanitisation just removes commas in preparation for writing as CSV.
    *
    * @param    string   String to sanitise.
    * @param    level    One of SANITISE_DIRPATH, SANITISE_FILENAME, SANITISE_DATA.
    * @return   String   Sanitised String.
    */
    protected String sanitise(String string, int level) {
        // "protected" for tests.
        
        // To adjust, see list at:
        // https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html

        String illegalFilenamesWin = "CON PRN AUX NUL COM1 COM2 COM3 COM4 COM5" +
            "COM6 COM7 COM8 COM9 LPT1 LPT2 LPT3 LPT4 LPT5 LPT6 LPT7 LPT8 LPT9";
                
        switch(level) {
            case SANITISE_DIRPATH -> string = string.replaceAll("[<>\"|?*]","-");

            case SANITISE_FILENAME -> {
                string = string.replaceAll("[<>:\"/\\|?*]","-");
                if (string.lastIndexOf(".") == string.length() - 1) {
                    string = string.substring(0,string.length() - 1);
                }
                if (string.lastIndexOf(" ") == string.length() - 1) {
                    string = string.substring(0,string.length() - 1);
                }
                if (illegalFilenamesWin.contains(string)) string = "Data-" + string; 

            }
            case SANITISE_DATA -> string = string.replaceAll("[,]",";");
            default -> string = "";
        };

        return string;
    }
    
    
    
    
    /**
    * Sets the defaults for warnings and exceptions in English if an appropriate 
    * language properties file is missing.
    *
    */
    private void gapFillLocalisedGUIText() {
        
        Properties defaults = new Properties();
        
        // Warnings and messages.
        
        defaults.setProperty("txtLoading", "Loading data.");
        defaults.setProperty("txtCreatingFileStruct", "Creating directories and files.");
        defaults.setProperty("txtGeneratedFile", "Generated file: ");
        defaults.setProperty("txtInDirectory", "...in directory: ");
        defaults.setProperty("txtFileHeaderIssue", "Unable to find header data " + 
                                                   "for file: "
                                                );
        defaults.setProperty("txtFileHeaderWriteIssue", "Please check data " + 
                                                        "headers; issue with " +
                                                        "writing header to file: "
                                                    );
        defaults.setProperty("txtFileWriteIssue", "Cannot write to file. Please " + 
                                                  "check you have permission to " + 
                                                  "write the file: "
                                                );
        defaults.setProperty("txtMetaReadFileIssue", "Please check associated " + 
                                                     "file header; issue writing " + 
                                                     "metadata into metadata file: "
                                                );        
        defaults.setProperty("txtMetaWriteFileIssue", "Issue writing metadata to " +
                                                      "file; please check you have " + 
                                                      "permission to write to: "
                                                );
        defaults.setProperty("txtCloseFileIssue", "Issue closing file: ");
        defaults.setProperty("txtRecordWriteIssue", "Issue writing records to a " + 
                                                    "file; please check you have " + 
                                                    "permission to write to: "
                                                );
        defaults.setProperty("txtDirWriteIssue", "Issue writing to a directory; " + 
                                                 "please check you have permission " + 
                                                 "to write to: "
                                            );
        defaults.setProperty("txtFileWriteIssue", "There's an issue writing a " + 
                                                 "file, please check you have " + 
                                                 "permission to write to: "
                                            );
                                            
        defaults.setProperty("txtHoldersVsNamesIssue", "Number of record store " +
                                                       "names and number of record " + 
                                                       "holders to store do not match." 
                                                    );

        defaults.setProperty("txtFieldNamesIssue", "Number of fieldnames does " + 
                                                  "not match number of fieldtypes " + 
                                                  "in: "
                                                );

        defaults.setProperty("txtConvertIssue", "Issue converting a value, please " + 
                                               "check the following: "
                                            );   
                                            
        defaults.setProperty("txtHadoopWriteIssue", "Problem writing to Hadoop file; " + 
                                                "please check you have permission " + 
                                                "to write to: "
                                            );   
        
        defaults.setProperty("txtHadoopReadIssue", "Problem reading from Hadoop file; " + 
                                                "please check you have permission " + 
                                                "to read from: "
                                            );  
        
        defaults.setProperty("txtHadoopFSIssue", "Problem opening hadoop; please " + 
                                                "make sure you can run it on your " + 
                                                "system and you have permission " + 
                                                "to access: "
                                            );            
        
        defaults.setProperty("txtHadoopCloseIssue", "Problem closing hadoop file; " + 
                                                "please make sure you can run it " + 
                                                "on your system and you have " + 
                                                "permission to access: "
                                            );  
        defaults.setProperty("txtMissingMetadata", "MISSINGFROMDATASET");                                     
        PropertiesSingleton.getInstance().setDefaults(defaults);
        
    }
    

    
    
    /**
    * Reports progress to reportingListeners.
    * <p>
    * Reports if progress is a multiple of total records / 100. 
    * If progress is zero or less, reports progress as 0 of 1.
    * 
    * @param        progress        Progress in record processing.
    * @param        dataset         Dataset to extract estimate of processing 
    *                               to be done.
    */
    public void reportProgress(int progress, IDataset dataset) {
        int totalEstimate = dataset.getEstimatedRecordCount();
        
        if (progress <= 0) {
            for (IReportingListener reportingListener : reportingListeners) {
                reportingListener.updateAppProgress(0, 1);
            }
            return;
        }    
        
        if ((totalEstimate > 0) && ((progress % (totalEstimate / 100)) == 0)) {
            for (IReportingListener reportingListener : reportingListeners) {
                reportingListener.updateAppProgress(progress, totalEstimate);
            }
        }
                
    }
    
    
    
    
    /**
    * Reports progress to reportingListeners.
    * <p>
    * Reports for an arbitrary progress and total worked towards.
    *
    * @param        progress        Value indicating progress through work total.
    * @param        total           Value indicating total work to do.
    */
    public void reportProgress(int progress, int total) {
       
        for (IReportingListener reportingListener : reportingListeners) {
            reportingListener.updateAppProgress(progress, total);
        }
            
    }
    
    
    
    
    /**
    * Reports message to reportingListeners.
    * 
    * @param        message         Message to reporting listeners.
    */
    public void reportMessage(String message) {
        
        for (IReportingListener reportingListener : reportingListeners) {
            reportingListener.updateAppMessage(message);
        }
      
    }

    
    
        
    //--------DEPRECATED METHODS------------------------------------------------
    
    
    
    
    /**
    * Adds a dataset to the relevant file in the store directory.
    * <p>
    * @deprecated
    * Only here to satisfy deprecated interface demands conditional on 
    * other classes. Redirects to bulkLoad.
    *
    * @param     dataset                        Dataset to load.
    * @throws    DBCreationException      Only if there is an issue.
    */
    @Deprecated
    public void load(IDataset dataset) throws DBCreationException {
        
        bulkLoad(dataset);

    }    




    /**
    * Messages out to ReportingListeners the first and last entries in the named 
    * table.
    * <p>
    * Will disconnect and connect to current store to prove persistence.
    *
    * @deprecated   Not needed for this class, but a convenient add-in for  
    *               proving it works without the need for unit testing or 
    *               database interrogation software.
    * @param        tableName               Table to use. 
    * @param        metadataTable           One of METADATA_TABLE or NORMAL_TABLE.
    * @throws       DBCreationException     If an issue with connecting.
    */    
    @Deprecated
    private void proveLoaded (String tableName, boolean metadataTable) throws DBCreationException {


        ArrayList<String> lines = new ArrayList<>();

        // Did implement this as a MapReduce job, but unfortunately 
        // it runs up against the file permission bug in Hadoop on Windows 
        // and seems to default to the Hadoop version of Hadoop's FileUtil, 
        // despite efforts to rewrite it to remove the issues. Will 
        // have to stay as a Map.Reader solution for now.
        if (metadataTable) {
            
            String path = store + tableName;
            try {
                disconnectStore();
                connectStore();
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }
            
            MapFile.Reader reader = null;
            
            try {
                reader = new MapFile.Reader(fileSystem, path, conf);
                Text outKey = new Text();
                Text outValue = new Text();
                reader.next(outKey, outValue);
                String line = outKey.toString() + " | " + outValue.toString();
                String lastLine = "";
                while (reader.next(outKey, outValue)) {
                    lastLine = outKey.toString() + " | " + outValue.toString();
                }
                lines.add(line);
                lines.add(lastLine);
                
                
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtHadoopReadIssue") + 
                    store
                );
            }

        } else {

            Path path = new Path(store + tableName);
            try {
                disconnectStore();
                connectStore();
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }

            
            BufferedReader buffer = null;
            String line = null;
            
            try {
                
                buffer = new BufferedReader(new InputStreamReader(fileSystem.open(path)));
                  
                // Find first and last lines.
                  
                line = buffer.readLine();  // Header 
                String lastLine = null;
                line = buffer.readLine();
                line = line.replace(","," | ");
                lines.add(line);
                while (line != null){
                    lastLine = line;
                    line = buffer.readLine();
                }
                lastLine = lastLine.replace(","," | ");
                lines.add(lastLine);
              
            } catch (IOException ioe) {

                if (debug) ioe.printStackTrace();
                throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtHadoopReadIssue") + 
                    store
                );
            
            } finally {
                try {
                    buffer.close();
                } catch (IOException ioe2) {
                    if (debug) ioe2.printStackTrace();
                    throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtHadoopCloseIssue") + 
                        store
                    );
                }
            }
        
        
        }
        

        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtTestOutput") + tableName + 
                System.lineSeparator() +
                lines.get(0) +
                System.lineSeparator() +
                lines.get(1) 
            );

        
    }



    
}