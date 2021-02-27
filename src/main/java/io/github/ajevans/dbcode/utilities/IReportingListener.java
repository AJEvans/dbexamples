/*
* Distributed under the MIT License: see package documentation.
*/
package io.github.ajevans.dbcode.utilities;




/**
* Simple interface for classes wishing to listen to the progress of data 
* supply or consumption.
*
* Partially parallels the api for javafx.concurrent.Task but allows for 
* bridging away from JavaFX.
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
* @see io.github.ajevans.dbcode.data.io.IDataSupplier#addReportingListener(IReportingListener reportingListener)
* @see io.github.ajevans.dbcode.data.io.IDataConsumer#addReportingListener(IReportingListener reportingListener)
*/
public interface IReportingListener{
    
    
    /**
    * Method called with message.
    *
    * @param     message    Message to report.
    */
    public void updateAppMessage(String message);
    
    
    
    /**
    * Method called with progress.
    *
    * @param     workDone    Fraction of max.
    * @param     max         Total work to do as a double.
    */
    public void updateAppProgress​(double workDone, double max);
    
    
    
}