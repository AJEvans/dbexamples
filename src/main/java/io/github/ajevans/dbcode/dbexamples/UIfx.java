/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbexamples;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.HashSet; 
import java.util.List;
import java.util.Locale; 
import java.util.Properties; 
import java.util.Set; 

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import io.github.ajevans.dbcode.data.io.DataLinker;
import io.github.ajevans.dbcode.data.io.DataException;
import io.github.ajevans.dbcode.data.io.IDataConsumer;
import io.github.ajevans.dbcode.data.io.IDataSupplier;
import io.github.ajevans.dbcode.dbconsumers.DerbyConsumer;
import io.github.ajevans.dbcode.dbconsumers.FlatFileConsumer;
import io.github.ajevans.dbcode.filesuppliers.CruTs2pt1Supplier;
import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* GUI and workhorse for application.
* <p>
* Interface can be internationalised by changing the locale to a 2-character 
* country language code in the <code>/META-INF/application.properties</code> file 
* and supplying an appropriate equivalent to <code>/META-INF/en.properties</code>.
* <p>
* Defaults and test data can also be set in 
* <code>/META-INF/application.properties</code>.
* <p>
* DataSuppliers and DataConsumers are added to the open and options menus of the 
* GUI at runtime. To add one:
* <ul>
* <li>Implement IDataSupplier/IDataConsumer with a zero-parameter constructor 
* only.</li>
* <li>Place the class file in the classpath for this application.</li>
* <li>Add the IDataSupplier/Consumer to the 
* <code>/META-INF/en-datasuppliers.properties</code> or 
* <code>/META-INF/en-dataconsumers.properties</code> file (where "en" can be 
* replaced by the relevant country language code) 
* in the format:<br>
* <code>full.class.name.DataSupplier=Open Menu Text</code><br>
* for example:<br>
* <code>io.github.ajevans.dbcode.dataparsers.CruTs2pt1Parser=CRU TS 2.1...</code></li>
* </ul>
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class UIfx extends Application {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------
    
    
    
    
    /**
    * Debugging flag, set by System variable passed in at command line:
    * <p>
    * <code>jar -jar -Ddebug="true" jarLocation.jar</code> 
    * <p>
    * rather than setting here / with accessor. Default "false".
    */
    private boolean debug = false;
    
    
    /**
    * File location for properties files in jar / install directories.
    * Note that "/" should work in Java on all platforms and from jars.
    */
    private String propertiesFileLocation = "/META-INF/";
        
    
    /**
    * Main GUI Stage.
    */
    private Stage stage = null;     // Class level for use in e.g. FileChooser.
    
    
    /**
    * Progressbar to listen to processing classes.
    */
    private ProgressBar progressBar = new ProgressBar();


    /**
    * Objects that can supply data - loaded at runtime from .properties file.
    */
    private ArrayList<IDataSupplier> dataSuppliers = new ArrayList<>(); 


    /**
    * Text for menu items about objects that can supply data - 
    * loaded from .properties file.
    */
    private ArrayList<String> dataSuppliersText = new ArrayList<>();


    /**
    * Objects that can consume data - loaded at runtime from .properties file.
    */
    private ArrayList<IDataConsumer> dataConsumers = new ArrayList<>();
    

    /**
    * Text for menu items about objects that can consumer data - 
    * loaded from .properties file.
    */
    private ArrayList<String> dataConsumersText = new ArrayList<>();


    /**
    * Objects currently chosen on the output menu.
    */
    private IDataConsumer currentConsumer = null;    


    /**
    * Directory to write local results to.
    * <p>
    * NB: Should be null here so the store in consumers is null 
    * at start and defaults used. 
    */
    private File outputDirectory = null;    


    /**
    * Menu item, here so can be enabled.
    */
    private MenuItem saveLogMenuItem = null;
    
    
    /**
    * Locale, read from application.properties. 
    * <p>
    * Used for gaining language.
    */
    private Locale locale = null; 


    /**
    * Text area for messages.
    */
    private TextArea textArea = null;




    //--------MAJOR METHODS-----------------------------------------------------
    



    /**
    * Default constructor.
    * <p>
    * Private as static main called to run application.
    *
    * @see io.github.ajevans.dbcode.dbexamples.DBExamples
    */
    private void UIfx() {}




    /**
    * Launches GUI.
    *
    * @param    args        Command line arguments, unused.
    */
    public static void main(String[] args) {
        
        launch();
    
    }
    
    
    
    
    /**
    * Initialises everything and starts GUI.
    * <p>
    * Not for calling; activated by the virtual machine on 
    * use of <code>launch</code> in <code>main</code>.
    * 
    * @param    stage        Passed in Javafx stage.
    */
    @Override
    public void start(Stage stage) {
        
        debug = DebugMode.getDebugMode();
 
        this.stage = stage;
        
        getLocalisedGUIText();
        getDataProcessors(locale.getLanguage() + "-datasuppliers.properties");
        getDataProcessors(locale.getLanguage() + "-dataconsumers.properties");
        buildGUI(stage);
        
        stage.show();
        
    }
    
    
    
    
    /**
    * Builds and displays the GUI.
    * <p>
    * Not for calling; called by <code>start</code>. 
    * Utilises localized and loaded-class text for GUI from 
    * <code>getLocalisedGUIText</code> and <code>getDataSuppliers</code>, both 
    * of which need calling first. 
    * 
    * @param    stage        Javafx stage passed in via <code>start</code>.
    */
    private void buildGUI(Stage stage) {
        
        
        // PropertiesSingleton full of properties read in from files 
        // in getLocalisedGUIText.
        stage.setTitle(
                PropertiesSingleton.getInstance().getProperty("txtAppName")
            );

        
        MenuBar menuBar = new MenuBar(); 
        Label outputLabel = new Label("Currently outputing to...");
        
        // File menu-----------------------------
        
        
        Menu fileMenu = new Menu(
                PropertiesSingleton.getInstance().getProperty("txtFileMenu")
            ); 
                
        // "Open" menu will be a sub-menu of the fileMenu.
        Menu openMenu = new Menu(
                PropertiesSingleton.getInstance().getProperty("txtOpenMenu")
            ); 
        
        
        // Menu text and classes for data suppliers are read from 
        // xx-datasuppliers.properties in getDataSuppliers() where xx is 
        // a localisation language indicator like "en". Note not read into 
        // easier PropertiesSingleton as easier to just iterate through these 
        // on their own. The menu listeners for these menu items essentially 
        // start the processing.
            
        
        // Add menu items for each data supplier.
                
        // NB: In the event this looks amenable to refactoring with 
        // alternative loop structures and/or an anonymous class, don't - 
        // it isn't: any refactoring causes issues with test classes.
        MenuItem tempMenuItem = null;
        MenuHandler openEventHandler = null;
        for (int i = 0; i < dataSuppliersText.size(); i++) {
            tempMenuItem = new MenuItem(dataSuppliersText.get(i));
            openEventHandler = new MenuHandler();            
            openEventHandler.setup(dataSuppliers.get(i), this);
            tempMenuItem.setOnAction(openEventHandler); 
            openMenu.getItems().add(tempMenuItem); 
        }
        
        fileMenu.getItems().add(openMenu);

        saveLogMenuItem = new MenuItem(
            PropertiesSingleton.getInstance().getProperty("txtSaveLogMenuItem")
        );
        saveLogMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                saveLog();
            }
        });   
        saveLogMenuItem.setDisable​(true);
        fileMenu.getItems().add(saveLogMenuItem);


        // Options menu--------------------------

                                    
        Menu optionsMenu = new Menu(
                PropertiesSingleton.getInstance().getProperty("txtOptionsMenu")     
            );

        // "Output to..." menu will be a sub-menu of the optionsMenu.
        Menu outputMenu = new Menu(
                PropertiesSingleton.getInstance().getProperty("txtOutputMenu")
            );                                     
        

        // Again, we load the potential consumers at runtime 
        // as we did for the suppliers, only this time we set 
        // it up as a radio button style menu, defaulting to the 
        // first in the list. The menu items essentially end up 
        // setting a currentConsumer instance variable which is 
        // then used in processing.
        
        ToggleGroup toggleGroup = new ToggleGroup();
        RadioMenuItem tempRadioMenuItem = null;

        String dialogBarText = PropertiesSingleton.getInstance().getProperty("txtOutputSetTo");

        // For some reason, menus add and push down, so we 
        // need to load in reverse!
        for (int i = dataConsumersText.size() - 1; i >= 0; i--) {
            tempRadioMenuItem = new RadioMenuItem(dataConsumersText.get(i));
            tempRadioMenuItem.setToggleGroup(toggleGroup);
            if (i == dataConsumersText.size() - 1) {
                tempRadioMenuItem.setSelected​(true);
                currentConsumer = dataConsumers.get(i);
                outputLabel.setText(
                    dialogBarText + 
                    dataConsumersText.get(i)
                );
            }
            openEventHandler = new MenuHandler(); 
            openEventHandler.setup(dataConsumers.get(i), outputLabel, this);
            tempRadioMenuItem.setOnAction(openEventHandler); 
            tempRadioMenuItem.setOnAction(openEventHandler); 
            outputMenu.getItems().add(tempRadioMenuItem); 
        }



        optionsMenu.getItems().add(outputMenu);

        // "Set directory" menu will be on the openMenu.
        MenuItem setDirectoryMenuItem = new MenuItem(
                PropertiesSingleton.getInstance().getProperty("txtSetDirectory")
            );
            
        setDirectoryMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                setDirectory();
            }
        });         
        
        optionsMenu.getItems().add(setDirectoryMenuItem); 
        

        // Add menus.

        menuBar.getMenus().add(fileMenu); 
        menuBar.getMenus().add(optionsMenu);
  
        // Layout and text area------------------

        VBox vb = new VBox(menuBar); 
        vb.setFillWidth​(true);
        vb.setId("-fx-vb");                     // For CSS.
        ObservableList vbNodeList = vb.getChildren(); 
        
        textArea = new TextArea();
        textArea.setText(PropertiesSingleton.getInstance().getProperty("txtWelcome"));
        textArea.setId("-fx-text-area");        // For CSS.
        VBox.setVgrow(textArea, Priority.ALWAYS);
        
        
        if (!debug) {
            // Normally supress errors. Shouldn't be any at this stage, 
            // except JVM errors, which do arise if Hadoop is used - it 
            // produces reflection access errors as standard for non-native 
            // versions. Not ideal, but the likelihood is low and the 
            // software can be put into debug mode.
            System.err.close();
            System.out.close();
        }
        
        vbNodeList.add(textArea);  


        // Dialog bar at base of app.
        

        progressBar.setProgress(0);
        
        AnchorPane anchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(progressBar, 0.0);
        AnchorPane.setLeftAnchor(progressBar, 0.0);
        AnchorPane.setTopAnchor(outputLabel, 1.0);
        AnchorPane.setRightAnchor(outputLabel, 10.0);
        anchorPane.getChildren().addAll(progressBar, outputLabel);
        progressBar.prefWidthProperty().bind(vb.widthProperty()); 
        
        vbNodeList.add(anchorPane);  
        
        // Pull it all together and style the JavaFX using CSS.
        
        Scene scene = new Scene(vb, 500, 300); 
        scene.getStylesheets().add(
            getClass().getResource(propertiesFileLocation + "gui.css").toExternalForm());
        stage.setScene(scene); 
        
    }
    
    
    
    
    /**
    * Allows user to save screen to log file.
    *
    */
    public void saveLog() {
        
        // Get file location.
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(
                PropertiesSingleton.getInstance().getProperty("txtSaveLogChooser")
            );
        try {                            
            fileChooser.setInitialDirectory​(
                                     new File(System.getProperty("user.home")));
        } catch (Exception e) {
            // If it doesn't work it should default to app opening directory.
            if (debug) e.printStackTrace();
        }
        fileChooser.setInitialFileName​("fileConversionLog.txt");
        
        File file = fileChooser.showSaveDialog​(stage);
        
        // Write file.
        
        if (file != null) {
            try {
                FileWriter writer = new FileWriter(file);
                BufferedWriter buffer = new BufferedWriter(writer);
                buffer.write(textArea.getText());
                buffer.close();
            } catch (IOException ioe) {
                if (debug) ioe.printStackTrace();
                alert(PropertiesSingleton.getInstance().getProperty("txtSaveLogIssue"));
            }
        }
        
    }
    
    
    
    
    /**
    * Opens directory-open dialog to pick an output directory.
    *
    */
    public void setDirectory(){
        
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle(
                PropertiesSingleton.getInstance().getProperty("txtSaveLogChooser")
            );
        try {                            
            fileChooser.setInitialDirectory​(
                                     new File(System.getProperty("user.home")));
        } catch (Exception e) {
            // If it doesn't work it should default to app opening directory.
            if (debug) e.printStackTrace();
        }
        
        File directory = fileChooser.showDialog​(stage);
        
             
        if (directory != null) {
            outputDirectory = directory;
        }
    }
    
    
    
    
    /**
    * Opens file-open dialog to choose input.
    * <p>
    * Users can open multiple files. 
    * Passes files, if chosen, to the appropriate 
    * {@link io.github.ajevans.dbcode.data.io.IDataSupplier} 
    * via <code>run</code>. Private, but called from <code>MenuItems</code> 
    * via a inner <code>MenuHandler</code> class objects. 
    *
    * @param     dataSupplier            Data supplier.
    * @see       #buildGUI(Stage stage) 
    * @todo      Although we catch unreadable files down the line, it would 
    *            be nice to filter <em>out</em> known unusable file extensions here.  
    *            Unfortunately none of the default filechooser systems allow this,  
    *            though for a production version it might be worth implementing 
    *            this <a href="https://stackoverflow.com/questions/27436534/javafx-filechooser-exclude-extensions">StackOverflow</a> 
    *            solution by <a href="https://stackoverflow.com/users/3356804/vian">vian</a>.
    * @todo      Code within this method is a candidate for refactoring - see 
    *            src code.
    */
    private void openFile(IDataSupplier dataSupplier) {
        
        // We might be opening a new file to use with 
        // an old in-memory dataConsumer, so reset it or 
        // it'll use the old recordStoreNames etc. 
        // This needs refactoring really - there's little 
        // point in holding instances in the menu if all 
        // we're going to use them for is their name here - we 
        // might as well just hold the names. 
        
        String consumerClass = currentConsumer.getClass().getName();
        String supplierClass = dataSupplier.getClass().getName();
        

        currentConsumer = (IDataConsumer)getClassByName(consumerClass);
        dataSupplier = (IDataSupplier)getClassByName(supplierClass);
        
                            

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(
                PropertiesSingleton.getInstance().getProperty("txtOpenChooser")
            );
                                                                
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        
        if ((files != null) && (files.get(0) != null)) {

        
            // It's almost impossible to pick files from more than 
            // one directory, but just incase we add the files' directory paths 
            // to a hashset which should only allow in unique entries. We can then 
            // check its size to make sure only one directory path has been chosen.
            Set<File> paths = new HashSet<File>();
            ArrayList<String> filenames = new ArrayList<>();
            for (File file: files) {
                if (!file.isDirectory()) {    
                    paths.add(file.getParentFile());
                }
                filenames.add(file.getName());
            }
            if (paths.size() > 1) {
                alert(PropertiesSingleton.getInstance().getProperty("txtOneDirectory")); 
                return;
            }

            // If we're ok, go ahead with the processing.
            
            // Set up the data supplier.
            
            try {
                dataSupplier.setSource((File)(paths.toArray()[0]));
                dataSupplier.setRecordHolderNames(filenames);
            } catch (DataException de) {
                alert(PropertiesSingleton.getInstance().getProperty("txtPathIssue")); 
                return;
            }
            
            // Set up the data linker.
            
            DataLinker dataLinker = null;
            
            
            // Set the worker dataLinker and register the GUI for its 
            // message, exception and progress updates.
            dataLinker = new DataLinker(dataSupplier, currentConsumer);
            progressBar.progressProperty().bind(dataLinker.progressProperty());
            textArea.textProperty().bind(dataLinker.messageProperty());
            // The following exception handler is the ultimate destination 
            // of all exceptions thrown by the dataLinker task/processing.
            ExceptionHandler exceptionHandler = new ExceptionHandler();
            exceptionHandler.setup(progressBar, dataLinker, this);
            dataLinker.exceptionProperty().addListener(exceptionHandler);
            

            // Run the task process in separate thread.
            Thread th = new Thread(dataLinker);
            th.setDaemon(true);
            th.start();
                
            saveLogMenuItem.setDisable(false);   

        }
       
    }




    //--------ACCESSOR / MUTATOR METHODS----------------------------------------
    



    /**
    * Mutator for currently chosen consumer on "output" menu.
    * <p>
    * Called by menu handler of output menu.
    *
    * @param     consumer    Consumer to set.
    *
    */
    private void setCurrentConsumer(IDataConsumer consumer) {
    
        this.currentConsumer = consumer;
    
    }




    //--------UTILITY METHODS---------------------------------------------------
    



    /**
    * Localization for GUI elements.
    * <p>
    * Takes language option from project/jar 
    * <code>/META-INF/application.properties</code> 
    * and then language-specific terms from e.g. <code>en.properties</code>
    * <p>
    * Defaults to English; defaults set in private instance variables.
    *
    * @todo Pick language on first use and download appropriate properties file.
    */
    private void getLocalisedGUIText() {

        try {
            PropertiesSingleton.getInstance().addProperties(
                            propertiesFileLocation + "application.properties" 
                        );
        } catch (IOException ioe) {
            // Defaults are provided.
            if (debug) ioe.printStackTrace();
        }


        String temp = PropertiesSingleton.getInstance().getProperty("locale");
        locale = new Locale(temp);
        
        try {
            PropertiesSingleton.getInstance().addProperties(
                    propertiesFileLocation + locale.getLanguage() + ".properties"
                );
        } catch (IOException ioe) {
            // Defaults are provided.
            if (debug) ioe.printStackTrace();
        }
        
        
        gapFillLocalisedGUIText();
        
    }




    /**
    * Sets the defaults for GUI, warnings, and exceptions in English if an appropriate 
    * language properties file is missing.
    *
    */
    private void gapFillLocalisedGUIText() {
    
        Properties defaults = new Properties();
        
        // GUI elements
        defaults.setProperty("locale","en");
        defaults.setProperty("txtAppName", "DBExamples");
        defaults.setProperty("txtWelcome", "Welcome - open a file to read into a local database.");
        defaults.setProperty("txtFileMenu", "File");
        defaults.setProperty("txtOpenMenu", "Open");
        defaults.setProperty("txtSaveLogMenuItem", "Save current screen as log file...");
        defaults.setProperty("txtSaveLogChooser", "Save log as file");
        defaults.setProperty("txtOpenChooser", "Choose a file to open");
        defaults.setProperty("txtSetDirectory", "Set the directory for local outputs...");
        defaults.setProperty("txtSaveDirectory", "Pick a directory for local outputs...");
        defaults.setProperty("txtFinished", "Finished.");
        defaults.setProperty("txtOutputSetTo", "Currently set to output to ");
        defaults.setProperty("txtError", "Error");
        
        // Warnings
        defaults.setProperty("txtSaveLogIssue", "Issue writing file; please " + 
                                                "check you have permission to " + 
                                                "write to this directory."
                                            );
        defaults.setProperty("txtOneDirectory", "Please only select files " +
                                                "from one directory to avoid " + 
                                                "picking files with the same " +
                                                "name."
                                            );
        defaults.setProperty("txtSupplyConsumeMissing", "Data processors missing. " +
                                                        "Software corrupted: " + 
                                                        "please re-download the " +
                                                        "software."
                                                    );
         defaults.setProperty("txtPathIssue", "There seems to be an issue with " +
                                              "the file/s you chose: please " + 
                                              "choose again. "
                                            );
                                                                                                     
        PropertiesSingleton.getInstance().setDefaults(defaults);

    }
    



    /**
    * Loads the classes and menu text for available data suppliers and 
    * consumers.
    * <p>
    * For details of the associated files needed to add suppliers and  
    * consumers, see class-level docs.
    *
    * @param     fileName    Either "datasuppliers.properties" or 
    *                                "dataconsumers.properties".
    */
    private void getDataProcessors(String fileName) {

        // Load data supplier details from properties file.

        // Easier not to use the properties singleton used for other properties, here, 
        // as simplier to loop just through these elsewhere.
        InputStream is = getClass().getResourceAsStream(
                            propertiesFileLocation + fileName 
                        );
        
        String missingMessage = 
            PropertiesSingleton.getInstance().getProperty("txtSupplyConsumeMissing");
            
        Properties properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            alert(missingMessage);
        }
        
        // Store the menu text; turn each class string into an object and store that 
        // as well.
        
        properties.forEach((key,value) -> {

            if (fileName.substring(3).equals("datasuppliers.properties")) {
                dataSuppliersText.add((String)value);
                dataSuppliers.add((IDataSupplier)getClassByName((String)key));
            } else {
                dataConsumersText.add((String)value);
                dataConsumers.add((IDataConsumer)getClassByName((String)key));
            }

        });
        
    }
    
    
    
    
    /**
    * Instantiates objects by class name.
    *
    * @param    name    Fully qualified class name.
    * @return   Object  Object requested. Cast elsewhere as required.
    */
    private Object getClassByName(String name) {
        
        String missingMessage = 
            PropertiesSingleton.getInstance().getProperty("txtSupplyConsumeMissing");
            
        // Objects.
        try {
            Class myClass = Class.forName(name);
            Constructor constructor = myClass.getConstructor();
            return (Object) constructor.newInstance();
        } catch (ClassNotFoundException cnfe) {
            if (debug) cnfe.printStackTrace();
            alert(missingMessage);
        } catch (NoSuchMethodException nsme) {
            if (debug) nsme.printStackTrace();
            alert(missingMessage);;
        } catch (InstantiationException ie) {
            if (debug) ie.printStackTrace();
            alert(missingMessage);
        } catch (IllegalAccessException iae) {
            if (debug) iae.printStackTrace();
            // Seems unlikely at this stage, so generic alert.
            alert(missingMessage); 
        } catch (InvocationTargetException ite) {
            if (debug) ite.printStackTrace();
            alert(missingMessage);
        }
            
        return null;
        
    }
        
    
    
    
    /**
    * Pops up an error message.
    *
    * @param    message     Message to display.
    */
    private void alert(String message){
                Alert alert = new Alert(
                                Alert.AlertType.ERROR, 
                                message);
                alert.setHeaderText(null);
                alert.setTitle(
                    PropertiesSingleton.getInstance().getProperty("txtError")
                );
                alert.initStyle(StageStyle.UTILITY);
                alert.showAndWait();
    }





    //--------UTILITY CLASSES------------------------------------------------------------
    



    /**
    * Small inner class to deal with menu events.
    * <p>
    * Private inner class as it's of no use to anything else.
    * <p>
    * Needed because of inner-classes final-variables issue.
    */
    private class MenuHandler implements EventHandler<ActionEvent> {


        /**
        * Data supplier to use.
        */
        private IDataSupplier supplier = null;
        
        
        /**
        * Data consumer to use.
        */
        private IDataConsumer consumer = null;
        
        /**
        * To display choice to user. 
        */
        private Label outputLabel = null;
        
        
        /**
        * The object made by the system of the outer class.
        */
        private UIfx uifx = null;
        
        
        
        
        /** 
        * Handles menu events. 
        * <p>
        * Calls <code>UIfx.openFile</code>.
        *
        * @param     menuEvent        Event associated with a menu item.
        */
        public void handle(ActionEvent menuEvent) { 
            
            if (menuEvent.getSource() instanceof RadioMenuItem) {
                uifx.setCurrentConsumer(consumer);
                outputLabel.setText(
                        PropertiesSingleton.getInstance().getProperty("txtOutputSetTo") + 
                        ((MenuItem)menuEvent.getSource()).getText()
                    );
            } else {
                uifx.openFile(supplier);
            }
            
        } 
        
        
        
        
        /**
        * Sets various variables within the handler.
        *
        * @param     supplier  Data supplier to use.
        * @param     uifx      The object made by the system of the outer class.
        */
        public void setup(IDataSupplier supplier, UIfx uifx) {

            this.supplier = supplier;
            this.uifx = uifx;

        }
        
        
        
        
        /**
        * Sets various variables within the handler.
        *
        * @param     consumer       Data consumer to use.
        * @param     outputLabel    To display choice to user.        
        * @param     uifx           The object made by the system of the outer class.
        */
        public void setup(IDataConsumer consumer, Label outputLabel, UIfx uifx) {

            this.consumer = consumer;   
            this.outputLabel = outputLabel;            
            this.uifx = uifx;

        }
        
    }    
    


    /**
    * Small inner class to deal with exceptions from tasks.
    * <p>
    * Private inner class as it's of no use to anything else.
    * <p>
    * Needed because of inner-classes final-variables issue.
    */
    private class ExceptionHandler implements ChangeListener<Throwable> {    
    
    
        /**
        * GUI progress bar.
        */
        private ProgressBar progressBar = null;
        
        
        /**
        * Task used in processing.
        */
        private DataLinker dataLinker = null;
        
        
        /**
        * The object made by the system of the outer class.
        */
        private UIfx uifx = null;
   
   
   
        public void changed(ObservableValue<? extends Throwable> prop, 
                           final Throwable oldValue, final Throwable newValue) {
                               
            if (newValue != null) {

                uifx.alert(((Exception)newValue).getMessage());
                dataLinker.updateAppProgress(0,1);
                dataLinker.updateAppMessage(dataLinker.getMessage() + System.lineSeparator() + "Halted");
                
            } 
        }
        
        
        
        
        /**
        * Sets various variables within the handler.
        *
        * @param     progressBar  GUI progress bar.
        * @param     dataLinker   Task used in processing.
        * @param     uifx         The object made by the system of the outer class.
        */
        public void setup(ProgressBar progressBar, DataLinker dataLinker, UIfx uifx) {

            this.progressBar = progressBar;    
            this.dataLinker = dataLinker;
            this.uifx = uifx;

        }
        
        
        
        
    }
 
 


}