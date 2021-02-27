/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.dbexamples;




/**
* Main class for starting application. 
* <p>
* At the moment, just launches the 
* {@link io.github.ajevans.dbcode.dbexamples.UIfx} GUI. This is needed to run 
* the application from a jar containing dependencies, see: 
* <a href="https://stackoverflow.com/questions/52653836/maven-shade-javafx-runtime-components-are-missing/52654791#52654791">StackOverflow</a>.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class DBExamples {
    
    
    
    
    /**
    * Unused default constructor.
    */
    public DBExamples() {}
    
    
    
    
    /**
    * Starts application. 
    * <p>
    * Launches UIfx interface via its <code>main</code>.
    * @param args String[] of arguments from command line, passed through to UIfx.
    */
    public static void main(String[] args) {
        
        UIfx.main(args);
    
    }
        
 
 
 
}
