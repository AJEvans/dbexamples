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
* Unit tests for DerbyConsumer, minus GUI, using the slower data push pathway. 
* <p>
* This is just functional tests for the slow-loading large-file push pathway. 
* See DerbyConsumerTest for the majority of tests against this class.
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
public class DerbyConsumerPushTest { 


    
    
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
    private CruTs2pt1Supplier dataSupplier  = new CruTs2pt1Supplier();
    
    
    /**
    * Data consumer to use.
    */
    private DerbyConsumer dataConsumer  = new DerbyConsumer();



    
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
                dataLinker.setMode(DataLinker.PUSH_MODE);
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
