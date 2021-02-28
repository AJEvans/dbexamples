## Notes on data and metadata

### Data and validity

Information about the data, its currency, and parsing can be found in the [docs for CruTS2pt1Supplier](apidocs/io/github/ajevans/dbcode/filesuppliers/CruTs2pt1Supplier.html). 
Note that this class is deprecated to flag its association with a superseded dataset, but this isn't intended to stop its use, or the data's use. 

The data comes ontology-free (although a brief description is available online - see [docs for CruTS2pt1Supplier](apidocs/io/github/ajevans/dbcode/filesuppliers/CruTs2pt1Supplier.html)). 
This makes for validation (for example, that data is within range), that is very broad. There is, for example, no indication of whether any numerical data 
can be floating point, even where it is currently integer data, or whether decimal places will be used when present (for example, in coordinate systems). All numerical data 
is therefore treated as decimal, though generally of fixed scale (so, for example, integers are treated as zero-decimal-place decimals until the need arises to treat them otherwise). 
Numerical data is held as Java BigDecimals, the numerical precision of which is to ANSI X3.274-1996 / X3.274-1996/AM 1-2000 standards. Dates are maintained internally in a <span style="text-decoration: underline dotted gray;" title="Coordinated Universal Time">UTC</span> calendaring system.  

The system carries out the following validations, flagged to the user where 
the data fails:

- Expected numerical data is checked to be numerical.
- Numerical data is treated as five significant figures or less (data columns being five digits wide).
- Missing data without the appropriate flag, and extra data, is flagged. 
- Date data is checked as representing real dates (e.g. not 43rd of the 13th month). 

The coordinate system limits aren't given (though it seems likely that negative numbers are West for latitude and South for longitude and both range +/- 180); given 
this, coordinates aren't validated, though it would be simple to do so if information was confirmed. In general, the absence of value domain information makes validation limited.

&nbsp;

### Database and file output

Files are saved in a directory named after the dataset (i.e. "CRU TS 2.1"; or "CRUTS21" for database files). This directory is within either a "dbexamples-flatfiles" or 
"dbexamples-databases" directory (the latter for Derby and Hadoop) (these latter defaults can be changed &ndash; see [code description](code.html)). All directories 
are, in turn, made either in the directory chosen by the user or their default home directory. Each file or database table is named after the CRU TS observation type (e.g. "pre" for precipitation), 
 the date range represented in the file (e.g. 1991 2000), and an integer number just incase more than one file has the same data and observation coverage 
 but a different geography (e.g. "pre 1991 2000 1.csv"; or "PRE199120001" for a table). Flat files are saved as comma-separated variables (CSV); other files are not viewable directly, but where this 
is the case, writing can be confirmed in the user interface which will display a test-extract of the first and last record as files are made. 
Each dataset and file comes with a metadata file or table in the paired key=value format. For flat files these are called <i>filename</i>META.properties, while 
for Derby and Hadoop, they are called <i>filename</i>META.
 
The database used is the Derby system. Derby is an Open Source Apache Software Foundation database. It runs without 
installation, and has a small footprint, making it a good choice for bundling with applications. For more information see the 
[Derby website](https://db.apache.org/derby/). As distributed "database-like" filesystems are increasingly in use for large file 
storage and processing, a Hadoop output is also provided. Again, Hadoop runs bundled within the application, although it is most 
powerful when used across a set of networked machines. For more information, see the [Hadoop website](https://hadoop.apache.org/).

The specification shows the date data held in the database in US-format date strings (i.e. first two digits are month). This is 
the format which the first and last records in tables are listed in within the <span style="text-decoration: underline dotted gray;" title="Graphical User Interface">GUI</span> 
after tables are created (these are drawn from the table to prove the data is there). However, as there's no day information in the files I'd want to clarify this 
requirement with the client with regards the values actually held in storage; I've gone with holding the dates as unformatted date 
information as this helps with validation, though it would be easy enough to shift this to US-format date strings 
or display them appropriately in any given output. The flat file consumer outputs the dates as <span style="text-decoration: underline dotted gray;" title="International Organization for Standardization">ISO</span> 2014 / <span style="text-decoration: underline dotted gray;" title="International Organization for Standardization">ISO</span> 8601-2:2019 standard (YYYY-MM-DD) as these files are not intended to match the 
database section of the specification, albeit they may be the simplest way to check the data is held. The Hadoop files hold dates as US format strings. 
In the absence of day information it is assumed all monthly dates attributed to values in the files are associated with the first day of the month. 

The application outputs first normal form data tables, as outlined in the specification. In the absence of use-cases, no attempt has been made to 
tune the Derby database to specific, e.g. indexing. In the absence of a per-record unique identifier in the files, 
a simple unique key hasn't been allocated explicitly, though a unique natural key could be generated from a hash of the date, observation, and coordinates if needed.

&nbsp;

### Metadata

There is minimal metadata associated with the data, in the form of a file header, the components of which are briefly described online (see [docs for CruTS2pt1Supplier](apidocs/io/github/ajevans/dbcode/filesuppliers/CruTs2pt1Supplier.html)). 

Nevertheless, metadata tables and files are generated for each dataset (set of database tables/files) and record holder (specific tables and files); see above for the naming 
conventions for these. In the absence of much formal and embedded information on the files' derivation or ontological status, or an Application Profile, the metadata schema used 
is the high-level Dublin Core schema (<span style="text-decoration: underline dotted gray;" title="International Organization for Standardization">ISO</span> 15836:2009), which was developed for cross-domain resource description. This has been extended with additions to cover 
additional information in the dataset. A production-level example might implement a fuller climate-data metadata schema, save that the data is aggregated and derived, which removes 
much of the information that would be covered (such as station data). A schema centred on <span style="text-decoration: underline dotted gray;" title="International Organization for Standardization">ISO</span> 19115-1:2014 "Geographical Information" could be utilised, 
along with <span style="text-decoration: underline dotted gray;" title="International Organization for Standardization">ISO</span> 19115-2:2009, the extension for gridded data &ndash; although much of the latter centres on imagery. Overall, however, alternatives to Dublin Core (and indeed, 
alternative output formats such as <span style="text-decoration: underline dotted gray;" title="Open Geospatial Consortium">OGC</span> NetCDF) are strongly dependent on use-cases. 

<strong>Note that, in line with the specification, no attempt has been made to scale the data; however, comments on scaling and a link to further information about 
the file data are given in the `notes` metadata section for each file/table and displayed to the user.</strong>


