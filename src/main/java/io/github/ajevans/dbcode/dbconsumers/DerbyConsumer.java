/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbconsumers;


import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;            // Only used in proveLoaded.
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;                  // Only used in proveLoaded.
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IMetadata;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.data.structures.IRecordHolder;
import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.IReportingListener;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Class for connecting to and manipulating a Derby database.
* <p>
* Derby is an Open Source Apache Software Foundation database. It runs without 
* installation, and has a small footprint, making it a good choice for bundling 
* with applications. For more information see the 
* <a href="https://db.apache.org/derby/">Derby website</a>.
* <p>
* This class works with an <code>IDataset</code> (e.g. directory) containing one  
* or more <code>IRecordHolders</code> (e.g. files) containing 
* <code>IRecords</code> (e.g. rows) of values. It writes these to a 
* store (in this case a database) as record stores (tables), along with 
* an appropriate set of metadata tables.
* <p>
* <code>IReportingListener</code> objects may be registered with objects of 
* this class to receive suitable progress reporting and messaging. In general, 
* exceptions not dealt with internally are re-thrown for calling objects to 
* deal with. Messages are user-friendly. 
* <p>
* If developers have a specific database and set of table names  
* they'd like to use, they should first call <code>setStore</code> 
* and <code>setRecordStoreNames</code>. If these are missing when 
* <code>initialise</code> is finally called, <code>initialise</code> 
* will build a directory in the user's space (called after 
* <code>defaultDatabaseDirectory</code> in <code>application.properties</code>) 
* within which will be a database named after the dataset or 
* <code>DEFAULT</code> and tables named after the record holders or 
* <code>DEFAULTx</code> where <code>x</code> is an integer.
* <p>
* Either way, all developers should call <code>initialise</code> with 
* a dataset that at least has the fields and fieldtypes in it. This will 
* build the database structures with headers and set up progress monitoring. 
* <p>
* They can then get data written to the database by either calling 
* <code>bulkLoad</code> with a complete dataset object or 
* <code>load</code> with a collection of records. 
* <p>
* Note that because instance variables will hold a wide variety of information 
* on pervious writes, it is essential that for each new set of files / dataset 
* <strong>a new instance of this class is used</strong>.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class DerbyConsumer implements IDataConsumer {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------
    
    
    
    
    /**
    * Connection for SQL requests.
    */
    private Connection connection = null;
    
    
    /**
    * Debugging flag, set by System variable passed in <code>-Ddebug=true</code> 
    * rather than setting here / with accessor.
    */
    private boolean debug = false;
    
    
    /**
    * Path to database used.
    */
    private String store = null;
    
    
    /**
    * Record store (table) names used.
    */
    private ArrayList<String> recordStoreNames = null;
    
    
    /**
    * Default directory to create within store.
    */
    private final String DEF_DB_DIR = "dbexamples-databases";
    
    /**
    * Default directory for temp files.
    */
    private final String DEF_FILE_DIR = "dbAppTempFiles-youCanDelete";
    
    
    /**
    * Constant for aggresive sanitisation.
    */
    private final int SANITISE_VIGOROUS = 0;
    
    
    /**
    * Constant for database object name sanitisation.
    */
    private final int SANITISE_NAME = 1;    
    
    
    /**
    * Constant for weak sanitisation to remove a few problematic chars.
    */
    private final int SANITISE_WEAK = 2;
    
    
    /**
    * Constant for sql statement running.
    */
    private final boolean INSERT_SMT = true;
    
    
    /**
    * Constant for sql statement running.
    */
    private final boolean CREATE_SMT = false;
        
    
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
    * Listeners interested in updates on progress.
    */
    private ArrayList<IReportingListener> reportingListeners = new ArrayList<>();


    /**
    * Used in memory management. 
    * Pushing an entire dataset to Derby via INSERTs risks the database 
    * holding very large data structures in memory, so we 
    * chunk it up where this is the case. This would increase processing time, 
    * but there's a time gain from not hitting memory limits; 
    * profiling suggests an optimal at ~2000 records for a 8Gb PC, 
    * but with a wide enough optimal range that there is little 
    * point in, e.g. setting this in-run using a greedy algorithm.
    *
    * @deprecated 
    * See <code>load(IDataset dataset)</code> docs for details of 
    * deprecation.
    * @see #load(IDataset dataset)
    */
    @Deprecated
    private int chunkRecordsBy = 2000;
    
    
    
    
    //--------MAJOR METHODS-----------------------------------------------------
    
    
    
    
    /**
    * Default constructor.
    */
    public DerbyConsumer() {
        
        debug = DebugMode.getDebugMode();
        gapFillLocalisedGUIText();
        
    }
    
    
    
    
    /**
    * Sets up the data store (database and tables).
    * <p>
    * If a store (database) path and/or record store (table) names haven't been 
    * set using the <code>setSource</code> / <code>setRecordStoreNames</code> 
    * methods, this method deals with this. It pulls the titles from the dataset 
    * passed in for database name and from the dataset record holders for table 
    * names. The default location for the database is a directory within 
    * the user's home directory (see class docs). 
    * All names and paths are sanitised.
    * <p>
    * It then creates the relevant database and tables, or connects to the 
    * database if it already exists. The connection remains until 
    * <code>disconnectStore</code> called.
    * <p>
    * To reset objects of this class, be sure to call <code>setStore</code>
    * and then <code>initialise</code> - just calling <code>initialise</code> 
    * will result in recursion of the store directories.
    *
    * @param     dataset         Dataset to store - note that this need 
    *                            not be filled with records as long as it 
    *                            has recordHolders, metadata, and field data.
    * @throws    DBCreationException    If issues arise.
    */
    public void initialise(IDataset dataset) throws DBCreationException {
        
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtCreatingDBStruct")
            );
        
        // Check and build database path.
        String datasetTitle = sanitise(
                                findTitle(dataset.getMetadata()), 
                                SANITISE_NAME
                            );
        
        
        String dbDirectory = PropertiesSingleton.getInstance().getProperty(
                                            "defaultDatabaseDirectory", DEF_DB_DIR
                                        );
        
        if (store == null) {
            
            setStore(System.getProperty("user.home") + 
                                        File.separator + 
                                        dbDirectory + 
                                        File.separator + 
                                        datasetTitle + 
                                        File.separator);

            
        } else {
            
            // NB note we don't add separator to the store here 
            // as store will have already got this going through setStore.
            setStore(store + 
                    dbDirectory + 
                    File.separator + 
                    datasetTitle + 
                    File.separator);
        }
        
        

        
        // Check and build table name.
        
        if (recordStoreNames == null) {

            ArrayList<IRecordHolder> recordHolders = dataset.getRecordHolders();
            ArrayList<String> tempRecordHolderNames = 
                                          new ArrayList<>(recordHolders.size());

            String name = "";
            
            for (int i = 0; i < recordHolders.size(); i++) {
            
                name = findTitle(recordHolders.get(i).getMetadata());
                // For defaults, add number so each table different.
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
        
        // Try and connect to database, and create it if this fails.
        
        try {
            connectStore();
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
        

        // Build a metadata table for the dataset.
        try {
            buildMetadataTable(dataset.getMetadata(), datasetTitle + "META");
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }

        // Build the tables along with field names and types to 
        // store in the tables. Build a metadata table for each table.

        for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
            
            try {
                buildTable(dataset, i);
                buildMetadataTable(
                        dataset.getRecordHolder(i).getMetadata(), 
                        recordStoreNames.get(i) + "META");
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }
            
            reportMessage(
                    PropertiesSingleton.getInstance().getProperty("txtGeneratedTable") + 
                    recordStoreNames.get(i)
                );
            
        }    

        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtInDatabase") +    
                store
            );
        
    }
    
    
    
    
    /**
    * Sets up the relevant database table. 
    *
    * @param     dataset     The dataset containing the table to be built.
    *                        Record holders can be empty of data at this point,  
    *                        but must contain field information and metadata.
    * @param    index        The index of the table to build in 
    *                        <code>recordStoreNames</code>. 
    * @throws   DBCreationException       If there's an issue creating table.
    */
    private void buildTable(IDataset dataset, int index) 
                                                  throws DBCreationException {

        IRecordHolder recordHolder = dataset.getRecordHolder(index);
        ArrayList<String> fieldNames = recordHolder.getFieldNames();
        ArrayList<Class> fieldTypes = recordHolder.getFieldTypes();


        if (fieldNames.size() != fieldTypes.size()) {
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFieldNamesIssue") + 
                    dataset.getRecordHolder(index)
                );
        }    
        
    
        ArrayList<String> fieldDefs = new ArrayList<>(fieldTypes.size());
        
        // Map field types to Derby database types.
        
        fieldTypes.forEach(classType -> {
            String classString = classType.getName();
            // Adjusted for less fancy JRE still being distributed.
            /*
            switch(classString) {
                case "java.lang.String" -> classString = "VARCHAR(255)";
                case "java.lang.Integer" -> classString = "INTEGER";    
                case "java.util.GregorianCalendar" -> classString = "DATE";
                case "java.math.BigDecimal" -> classString = "DECIMAL";
                default -> classString = "VARCHAR(255)";
            }
            */
            switch(classString) {
                case "java.lang.String":  classString = "VARCHAR(255)"; break;
                case "java.lang.Integer": classString = "INTEGER"; break;   
                case "java.util.GregorianCalendar": classString = "DATE"; break;
                case "java.math.BigDecimal": classString = "DECIMAL"; break;
                default: classString = "VARCHAR(255)";
            }
            fieldDefs.add(classString);
            
        });

        // Build table creating SQL statement.
        // Externally derived components are sanitised before use.
        
        String sql = "CREATE TABLE " + recordStoreNames.get(index) + " (";
        
        String fieldName = "";
        String fieldDef = "";
        
        for (int i = 0; i < fieldNames.size(); i++) {
        
            fieldName = fieldNames.get(i);
            fieldDef = fieldDefs.get(i);
            fieldName = sanitise(fieldName, SANITISE_NAME); // Forced to upper 
                                                            // case here as well.
            sql = sql + fieldName + " " + fieldDef + ",";
        
        };
        
        sql = sql.substring(0, sql.length() - 1);
        sql = sql + ")";

        try {
            runStatement(sql, recordStoreNames.get(index), CREATE_SMT);
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
    }
    
    
    
    
    /**
    * Sets up a metadata table for each table. 
    *
    * @param     metadata             The metadata to be written to the metadata 
    *                                 table.
    * @param    metadataTableName     The name of the metadata table.
    * @throws   DBCreationException If there's an issue making table.
    */
    private void buildMetadataTable(IMetadata metadata, String metadataTableName) 
                                                    throws DBCreationException {
        
        
        ArrayList[] metadataArrays = metadata.getAll();
        
        ArrayList<String> fieldNames = metadataArrays[0];
        ArrayList<Class> fieldTypes = metadataArrays[1];
        ArrayList<String> fieldDefs = new ArrayList<>(fieldTypes.size());
        ArrayList fieldValues = metadataArrays[2];
        
        
        // Build table-creating SQL statement.
        // Externally derived components are sanitised before use.
        
        String sql = "CREATE TABLE " + 
                     metadataTableName + 
                     " (CATEGORY VARCHAR(255), VALUE LONG VARCHAR)";
        
        try {
            runStatement(sql, metadataTableName, CREATE_SMT);
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }

        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtGeneratedMetaTable") + 
                metadataTableName
            );
        
        // Now add metadata to table.
        
        sql = "";
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        // Run through the records converting the Java types to 
        // suitable INSERT statement types. 
        // In general, we could use JDBC methods for this 
        // conversion, but this has the advantage for future 
        // reuse that it will work with pretty much anything as well 
        // as Derby, and we can do some sanitisation on the way in.

        String fieldName= null;
        String txtMissingMetadata = 
            PropertiesSingleton.getInstance().getProperty("txtMissingMetadata");
        
        for (int i = 0; i < fieldNames.size(); i++) {
        
            // Just incase, but most default to "".
            if (fieldValues.get(i) == null) continue;
                    
            
            fieldName = fieldNames.get(i);
            fieldName = sanitise(fieldName, SANITISE_NAME); // Forced to upper 
                                                            // case here as well.
            
            sql = "INSERT INTO " + 
                  metadataTableName + " (CATEGORY, VALUE) VALUES ("; 

            sql = sql + "'" + fieldName + "', ";

            String classString = fieldTypes.get(i).getName(); 
        
            try {
                
                // Adjusted for less fancy JRE still being distributed.
                /*
                sql = switch(classString) {
                    
                    case "java.lang.String" -> sql + "'" + 
                                sanitise((String)fieldValues.get(i), SANITISE_WEAK) + 
                                "'" + ")";
                    
                    case "java.lang.Integer" -> sql + 
                                fieldValues.get(i).toString() + 
                                ")";
                    
                    case "java.util.GregorianCalendar" -> sql + "'" + sdf.format(
                                ((GregorianCalendar)fieldValues.get(i)).getTime()
                                ) + "')";
                    
                    case "java.math.BigDecimal" -> sql + 
                                fieldValues.get(i).toString() + 
                                ")";
                    
                    default -> sql + "''" + ")";
                    
                };
                */
                switch(classString) {
                    
                    case "java.lang.String": sql =  sql + "'" + 
                                sanitise((String)fieldValues.get(i), SANITISE_WEAK) + 
                                "'" + ")"; 
                                break;
                    
                    case "java.lang.Integer": sql = sql + 
                                fieldValues.get(i).toString() + 
                                ")"; 
                                break;
                    
                    case "java.util.GregorianCalendar": sql = sql + "'" + sdf.format(
                                ((GregorianCalendar)fieldValues.get(i)).getTime()
                                ) + "')"; 
                                break;
                    
                    case "java.math.BigDecimal": sql = sql + 
                                fieldValues.get(i).toString() + 
                                ")"; 
                                break;
                    
                    default: sql = sql + "''" + ")"; 
                    
                };
                sql = sql.replace("''", "'" + txtMissingMetadata + "'");
          
            } catch (RuntimeException rte) {
                if (debug) rte.printStackTrace();
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtMetaReadIssue") + 
                        metadataTableName 
                    );
            }
            
            try {
                runStatement(sql, metadataTableName, INSERT_SMT);
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }
        }
        
        // Not needed, but just to prove it is there for this application use.
        proveLoaded(metadataTableName, METADATA_TABLE);
        
    }
    
    
    
    
    /**
    * This method bulk-loads a whole dataset into the database the quickest 
    * (but memory hungry) way possible - via a CSV file.
    * <p>
    * It will attempt to write the dataset as one or more temp files to the 
    * user's home space, and then load the files into the database. 
    * For a file of 60000 records, this is an order of magnitude 
    * faster than loading by INSERTs, but it does require write access. 
    * <code>initialise</code> must have been called first to prepare the database.
    * <p>
    * This method calls <code>disconnectStore</code> when done to clean up 
    * and garbage collect.
    *
    * @param     dataset                 The dataset to load.
    * @throws    DBCreationException     If there is an issue loading.
    * @todo      Remove code that proves table filled. This is just for this 
    *            specific application. 
    */
    public void bulkLoad(IDataset dataset) throws DBCreationException {
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtLoading")
            );
            
            
        // Tell the flatFileConsumer where to write the file/s:
        
        FlatFileConsumer flatFileConsumer = new FlatFileConsumer();
        String datasetTitle = sanitise(findTitle(
                                            dataset.getMetadata()), SANITISE_NAME
                                        );
        
        // Make ourselves a reference to some temporary file space.
        // Ideally, the user's temp directory, where we make a 
        // subdirectory "dbLoad-youCanDelete-x" where x is a series of 
        // OS chosen ints. If this doesn't work, we try to get back to 
        // the original store directory (remembering this has been 
        // messed with in "initialise") and build a temp directory 
        // at the same level as the database directory called 
        // "dbAppTempFiles-youCanDelete".
        String path = null;
        try {
            path = (Files.createTempDirectory("dbLoad-youCanDelete-")).toString();
        } catch (IOException ioe){
            String parentDirectory = (new File(store)).getParent();
            path =  parentDirectory + DEF_FILE_DIR + File.separator;
        }            
        
        path = path + File.separator + datasetTitle + File.separator;
    
       
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtGeneratingTemp") + 
                path
            );

        // We then set this as the flatFileConsumer's store.
        try {
            flatFileConsumer.setStore(path);
            flatFileConsumer.setRecordStoreNames(recordStoreNames);
        } catch (FlatFileCreationException ffce) {
            // Not actually thrown with a FlatFileConsumer; 
            // just in for consistency with interface.
        }
        

        // Allocate file writing 20% of progress.
        reportProgress(20, 100);
        
        
        // As bulk loading needs the file read into the database 
        // not to have a header, we don't initialise the flatFileConsumer.
        // This also means our path won't be messed with.
        // The only thing we lose is record-by-record progress monitoring,  
        // which isn't really possible with bulk loading anyhow. Helpfully,  
        // the main wait is for the loading not the file writing. 
        try {
            flatFileConsumer.bulkLoad(dataset);
        } catch (FlatFileCreationException ffce) {
            if (debug) ffce.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileWriteIssue") + 
                    path
                );
        }
        
        // Load the file/s into the database.
        
        for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
        
            String sql = "CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE (null,'" + 
                         recordStoreNames.get(i) + "','" + 
                         path + recordStoreNames.get(i) + ".csv" + 
                         "',',', null, null, 0)"; 
            
            try {
                runStatement(sql, recordStoreNames.get(i), CREATE_SMT);
            } catch (DBCreationException dbce) {
                if (debug) dbce.printStackTrace();
                throw dbce;
            }
            
            
            // Divvy up the remaining 80% of progress across 
            // number of recordHolders.
            int done = 80 - (80 / (dataset.getRecordHolders().size() - i)); 
            reportProgress(20 + done, 100);
        
        
            // Not needed, but just to prove it is there for this application use.
            proveLoaded(recordStoreNames.get(i), NORMAL_TABLE);
        
        
        }
        
        // Zero progress.
        reportProgress(0, 1);
        
        
        
        // At this point we've done our job as a consumer.
        // Disconnecting frees up resources and does a garbage collection, 
        // though it has to be said that Derby still maintains a very 
        // large footprint in memory.
        try {
            disconnectStore();
        } catch (DBCreationException dbce) {
            // See disconnectSource - only thrown in debug mode.
            if (debug) dbce.printStackTrace();
        }
    
    }
    
    
    
    
    /**
    * Adds multiple records to current database.
    * <p>
    * This little at the moment than call <code>storeRecords</code> with 
    * the records, but we keep it as a separate method as it acts as a gateway 
    * for data in the supplier/consumer push model and we may want to add 
    * functionality to the gateway in the future.
    * <p>
    * Users of the push model should call <code>disconnectStore</code> 
    * when done, as the consumer keeps the connection open under this model.
    * 
    * @param     records                    ArrayList of records.
    * @throws    DBCreationException        Only if there is an issue.
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
        
        // Note that the data suppiler completes a garbage collection 
        // after every call of this method.
        
        // If all data with us, zero progress.
        if (progress >= totalEstimate) {
            reportProgress(0, 1);
            try {
                disconnectStore();
                
                // Not needed, but just to prove it is there for this application use.
                for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
                    proveLoaded(recordStoreNames.get(i), NORMAL_TABLE);
                }
            } catch (DBCreationException dbce) {
                // See disconnectSource - only thrown in debug mode.
                if (debug) dbce.printStackTrace();
            }
        }

        

    }    



    
    /**
    * INSERTs a collection of records into the database. 
    * <p>
    * This is very slow compared with bulk loading, 
    * but used judiciously can use less memory. A 60,000 
    * 4-value records file loaded 120 records at a time used 10% of the memory 
    * of bulk-loading, but took an order of magnitude more time (30 mins). 
    *
    * @param     records                    ArrayList of records to write.
    * @throws    DBCreationException        Only if there is an issue.
    */
    private void storeRecords(ArrayList<IRecord> records) 
                                                throws DBCreationException {
        
        // Create a scrollable statement.
        // Note we don't use runStatement here, as it opens and shuts 
        // a statement each run - here we're running each record, and 
        // opening and closing that many statements rather than reusing 
        // one is very process heavy.

        Statement statement = null;
        try {
            statement = connection.createStatement(
                                            ResultSet.TYPE_SCROLL_SENSITIVE, 
                                            ResultSet.CONCUR_UPDATABLE
                                        );
        } catch (SQLException sqle) {
            if (debug) sqle.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtSQLIssue")
                );
        }

        // Get the fields and tables to INSERT into.

        IRecordHolder table = records.get(0).getParentRecordHolder();        
        IDataset dataset = table.getParentDataset();
        ArrayList<String> fieldNames = table.getFieldNames();
        ArrayList<Class> fieldTypes = table.getFieldTypes();
        int tableIndex = dataset.getRecordHolders().indexOf​(table);
        String tableName = recordStoreNames.get(tableIndex);
        
                    
        if (((fieldNames == null) || (fieldTypes == null)) || 
            ((table == null) || (tableIndex == -1))) {
                String troubledTable = tableIndex > 0 ? tableName : 
                    PropertiesSingleton.getInstance().getProperty("txtUnknownTable");
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtTableInfoIssue") +  
                        troubledTable
                    );

        }
        
        
        // Build the INSERT SQL statement.
        
        String sql = "";
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        // Run through the records converting the Java types to 
        // suitable INSERT statement types. 
        // In general, we could use JDBC methods for this 
        // conversion, but this has the advantage for future 
        // reuse that it will work with pretty much anything as well 
        // as Derby, and we can do some sanitisation on the way in.
        
        for (int j = 0; j < records.size(); j++) {
            
            ArrayList values = records.get(j).getValues();
            
            sql = "INSERT INTO " + tableName + " (";
            
            for (int k = 0; k < values.size(); k++) {
                
                 sql = sql + fieldNames.get(k) + ", ";
                      
            }
            
            sql = sql.substring(0, sql.length() - 2); // Remove last comma-space.
            
            sql = sql + ") VALUES (";
            
            for (int k = 0; k < values.size(); k++) {
                
                String classString = fieldTypes.get(k).getName();

                try {
                    // Adjusted for less fancy JRE still being distributed.
                    /*
                    sql = switch(classString) {
                        
                        case "java.lang.String" -> sql + "'" + 
                                    sanitise((String)values.get(k), SANITISE_WEAK) + 
                                    "'" + ", ";
                        
                        case "java.lang.Integer" -> sql + 
                                    values.get(k).toString() + 
                                    ", ";
                        
                        case "java.util.GregorianCalendar" -> sql + "'" + sdf.format(
                                    ((GregorianCalendar)values.get(k)).getTime()
                                    ) + "', ";
                        
                        case "java.math.BigDecimal" -> sql + 
                                    values.get(k).toString() + 
                                    ", ";
                        
                        default -> sql + "'" + values.get(k) + "'" + ", ";
                        
                    };
                    */
                    switch(classString) {
                        
                        case "java.lang.String": sql = sql + "'" + 
                                    sanitise((String)values.get(k), SANITISE_WEAK) + 
                                    "'" + ", ";
                                    break;
                        
                        case "java.lang.Integer": sql = sql + 
                                    values.get(k).toString() + 
                                    ", ";
                                    break;
                        
                        case "java.util.GregorianCalendar": sql = sql + "'" + sdf.format(
                                    ((GregorianCalendar)values.get(k)).getTime()
                                    ) + "', ";
                                    break;
                        
                        case "java.math.BigDecimal": sql = sql + 
                                    values.get(k).toString() + 
                                    ", ";
                                    break;
                        
                        default: sql = sql + "'" + values.get(k) + "'" + ", ";
                        
                    };
                    
                } catch (RuntimeException rte) {
                    if (debug) rte.printStackTrace();
                    throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtConvertIssue") + 
                            tableName + 
                            " -> " + (String)values.get(k)
                        );
                }
                
                
 

            }
            sql = sql.substring(0, sql.length() - 2); // Remove last comma-space.
            
            sql = sql + ")";
          
            // Run statement for this record.
            try {
                statement.executeUpdate(sql);
            } catch (SQLException sqle) {
                if (debug) sqle.printStackTrace();
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtTableWriteIssue") +
                        tableName 
                    );
            } 
            
            // Monitor and report progress.
            // Although the source of the data is probably
            // doing this as well, it should match up.
            
            progress++;

            reportProgress(progress, dataset);
            
            Runtime.getRuntime().gc();
            
        }
        
        try {
            if (statement != null) statement.close();
        } catch (SQLException sqle) {
            if (debug) sqle.printStackTrace();
            throw new DBCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtCloseTableIssue") + 
                    tableName  
                );
        }
        
    }
    
    
    
    
    //--------ACCESSOR / MUTATOR METHODS----------------------------------------
    
    
    
    
    /**
    * Sets the location for the store / database. 
    * <p>
    * If not set the parameters can be set from the dataset 
    * via the <code>initialise</code> method.
    * <p>
    * Sanitises input with SANITISE_WEAK.
    * 
    * @param     store                    File path of database to connect to. 
    * @throws    DBCreationException      Not used in this implementation.
    * @see       #initialise(IDataset dataset)
    */    
    public void setStore(String store) throws DBCreationException {
        
        if (store == null || store.equals("")) {
            this.store = null;
            return;
        }
        
        if (!store.endsWith(File.separator)) {
            store = store + File.separator;
        }
        
        this.store = sanitise(store, SANITISE_WEAK);
        
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
    * Sets the record store (table) names for the store (database). 
    * <p>
    * If not set the parameters can be set from the dataset 
    * via the <code>initialise</code> method. 
    * <p>
    * Sanitises input with SANITISE_NAME.
    * <p>
    * If set here directly (for example, the user 
    * enters the name/s) it's the implementers responsibility to make sure 
    * the number of record stores named and number supplied in the dataset 
    * to <code>initialise</code> match up or <code>initialise</code>
    * will throw an exception.
    * 
    * @param    recordStoreNames         Names of record stores (tables) to make.
    * @throws   DBCreationException      Not used in this implementation.
    * @see      #initialise(IDataset dataset)
    */    
    public void setRecordStoreNames(ArrayList<String> recordStoreNames) 
                                                    throws DBCreationException {
        
        if (recordStoreNames == null) {
            this.recordStoreNames = null;
            return;
        }
        
        ArrayList<String> temp = new ArrayList<>(recordStoreNames.size());
        
        recordStoreNames.forEach(recordHolderName -> {
                    // Capitalised here as well; which is what Derby expects.
                    temp.add(sanitise(recordHolderName, SANITISE_NAME)); 
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
    * @param     metadata    Metadata object of unknown schema.
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
    * Connects to current store / database.
    *
    * @throws     DBCreationException    If there is an issue.
    */
    public void connectStore()  throws DBCreationException {
        
        // Try and connect to database/table, and create it if this fails.
        // NB store assured with filed separator in setStore.
        
        String strUrl = "jdbc:derby:" + store;
        
        try {
            connection = DriverManager.getConnection(strUrl);
        } catch  (SQLException sqle1) {
            
            try {
                connection = DriverManager.getConnection(strUrl + ";create=true");
            } catch  (SQLException sqle2) {
                if (debug) sqle2.printStackTrace();
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtCreateDBIssue") +
                        store
                    );
            }
            
        }
        
    }
    
    
    
    
    /**
    * Disconnects from current store / database and garbage collects.
    *
    * @throws     DBCreationException     Not thrown in this implementation.
    */
    public void disconnectStore() throws DBCreationException   {
        
        try {
            connection.close();
        } catch (SQLException sqle) {
            // Unlikely to be an issue if the database exists, 
            // and associated conditions around this are caught before.
            if (debug) sqle.printStackTrace();
        }
        Runtime.getRuntime().gc();
        
    }
    
    
    
    
    /**
    * Runs SQL statements and closes them.
    * <p>
    * If the statement requires creation the method will try to connect 
    * to the table, and if it exists it will DROP it and rebuild. 
    *
    * @param     sql          The SQL statement.
    * @param     tableName    The table involved; this is for reporting, not 
    *                         action as the table should be in the SQL if needed.
    * @param     statementType     For clarity, use INSERT_SMT or CREATE_SMT. 
    * @throws    DBCreationException  If there's an issue.
    */
    private void runStatement(String sql, String tableName, boolean statementType) 
                                                    throws DBCreationException {
        
        Statement statement = null;

        try {

            if (statementType == INSERT_SMT) {
                statement = connection.createStatement(
                                ResultSet.TYPE_SCROLL_SENSITIVE, 
                                ResultSet.CONCUR_UPDATABLE
                            );
                statement.executeUpdate(sql);            
            } else {
                statement = connection.createStatement();
                statement.execute(sql);
            }
            
        } catch (SQLException sqle1) {
            
            // If we're trying to create and we can't because the 
            // table already exists, delete it.
            if (statementType == CREATE_SMT) {
                try {
                    String dropTable = "DROP TABLE " + tableName;
                    statement = connection.createStatement();
                    statement.execute(dropTable);
                    statement = connection.createStatement();
                    statement.execute(sql);
                    
                    reportMessage("Deleted and rebuilt table " + tableName);
                    
                } catch (SQLException sqle2) {
                    if (debug) sqle2.printStackTrace();
                    throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtTableWriteIssue") + 
                            tableName
                        );
                }
            } else {
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtTableWriteIssue") + 
                        tableName
                    );
            }
            
        } finally {

            try {
                if (statement != null) statement.close();
            } catch (SQLException sqle3) {
                if (debug) sqle3.printStackTrace();
                throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtTableWriteIssue") + 
                        tableName
                    );
            }
            
        }
        
    }
        
    
    
    
    /**
    * Sanitises Strings.
    * <p>
    * Vigorous sanitisation removes all non-alphanumeric characters in the 
    * range <code>a-z</code>, <code>A-Z</code>, <code>0-9</code>, commas, 
    * spaces and tabs. 
    * <p>
    * Weak sanitisation removes characters likely to be problematic in databases 
    * (<code>" ' ; () =</code>).
    * <p>
    * Name sanitisation sanitises Strings to potential database object names 
    * fairly aggresively (removes spaces, capitalises, makes sure it starts 
    * with a alphabetic character and not "ii", removes all but 0 through 9, 
    * #, @, and $, and makes sure they're 256 or less bytes*).
    * <p>
    * *Assumes Java-like representation, i.e. two bytes per char.
    *
    * @param    string    String to sanitise.
    * @param    level    One of SANITISE_VIGOROUS, SANITISE_WEAK, SANITISE_NAME.
    * @return    String    Sanitised String.
    */
    protected String sanitise(String string, int level) {
        // "protected" for tests.
        
        // To adjust, see list at:
        // https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
        
        // Adjusted for less fancy JRE still being distributed.
        /*
        switch(level) {
            // Remove anything not an alphabet char, comma, space, number or tab.
            case SANITISE_VIGOROUS -> string = 
                                   string.replaceAll("[^a-zA-Z,\\s\\d\\t]"," ");
            
            // Place "A" in front of "ii", and not an alphabetic char. 
            // Capitalise. Remove anything not alphanumeric or #,@,$. 
            case SANITISE_NAME -> {
                string = string.replaceAll("^ii","Aii");
                string = string.toUpperCase();
                string = string.replaceAll("(^[^A-Z])","A");
                string = string.replaceAll("[^A-Z\\d#@$$]","");
                while (string.getBytes().length > 256) {
                    string = string.substring(0,string.length()-1);
                }

            }
            
            // Remove some problematic punctuation: \"';()=
            case SANITISE_WEAK -> string = string.replaceAll("[\"\';()=]"," ");
            
            default -> string = ""; // Fail safe(ish).
        };
        */
        switch(level) {
            // Remove anything not an alphabet char, comma, space, number or tab.
            case SANITISE_VIGOROUS:string = 
                                   string.replaceAll("[^a-zA-Z,\\s\\d\\t]"," ");
                                   break;
            // Place "A" in front of "ii", and not an alphabetic char. 
            // Capitalise. Remove anything not alphanumeric or #,@,$. 
            case SANITISE_NAME: 
                string = string.replaceAll("^ii","Aii");
                string = string.toUpperCase();
                string = string.replaceAll("(^[^A-Z])","A");
                string = string.replaceAll("[^A-Z\\d#@$$]","");
                while (string.getBytes().length > 256) {
                    string = string.substring(0,string.length()-1);
                }
                break;
            
            // Remove some problematic punctuation: \"';()=
            case SANITISE_WEAK: string = string.replaceAll("[\"\';()=]"," ");
                                break;
            default: string = ""; // Fail safe(ish).
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
        defaults.setProperty("txtCreatingDBStruct", "Creating database and tables.");
        defaults.setProperty("txtHoldersVsNamesIssue", "Number of record store " +
                                                       "names and number of record " + 
                                                       "holders to store do not match." 
                                                    );
       defaults.setProperty("txtGeneratedTable", "Generated table: ");
       defaults.setProperty("txtInDatabase", "...in database: ");
       defaults.setProperty("txtFieldNamesIssue", "Number of fieldnames does " + 
                                                  "not match number of fieldtypes " + 
                                                  "in: "
                                                );
       defaults.setProperty("txtGeneratedMetaTable", "Generated metadata table: ");
       defaults.setProperty("txtMetaReadIssue", "Please check associated file " + 
                                                "header; issue reading metadata " + 
                                                "into metadata table: "
                                            );
       defaults.setProperty("txtGeneratingTemp", "Generating temp file at: ");
       defaults.setProperty("txtFileWriteIssue", "There's an issue writing a " + 
                                                 "file, please check you have " + 
                                                 "permission to write to: "
                                            );
       defaults.setProperty("txtSQLIssue", "Issue creating an SQL statement. " + 
                                           "Please check the database has been " +
                                           "created in listing above."
                                        );
       defaults.setProperty("txtUnknownTable", "UNKNOWN");
       defaults.setProperty("txtTableInfoIssue", "Issue finding table " + 
                                                 "information for table: "
                                            );
       defaults.setProperty("txtConvertIssue", "Issue converting a value, please " + 
                                               "check the following: "
                                            );
       defaults.setProperty("txtTableWriteIssue", "Issue writing a table; please " + 
                                                  "check file associated with: "
                                            );
       defaults.setProperty("txtCloseTableIssue", "Issue closing table: ");
       defaults.setProperty("txtCreateDBIssue", "There is an issue connecting " + 
                                                "to or creating a database. " + 
                                                "Please make sure you have access " + 
                                                "permission to: "
                                            );
        defaults.setProperty("txtTestOutput", "Here's the first and last " + 
                                              "entries (US date format) from table: "
                                            );
        
        defaults.setProperty("txtProofReadIssue", "There's an issue proving your " + 
                                                  "table exists; please check there " + 
                                                  "are files in the created database" + 
                                                  "directory: "
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
    * This method was an early element of the data consumer built on top of 
    * the line-by-line reader to test the whole dataset, and INSERTs a 
    * whole dataset a line at a time. 
    * <p>
    * It is both memory heavy and slow. The only reason for having it here 
    * is that it is imaginable that some uses might want to avoid writing 
    * the temp file/s needed for bulk loading.
    * <p>
    * @deprecated
    * This method adds a whole dataset, using INSERT to add a record at a time. 
    * While it chunks the dataset into blocks to reduce memory 
    * use on the database side, it is still memory heavy and 
    * an order of magnitude slower than <code>bulkLoad</code>.
    * 
    * @param     dataset                    Dataset of data.
    * @throws    DBCreationException      If is there is an issue.
    */
    @Deprecated
    public void load(IDataset dataset) throws DBCreationException {
        
        
        IRecordHolder table = null;
        ArrayList<IRecord> records = null;

        // For progress monitoring.
        int progress = 0;
            
        // Run through each record holder (table).
        try {
            for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
            
                table = dataset.getRecordHolders().get(i);
                records = table.getRecords();
                
                ArrayList<IRecord> chunks = new ArrayList<>();
                        
                // Chunk the table into sections of records, and process.
                
                for (int j = 0; j < records.size(); j = j + chunkRecordsBy) {
                
                    chunks = new ArrayList<>();
                    for (int k = 0; k < chunkRecordsBy; k++) {
                        chunks.add(records.get(j + k));
                    }

                    storeRecords(chunks);
                    progress++; 
                    reportProgress(progress, dataset);
                    
                    // Try to regain some memory with a garbage collection.
                    Runtime.getRuntime().gc();

                }
            
            }
        } catch (DBCreationException dbe) {
            throw dbe;
        }
        
         // Zero progress.
        reportProgress(0, 1);

        // Clean up.
        disconnectStore();
                
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
    private void proveLoaded(String tableName, boolean metadataTable) throws DBCreationException {


        ArrayList<String> lines = new ArrayList<>();

        try {
            disconnectStore();
            connectStore();
        } catch (DBCreationException dbce) {
            if (debug) dbce.printStackTrace();
            throw dbce;
        }
        
        // Statement to get a cursor from table. 
        
        Statement statement = null;
        try {
            statement = connection.createStatement(
                                        ResultSet.TYPE_SCROLL_SENSITIVE, 
                                        ResultSet.CONCUR_UPDATABLE
                                    );
        } catch (SQLException sqle) {
            if (debug) sqle.printStackTrace();
            throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtProofReadIssue") + 
                        tableName
            );
        }


        if (metadataTable) {
            
            ResultSet rs = null;
            
            try {
                
                rs = statement.executeQuery("SELECT CATEGORY, VALUE FROM " + 
                                            tableName
                                        );
                                        
            } catch (SQLException sqle) {
                if (debug) sqle.printStackTrace();
                throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtProofReadIssue") + 
                            tableName
                );
            }
            
            try {
                rs.first();
                String firstLine = rs.getString​("CATEGORY");
                firstLine = firstLine + " | " +rs.getString​("VALUE");
                rs.last();
                lines.add(firstLine);
                String lastLine = rs.getString​("CATEGORY");
                lastLine = lastLine + " | " +rs.getString​("VALUE");
                lines.add(lastLine);
            } catch (SQLException sqle) {
                if (debug) sqle.printStackTrace();
                throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtProofReadIssue") + 
                            tableName
                );
            }
            
        } else {

            
            ResultSet rs = null;
            try {
                
                rs = statement.executeQuery("SELECT * FROM " + 
                                            tableName
                                        );
                                        
            } catch (SQLException sqle) {
                if (debug) sqle.printStackTrace();
                throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtProofReadIssue") + 
                            tableName
                );
            }


            // Note US format.
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

            boolean first = true;
            
            try {
            
                // Find out how many columns in table.
                
                ResultSetMetaData rsmd = rs.getMetaData();
 
                // Only need to run this loop twice: first and last record.
                
                while (true) {
                
                    String line = "";
                    
                    if (first) {
                        rs.first();
                        
                    } else { 
                        rs.last();
                    }

                    // Loop through columns and turn data into Strings.
                    
                    for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
                     
                        String type = rsmd.getColumnTypeName(i);
                        // Adjusted for less fancy JRE still being distributed.
                        /* 
                        String value = switch(type) {
                            case "VARCHAR(255)" -> rs.getString(i);
                            case "INTEGER" -> (new Integer(rs.getInt(i))).toString();    
                            case "DATE" -> sdf.format(rs.getDate​​(i));
                            case "DECIMAL" -> rs.getBigDecimal​(i).toString(); 
                            default -> rs.getString(i);
                        };
                        */
                        String value = "";
                        switch(type) {
                            case "VARCHAR(255)": value = rs.getString(i); break;
                            case "INTEGER": value = (new Integer(rs.getInt(i))).toString();  break;   
                            case "DATE": value = sdf.format(rs.getDate​​(i)); break;
                            case "DECIMAL": value = rs.getBigDecimal​(i).toString();  break;
                            default: value = rs.getString(i);
                        };
                        line = line + " | " + value;
                 
                    }    
                 
                    // Add to arraylist for printing.
                    lines.add(line);    
                    if (!first) break;
                    first = false;
                }
            
                
                
            } catch (SQLException sqle) {
                if (debug) sqle.printStackTrace();
                throw new DBCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtProofReadIssue") + 
                            tableName
                );
            } 
            
        }


        try {
            statement.close();
        } catch (SQLException sqle) {
            if (debug) sqle.printStackTrace();
            throw new DBCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtProofReadIssue") + 
                        tableName
            );
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