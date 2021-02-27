/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.data.structures;


import java.util.ArrayList;
import java.util.GregorianCalendar;

import io.github.ajevans.dbcode.utilities.DebugMode;



/**
* Class for generic metadata (i.e. not specific to, e.g. databases).
* <p>
* Relevant subset of generic metadata tags taken from 
* <a href="https://www.dublincore.org/specifications/dublin-core/dcmi-terms/">Dublin 
* Core Metadata Initiative (DCMI) terms</a> (20th Jan 2020)
* with the addition of <code>dateLastEdited</code>, <code>version</code>, 
* <code>notes</code>, and metametadata <code>dateFormat</code>.
* <p>
* Note that dates are stored as <code>java.util.GregorianCalendar</code> 
* objects, not DCMI 
* <a href="https://www.iso.org/iso-8601-date-and-time-format.html">ISO 8601</a> 
* strings
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
public class Metadata implements IMetadata {
    
    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------    
    
    
    
    
    /**
    * Debugging flag, set by System variable passed in <code>-Ddebug=true</code> 
    * rather than setting here / with accessor.
    */
    private boolean debug = false;


    /**
    * Standard.
    */
    private String standard = "Dublin Core subset with additions";
    
    
    /**
    * DCMI title.
    */
    private String title = ""; 
    
    
    /**
    * DCMI creator.
    */
    private String creator = "";
    
    
    /**
    * DCMI source.
    */
    private String source = "";
    
    
    /**
    * For other notes.
    */
    private String notes = "";
    
    
    /**
    * DCMI dateSubmitted.
    */
    private GregorianCalendar dateSubmitted = null;


    /**
    * For last edited date.
    */    
    private GregorianCalendar dateLastEdited = null;
    
    
    /**
    * Date format, incase there is doubt about when zero date represents.
    */
    private String dateFormat = "java.util.GregorianCalendar";
    
    
    /**
    * Version.
    */
    private String version = "";

    
    
    
    //--------MAJOR METHODS-----------------------------------------------------    
    


    
    /**
    * Generic constructor.
    *
    */
    public Metadata() {
        debug = DebugMode.getDebugMode();
    }
    
    


    //--------ACCESSOR / MUTATOR METHODS----------------------------------------    

    
    
    
    /**
    * Sets metadata standard.
    *
    * @param    standard    Metadata standard or equivalent description.
    */
    public void setStandard(String standard) {
        this.standard = standard;
    }
    
    
    
    
    /**
    * Gets metadata standard.
    *
    * @return    String    Metadata standard or equivalent description.
    */
    public String getStandard() {
        return standard;
    }
    
    
    
    
    /**
    * Sets title.
    *
    * @param    title    Title to set.
    */
    public void setTitle(String title) {
        this.title = title;
    }




    /**
    * Gets title.
    *
    * @return    String    Title got.
    */
    public String getTitle() {
        return title;
    }
    
    
    
    
    /**
    * Sets creator.
    *
    * @param    creator    Creator to set.
    */
    public void setCreator(String creator) {
        this.creator = creator;
    }




    /**
    * Gets creator.
    *
    * @return    String    Creator got.
    */
    public String getCreator() {
        return creator;
    }


    
    
    /**
    * Sets source.
    *
    * @param    source    Source to set.
    */
    public void setSource(String source) {
        this.source = source;
    }
    
    
    
    
    /**
    * Gets source.
    *
    * @return    String    Source got.
    */
    public String getSource() {
        return source;
    }




    /**
    * Sets notes.
    *
    * @param    notes    Notes to set.
    */    
    public void setNotes(String notes) {
        this.notes = notes;
    }




    /**
    * Gets notes.
    *
    * @return    String    Notes got.
    */
    public String getNotes() {
        return notes;
    }    


    

    /**
    * Sets dateSubmitted.
    *
    * @param    dateSubmitted    DateSubmitted to set.
    */
    public void setDateSubmitted(GregorianCalendar dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }




    /**
    * Gets dateSubmitted.
    *
    * @return    GregorianCalendar    DateSubmitted got.
    */
    public GregorianCalendar getDateSubmitted() {
        return dateSubmitted;
    }        
    
    
    
    
    /**
    * Sets dateLastEdited.
    *
    * @param    dateLastEdited    DateLastEdited to set.
    */
    public void setDateLastEdited(GregorianCalendar dateLastEdited) {
        this.dateLastEdited = dateLastEdited;
    }




    /**
    * Gets dateLastEdited.
    *
    * @return    GregorianCalendar    DateLastEdited got.
    */
    public GregorianCalendar getDateLastEdited() {
        return dateLastEdited;
    }
    
    
    
    
    /**
    * Gets dateFormat.
    *
    * @return    String    DateFormat got.
    */
    public String getDateFormat() {
        return dateFormat;
    }
    
    
    
    
    /**
    * Sets version.
    *
    * @param    version     Version to set.
    */
    public void setVersion(String version) {
        this.version = version;
    }
    
    
    
    
    /**
    * Gets version.
    *
    * @return    String    Version got.
    */
    public String getVersion() {
        return version;
    }
        
    


    //--------UTILITY METHODS---------------------------------------------------    

    
    
    
    /**
    * Gets the metadata as an array of <code>ArrayLists</code>.
    * 
    * <ul>
    * <li>array[0] = The category names as Strings.</li>
    * <li>array[1] = The data types of the categories as Class</li>
    * <li>array[2] = The category values as objects.</li>
    * </ul>
    *
    * @return    ArrayList[]        Array of metadata.
    */
    public ArrayList[] getAll(){ 
    
        ArrayList<String> names = new ArrayList<>();
        names.add("standard");
        names.add("title");
        names.add("creator");
        names.add("source");
        names.add("notes");
        names.add("dateSubmitted");
        names.add("dateLastEdited");
        names.add("dateFormat");
        names.add("version");
        
        ArrayList<Class> types = new ArrayList<>();
        try {
            types.add(Class.forName("java.lang.String"));
            types.add(Class.forName("java.lang.String"));
            types.add(Class.forName("java.lang.String"));
            types.add(Class.forName("java.lang.String"));
            types.add(Class.forName("java.lang.String"));
            types.add(Class.forName("java.util.GregorianCalendar"));
            types.add(Class.forName("java.util.GregorianCalendar"));
            types.add(Class.forName("java.lang.String"));
            types.add(Class.forName("java.lang.String"));        
        } catch (ClassNotFoundException cnfe)  {
            // Issues with this unlikely.
            if (debug) cnfe.printStackTrace();
        }
        ArrayList values = new ArrayList();
        values.add(standard);
        values.add(title);
        values.add(creator);
        values.add(source);
        values.add(notes);
        values.add(dateSubmitted);
        values.add(dateLastEdited);
        values.add(dateFormat);
        values.add(version);
        
        ArrayList[] arrayLists = new ArrayList[3];
        arrayLists[0] = names;
        arrayLists[1] = types;
        arrayLists[2] = values;
        
        return arrayLists;
        
    }



}