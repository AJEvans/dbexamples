/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.io;


import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javafx.application.Platform;
import javafx.concurrent.Task;

import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.IReportingListener;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;



/**
* Connects an <code>IDataSupplier</code> to an <code>IDataConsumer</code>.
* <p>
* Links between <code>IDataSupplier</code> and <code>IDataConsumer</code> 
* abstractions, each with their own strategy-pattern hierarchies of 
* implementing objects chosen by the user/GUI.
* <p>
* Extends the JavaFX Task class; this allows it to be bound to GUI elements and 
* therefore send them data to display; also runs processing as a separate thread from 
* the GUI preventing GUI hanging. 
* <p>
* Note that this includes Task management of exceptions, which can be passed to 
* the GUI. As suppliers and consumers tend to throw all 
* exceptions upwards to this object, this is the end point for most of their 
* exceptions as well.
* <p>
* Note that supplier and consumer should be set up with their basic connections 
* to sources and stores before the data linker is run as a thread. In line with 
* JavaFX threading recommendations the supplier and consumer should not be 
* mutated following the call to <code>run</code> on <code>DataLinker</code> objects 
* to prevent unexpected threading issues.
*
* Instantiations can be run "headless" without threading and the GUI by 
* calling <code>process</code>. 
*
* For testing, the system can be put into <code>PUSH_MODE</code> mode. During this, the 
* data supplier pushes data in small blocks to registered consumer observers rather 
* that making it all available at once through a public method (<code>PULL_MODE</code>). 
* This is significantly slower, but minimises memory overheads. Under the default 
* <code>MEMORY_CHECK_MODE</code>, the system assesses available memory against file 
* sizes and decides how to handle data transfer to avoid running out of memory 
* and causing runtime issues. This can also be set from the command line using:<br> 
* <kbd>-Dmode=0</kbd><br> 
* where <kbd>0</kbd> is <code>PUSH_MODE</code>, or  <kbd>1</kbd> for <code>PULL_MODE</code> 
* or <kbd>2</kbd> for <code>MEMORY_CHECK_MODE</code>.
* 
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
* @see    IDataSupplier
* @see    IDataConsumer
*/
public class DataLinker extends Task<Void> implements IReportingListener {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------
    
    
    
    
    /**
    * Debugging flag, set by System variable passed in <code>-Ddebug=true</code> 
    * rather than setting here / with accessor.
    */
    private boolean debug = false;
    
    
    /**
    * Mode flag, used for forcing the linker into the slower push mode.
    */
    public static final int PUSH_MODE = 0;
    
    
    /**
    * Mode flag, used for forcing the linker into the faster but more 
    * memory-hungrey pull mode.
    */
    public static final int PULL_MODE = 1;
    
    
    /**
    * Mode flag, used for setting the linker to the default mode where it 
    * checks memory against file sizes and decides how to handle data transfer.
    */
    public static final int MEMORY_CHECK_MODE = 2;
    
    
    /**
    * The mode: PUSH_MODE; PULL_MODE; or the default MEMORY_CHECK_MODE.
    */
    private int mode = 2;
    
    
    /**
    * The data supplier to link.
    */
    private IDataSupplier supplier = null;
        
        
    /**
    * The data consumer to link.
    */
    private IDataConsumer consumer = null;
    
    
    
    
    //--------MAJOR METHODS-----------------------------------------------------
    
    
    
    
    /**
    * Generic constructor, private to prevent use in instantiation. 
    * <p>
    * Linker must be supplied with supplier and consumer - use 
    * <code>DataLinker(IDataSupplier supplier, IDataConsumer consumer)</code>.
    *
    * @see #DataLinker(IDataSupplier supplier, IDataConsumer consumer)
    */
    private DataLinker() {}
    
    
    
    
    /**
    * Constructor to link <code>IDataSupplier</code> and 
    * <code>IDataConsumer</code>.
    *
    * @param     supplier         Supplies access to a data source.
    * @param     consumer         Supplies access to a data store.
    */
    public DataLinker(IDataSupplier supplier, IDataConsumer consumer) {
     
        this.supplier = supplier;
        this.consumer = consumer;
        debug = DebugMode.getDebugMode();
        gapFillLocalisedGUIText();
        
        try {
            mode = Integer.parseInt(System.getProperty("mode"));
        } catch (Exception e) {
            mode = MEMORY_CHECK_MODE;
        }

    }    
    
    
    
    /**
    * Main method called by thread running system. 
    * <p>
    * Registers instantiated object of this class as a report listener 
    * with the supplier and consumer. 
    *
    */
    @Override
	public Void call() throws Exception {  
    
    
        // Register this object with the supplier and consumer 
        // for reports. It will pass data from these to GUI elements 
        // bound to it as a JavaFX Task. 
        supplier.addReportingListener(this);
        consumer.addReportingListener(this);
        
        // Processing separated off into "process" method to allow 
        // tests to call it, avoid the JavaFX threading and thereby 
        // ensure the process runs to completion before later tests are run 
        // (simply calling call() seems to enact threading even if you 
        // don't go via super.run()). Also allows us to avoid the above 
        // bound-to-GUI-listener registration in tests.
        process();
        
        // Last point for code running within FX thread.        
        updateAppMessage(
            PropertiesSingleton.getInstance().getProperty("txtFinishedProcess")
        );
        
        // Void is the null type demanded by the Task extension (see 
        // method declaration).
        return null;
        
    }
        
        
      
      
    /**
    * Does the bulk of the processing.
    *
    * @throws   Exception   Here to catch any exception for the Task exception 
    *                       model.
    */
    public void process()  throws Exception {    
    
       
    
        // Get the supplier to read in the data headers etc. 
        // and send these to the data consumer to build storage.
        try {
            supplier.initialise();
            consumer.initialise(supplier.getDataset());
        } catch (DataException de) {
            if (debug) de.printStackTrace();
            throw de;  // Main reason here is wrong file type being read.
        }

        
        // Work out whether there's room for the files in memory, 
        // and the faster "pull" method, or if the files need to be 
        // "pushed" a chunk at a time into the data consumer using the 
        // observer model. Because a whole dataset is potentially held with each 
        // read and write, this needs calculating for all files, not 
        // each individually.
        
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        
        // Get source (e.g. directory) and recordHolderNames (e.g. filenames).
        // N.B. In the event this class is used ultimately for something other 
        // than reading from files this section will need adjustment. 
        File source = supplier.getSource(); 
        ArrayList<String> recordHolderNames = supplier.getRecordHolderNames(); 
        
        // The vagaries of free memory calculations make this a lower bound, 
        // but we reduce by 200MiB for safety as well. 
        long memoryForFile = (runtime.maxMemory() - 209715200) - 
                             (runtime.totalMemory() - runtime.freeMemory());
        
        // Impossible, again, to know the space needed to work with any 
        // given amount of bytes, but profiling of the sample file suggests
        // a multiplier of ~14 per byte processed into objects in the 
        // app, but ~91 by Derby during processing.
        long fileSizeDuringProcessing = 0;
        File currentFile = null;
        try {
            for (int i = 0; i < recordHolderNames.size(); i++) {
                currentFile = new File (source + recordHolderNames.get(i));
                fileSizeDuringProcessing = (currentFile.length() * 105) +
                                                fileSizeDuringProcessing;
            }
        } catch (RuntimeException rte) {
            if (debug) rte.printStackTrace();
            throw new DataException(
                    PropertiesSingleton.getInstance().getProperty("txtSizeIssue")
                );
        }

        try {
            if  (
                    ((fileSizeDuringProcessing > memoryForFile) && (mode == MEMORY_CHECK_MODE)) 
                    || 
                    (mode == PUSH_MODE)
                ) {
                updateAppMessage(
                    PropertiesSingleton.getInstance().getProperty("txtLargeFile")
                );
                pushDataAsRead();
            } else {
                pullDataAsOne();    
                
            }

        } catch (DataException de) {
            if (debug) de.printStackTrace();
            throw de;
        }

    }
    
    
    
    
    /**
    * Sets up an observer model so the <code>IDataSupplier</code> can push data 
    * to the observing <code>IDataConsumer</code> piecemeal as read in. 
    * <p>
    * Used where memory issues potentially acting. Suppliers push
    * <code>ArrayList&lt;IRecord&gt;</code> to <code>consumer.load</code>
    *
    * @throws     DataException    If there is an issue.
    */
    public void pushDataAsRead() throws DataException {
        
        supplier.addDataListener(consumer);
        try { 
            supplier.pushData();
        }catch (DataException de) {
            if (debug) de.printStackTrace();
            throw de;
        }
        
        // Consumer doesn't know when end of transfer 
        // is, so we need to disconnected it from its 
        // store.
        consumer.disconnectStore();
        
    }
    
    
    
    
    /**
    * Pulls the whole dataset from the <code>IDataSupplier</code> to 
    * the <code>IDataConsumer</code> when read in.
    * <p>
    * Used where memory issues less problematic.
    *
    * @throws     DataException    If there is an issue.
    */
    public void pullDataAsOne() throws DataException {
        try {
            supplier.readData();
            consumer.bulkLoad(supplier.getDataset());
        }catch (DataException de) {
            if (debug) de.printStackTrace();
            throw de;
        }
  
    }
   



    /**
    * Sets the datalinker mode.
    * <p>
    * For testing, the system can be put into <code>PUSH_MODE</code> mode. During 
    * this, the data supplier pushes data in small blocks to registered consumer 
    * observers rather that making it all available at once through a public method 
    * (<code>PULL_MODE</code>). This is significantly slower, but minimises memory 
    * overheads. Under the default <code>MEMORY_CHECK_MODE</code>, the system 
    * assesses available memory against file sizes and decides how to handle 
    * data transfer. 
    * <p>
    * This is used for testing push mode. 
    *
    * @param    mode    PUSH_MODE; PULL_MODE; or the default MEMORY_CHECK_MODE.
    *
    */
    public void setMode(int mode) {
        
        this.mode = mode;
        
    }




    /**
    * Method called with message.
    *
    * Passed to Task inherited method.
    *
    * @param     message    Message to report.
    */
    public void updateAppMessage(String message) {
     
     
        // The convoluted JavaFX concurrently model means this 
        // method runs on the task thread while updateMessage runs 
        // on the FX Thread, so we need to pass updates back to 
        // the FX thread to run when it can (which should be 
        // immediately, as all the hard work is on the task thread.
        
        // Try-catch is for testing when this class invokes this method directly; 
        // strangely if another classes invoke it, Platform seems to delay 
        // indefinitely so exceptions aren't thrown.
        try {
            Platform.runLater(new Runnable() {
                @Override 
                public void run(){
                    updateMessage(getMessage() + System.lineSeparator() + message);
                }
            });
        } catch (IllegalStateException ise) {
            if (debug) ise.printStackTrace();
        }
     
    }
    
    
    
    
    /**
    * Method called with progress.
    *
    * Passed to Task inherited method.
    *
    * @param     workDone    Fraction of max.
    * @param     max         Total work to do as a double.
    */
    public void updateAppProgress​(double workDone, double max){
        
        // The convoluted JavaFX concurrently model means this 
        // method runs on the task thread while updateMessage runs 
        // on the FX Thread, so we need to pass updates back to 
        // the FX thread to run when it can (which should be 
        // immediately, as all the hard work is on the task thread.
        
        // Try-catch is for testing when this class invokes this method directly; 
        // strangely if another classes invoke it, Platform seems to delay 
        // indefinitely so exceptions aren't thrown.
        try {
            Platform.runLater(new Runnable() {
                @Override 
                public void run(){
                    updateProgress(workDone, max);
                }
            });
        } catch (IllegalStateException ise) {
            if (debug) ise.printStackTrace();
        }
    }




    /**
    * Sets the defaults for warnings and exceptions in English if an appropriate 
    * language properties file is missing.
    *
    */
    private void gapFillLocalisedGUIText() {
        
        Properties defaults = new Properties();
        
        // Warnings and messages.
        
        defaults.setProperty("txtFinishedProcess", "Finished processing.");
        defaults.setProperty("txtSizeIssue", "There is a problem determining " + 
                                             "the size of a file. Please check " + 
                                             "you have access permission."
                                        );
        defaults.setProperty("txtLargeFile", "Large dataset: this may take a while.");                                                                                             
        PropertiesSingleton.getInstance().setDefaults(defaults);
    }
    
    
}





