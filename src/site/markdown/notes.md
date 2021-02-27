## Project outline

The project specifications were to read in a CSV file, transform it, and output it as a database table, with the user being able to choose the initial file. 


This is achievable with a file chooser dialog, CSV reader, CSV writer (to write out an intermediate file in the correct format), and an SQL bulk file loader. 

&nbsp;

However broader software design principles come into play for a well-formed solution:

1) UI/UX design: the file chooser needs building into a coherent user experience.

2) Software longevity and maintenance requires an extendable class structure with loose class coupling founded on sound abstraction.

3) The software needs to cope with a wide variety of file sizes.

4) Potentially uncleaned data needs to be validated and the data needs to be usable.

4) A suite of appropriate tests are needed to ensure the software works properly. 



&nbsp;

To address these requirements, in turn:

1) A GUI was constructed that walks the user through the conversion process; methods were adapted for GUI-centred, piped, and automated loading; internationalisation implemented.

2) Input (from a file) and output (to databases/files) were abstracted to strategy-pattern hierarchies allowing linking and data transfer between the two at the interface level. A suitable abstract data model was developed to work across the system. This abstraction was further integrated into the GUI through class-loading of input and output implementations at runtime. Even where the classes are specific (e.g. to a file or output type) they've been kept as generic as possible internally to act as reworking foundations for future classes.

3) Less rapid but less memory-heavy large file contingencies were built into the system. 

4) In the absence of a full ontological structure the data is validated against the limited description in the header, and metadata tables produced.

5) Appropriate tests were utilised throughout development. 


&nbsp;

### More detail on class structure, data validation, outputs and metadata

For detail on the code and data output, please see:

- [Code and UML](code.html) 

- [Data and metadata](data.html)

