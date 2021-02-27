/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.filesuppliers;


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
import io.github.ajevans.dbcode.data.structures.IDataset;
import io.github.ajevans.dbcode.data.structures.IMetadata;
import io.github.ajevans.dbcode.data.structures.IRecord;
import io.github.ajevans.dbcode.data.structures.Row;
import io.github.ajevans.dbcode.data.structures.Metadata;
import io.github.ajevans.dbcode.data.structures.Table;
import io.github.ajevans.dbcode.data.structures.TabulatedDataset;
import io.github.ajevans.dbcode.utilities.DebugMode;
import io.github.ajevans.dbcode.utilities.IReportingListener;
import io.github.ajevans.dbcode.utilities.PropertiesSingleton;




/**
* Parser for Climate Research Unit Time Series 2.1 files gridded files. 
* <p>
* The class reads a set of recordHolders (files) in a source (directory) and 
* provides the recordHolders as an <code>IDataset</code> containing 
* <code>IRecordHolder</code> objects, each containing <code>IRecord</code> 
* objects (rows). Each dataset and recordHolder comes with an <code>IMetadata</code> 
* object.
* <p>
* <code>IReportingListener</code> objects may be registered with objects of 
* this class to receive suitable progress reporting and messaging. In general, 
* exceptions not dealt with internally are re-thrown for calling objects to 
* deal with. Messages are user-friendly. 
* <p>
* Note that because instance variables will hold a wide variety of information 
* on pervious writes, it is essential that for each new set of files / dataset 
* <strong>a new instance of this class is used</strong>.
* <p>
* This parser works on climate data files produced by Dr Tim Mitchell 
* (<a href="https://web.archive.org/web/20191010071620/https://crudata.uea.ac.uk/~timm/index.html">archived 
* homepage</a>) while at the Tyndall Centre for Climate Change Research 
* and released 23rd January 2004. It is designed for CRU TS 2.1, but will work 
* for CRU TS 2.0 (indeed, some 2.1 is distributed in 2.0 files).
* <p>
* The data comprises climate records for the global land surface interpolated from 
* real observations to a 0.5 degree grid at a monthly time series<SUP>1</SUP>. 
* Data for nine observed and derived variables are available 
* (temperature, diurnal temperature range, daily minimum and maximum temperatures, 
* precipitation, wet-day frequency, frost-day frequency, vapour pressure, and 
* cloud cover), but each file contains only one. 
* <p>
* The data format is bespoke<SUP>2</SUP>. A description can be found via the 
* <a href="https://crudata.uea.ac.uk/~timm/grid/CRU_TS_2_1.html">data homepage</a>.  
* <p>
* <strong>NB: The data has been superseded, most recently by CRU TS 4.04 
* (24 April 2020: <a href="https://crudata.uea.ac.uk/cru/data/hrg/">data, 
* associated papers, and GoogleEarth visualisations</a>).</strong>
* <p>
* Notes:
* <div style="font-size: smaller;">
* <SUP>1</SUP>Timothy D. Mitchell and Philip D. Jones (2005) An improved method 
* of constructing a database of monthly climate observations and associated 
* high-resolution grids. International Journal of Climatology, 25 (6), 693-712 
* [online] <a href="https://doi.org/10.1002/joc.1181">https://doi.org/10.1002/joc.1181</a> 
* (<a href="http://sdwebx.worldbank.org/climateportal/doc/mitchelljones.pdf">alternative</a>). 
* Accessed 7th February 2021.
* <p>
* <SUP>2</SUP>The headers note these are "grim" files, and there is 
* some belief online that they are "GPS Receiver Interface Module (GRIM)" files, but this 
* is likely to be a typo for "grid" files, which is how they are described by Mitchell elsewhere. 
* They are similar to other multi-channel raster formats and flat ACSII grid formats like 
* ESRI's ARCINFO GRID format.
* </div>
*
* @deprecated     This data has been superseded (see this page), and 
*                 it's therefore likely this class will be removed at some point.
* @todo           It's likely we could make an more abstract file parser at 
*                 some point, set up by a properties file.
* @todo Localise metadata notes?
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
@Deprecated
public class CruTs2pt1Supplier implements IDataSupplier {

    
    
    
    //--------INSTANCE VARIABLES------------------------------------------------
    
    
    
    
    /**
    * Debugging flag, set by System variable passed in <code>-Ddebug=true</code> 
    * rather than setting here / with accessor.
    */
    private boolean debug = false;
    
    
    /**
    * Source directory for reading.
    */
    private File source = null;
    
    
    /**
    * Source filenames for reading.
    */
    private ArrayList<String> recordHolderNames = null;
    
    
    /**
    * Store for all tables in this dataset.
    */
    private TabulatedDataset tabulatedDataset = null;
    
    
    /**
    * Output field names for this file type.
    */
    private ArrayList<String> fieldNames =  null;
    
    
    /**
    * Output field classes for this file type.
    */
    private ArrayList<Class> fieldTypes = null;
    
    
    /**
    * Main file connection.
    */
    private BufferedReader buffer = null;
    
    
    /**
    * Register for data consumers wishing to listen for 
    * pushed data.
    */
    private ArrayList<IDataConsumer> listeners = new ArrayList<>();

  
    /**
    * Listeners interested in updates on progress.
    */
    private ArrayList<IReportingListener> reportingListeners = new ArrayList<>();

    
    /**
    * Number of lines to read for data source header.
    */
    private int numberOfHeaderLines = 5;
    
    
    /**
    * Format for any dates in source header.
    */
    private String metadataDatePattern = "dd.MM.yyyy";
    
    
    /**
    * As the data isn't marked up for date beyond info in the header.
    */
    private int startYear = -1;
    
    
    /**
    * As the data isn't marked up for date beyond info in the header.
    */
    private int endYear = -1;
    
    
    /**
    * Could calculate this when needed locally, but better to do it once.
    */
    private int years = -1;
    
    
    /**
    * As the data isn't marked up for date beyond info in the header.
    *
    * @todo Calculate this from file.
    */
    private int valuesPerYear = 12;
    
    
    /**
    * Width of a data column in width-delimited data.
    */
    private int dataTokenWidth = 5;
    
    
    /**
    * For monitoring progress at reading.
    */
    private int progress = 0;
    
    
    
    //--------MAJOR METHODS-----------------------------------------------------
        
    

    
    /**
    * Generic constructor. 
    */
    public CruTs2pt1Supplier() {
        
        debug = DebugMode.getDebugMode();
        gapFillLocalisedGUIText();
        
    }




    /**
    * Sets up the data supplier ready to read data.
    * <p>
    * It creates the relevant internal data structures in 
    * preparation for reading the data, including reading in and parsing 
    * file headers. 
    * <p>
    * Note that this method is kept separate from the <code>setSource</code> 
    * and <code>recordHolderNames</code> methods to enable 
    * piped data processing implementations where a series of 
    * suppliers and consumers set up prior to activation. However, 
    * <code>setSource</code> and <code>recordHolderNames</code> must be called  
    * prior to this method being called so it has something to initialise.
    * <p>
    * If a source path and record holder names haven't been set using the 
    * <code>setSource</code> / <code>setRecordHolderNames</code> methods, this 
    * method throws a <code>ParseFailedException</code>. 
    *
    * @throws    ParseFailedException    Usually if there is an issue reading 
    *                                    a file; e.g. the wrong file type, the 
    *                                    file has no data, or 
    *                                    the source file is missing. It 
    *                                    makes sense for callers to cancel 
    *                                    further attempts at reading at this point.
    */
    public void initialise() throws ParseFailedException {
        
        
        if ((source == null) || (recordHolderNames == null)) {
            throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtNoFileIssue")
                );
        }            
        
        initialiseFields();
        setupDataset();
        int estimatedRecordCount = 0;
        
        // Build files, metadata, and file headers for each file in turn.
        
        for (int i = 0; i < recordHolderNames.size(); i++) {
            try {
                connectSource(i);
                parseHeader(i);
            } catch (ParseFailedException pfe) {
                if (debug) pfe.printStackTrace();
                // Exceptions raised through failed parsing of headers 
                // are likely due to the wrong file type being read or 
                // an otherwise unreadable file. 
                // We therefore pass back to caller of initialise to end 
                // execution.
                throw pfe;
            } 
            
            estimatedRecordCount = estimateRecordCount(i); // For progress monitoring.
            if (estimatedRecordCount == 0) {
                throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtNoDataIssue")
                );
            }
            int totalEstimate = tabulatedDataset.getEstimatedRecordCount();
            totalEstimate = totalEstimate + estimatedRecordCount;
            tabulatedDataset.setEstimatedRecordCount(totalEstimate);
        }

        
    }




    /**
    * Sets up the fields for this data type.
    * <p>
    * For this data type, fields, and their type in the system, are:
    * <dl>
    * <dt>Xref</dt><dd>java.math.BigDecimal</dd>
    * <dt>Yref</dt><dd>java.math.BigDecimal</dd>
    * <dt>Date</dt><dd>java.util.GregorianCalendar</dd>
    * <dt>Value</dt><dd>java.math.BigDecimal</dd>
    * </dl>
    *
    */
    private void initialiseFields() {
        
        // There's no ontology metadata within the files of this data type, 
        // so this is hardwired here.
        
        fieldNames = new ArrayList<>(4);
        fieldNames.add("Xref");
        fieldNames.add("Yref");
        fieldNames.add("Date");
        fieldNames.add("Value");

        fieldTypes = new ArrayList<>(4);
        try {
            fieldTypes.add(Class.forName("java.math.BigDecimal"));
            fieldTypes.add(Class.forName("java.math.BigDecimal"));
            fieldTypes.add(Class.forName("java.util.GregorianCalendar"));
            fieldTypes.add(Class.forName("java.math.BigDecimal"));
        } catch (ClassNotFoundException cnfe) {
            if (debug) cnfe.printStackTrace();
            cnfe.printStackTrace();
        }
        
    }




    /**
    * Sets up a data structure ready for the data.
    *
    */
    private void setupDataset() {
        
        tabulatedDataset = new TabulatedDataset();
        tabulatedDataset.setMetadata((IMetadata) new Metadata());
        for (int i = 0; i < recordHolderNames.size(); i++) {
            Table table = new Table(tabulatedDataset);
            table.setMetadata((IMetadata) new Metadata());
            table.setFieldNames(fieldNames);
            table.setFieldTypes(fieldTypes);
            tabulatedDataset.addRecordHolder(table);
        }
        
    }
    
    
    
    
    /**
    * This estimates the records in the file.
    * <p>
    * It is an accurate estimate here, but generally this 
    * estimate within this system should only be used for 
    * progress measurement and reporting no data files - not an accurate count 
    * of records actually read.
    *
    * @param     index           The position of the data to connect to in 
    *                            recordHolderNames.
    * @return    int             Estimate of record count.
    */
    private int estimateRecordCount(int index) {

        // When this is called, the buffer is at the header end 
        // of the currently indexed record holder file. 
       
        // Count file blocks.
        
        ArrayList<String> rows = null;
        int blockCount = 0; 
        try {
            while ((rows = readLines(years + 1)) != null) {
                blockCount++;
            }
        } catch (ParseFailedException pfe) {
            // The above only happens after we've read the 
            // header, so readLine failures unlikely.
            if (debug) pfe.printStackTrace();
        }
        
        
        // Clean up and reset stream to end of header.
        
        try {
            disconnectSource();
            connectSource(index);
            readLines(numberOfHeaderLines);
        } catch (ParseFailedException de) {
            // The above is repeating previous actions, 
            // so shouldn't cause issues.
            if (debug) de.printStackTrace(); 
        }
    
        return (blockCount * years * valuesPerYear);
            
    }
    
    
    
    
    /**
    * Reads a set of lines and returns them as an unparsed ArrayList of Strings.
    * <p>
    * Returns <code>null</code> only if all lines pulled by 
    * <code>numberOfLines</code> are <code>null</code>. It's therefore possible 
    * to get a smaller than expected <code>ArrayList</code> at the end 
    * of a file whose size to read is not "<code>% numberOfLines == 0</code>". 
    * However, the next call will return <code>null</code>.
    *
    * @param        numberOfLines            Number of lines to read.
    * @return       ArrayList                Strings, one per line, or 
    *                                        null at the end of the file.
    * @throws       ParseFailedException     If there is an issue.
    */
    private ArrayList<String> readLines(int numberOfLines) 
                                                   throws ParseFailedException {

        ArrayList<String> lines = new ArrayList<>();

        String line = "";
        
        try {
            
            for (int i = 0; i < numberOfLines; i++)  {
                
                line = buffer.readLine();
                if (line != null) lines.add(line);
                
            }
            
        } catch (IOException ioe) {
            if (debug) ioe.printStackTrace();
            throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtFileConnectionIssue")
                );
        }
        
        // For EOF - used in read-until-null loops.
        
        if (lines.size() > 0) {
            return lines;
        } else {
            return null;
        }
        
        
    }




    /**
    * Fills the dataset with data.
    * <p>
    * Primitives are boxed.
    *
    * @throws     ParseFailedException    If there is an issue.
    */
    public void readData()  throws ParseFailedException {
        
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtRead")
            );
      
        Table table = null;
        for (int i = 0; i < recordHolderNames.size(); i++) {
            
            // Disconnect wherever we are and reconnect 
            // to start of first recordHolder.
            
            try {
                disconnectSource();
                connectSource(i);
                readLines(numberOfHeaderLines);
            } catch (ParseFailedException de) {
                // At this point we've made all checks that 
                // files exist and headers are readable, so this should 
                // be ok.
                if (debug) de.printStackTrace();
            }
        
            table = (Table)tabulatedDataset.getRecordHolder(i);
            try {
                readTable(table);
            } catch (ParseFailedException pfe) {
                if (debug) pfe.printStackTrace();
                throw pfe;
            }
            
        }
        
        // Finished, so zero progress.
        reportProgress(0, 1);
        
        // Disconnect and clean up; garbage collect.
        
        try {
            disconnectSource();
        } catch (ParseFailedException de) {
            // See disconnectSource - only thrown in debug mode.
            if (debug) de.printStackTrace();
        }
        
    }
    



    /**
    * Fills a table with data.
    *
    * @param      table                     Table to add rows to.
    * @throws     ParseFailedException      If there is an issue.
    */
    public void readTable(Table table)  throws ParseFailedException {
        
        ArrayList<IRecord> rows = null;
        
        try {    
        
            while ((rows = getParsedDataBlockAsRows(table)) != null) {
                
                table.addRecords(rows);

            }
            
        } catch (ParseFailedException pfe) {
            if (debug) pfe.printStackTrace();
            throw pfe;
        }
        
    }


    
    
    /**
    * Parses the header of the data source. 
    * <p>
    * The data is used for internal data parsing, but is also 
    * written to the dataset metadata "notes" category.
    * <p>
    * Probably the most significant things this method does is set the 
    * dataset metadata tag "title" to the third line of the header, which 
    * should be the data type "CRU TS 2.1" (if you read in 2.0 files or a 
    * mix it will be whatever is read in last). This becomes the dataset 
    * name when processed. It also sets each record holder (file / table) 
    * "title" to the second line, which should be the shortened observation 
    * type, for example ".pre = precipitation (mm)" becomes "pre", adding the 
    * following information: 
    * <ul>
    * <li>start year</li> 
    * <li>end year</li>
    * <li>number of the file 
    * read, starting with one</li>
    * </ul>
    * ...just incase there's more than one file of the 
    * same type read. This becomes the record holder name when processed.
    * 
    * @param     index                  The position of the table to connect to  
    *                                   in recordHolderNames.
    * @throws    ParseFailedException   This exception should get passed 
    *                                   back to the caller of 
    *                                   <code>initialise</code> to 
    *                                   end attempts at reading. Contains the 
    *                                   message "Having difficulty reading this 
    *                                   file. Are you sure it is CRU TS 2.x format?"
    * @todo     Detailed reporting of poor quality header information.
    * @todo     Need to get time metadata from the files.
    */
    private void parseHeader(int index) throws ParseFailedException {
        
        // There's a fair amount of code here that looks like it could 
        // have been cut out by sticking with Strings throughout, however, 
        // use of things classes like BigDecimal offer is a check of the data 
        // and they help with consistent formatting. 
        // The method might also be smaller using StringTokenizers, 
        // but on balance using indexOf helps with class reworking 
        // and is more transparent.
        
        ArrayList<String> header = null;
        
        try {
            header = readLines(numberOfHeaderLines);
        } catch (ParseFailedException pfe) {
            if (debug) pfe.printStackTrace();
            throw pfe;
        }
        
        Metadata datasetMetadata = (Metadata) tabulatedDataset.getMetadata();
        Metadata tableMetadata = 
                   (Metadata) tabulatedDataset.getRecordHolder(index).getMetadata();
        
        // Header line 1
        
        String line1 = header.get(0);
        int temp = line1.indexOf(" created");
        if (temp < 0) {
            throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtFileFormatIssue")
                );
        }

        SimpleDateFormat sdf = new SimpleDateFormat(metadataDatePattern);
        sdf.setLenient(false);
        
        try {

            String source = line1.substring(0, line1.indexOf(" created"));
            datasetMetadata.setSource(source);
            tableMetadata.setSource(source);
            String dateString = line1.substring(line1.indexOf("on ") + 3, 
                                                line1.indexOf(" at"));
            GregorianCalendar calendar = new GregorianCalendar();
            //calendar.setLenient(false);
            try {
                calendar.setTime(
                    sdf.parse(dateString)
                );
            } catch (ParseException pe) {
                if (debug) pe.printStackTrace();
                throw new ParseFailedException(
                        PropertiesSingleton.getInstance().getProperty("txtHeaderDateIssue") + 
                        dateString
                    );
            }

            tableMetadata.setDateSubmitted(calendar);
            String creator = line1.substring(line1.indexOf("by ") + 3);
            tableMetadata.setCreator(creator);
            
            // Header lines 2 & 3
            
            String title = header.get(2);
            datasetMetadata.setTitle(title);
                
            // Header line 4
            
            String boxString = header.get(3);
            
            int startIndex = boxString.indexOf("[Long=") + 6;
            int endIndex = boxString.indexOf("]", 0);
            String longString = boxString.substring(startIndex, endIndex);
            
            BigDecimal longMin = new BigDecimal(
                            longString.substring(0, 
                                                longString.indexOf(",")).trim()
                            );
            BigDecimal longMax = new BigDecimal(
                            longString.substring(longString.indexOf(",") + 1, 
                                                longString.length()).trim()
                            );
            
            startIndex = boxString.indexOf("[Lati=", endIndex) + 6;
            endIndex = boxString.indexOf("]", endIndex + 1);
            String latiString = boxString.substring(startIndex, endIndex);
            
            BigDecimal latiMin = new BigDecimal(
                            latiString.substring(0, 
                                                latiString.indexOf(",")).trim()
                            );
            BigDecimal latiMax = new BigDecimal(
                            latiString.substring(latiString.indexOf(",") + 1, 
                                                latiString.length()).trim()
                            );
            
            startIndex = boxString.indexOf("[Grid X,Y=", endIndex) + 10;
            endIndex = boxString.indexOf("]", endIndex + 1);
            String gridString = boxString.substring(startIndex, endIndex);
            
            BigDecimal numberXgridboxes = new BigDecimal(
                            gridString.substring(0, 
                                                gridString.indexOf(",")).trim()
                            );
            BigDecimal numberYgridboxes = new BigDecimal(
                            gridString.substring(gridString.indexOf(",") + 1, 
                                                gridString.length()).trim()
                            );
            BigDecimal totalPossibleBoxes = 
                                    numberXgridboxes.multiply(numberYgridboxes);

            String notes = 
            "Bounding box details: " + System.lineSeparator() +
            "longMin = " + longMin + System.lineSeparator() +
            "longMax = " + longMax + System.lineSeparator() +        
            "latiMin = " + latiMin + System.lineSeparator() +    
            "latiMax = " + latiMax + System.lineSeparator() +    
            "numberXgridboxes = " + numberXgridboxes + System.lineSeparator() +    
            "numberYgridboxes = " + numberYgridboxes + System.lineSeparator() + 
            "Number of potential boxes = " + totalPossibleBoxes + 
                                     System.lineSeparator();

            
            // Header line 5
            
            String seriesString = header.get(4);
            
            startIndex = seriesString.indexOf("[Boxes=") + 7;
            endIndex = seriesString.indexOf("]", 0);
            String validBoxesString = 
                            seriesString.substring(startIndex, endIndex).trim();
            int validBoxes = Integer.parseInt(validBoxesString);
            
            startIndex = seriesString.indexOf("[Years=") + 7;
            endIndex = seriesString.indexOf("-", startIndex);
            String startYearString = 
                            seriesString.substring(startIndex, endIndex).trim();
            startYear = Integer.parseInt(startYearString);
            
            startIndex = endIndex + 1;
            endIndex = seriesString.indexOf("]", startIndex);
            String endYearString = 
                            seriesString.substring(startIndex, endIndex).trim();
            endYear = Integer.parseInt(endYearString);
            
            years = (endYear - startYear) + 1;
            
            notes = notes + 
            "Number of valid boxes = " + validBoxesString + System.lineSeparator() +
            "Time series details:" + System.lineSeparator() +
            "Starting year = " + startYearString + System.lineSeparator() +
            "Ending year = " + endYearString + System.lineSeparator();        
            
            startIndex = seriesString.indexOf("[Multi=") + 7;
            endIndex = seriesString.indexOf("]", startIndex);
            BigDecimal multiplier = new BigDecimal(
                                        seriesString.substring(startIndex, 
                                                                endIndex).trim()
                                    );
            
            startIndex = seriesString.indexOf("[Missing=") + 9;
            endIndex = seriesString.indexOf("]", startIndex);        
            String missingDataString = 
                            seriesString.substring(startIndex, endIndex).trim();
            
            notes = notes + 
            "Data details:" + System.lineSeparator() +
            "Multipiler = " + multiplier + System.lineSeparator() +
            "Missing Data Flag = " + missingDataString + System.lineSeparator() +
            " ---------" + System.lineSeparator() + 
            "NB: The data should be multiplied by the multiplier to gain true values" + 
            System.lineSeparator();
            
            notes = notes + 
            """
            Information on this data can be found at:
            https://crudata.uea.ac.uk/~timm/grid/CRU_TS_2_1.html
            """;
            
            tableMetadata.setNotes(notes);
            
            // Set the table title to observation type plus years and index 
            // starting at 1 (index added incase there's more than one file of same 
            // observation type and years).

            String observation = header.get(1);
            observation = observation.substring(
                                        observation.indexOf(".") + 1,
                                        observation.indexOf("=") 
                                    );
            tableMetadata.setTitle(observation.trim() + " " +
                                    startYearString + " " + 
                                    endYearString + " " + 
                                    String.valueOf(index + 1));
                                    
            
            String report = PropertiesSingleton.getInstance().getProperty("txtDatasetRead") + 
                            datasetMetadata.getTitle() + System.lineSeparator() +
                            PropertiesSingleton.getInstance().getProperty("txtDetails") + 
                            System.lineSeparator() +
                            " ---------" + 
                            System.lineSeparator() +
                            "Title: " + tableMetadata.getTitle() +       
                            System.lineSeparator() +
                            "Notes: " + System.lineSeparator() + 
                            tableMetadata.getNotes() +  
                            " ---------" + System.lineSeparator();
            
            reportMessage(report);
            
            
        } catch (RuntimeException rte) {
            if (debug) rte.printStackTrace();
            throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtHeaderFormatIssue")
                );
                                        
        }
        
    }
    
    
    
    
    /**
    * Reads a data block and turns it into records. 
    * <p>
    * In this file format a data block 
    * is a Xref/Yref header plus a set of rows representing years. 
    * Values across a row are monthly. We therefore read a 
    * block at a time rather than a row at a time. 
    * <p>
    * Reports progress to any ReportingListeners.
    *
    * @param     table        This is used to connect rows with parent tables.
    * @return    ArrayList    An ArrayList of rows, each row containing data in the 
    *                         appropriate field order.
    * @throws    ParseFailedException    If there's an issue.
    * @see       #getFieldNames()
    * @see       #getFieldTypes()
    */
    private ArrayList<IRecord> getParsedDataBlockAsRows(Table table) 
                                                   throws ParseFailedException {
        
        ArrayList<IRecord> rows = new ArrayList<>();
        ArrayList<String> lines = null;
        
        // Get the files from the file.
        
        try {
            lines = readLines(years + 1); // +1 for block header
        } catch (ParseFailedException pfe) {
            throw pfe;
        }
        
        if (lines == null) {
            return null;
        }
        
        // Line 1 (block header), Xref, Yref
        
        String line1 = lines.get(0);
        int index = line1.indexOf("Grid-ref=") + 9;
        String numbers = line1.substring(index);
        index = numbers.indexOf(",") + 1;
        BigDecimal xRef = null;
        BigDecimal yRef = null;
        
        try {
            xRef = new BigDecimal(numbers.substring(0,index - 1).trim());
            yRef = new BigDecimal(numbers.substring(index).trim());        
        } catch (NumberFormatException nfe) {
            if (debug) nfe.printStackTrace();
            throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtXYFormatIssue")
                 );
        }            
        // Subsequent lines in the block.
        // Parsed into row format:
        // Xref, Yref, Date, Value
        // The date must be calculated from location in block and 
        // year metadata from file header.

        Row row = null;
        GregorianCalendar date = null;
        int currentYear = startYear;
        int currentMonth = 0; // Jan
        int currentDay = 1;   // Doesn't change for this dataset, as monthly, but for clarity.
        String line = "";
        int startIndex = 0;
        int endIndex = dataTokenWidth;
        String token = "";
        
        // Lines after block header.
        
        // Despite potential appearances, the data is in 5 character blocks, 
        // not space or tab delimited - numbers larger than 9999 run into 
        // the previous number, so we can't use a delimited tokenizer here.
        
        for (int i = 1; i < lines.size(); i++) {
            
            line = lines.get(i);
            if (line.length() != valuesPerYear * dataTokenWidth) 
                throw new ParseFailedException(
                        PropertiesSingleton.getInstance().getProperty("txtLineFormatIssue") + 
                        line
                    );
            currentMonth = 0;
            startIndex = 0;
            endIndex = dataTokenWidth;
        
            // Construct data record from line.
            
            for (int j = 0; j < valuesPerYear; j++) {
                
                row = new Row(table);
                row.addValue(xRef);
                row.addValue(yRef);
                date = new GregorianCalendar(currentYear, currentMonth, currentDay);
                date.setLenient(false);
                row.addValue(date);
                token = (line.substring(startIndex, endIndex)).trim();
                if (token.length() == 0) throw new ParseFailedException(
                        PropertiesSingleton.getInstance().getProperty("txtMissingDataIssue") + 
                        line
                    );
                row.addValue(new BigDecimal(token));
                startIndex = startIndex + dataTokenWidth;
                endIndex = endIndex + dataTokenWidth;
                rows.add((IRecord)row);
                currentMonth++;
                
                // Report progress.
                progress++;
                reportProgress(progress, tabulatedDataset);
                
            }
            
            if (endIndex < line.length() + dataTokenWidth) 
                throw new ParseFailedException(
                        PropertiesSingleton.getInstance().getProperty("txtExtraValuesIssue") +
                        line
                    );
            
            currentYear++;
            
        }

        return rows;
        
    }


    
    
    /**
    * Pushes data to consumers registered as data listeners.
    * <p>
    * The method reads a data block at a time and pushes it 
    * to registered data consumers for processing by calling 
    * their <code>load(ArrayList&lt;IRecords&gt; records)</code> method 
    * when reading completed.
    * <p>
    * Garbage collects at the end of each push.
    *
    * @throws       ParseFailedException    If there is an issue.
    * @see          #addDataListener(IDataConsumer consumer)
    */
    public void pushData()  throws ParseFailedException {
        
        reportMessage(
                PropertiesSingleton.getInstance().getProperty("txtPush")
            );

        reportProgress(2, 100); // Just to let the user know something is happening.

        // Loop through list of files.
        
        for (int i = 0; i < tabulatedDataset.getRecordHolders().size(); i++) {
            
            // Disconnect wherever we are and reconnect 
            // to start of first recordHolder.
            
            try {
                disconnectSource();
                connectSource(i);
             
                readLines(numberOfHeaderLines);
            } catch (ParseFailedException de) {
                // At this point we've made all checks that 
                // files exist and headers are readable, so this should 
                // be ok.
                if (debug) de.printStackTrace();
            }
            
            Table table = (Table)tabulatedDataset.getRecordHolder(i); 

            ArrayList<IRecord> rows = null;    
            
            try {
                
                // Loop through the current file. 
                
                while ((rows = getParsedDataBlockAsRows(table)) != null) {

  
                    for (IDataConsumer listener: listeners) {
                        try {
                            listener.load(rows);                        
                        } catch (DataException de) {
                            if (debug) de.printStackTrace();
                            throw new ParseFailedException(de.getMessage());
                        }
                    }

                }


            } catch (ParseFailedException pfe) {
                if (debug) pfe.printStackTrace();
                throw pfe;
            }
        }

        // Finished, so zero progress. 
        reportProgress(0, 1);   

    }

    
    
    
    //--------ACCESSOR / MUTATOR METHODS----------------------------------------
            
    
    
        
    /**
    * Connect to a <code>File</code>.
    * <p>
    * Reading begun under <code>initialisation</code>.
    *
    * @throws  ParseFailedException     Not used in this implementation.
    */
    public void setSource(File source) throws ParseFailedException {

        this.source = source;
        
    }    
    



    /**
    * Gets the source file.
    *
    * @return     File     The source file.
    */
    public File getSource() {
        return source;
    }
    



    /**
    * Sets the names of files to read.
    *
    * @param    recordHolderNames        ArrayList of names.
    * @throws  ParseFailedException     Not used in this implementation.
    */
    public void setRecordHolderNames(ArrayList<String> recordHolderNames) 
                                                    throws ParseFailedException {
        
        this.recordHolderNames = recordHolderNames;
        
    }
    
    
    
    /**
    * Gets the names of files to read.
    *
    * @return    recordHolderNames        ArrayList of names.
    */
    public ArrayList<String> getRecordHolderNames() {
        
        return recordHolderNames;
        
    }

    
    
    
    /**
    * Gets the names of fields.
    *
    * @return    ArrayList   ArrayList of names.
    */
    public ArrayList<String> getFieldNames() {
        return fieldNames;
    }




    /**
    * Gets the type of fields.
    * <p>
    * Primitives are boxed.
    *
    * @return    ArrayList   ArrayList of Classes.
    */
    public ArrayList<Class> getFieldTypes() {
        return fieldTypes;
    }




    /**
    * Gets the dataset. 
    * <p>
    * Note that the dataset will not be implemented and filled with 
    * fields and metadata until <code>initialise</code> called.  
    * It will not be filled with data until <code>readData</code> called.
    *
    * @return     IDataset    The dataset.
    */
    public IDataset getDataset() {
        return (IDataset)tabulatedDataset;
    }




    /**
    * Register for data pushes.
    *
    * @param    consumer    Data consumer.
    */
    public void addDataListener(IDataConsumer consumer) {
        
        listeners.add(consumer);
        
    }



    
    /**
    * For objects wishing to get progress reports on data reading.
    *
    * @param     reportingListener    Object wishing to gain reports.
    * @see       io.github.ajevans.dbcode.utilities.IReportingListener
    */
    public void addReportingListener(IReportingListener reportingListener){ 
        reportingListeners.add(reportingListener);
    }
    
    
    
    
    //--------UTILITY METHODS---------------------------------------------------
    
    
    
    
    /**
    * Connects to a record holder (e.g. file) in the 
    * current source (directory).
    *
    * @param     index           Index of record holder to connect to in 
    *                            collection set using 
    *                            <code>setRecordHolderNames</code>.
    * @throws    ParseFailedException    Only if there is an issue.
    */
    public void connectSource(int index) throws ParseFailedException {
        
        File sourceFile = new File(source.getPath() + File.separator + 
                                        recordHolderNames.get(index));

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile); 
            // Debug option to load sample file. 
            // Not useful (indeed, counterintuitive) for non-development 
            // debugging so turned off for now.
            /*
            if (debug) {
                String defaultFile = 
                            PropertiesSingleton.getInstance().getProperty(
                                            "defaultSource", null
                                );
                if (defaultFile != null) {
                    inputStream = getClass().getResourceAsStream("/" + defaultFile);
                }
            }
            */
        } catch (FileNotFoundException fnfe) {
            if (debug) fnfe.printStackTrace();
            throw new ParseFailedException(
                    PropertiesSingleton.getInstance().getProperty("txtFileReadingIssue") +
                    recordHolderNames.get(index)
                );
        }
        Reader reader = new InputStreamReader(inputStream);
        buffer = new BufferedReader(reader);
        
    }
    
    
    
    
    /**
    * Disconnects from current source and any file.
    * <p>
    * Forces a garbage collection.
    *
    * @throws     ParseFailedException     Not thrown in this implementation.
    */
    public void disconnectSource() throws ParseFailedException {
        
        try {
            buffer.close();
        } catch (IOException ioe) {
            // Unlikely to be an issue if the buffer exists, 
            // and associated conditions around this are caught before.
            if (debug) ioe.printStackTrace();
        }

        // Force collection of any row objects etc.
        Runtime.getRuntime().gc();
        
    }
    
    
    
    
    /**
    * Sets the defaults for warnings and exceptions in English if an appropriate 
    * language properties file is missing.
    *
    */
    private void gapFillLocalisedGUIText() {
        
        Properties defaults = new Properties();
        
        // Warnings and messages.
        
        defaults.setProperty("txtPush", "Pushing data file piecemeal.");
        defaults.setProperty("txtRead", "Reading in file.");
        defaults.setProperty("txtDatasetRead", "Dataset read in: ");
        defaults.setProperty("txtDetails", "Details from file: ");
        defaults.setProperty("txtNoFileIssue", "No file/s chosen to read.");
        defaults.setProperty("txtNoDataIssue", "At least one of the files does " + 
                                               "not appear to contain data."
                                            );
        defaults.setProperty("txtFileConnectionIssue", "Cannot connect to file. " + 
                                                       "Please check you have " + 
                                                       "permission to read the " + 
                                                       "file/s."
                                                    );
        defaults.setProperty("txtFileFormatIssue", "Having difficulty reading " +
                                                   "a file. Are you sure all " + 
                                                   "your files are CRU TS 2.x " + 
                                                   "format?"
                                                );
        defaults.setProperty("txtHeaderDateIssue", "There is a problem with a " + 
                                                   "date in a file header: "
                                                );
        defaults.setProperty("txtHeaderFormatIssue", "There has been a problem " + 
                                                     "reading a file header."
                                                );
        defaults.setProperty("txtXYFormatIssue", "There is a problem reading a " + 
                                                 "grid reference in a file."
                                                );
        defaults.setProperty("txtLineFormatIssue", "Ill-formatted line, extra, or " + 
                                                   "missing data within: "
                                                );
        defaults.setProperty("txtMissingDataIssue", "Missing data without " + 
                                                    "missing data flag within: "
                                                );
        defaults.setProperty("txtExtraValuesIssue", "Extra / too-long values at: ");
        defaults.setProperty("txtFileReadingIssue", "There is a problem " + 
                                                    "reading the file: "
                                                );
        PropertiesSingleton.getInstance().setDefaults(defaults);
        
    }
    
    
    
    
    /**
    * Reports progress to reportingListeners.
    * <p>
    * Reports if progress is a multiple of total records / 100. 
    * If progress is zero or less, reports progress as 0 of 1.
    * 
    * @param        progress        Progress in record processing.
    * @param        dataset         Dataset to extract estimate of processing 
    *                               to be done.
    */
    public void reportProgress(int progress, IDataset dataset) {
        int totalEstimate = dataset.getEstimatedRecordCount();
        
        if (progress <= 0) {
            for (IReportingListener reportingListener : reportingListeners) {
                reportingListener.updateAppProgress(0, 1);
            }
            return;
        }    
        
        if ((totalEstimate > 0) && ((progress % (totalEstimate / 100)) == 0)) {
            for (IReportingListener reportingListener : reportingListeners) {
                reportingListener.updateAppProgress(progress, totalEstimate);
            }
        }
                
    }
    
    
    
    
    
    /**
    * Reports progress to reportingListeners.
    * <p>
    * Reports for an arbitrary progress and total worked towards.
    *
    * @param        progress        Value indicating progress through work total.
    * @param        total           Value indicating total work to do.
    */
    public void reportProgress(int progress, int total) {
       
        for (IReportingListener reportingListener : reportingListeners) {
            reportingListener.updateAppProgress(progress, total);
        }
            
    }
    
    
    
    
    /**
    * Reports message to reportingListeners.
    * 
    * @param        message         Message to reporting listeners.
    */
    public void reportMessage(String message) {
        
        for (IReportingListener reportingListener : reportingListeners) {
            reportingListener.updateAppMessage(message);
        }
      
    }
    
    
    
    
}