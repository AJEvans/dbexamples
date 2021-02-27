/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbconsumers;


import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

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

import io.github.ajevans.dbcode.data.io.DataLinker;
import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.io.IDataSupplier;
import io.github.ajevans.dbcode.filesuppliers.CruTs2pt1Supplier;
import io.github.ajevans.dbcode.data.structures.Metadata;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Unit tests for HadoopConsumer, minus GUI. 
* <p>
* Tests were built for edge conditions for a few specific methods, but 
* the most significant tests run through a full file load and 
* check metadata ('title') and the first and last lines of output data files. As 
* data entry is checked with the supplier, these were largely implemented to check 
* soundness during development.
* <p>
* Reads defaults and first and last lines from <code>application.properties</code> 
* to allow for simple replacement of data for fundamental tests.
* <p>
* JUnit 5.
* <p>
* Writes files to user directory.
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Unit tests for DerbyConsumer, minus GUI")
public class HadoopConsumerTest { 


    
    
    //--------INSTANCE VARIABLES------------------------------------------------
    
    


    /**
    * Directory for properties files.
    */
    private String propertiesDirectory = "META-INF";
    
        
    /**
    * Directory database is in - gained from properties.
    */
    private String defaultDatabaseDirectory = null;
    
    
    /**
    * Directory files are in - gained from properties.
    */
    private String defaultFileDirectory = null;
    
    
    /**
    * Default database name - gained from properties.
    */
    private String defaultDataset = null;
    
    
    /**
    * Default table name - gained from properties.
    */
    private String defaultTable = null;
    

    /**
    * Default table name without characters removed - gained from properties.
    */
    private String defaultTableOriginal = null;
    
    
    /**
    * Default source file - gained from properties.
    */
    private String defaultSource = null;


    /**
    * Default source file without characters removed - gained from properties.
    */
    private String defaultDatasetOriginal = null;
        
    
    /**
    * Default source file - gained from properties.
    */
    private String defaultSource1stLine = null;
    
    
    /**
    * Default source file - gained from properties.
    */
    private String defaultSourceLastLine = null;
    
    
    /**
    * Database location - constructed from default variables.
    */
    private String store = null; 
    
    
    /**
    * Data supplier to use.
    */
    private CruTs2pt1Supplier dataSupplier  = new CruTs2pt1Supplier();
    
    
    /**
    * Data consumer to use.
    */
    private HadoopConsumer dataConsumer  = new HadoopConsumer();
    
    
    /**
    * HDFS.
    */
    FileSystem fileSystem = null;
    
    
    /**
    * Configuration.
    */
    Configuration conf = null;


    
    //--------LIFECYCLE METHODS-------------------------------------------------
    
    


    /**
    * Runs the application to build data files.
    *
    * Uses default locations from the app's META-INF/application.properties file.
    */
    @BeforeAll
    @DisplayName("Load data")
    void loadData() {

        // Get the properties file for the app and read out the default values.

        try {
            
            PropertiesSingleton.getInstance().addProperties(
                    propertiesDirectory + File.separator + "application.properties"
                );
            
            defaultDatabaseDirectory = 
                PropertiesSingleton.getInstance().getProperty("defaultDatabaseDirectory");
            
            defaultFileDirectory = 
                PropertiesSingleton.getInstance().getProperty("defaultFileDirectory");
                
            defaultDataset = 
                PropertiesSingleton.getInstance().getProperty("defaultDataset");
                
            defaultTable = 
                PropertiesSingleton.getInstance().getProperty("defaultTable");

            defaultTableOriginal = 
                PropertiesSingleton.getInstance().getProperty("defaultTableOriginal");
                
            defaultSource = 
                PropertiesSingleton.getInstance().getProperty("defaultSource");

            defaultDatasetOriginal = 
                PropertiesSingleton.getInstance().getProperty("defaultDatasetOriginal");
                
            defaultSource1stLine = 
                PropertiesSingleton.getInstance().getProperty("defaultSource1stLine");
                
            defaultSourceLastLine = 
                PropertiesSingleton.getInstance().getProperty("defaultSourceLastLine");
            
            store = System.getProperty("user.home") + 
                    File.separator + 
                    defaultDatabaseDirectory + 
                    File.separator + 
                    defaultDatasetOriginal + 
                    File.separator;
            
            System.out.println("[NOTE] These tests will write files in " +  
                                System.getProperty("user.home") + 
                                File.separator + 
                                defaultDatabaseDirectory + 
                                File.separator
                            );
            
            
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(defaultSource).getFile());
            File absolutePath = file.getAbsoluteFile();
            
            // setRecordHolderNames method takes in ArrayList of files, so...

            ArrayList<String> filenames = new ArrayList<>();
            filenames.add(absolutePath.getName());
                      
            // Go ahead with the processing.
           
            try {
                dataSupplier.setSource(absolutePath.getParentFile());
                dataSupplier.setRecordHolderNames(filenames);
            } catch (Exception de) {
                System.out.println(de.getMessage());
            }
            
            DataLinker dataLinker = null;
            
            try {
                
                // Set the worker dataLinker and register the GUI for its 
                // message, exception and progress updates.
                dataLinker = new DataLinker(dataSupplier, dataConsumer);
               
            } catch (Exception de) {
                // Print message passed up by dataLinker.
                // This is ultimate destination of most exceptions 
                // in the system.
                System.out.println(de.getMessage());
            }

            // Run the app in headless mode.
            
            try{
                dataLinker.process();
            } catch (Exception e) {
                e.printStackTrace();
            }

            
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        
        try {
            
            conf = new Configuration();
            conf.setBoolean("dfs.support.append", true);
          
            try {
                fileSystem = FileSystem.get(conf);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            
            
        } catch (SecurityException se) {
             se.printStackTrace();
        }

    }
    



    /**
    * Cuts file system connection.
    *
    */
    @AfterAll
    @DisplayName("Disconnect from file system")
    public void disconnect() {
    
        try {
            fileSystem.close();
        } catch (IOException ioe) {
           ioe.printStackTrace();
        }
    
    }



    
    //--------BASIC EDGE CASE ETC TESTS-----------------------------------------
    
    
    
    
    /**
    * Tests setStore flags empty Strings as null.
    *
    */
    @Test
    @DisplayName("(setStore) \"\" -> null")
    public void setStoreAsNullTest() {
        
        // As this runs in random sequence, store 
        // current value and return at end.
        String currentStore = dataConsumer.getStore();
        
        try {
            dataConsumer.setStore("");
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        String newStore = dataConsumer.getStore();
        
        try {
            dataConsumer.setStore(currentStore);
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        assertNull(newStore);
        
    }
    
    
    
    
    /**
    * Tests setStore and its sanitisation and separator add.
    *
    */
    @Test
    @DisplayName("(setStore) unsanitisedString -> sanitisedString")
    public void setStoreSanitiseTest() {
        
        // As this runs in random sequence, store 
        // current value and return at end.
        String currentStore = dataConsumer.getStore();
        
        try {
            dataConsumer.setStore("c:\\Robert');DROP TABLE students;--tmp");
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        String newStore = dataConsumer.getStore();
        
        try {
            dataConsumer.setStore(currentStore);
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
      
        String expected = "c:\\Robert');DROP TABLE students;--tmp\\";
 
        assertTrue​(expected.equals(newStore));
        
    }
    
    
    
    
    /**
    * Just checks sending setRecordStoreNames null returns early leaving 
    * them as null.
    *
    * @deprecated       Was needed, now of limited excitment.
    */
    @Test
    @DisplayName("(setRecordStoreNames) null -> null")
    @Deprecated
    public void setRecordStoreNamesAsNullTest() {
        
        // As this runs in random sequence, store 
        // current value and return at end.
        ArrayList<String> currentNames = dataConsumer.getRecordStoreNames();
        
        try {
            dataConsumer.setRecordStoreNames(null);
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        ArrayList<String>  newStore = dataConsumer.getRecordStoreNames();
        
        try {
            dataConsumer.setRecordStoreNames(currentNames);
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        assertNull(newStore);
        
    }
    
    
    
    
    /**
    * Tests setRecordStoreNames and its sanitisation.
    * 
    */
    @Test
    @DisplayName("(setRecordStoreNames) unsanitisedStrings -> sanitisedStrings")
    public void setRecordStoreNamesSanitiseTest() {
        
         // As this runs in random sequence, store 
        // current values and return at end.
        ArrayList<String> currentNames = dataConsumer.getRecordStoreNames();
        
        ArrayList<String> newNames = new ArrayList<>();
        newNames.add("a\"b<c/");
        newNames.add("abc.");     // Illegal name.
        newNames.add("abc ");     // Illegal name.
        newNames.add("CON");      // Illegal name.
        
        try {
            dataConsumer.setRecordStoreNames(newNames);
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        ArrayList<String> actualLines = dataConsumer.getRecordStoreNames();
        
        try {
            dataConsumer.setRecordStoreNames(currentNames);
        } catch (DBCreationException dbe) {
            dbe.printStackTrace();
        }
        
        ArrayList<String> expectedLines = new ArrayList<>();
        expectedLines.add("a-b-c-");
        expectedLines.add("abc");
        expectedLines.add("abc");
        expectedLines.add("Data-CON");     
 
        assertLinesMatch(expectedLines, actualLines);
        
    }
    
    
    
    
    /**
    * Tests whether a missing title setup returns the default.
    * <p>
    * Note that this is not the same as returning a default for 
    * an empty string (the starting point for Metadata), though maybe 
    * it should be. 
    *
    */
    @Test
    @DisplayName("(findTitle) null -> DEFAULT")
    public void findTitleDefaultTest() {
        
        Metadata metadata = new Metadata();
        String expected = "DEFAULT";
        metadata.setTitle(null);
        String actual = dataConsumer.findTitle(metadata);
        
        assertTrue​(expected.equals(actual));
        
    }
    
    
    
    
    /**
    * Tests whether the consumer can find the database title in metadata.
    *
    */
    @Test
    @DisplayName("(findTitle) title -> title")
    public void findTitleTest() {
        
        Metadata metadata = new Metadata();
        String expected = "This is a title";
        metadata.setTitle(expected);
        String actual = dataConsumer.findTitle(metadata);
        
        assertTrue​(expected.equals(actual));
        
    }
        
    
    
    
    /**
    * Test sanitise for a SANITISE_DIRPATH run.
    *
    */
    @Test
    @DisplayName("(sanitise(SANITISE_DIRPATH)) unsanitisedString -> sanitisedString")
    public void sanitiseVigorousTest() {
        
        String actual = dataConsumer.sanitise("a//?b\"c*", 0);
        String expected = "a//-b-c-";        

        assertTrue​(expected.equals(actual));
        
    }
    
    
    
    
    /**
    * Test sanitise for a SANITISE_FILENAME run.
    *
    */
    @Test
    @DisplayName("(sanitise(SANITISE_FILENAME)) unsanitisedString -> sanitisedString")
    public void sanitiseNameTest() {
        
        
        ArrayList<String> lines = new ArrayList<>();
        lines.add("a\"b<c/");
        lines.add("abc.");     // Illegal name.
        lines.add("abc ");     // Illegal name.
        lines.add("CON");      // Illegal name.
        
        ArrayList<String> actualLines = new ArrayList<>();
        
        for (String line: lines) {
            actualLines.add(dataConsumer.sanitise(line, 1));
        }
        
        ArrayList<String> expectedLines = new ArrayList<>();
        expectedLines.add("a-b-c-");
        expectedLines.add("abc");
        expectedLines.add("abc");
        expectedLines.add("Data-CON");        

        assertLinesMatch(expectedLines, actualLines);
        
    }
    
    
    
    
     /**
    * Test sanitise for a SANITISE_DATA run.
    *
    */
    @Test
    @DisplayName("(sanitise(SANITISE_DATA)) unsanitisedString -> sanitisedString")
    public void sanitiseWeakTest() {
        
        String actual = dataConsumer.sanitise("a,b,c", 2);
        String expected = "a;b;c";       
        assertTrue​(expected.equals(actual));
        
    }
    


    
    //--------FULL RUN TESTS----------------------------------------------------
    // These should run in order, though in truth it's not especially 
    // important beyond making sense as it reports.
    


    

    /**
    * Connects to the default data file and checks the first and last lines.
    * <p>
    * Uses default names from the app's META-INF/application.properties file along 
    * with first and last line from the same file.
    * <p>
    * Validates on first and last lines.
    * 
    */
    @Order(1)
    @Test
    @DisplayName("Data is loaded into file system")
    void dataLoadedTest() {

        Path path = new Path(store + defaultTableOriginal);
        ArrayList<String> actualLines = new ArrayList<>();
        ArrayList<String> expectedLines = new ArrayList<>();
        
        // Correct expectedLines are read in from properties file in @BeforeAll.
        expectedLines.add(defaultSource1stLine);
        expectedLines.add(defaultSourceLastLine);
        
        
        
        BufferedReader buffer = null;
        String line = null;
        
        try {
            
            buffer = new BufferedReader(new InputStreamReader(fileSystem.open(path)));
              
            // Find first and last lines.
              
            line = buffer.readLine();  // Header 
            String lastLine = null;
            line = buffer.readLine();
            line = UStoISODate(line);
            line = line.replace(","," ");
            actualLines.add(line);

            while (line != null){
                lastLine = line;
                line = buffer.readLine();
            }
            lastLine = UStoISODate(lastLine);
            lastLine = lastLine.replace(","," ");
            actualLines.add(lastLine);
  
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                buffer.close();
            } catch (IOException ioe2) {
                ioe2.printStackTrace();
            }
        }

        assertLinesMatch​(expectedLines, actualLines);

    }




    /**
    * Converts strings in the format "XX,YY,mm/dd/yyy,ZZ" to the format 
    * "XX,YY,yyyy-MM-dd,ZZ", ie, from US date format to ISO 8601.
    *
    * @param    line        Line to convert.
    * @return   String      Converted line.
    */
    private String UStoISODate(String line) {
        
        int firstComma = line.indexOf(",");
        int secondComma = line.indexOf(",", firstComma + 1);
        
        String lineStart = line.substring(0, secondComma);
        String lineEnd = line.substring(line.indexOf(",", secondComma + 1) + 1);
        String dateString = line.substring(secondComma + 1, line.indexOf(",", secondComma + 1));
        
        String year = dateString.substring(6); 
        String month = dateString.substring(0,2);  
        String day = dateString.substring(3,5);        
        String newDateString = year + "-" + month + "-" + day;
        
        line = lineStart + "," + newDateString + "," + lineEnd;
        
        return line;
        
    }




    /**
    * Checks the default file's metadata. 
    * <p>
    * Validates on title.
    * <p>
    * Uses default names from the app's META-INF/application.properties file.
    */
    @Order(2)
    @Test
    @DisplayName("Metadata is loaded into file system")
    void metadataLoadedTest() {

        String path = store + defaultTableOriginal + "META";
        
        MapFile.Reader reader = null;
        String titleValue = "";
        
        try {
            reader = new MapFile.Reader(fileSystem, path, conf);
            Text outKey = new Text();
            Text outValue = new Text();
             
            while (reader.next(outKey, outValue)) {
                if (outKey.toString().equalsIgnoreCase("title")) {
                    titleValue = outValue.toString();
                    break;
                }
            }
  
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        titleValue = titleValue.replace(".","");
        titleValue = titleValue.replace(" ","");

        assertTrue(defaultTable.equalsIgnoreCase(titleValue));

    }




}
