/**
* Provides a set of tests for IDataConsumers. 
* <p>
* Tests were built for edge conditions for a few specific methods, but 
* the most significant tests run through a full database/file load and 
* check metadata ('title') and the first and last lines of output files/tables. 
* As data entry is checked with the supplier, these were largely implemented to 
* check soundness during development. FlatFileConsumer tests were removed as the 
* utility methods are identical to HadoopConsumer and the overall function of the 
* class is tested with DerbyConsumer.
*
* <div style="width:600px;">
* MIT License
* <p>
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* <p>
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
* <p>
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*</div>
*
* Copyright (c)
*
* @author <a href="https://ajevans.github.io/">Andy Evans</a>
* @mvnversion
*/
package io.github.ajevans.dbcode.dbconsumers;