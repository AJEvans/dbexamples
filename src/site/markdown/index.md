## Introduction

Project builds a jar-based app that reads in Climate Research Unit Time Series 2.1 files gridded files, parses and transforms them, and writes them to a 
variety of outputs (flat-file; <span style="text-decoration: underline dotted gray;" title="Structured Query Language">SQL</span> database; distributed filesystem). A test file is [available online](https://jbasoftware.com/files/jba-software-code-challenge-data-transformation.zip). 
A set of [notes](notes.html) is available for 
the software internals, data outputs, and design reasoning. The application is 
written in the Java language for portability. 

&nbsp;

![GUI showing file menu](images/gui-filemenu.jpeg) ![GUI showing options](images/gui-options.jpeg) 

&nbsp;

To run the application, you'll need the latest Java install: [download site](https://www.java.com/en/). If your computer already has Java installed, please make sure it is the 
latest version.

Then download this [jar file](releases/dbexamples-1.0-jar-with-dependencies.jar) somewhere convenient.

If Java has installed properly (and it doesn't always), you should then be able to double-click the jar file to run it. If this fails, open 
a command prompt / terminal in the directory where you have saved the file*, and enter:

`java -jar dbexamples-1.0-jar-with-dependencies.jar`

(*You can do this in Windows by navigating to the directory in Windows Explorer and typing `cmd` in the address bar. 
For Macs, open Terminal, type `cd ` (with a space after it) and then drag the folder from Finder into Terminal. Press enter.)

The application should be fairly self-explanatory. You can set the output type and location under the `Options` menu. The 
outputs will appear in your home directory unless you choose otherwise under `Options`. The directories and files it 
produces should be relatively self explanatory (any databases files will obviously be complicated in detail but 
the directories should be obvious), but if 
you'd like more information on these, see [notes on data and metadata](data.html). Database entries can be confirmed in the 
user interface: the first and last records of any given table are listed.




&nbsp;

Although the code should adapt to large, memory hungry, files, this will be much slower. You can improve the memory for a standard run 
by (instead of the above) typing:

`java -jar -Xmx4g dbexamples-1.0-jar-with-dependencies.jar`

where the "4g" is the amount of memory you'd like to allocate to the process in gigabytes.

&nbsp; 

### Building and testing the source code

If you want to rebuild the application, the simplest (indeed only practical) way to build the source code is to download [Maven](https://maven.apache.org/) - again, if you have it installed, make sure you 
have the latest version. If you haven't got it installed, you'll need to download and install the latest Oracle [Java Development Kit (JDK)](https://www.oracle.com/uk/java/technologies/javase-downloads.html) 
(or less restrictive [Open JDK](https://openjdk.java.net/)) first, install it, and then follow the 
[Maven install instructions](https://maven.apache.org/install.html). 

You can then download the [source code repository](https://github.com/AJEvans/dbexamples) somewhere convenient. 

Find the download and extract it. Go into the directory produced, and you should find a file called 
`pom.xml`. Open a command prompt within this directory, and type:

`mvn package`

This will download the other code libraries needed for the application (for example, the database code) and compile the code. 
As it does this, it will also run a series of unit tests to confirm the code runs ok. 

Although Maven will generate a `target` directory, ignore this; if, instead, you now look 
in the `src` directory (which should be in the same directory as the `pom.xml`) and then `resources` and `releases` you should find a 
newly built jar file that you can run as above. If you want to rebuild this website including all the JavaDocs for the code as well, locally, you can 
type:

`mvn site`

This will generate the site on your local drive. If you look in the `target` directory mentioned above, you should see a `site` directory. 
Go into this and open the `index.html` to open this page.