/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.filesuppliers;


import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow​;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Properties;

import io.github.ajevans.dbcode.data.io.DataException;
import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.io.IDataSupplier;
import io.github.ajevans.dbcode.data.io.DataLinker;
import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IMetadata;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.data.structures.Row;
import io.github.ajevans.dbcode.data.structures.Metadata;
import io.github.ajevans.dbcode.data.structures.Table;
import io.github.ajevans.dbcode.data.structures.TabulatedDataset;
import io.github.ajevans.dbcode.dbconsumers.DerbyConsumer;
import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.IReportingListener;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Unit tests for CruTs2pt1Supplier, minus GUI. 
* <p>
* Tests were built for edge conditions for a few specific methods, but 
* the most significant tests run through a full file load.
* <p>
* Reads defaults and first and last lines from <code>application.properties</code> 
* to allow for simple replacement of data for fundamental tests.
* <p>
* JUnit 5.
* <p>
* <strong>Note: because of the nature of the exception testing these 
* tests only work with the English language localisation.</strong>
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Unit tests for CruTs2pt1Supplier, minus GUI")
public class CruTs2pt1SupplierTest { 


    
    
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
    * Record count in default source.
    */
    private int defaultSourceCount  = 0;
    
    
    /**
    * Data supplier to use.
    */
    private CruTs2pt1Supplier dataSupplier  = null;
    
    
    /**
    * Data consumer to use.
    */
    private DerbyConsumer dataConsumer  = null;
    
    
    
    
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
            
            defaultSourceCount = 
                (new Integer(PropertiesSingleton.getInstance().getProperty("defaultSourceCount"))).intValue();
                
            /*
            store = System.getProperty("user.home") + 
                    File.separator + 
                    defaultDatabaseDirectory + 
                    File.separator + 
                    defaultDatasetOriginal + 
                    File.separator;
            */
            System.out.println("[NOTE] These tests will write files in " +  
                                System.getProperty("user.home") + 
                                File.separator + 
                                defaultDatabaseDirectory + 
                                File.separator
                            );
            
            DebugMode.setDebugMode(true);
            
            try {
                connectAndProcess(defaultSource);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
    
    
    /**
    * Connects to a file and processes it.
    * <p>
    * Uses <code>classLoader.getResource()</code> so file must be in 
    * test "resources" directory.
    * 
    * @param    filename    The filename to connect to.
    * @throws   Exception   A wide variety of exceptions - see exception tests.
    */
    private void connectAndProcess(String filename) throws Exception {
            
            dataSupplier  = new CruTs2pt1Supplier();
            dataConsumer  = new DerbyConsumer();
            
            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource(filename).getFile());
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
               throw e;
            }
            
    }
    
    


    /**
    * Releases file.
    *
    */
    @AfterAll
    @DisplayName("Releases file")
    public void disconnect() {
        try {
            dataSupplier.disconnectSource();
        } catch (ParseFailedException pfe) {
            pfe.printStackTrace();
        }
    }



    
    //--------BASIC EDGE CASE ETC TESTS-----------------------------------------
    // Ordered to make sure (1) is done first on with a normal runthrough.
    
    
    
    
    /**
    * This is largely checked in the full runthrough, but here we check the 
    * estimate.
    *
    */
    @Order(1)
    @Test
    @DisplayName("(initialisation) -> record count estimation")
    public void initialisationlTest() {  
    
        int recordCount = dataSupplier.getDataset().getEstimatedRecordCount();
        
        assertEquals(new Integer(recordCount), new Integer(defaultSourceCount));
        
    }
    
    

    
    //--------DATA EDGE CASE ETC TESTS------------------------------------------




    /**
    * Test for data with missing header.
    * <p>
    * Unfortunately, as the end point of a wide variety of exceptions, 
    * the data linker throws generic Exceptions for the GUI to pick up 
    * and display. However, the internal messages vary, so we can test 
    * those.
    */
    @Order(2)
    @Test
    @DisplayName("(initialisation) missing header -> exception")
    public void missingHeaderTest() {  
    
        Exception exception = assertThrows(Exception.class, () -> {
            connectAndProcess("noheader.pre");
        });
     
        String expectedMessage1 = "header";
        String expectedMessage2 = "format";
        String actualMessage = exception.getMessage();

        assertTrue((actualMessage.contains(expectedMessage1) || 
                    actualMessage.contains(expectedMessage2)));
                
    }




    /**
    * Test for file with missing data.
    * <p>
    * Unfortunately, as the end point of a wide variety of exceptions, 
    * the data linker throws generic Exceptions for the GUI to pick up 
    * and display. However, the internal messages vary, so we can test 
    * those.
    */
    @Order(3)
    @Test
    @DisplayName("(initialisation) missing data -> exception")
    public void missingDataTest() {  
    
         Exception exception = assertThrows(Exception.class, () -> {
            connectAndProcess("nodata.pre");
        });
     
        String expectedMessage = "does not appear to contain data";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        
    }




    /**
    * Test for file with a missing data value.
    * <p>
    * Unfortunately, as the end point of a wide variety of exceptions, 
    * the data linker throws generic Exceptions for the GUI to pick up 
    * and display. However, the internal messages vary, so we can test 
    * those.
    */
    @Order(4)
    @Test
    @DisplayName("(initialisation) missing data value -> exception")
    public void missingDataValueTest() {  
    
         Exception exception = assertThrows(Exception.class, () -> {
            connectAndProcess("missingnumber.pre");
        });
  
        String expectedMessage = "missing data";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        
    }
    
    
    
    
    /**
    * Test for file with an extra long line or extra data value.
    * <p>
    * Unfortunately, as the end point of a wide variety of exceptions, 
    * the data linker throws generic Exceptions for the GUI to pick up 
    * and display. However, the internal messages vary, so we can test 
    * those.
    */
    @Order(5)
    @Test
    @DisplayName("(initialisation) extra // long data -> exception")
    public void extraDataValueTest() {  
    
        Exception exception = assertThrows(Exception.class, () -> {
            connectAndProcess("largenumberwrong.pre");
        });
  
        String expectedMessage = "Ill-formatted line, extra";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        
    }

    
    
    
    /**
    * Test for header with invalid date, e.g. 32/13/2004.
    * <p>
    * Unfortunately, as the end point of a wide variety of exceptions, 
    * the data linker throws generic Exceptions for the GUI to pick up 
    * and display. However, the internal messages vary, so we can test 
    * those.
    */
    @Order(6)
    @Test
    @DisplayName("(initialisation) invalid date -> exception")
    public void invalidDateTest() {  
    
        Exception exception = assertThrows(Exception.class, () -> {
            connectAndProcess("invaliddate.pre");
        });
  
        String expectedMessage = "problem with a date";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        
    }




    /**
    * Test for file with a correct five significant figures running into 
    * each other.
    * <p>
    * This should not throw an exception as data is width (not space) delimited.
    *
    */
    @Order(7)
    @Test
    @DisplayName("(initialisation) long but ok data -> no exception")
    public void longDataValuesTest() {  
    
        assertDoesNotThrow​(() -> {connectAndProcess("largenumberright.pre");} );

    }




}



















