/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.utilities;


import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;




/**
* Singleton to hold all the properties files associated with an application.
* <p>
* Having this as a singleton cuts down on class coupling and makes 
* default properties available in testing.
* <p>
* Set up the singleton by calling <code>addProperties</code> one or more times 
* with a properties file. You can then call the standard Properties methods on 
* its instance, e.g.
* <p>
* <code>String value = PropertiesSingleton.getInstance().getProperty("key");</code>
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
* @see java.util.Properties
*/ 
public final class PropertiesSingleton extends Properties {
    
    
    
    
    /**
    * Lazy initialization instance object.
    *
    */
    private static PropertiesSingleton instance = null;




    /**
    * Default constructor set to private in line with singleton pattern.
    *
    */     
    private PropertiesSingleton() {        
    }
    
    
    
    
    /**
    * Method to get instance in line with singleton pattern.
    *
    * @return     PropertiesSingleton        Single instance of this class.
    */ 
    public static synchronized PropertiesSingleton getInstance() {
        
        if(instance == null) {
            instance = new PropertiesSingleton();
        }
        
        return instance;
    }

    
    
    
    /**
    * Add a file of properties to the singleton.
    * 
    * @param     propertiesFilePath        Properties file to read.
    * @throws    IOException               If the file isn't found, though there 
    *                                      is the opportunity to load defaults 
    *                                      later through <code>setDefaults</code>.
    */
    public void addProperties(String propertiesFilePath) throws IOException {

        // The following check allows this to run both inside a jar 
        // (first statement) and from a filesystem (second statements) - for 
        // example, when testing.
        
        InputStream inputStream = getClass().getResourceAsStream(propertiesFilePath);
        if (inputStream == null) {
            ClassLoader classLoader = getClass().getClassLoader();
            inputStream = classLoader.getResourceAsStream(propertiesFilePath);
        }
        
        Properties prop = new Properties();
        try {
            prop.load(inputStream);
        } catch (IOException ioe) {
            throw ioe;
        }    
        prop.stringPropertyNames().forEach(name -> {
            setProperty​(name, prop.getProperty(name));
        });
    
    }
    
    
    
    
    /**
    * Sets a default set of properties.
    * <p>
    * We can't use the usual way of setting these via the constructor.
    *
    * @param    defaults    Default values to add where missing. If the key 
    *                       already exists, the default will not be used.
    */
    public void setDefaults (Properties defaults) {
        
        defaults.stringPropertyNames().forEach(name -> {
            if (this.getProperty(name) == null) {
                this.setProperty​(name, defaults.getProperty(name));
            }
        });
        
    }
    


    
}