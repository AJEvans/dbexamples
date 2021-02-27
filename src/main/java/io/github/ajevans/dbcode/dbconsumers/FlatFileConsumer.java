/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbconsumers;


import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Properties;

import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.data.structures.IRecordHolder;
import io.github.ajevans.dbcode.data.structures.IMetadata;
import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.IReportingListener;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Class for writing out flat files to a directory.
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
* Dates are written in ISO 8601 format, that is: YYYY-MM-DD.
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
public class FlatFileConsumer implements IDataConsumer {
    
    
    
    
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
    * Connects to files at various times.
    */
    private BufferedWriter bufferedWriter = null;
    
    
    /**
    * Extension for files.
    */
    private String fileExtension = ".csv";
    
    
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
    private final String DEF_FILE_DIR = "dbexamples-flatfiles";    
    
    
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
    * Used for progress monitoring.
    */
    private int progress = 0; 
    
    
    
    //--------MAJOR METHODS-----------------------------------------------------
    
    
    
    
    /**
    * Default constructor.
    */
    public FlatFileConsumer() {
        
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
    * @throws    FlatFileCreationException      If issues arise.
    */
    public void initialise(IDataset dataset) throws FlatFileCreationException {
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtCreatingFileStruct")
            );
            
            
        String datasetTitle = findTitle(dataset.getMetadata());
        
        // Check and build directory path. If missing, this will be the dataset name.
                
        String fileDirectory = PropertiesSingleton.getInstance().getProperty(
                                    "defaultFileDirectory", DEF_FILE_DIR
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
                throw new FlatFileCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtHoldersVsNamesIssue")
                    );
            }
            
        }

        // Build directories.
        
        try {
            connectStore();
        } catch (FlatFileCreationException ffce) {
            if (debug) ffce.printStackTrace();
            throw ffce;
        }

        
        // Build a metadata file for the dataset.
        
        try {
            buildMetadataFile(dataset.getMetadata(), datasetTitle + "META.properties");
        } catch (FlatFileCreationException ffce) {
            if (debug) ffce.printStackTrace();
            throw ffce;
        }

        // Build the files along with field names and types to 
        // store in the files. Build a metadata file for each record holder.

        for (int i = 0; i < dataset.getRecordHolders().size(); i++) {
            
            try {
                buildFile(dataset, i);
                buildMetadataFile(
                        dataset.getRecordHolder(i).getMetadata(), 
                        recordStoreNames.get(i) + "META.properties");
            } catch (FlatFileCreationException ffce) {
                if (debug) ffce.printStackTrace();
                throw ffce;
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
    * @throws    FlatFileCreationException      If issues arise.
    */
    private void buildFile(IDataset dataset, int index) 
                                                  throws FlatFileCreationException {    
        
        
        // Make files and write out file headers.
        
        ArrayList<String> fieldNames = null;
        ArrayList<Class> fieldTypes = null;
        String tableName = null;
        
            
        File file = new File(store + recordStoreNames.get(index) + fileExtension);
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileWriteIssue") + 
                    file 
                );
        }
        
        // Write out the headers.

        fieldNames = dataset.getRecordHolder(index).getFieldNames();
        fieldTypes = dataset.getRecordHolder(index).getFieldTypes();
        
        if ((fieldNames == null) || (fieldTypes == null)) {
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileHeaderIssue") + 
                    file
                );
        }
        
        if (fieldNames.size() != fieldTypes.size()) {
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFieldNamesIssue") + 
                    file
                );
        }
        
        String fields = "";
        
        for (String fieldName : fieldNames) {
            fields = fields + fieldName + ",";
        }
        
        fields = fields.substring(0, fields.length() - 1); // Remove last comma.
        
        try {
            bufferedWriter.append(fields);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileHeaderWriteIssue") + 
                    file
                );
        }
         
    }
    
    
    
    
    /**
    * Sets up a metadata file for each record holder file. 
    *
    * @param     metadata            The metadata to be written to the metadata 
    *                                file.
    * @param    metadataFileName     The name of the metadata file.
    * @throws   FlatFileCreationException   If issues arise.
    */
    private void buildMetadataFile(IMetadata metadata, String metadataFileName) 
                                                throws FlatFileCreationException {
        
        
        ArrayList[] metadataArrays = metadata.getAll();
        
        ArrayList<String> fieldNames = metadataArrays[0];
        ArrayList<Class> fieldTypes = metadataArrays[1];
        ArrayList<String> fieldDefs = new ArrayList<>(fieldTypes.size());
        ArrayList fieldValues = metadataArrays[2];
        

        
        File file = new File(store + metadataFileName);
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtFileWriteIssue") + 
                    file
                );
        }
        
        
        // Now add metadata to file.
        
        String line = "";
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        // Run through the records converting the Java types to 
        // suitable text.

        String fieldName= null;
                  
        for (int i = 0; i < fieldNames.size(); i++) {
        
            // Just incase, but most default to "".
            if (fieldValues.get(i) == null) continue;
            
            fieldName = fieldNames.get(i);

            line = fieldNames.get(i) + "=";
            
            String classString = fieldTypes.get(i).getName(); 
            
            try {
                line = switch(classString) {
                    
                    case "java.lang.String" -> line + fieldValues.get(i); 
                    
                    case "java.lang.Integer" -> line + fieldValues.get(i).toString();
                    
                    case "java.util.GregorianCalendar" -> line + sdf.format(
                                ((GregorianCalendar)fieldValues.get(i)).getTime()
                                );
                    
                    case "java.math.BigDecimal" -> line + fieldValues.get(i).toString();
                    
                    default -> line + fieldValues.get(i).toString();
                    
                };
            } catch (RuntimeException rte) {
                if (debug) rte.printStackTrace();
                throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtMetaReadFileIssue") + 
                    file
                );
            }
            
            // For multi-line lines, replace the linebreaks with pipe symbols.
            line = line.replaceAll("\\r\\n|\\r|\\n", " | " );
            
            try {
                bufferedWriter.append(line);
                bufferedWriter.newLine();
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtMetaWriteFileIssue") + 
                    file
                );
            }

        }
        
        reportMessage("Generated metadata file: " + metadataFileName);
        

        try {
            bufferedWriter.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtCloseFileIssue") + 
                    file
                );
        }

    }
    
    

    
    /**
    * This method bulk-loads a whole dataset into one or more files.
    * <p>
    * This method calls <code>disconnectStore</code> when done to clean up.
    *
    * @param     dataset                        The dataset to load.
    * @throws    FlatFileCreationException      If there's an issue.
    */
    public void bulkLoad(IDataset dataset) throws FlatFileCreationException {
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtLoading")
            );
        
        // Just check the directories exist. 
        // Should do if initialise called, but isn't always.
        try {
            connectStore();
        } catch (FlatFileCreationException ffce) {
            if (debug) ffce.printStackTrace();
            throw ffce;
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
                
                
            }
        } catch (FlatFileCreationException ffce) {
            if (debug) ffce.printStackTrace();
            throw ffce;
        }
        
                
        // Zero progress.
        reportProgress(0, 1);
        
        // We do a garbage collection here to free up resources 
        // though actually there aren't many hanging around with file writing.
        Runtime.getRuntime().gc();
                
    }
    
  
  
  
    /**
    * Adds multiple records to current store.
    * <p>
    * This little at the moment than call <code>storeRecords</code> with 
    * the records, but we keep it as a separate method as it acts as a gateway 
    * for data in the supplier/consumer push model and we may want to add 
    * functionality to the gateway in the future.
    * 
    * @param      records                       ArrayList of records.
    * @throws     FlatFileCreationException     Only if there is an issue.
    */
    public void load(ArrayList<IRecord> records) throws FlatFileCreationException {
        
        IRecordHolder table = records.get(0).getParentRecordHolder();        
        IDataset dataset = table.getParentDataset();
        int totalEstimate = dataset.getEstimatedRecordCount();
        
        try {
            storeRecords(records);
        } catch (FlatFileCreationException ffce) {
            if (debug) ffce.printStackTrace();
            throw ffce;
        }
        
        // We do a garbage collection here to free up resources 
        // though actually there aren't many hanging around with file writing.
        Runtime.getRuntime().gc();

        // If all data with us, zero progress.
        if (progress >= totalEstimate) {
            reportProgress(0, 1);
        }

    }    




    /**
    * Add a set of records to the relevant record store (file). 
    *
    * @param     records                    An ArrayList of the IRecords to add.
    * @throws    FlatFileCreationException  Only if there is an issue.
    */
    private void storeRecords(ArrayList<IRecord> records) throws FlatFileCreationException {
        
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
        
        File file = new File(store + tableName + fileExtension);

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file, true));
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtRecordWriteIssue") + 
                    file
                );
        }        

        
        // Loop through the record, getting values, 
        // converting to Strings and writing to file.
        
        String stringToWrite = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
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
                    
                    
                } catch (RuntimeException rte) {
                    if (debug) rte.printStackTrace();
                    throw new FlatFileCreationException(
                            PropertiesSingleton.getInstance().getProperty("txtConvertIssue") + 
                            tableName + " -> " + (String)values.get(k)
                        );                                
                }    
            }
                
           
            
            // Write string to file.
            
            // Remove last comma.
            stringToWrite = stringToWrite.substring(0, stringToWrite.length() - 1); 

            
            try {
                bufferedWriter.append(stringToWrite);
                bufferedWriter.newLine();
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                throw new FlatFileCreationException(
                        PropertiesSingleton.getInstance().getProperty("txtRecordWriteIssue") + 
                        file
                    );
            }
            
            // Progress monitoring.
            progress++;
            reportProgress(progress, dataset);
            
        }

       
        
        try {
            bufferedWriter.close();
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtCloseFileIssue") + 
                    file
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
    * @throws   FlatFileCreationException   Not used in this implementation.
    * @see      #initialise(IDataset dataset)
    */    
    public void setStore(String store) throws FlatFileCreationException {
        
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
    * @throws   FlatFileCreationException  Not used in this implementation.
    * @see      #initialise(IDataset dataset)
    */    
    public void setRecordStoreNames(ArrayList<String> recordStoreNames) 
                                              throws FlatFileCreationException {

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
    * Creates data directory and directory it is in if missing.
    *
    * @throws     FlatFileCreationException    If there's an issue.
    */
    public void connectStore() throws FlatFileCreationException {
                
        try {
            File directory = new File(store);

            if(!directory.getParentFile().exists()){

                 directory.getParentFile().mkdir();
                
            }
            
            if(!directory.exists()){

                 directory.mkdir();
                
            }
        } catch (SecurityException se) {
            if (debug) se.printStackTrace();
            throw new FlatFileCreationException(
                    PropertiesSingleton.getInstance().getProperty("txtDirWriteIssue") + 
                    store
                );
        }

    }
    
    
    
    
    /**
    * Unused for this IConsumer type.
    *
    * @throws     FlatFileCreationException    Not used in this implementation.
    */
    public void disconnectStore() throws FlatFileCreationException {
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
    private String sanitise(String string, int level) {

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

    
    
        
    //--------DECRECATED METHODS------------------------------------------------
    
    
    
    
    /**
    * Adds a dataset to the relevant file in the store directory.
    * <p>
    * @deprecated
    * Only here to satisfy deprecated interface demands conditional on 
    * other classes. Redirects to bulkLoad.
    *
    * @param     dataset                        Dataset to load.
    * @throws    FlatFileCreationException      Only if there is an issue.
    */
    @Deprecated
    public void load(IDataset dataset) throws FlatFileCreationException {
        
        bulkLoad(dataset);

    }    



    
}