/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.utilities;
 



/**
* Utility class for getting the system property "debug" set at the command line.
* <p>
* This is pulled from the JVM System property "debug" which 
* can be set at the command line: 
* <p>
* <code>jar -Ddebug="true" jarname.jar</code>
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class DebugMode {
    
    
    /**
    * Debug mode.
    */
    private static boolean debugMode = false;
    
    
    /**
    * Default constructor set to private to prevent instantiation.
    */
    private DebugMode() {}
    
    
    
    
    /**
    * Static method for getting the debug mode. 
    * <p>
    * i.e. call as: <code>DebugMode.getDebugMode()</code>
    *
    * @return    debugMode    The debug mode. Defaults to false.
    */
    public static boolean getDebugMode() {

        try {
            debugMode = Boolean.parseBoolean(System.getProperty("debug"));
        } catch (NullPointerException e) {
            debugMode = false;
        }
        
        return debugMode;
        
    }
    
    
    
    
    /**
    * Mutator for debugMode.
    * <p>
    * Avoid setting this programmatically outside of testing. Turns on 
    * exception stack trace printing.
    *
    * @param    debug   Whether to turn on debugging mode or not.
    */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }
    


    
}
    
