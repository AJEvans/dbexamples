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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet; 
import java.util.Set; 

import io.github.ajevans.dbcode.data.io.DataLinker;
import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.io.IDataSupplier;
import io.github.ajevans.dbcode.filesuppliers.CruTs2pt1Supplier;
import io.github.ajevans.dbcode.data.structures.Metadata;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Unit tests for DerbyConsumer, minus GUI. 
* <p>
* Tests were built for edge conditions for a few specific methods, but 
* the most significant tests run through a full database load and 
* check metadata ('title') and the first and last lines of output tables. 
* As data entry is checked with the supplier, these were largely implemented to 
* check soundness during development. 
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
public class DerbyConsumerTest { 


    
    
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
    * Default source file - gained from properties.
    */
    private String defaultSource = null;
    
    
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
    * Database connection - set up prior to all tests.
    */
    private Connection connection = null;
    
    
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
    * Runs the application to build the database.
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
                
            defaultSource = 
                PropertiesSingleton.getInstance().getProperty("defaultSource");
                
            defaultSource1stLine = 
                PropertiesSingleton.getInstance().getProperty("defaultSource1stLine");
                
            defaultSourceLastLine = 
                PropertiesSingleton.getInstance().getProperty("defaultSourceLastLine");
            
            store = System.getProperty("user.home") + 
                    File.separator + 
                    defaultDatabaseDirectory + 
                    File.separator + 
                    defaultDataset + 
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
            
            dataSupplier  = new CruTs2pt1Supplier();
            dataConsumer  = new DerbyConsumer();
           
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
 
        String strUrl = "jdbc:derby:" + store;
        
        try {
            connection = DriverManager.getConnection(strUrl);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        
    }
    



    /**
    * Cuts database connection.
    *
    */
    @AfterAll
    @DisplayName("Disconnect from database")
    public void disconnect() {
    
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
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
        
        String expected = "c:\\Robert   DROP TABLE students --tmp\\";
 
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
        newNames.add("Robert');DROP TABLE students;--");
        newNames.add("iiName");     // Illegal name.
        newNames.add("23Name");     // Illegal name.
        newNames.add(" Name");      // Illegal name.
        
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
        expectedLines.add("ROBERTDROPTABLESTUDENTS");
        expectedLines.add("AIINAME");
        expectedLines.add("A3NAME");
        expectedLines.add("ANAME");        
 
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
    * Test sanitise for a SANITISE_VIGOROUS run.
    *
    */
    @Test
    @DisplayName("(sanitise(SANITISE_VIGOROUS)) unsanitisedString -> sanitisedString")
    public void sanitiseVigorousTest() {
        
        String actual = dataConsumer.sanitise("A,B C$|/", 0);
        String expected = "A,B C   ";        
        assertTrue​(expected.equals(actual));
        
    }
    
    
    
    
    /**
    * Test sanitise for a SANITISE_NAME run.
    *
    */
    @Test
    @DisplayName("(sanitise(SANITISE_NAME)) unsanitisedString -> sanitisedString")
    public void sanitiseNameTest() {
        
        
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Robert');DROP TABLE students;--");
        lines.add("iiName");     // Illegal name.
        lines.add("23Name");     // Illegal name.
        lines.add(" Name");      // Illegal name.
        
        ArrayList<String> actualLines = new ArrayList<>();
        
        for (String line: lines) {
            actualLines.add(dataConsumer.sanitise(line, 1));
        }
        
        ArrayList<String> expectedLines = new ArrayList<>();
        expectedLines.add("ROBERTDROPTABLESTUDENTS");
        expectedLines.add("AIINAME");
        expectedLines.add("A3NAME");
        expectedLines.add("ANAME");        
 
        assertLinesMatch(expectedLines, actualLines);
        
    }
    
    
    
    
     /**
    * Test sanitise for a SANITISE_WEAK run.
    *
    */
    @Test
    @DisplayName("(sanitise(SANITISE_WEAK)) unsanitisedString -> sanitisedString")
    public void sanitiseWeakTest() {
        
        String actual = dataConsumer.sanitise("c:\\Robert');DROP TABLE students;--tmp", 2);
        String expected = "c:\\Robert   DROP TABLE students --tmp";       
        assertTrue​(expected.equals(actual));
        
    }
    


    
    //--------FULL RUN TESTS----------------------------------------------------
    // These should run in order, though in truth it's not especially 
    // important beyond making sense as it reports.
    
    
    
    
    /**
    * Tests whether the database has the table we expect.
    * <p>
    * Uses default table name from the app's META-INF/application.properties file.
    */
    @Order(1)
    @Test
    @DisplayName("Test table exists")
    public void tableExistsTest() {
        
        boolean found = false;
        
        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet rs = databaseMetaData.getTables(null, null, null, null);
            
            while(rs.next()){
                if (rs.getString("TABLE_NAME").equals(defaultTable)) {
                    found = true;
                }
            }

        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        
        assertTrue(found);
        
    }


    

    /**
    * Connects to the default database table and checks the first and last lines.
    * <p>
    * Uses default name from the app's META-INF/application.properties file along 
    * with first and last line from the same file.
    * <p>
    * Validates on first and last lines.
    * 
    */
    @Order(2)
    @Test
    @DisplayName("Data is loaded into database")
    void dataLoadedTest() {

        Statement statement = null;
        try {
            statement = connection.createStatement(
                                        ResultSet.TYPE_SCROLL_SENSITIVE, 
                                        ResultSet.CONCUR_UPDATABLE
                                    );
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        // Execute the statement and get a ResultSet
        ResultSet rs = null;
        try {
            
            rs = statement.executeQuery("SELECT Xref, Yref, Date, Value FROM " + 
                                        defaultTable
                                    );
                                    
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        ArrayList<String> actualLines = new ArrayList<>();
        ArrayList<String> expectedLines = new ArrayList<>();
        
        // Correct expectedLines are read in from properties file in @BeforeAll.
        expectedLines.add(defaultSource1stLine);
        expectedLines.add(defaultSourceLastLine);
        
        
        // Convert the resultset into comparible actualLines Strings.
        
        try {
            rs.first();
            BigDecimal xRef = rs.getBigDecimal​("Xref");
            BigDecimal yRef = rs.getBigDecimal​("Yref");
            Date date = rs.getDate​​("Date");
            BigDecimal value = rs.getBigDecimal​​("Value");
            actualLines.add(xRef.toString() + " " + 
                            yRef.toString() + " " + 
                            date.toString() + " " + 
                            value.toString()
                        );
            rs.last();
            xRef = rs.getBigDecimal​("Xref");
            yRef = rs.getBigDecimal​("Yref");
            date = rs.getDate​​("Date");
            value = rs.getBigDecimal​​("Value");
            actualLines.add(xRef.toString() + " " + 
                            yRef.toString() + " " + 
                            date.toString() + " " + 
                            value.toString()
                        );

        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        

        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        assertLinesMatch​(expectedLines, actualLines);
        
    }




    /**
    * Connects to the default database table and checks the associated 
    * metadata table. 
    * <p>
    * Validates on title.
    * <p>
    * Uses default name from the app's META-INF/application.properties file.
    */
    @Order(3)
    @Test
    @DisplayName("Metadata is loaded into database")
    void metadataLoadedTest() {

        Statement statement = null;
        try {
            statement = connection.createStatement(
                                        ResultSet.TYPE_SCROLL_SENSITIVE, 
                                        ResultSet.CONCUR_UPDATABLE
                                    );
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        // Execute the statement selecting everything in the 
        // table and get a ResultSet.
        
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        try {
            rs = statement.executeQuery("SELECT * FROM " + defaultDataset + "META");
            rsmd = rs.getMetaData();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        
        
        // Print the ResultSet
        String titleValue = "";
        boolean titleNext = false;
        
        try {

            // We're going to look for the metadata category "title" 
            // as its the one we can guarentee.
            
            int numberOfColumns = rsmd.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= numberOfColumns; i++) {
                    String columnValue = rs.getString(i);
                    if (titleNext) {
                        titleValue = columnValue;
                        titleNext = false;
                    }
                    if (columnValue.equals("TITLE")) titleNext = true;
                }
            }
            titleValue = titleValue.replace(".","");
            titleValue = titleValue.replace(" ","");

        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        

        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        assertTrue(defaultDataset.equals(titleValue));

    }

}
