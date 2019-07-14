/**
 *
 */

package ostrowski.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class provides diagnostic utility to an object.
 *
 * Usage:
 * -  Creates an instance of Diagnostics object during initialization
 *    of object that requires diagnostics.
 * -  Start message logging using the following methods:
 *    -> logMessage(int type, String source, String message, Exception ex)
 *    -> logMessage(int type, String source, String message)
 * -  Prior to exiting program, invoke
 *       endDiagnostics()
 */
public class Diagnostics {

   // Message Types
   //
   public  static final int    TYPE_INFO           = 1;
   private static final String TYPE_INFO_STRING    = "INFO";   // Each of these
   public  static final String TYPE_WARNING        = "WARN";   // EXACTLY FOUR
   public  static final String TYPE_DEBUG_INFO     = "DBUG";   // CHARACTERS LONG
   public  static final String TYPE_SYSTEM_OUT     = "CONS";
   public  static final String TYPE_STATISTICAL    = "STAT";
   public  static final String TYPE_MONITORS       = "MON ";
   public  static final String TYPE_OPERATIONS     = "OPER";
   public  static final String TYPE_ERROR          = "ERR ";
   public  static final String TYPE_SEVERE_ERROR   = "SERR";
   public  static final String TYPE_DEBUG_TRAP     = "TRAP";
   // These message types are identical in string content as other (non-manditory)
   // string message types, however we need to make sure that the JVM does not
   // use the exact same object for each. Therefore, we play a little game that
   // makes sure that TYPE_INFO_STRING != TYPE_MANDATORY_INFO. If the equal operator (=)
   // returns true for these, then there will actually be no difference between them,
   // and will lose all mandatory information on the console or log file.
   // This has another side-effect in that command line commands issued will not respond
   // unless LogInfoToConsole is turned on (which is usually not).
   public  static final String TYPE_MANDATORY_INFO = new String("IN") + new String("FO");
   public  static final String TYPE_MANDATORY_STAT = new String("ST") + new String("AT");


   // This is a system-depended CR-LF that we should use any time we
   // want to output a newline character.
   public static String lineSeparator = "\n";//(String) java.security.AccessController.doPrivileged(
                                             //    new sun.security.action.GetPropertyAction("line.separator"));

   // Message Sources
   public static final String SOURCE_SERVER = "SERVER";
   //public static final String SOURCE_CLIENT = "CLIENT";
   //public static final String SOURCE_APPLET = "APPLET";

   // Message Logging to file priority
   static final int IOTHREAD_PRIORITY = java.lang.Thread.MIN_PRIORITY;//usable types: MIN_PRIORITY, NORM_PRIORITY, MAX_PRIORITY

   // Message Numbering Format
   // 0 specifes a digit
   static final String DECIMAL_FORMAT = "00000";
   // Decimal Formatter
   private static DecimalFormat DEC_FORMAT = new DecimalFormat(DECIMAL_FORMAT);

   private static final String EXCEPTION_CALL_STACK            = "e";
   private static final String ASSOCIATION_OPERATION_ID        = "o";

   private static final String FILE_HEADER =
         "<?xml version='1.0' encoding='ISO-8859-1'?>" + lineSeparator +
         "<diags>"                                     + lineSeparator +
         "<legend>"                                    + lineSeparator +
         "\t<message>'m'</message>"                    + lineSeparator +
         "\t<m>"                                       + lineSeparator +
         "\t\t<attributes>"                            + lineSeparator +
         "\t\t\t<i sort='0'>identifier</i>"            + lineSeparator +
         "\t\t\t<t sort='0'>dateTime</t>"              + lineSeparator +
         "\t\t\t<y sort='1'>type</y>"                  + lineSeparator +
         "\t\t\t<h sort='1'>thread</h>"                + lineSeparator +
         "\t\t</attributes>"                           + lineSeparator +
         "\t\t<subnodes>"                              + lineSeparator +
         "\t\t\t<" + EXCEPTION_CALL_STACK        + " sort='0'>exception call stack</" + EXCEPTION_CALL_STACK        + ">" + lineSeparator +
         "\t\t\t<" + ASSOCIATION_OPERATION_ID    + " sort='1'>operationID</"          + ASSOCIATION_OPERATION_ID    + ">" + lineSeparator +
         "\t\t\t<d sort='0'>data</d>"                  + lineSeparator +
         "\t\t</subnodes>"                             + lineSeparator +
         "\t</m>"                                      + lineSeparator +
         "</legend>"                                   + lineSeparator;

   private static final String  FILE_FOOTER              = "</diags>";
   private static final String  START_COMMENT_DATA       = "<!--";
   private static final String  STOP_COMMENT_DATA        = "-->";

   // Message Format
   // {0}: Message number
   // {1}: Time Stamp
   // {2}: Type
   // {3}: Thread Name
   // {4}: Message
   // {5}: associations
   // {6}: exception
   // {7}: call stack dump
   private static final String MESSAGE_NORM_FORMAT = "<m i=\"{0}\" t=\"{1}\" y=\"{2}\" h=\"{3}\">{5}<d><![CDATA[{4}]]></d></m>";
   private static final String MESSAGE_XCPT_FORMAT = "<m i=\"{0}\" t=\"{1}\" y=\"{2}\" h=\"{3}\">{5}<d><![CDATA[{4}]]></d><"
                                                     + EXCEPTION_CALL_STACK + "><![CDATA[{6}\n{7}]]></" + EXCEPTION_CALL_STACK + "></m>";
   private static MessageFormat MSG_FORMAT_NORMAL    = new MessageFormat(MESSAGE_NORM_FORMAT);
   private static MessageFormat MSG_FORMAT_EXCEPTIONAL = new MessageFormat(MESSAGE_XCPT_FORMAT);

   // Property File Name
   private String _propertyFile;

   // Properties for Log File Name
   private String           _filePath;
   private int              _fileNumber;
   private RandomAccessFile _file;
   private boolean          _fastDiag;
   private long             _availableFileSizeRemaining;
   private long             _messageNumber;

   // Boolean bits for Loggings to File/Console
   private boolean _logInfoToConsole = true;
   private boolean _logInfoToFile = true;

   // To obtain parameters from Property File
   private DiagParameters _params;

   MessageQueue _queue;
   private IOThread _myThread;
   protected boolean _endDiags = false;

   private final Semaphore _lockThis;

   /**
    * DiagMsg is the class that contains Diagnostics message.
    */
   class DiagMsg {
      public String     _type;
      public String     _message;
      public String     _timeStamp;
      public String     _threadName;
      public Exception  _exception;
      public String     _associations;

      /**
       * Creates an instance of this class with a diagnostic message
       * and an exception.
       * @param type       the type of the message (INFO, ERR, ...)
       * @param source     the source of the message
       * @param message    the message to log
       * @param timeStamp  the time the message was created
       * @param threadName the name of the thread that created the message
       * @param e          the exception to log (null if no exception occured)
       */
      DiagMsg(String type, String message, String timeStamp, String threadName, Exception ex) {
         _type         = type;
         _message      = message;
         _timeStamp    = timeStamp;
         _threadName   = threadName;
         _exception    = ex;
         _associations = getAssociationsStringForThisThread();
         if (ex != null) {
            Vector<String> errors = new Vector<>();
            errors.add(timeStamp);
            errors.add("Exception caught during server operation:");
            errors.add(StringUtils.getCallStack(ex));
            writeStringsToExceptionsFile(errors, message, threadName);
         }
      }
   }

   /**
    * Creates an instance of this class.
    * @param propertyFile  the filename that defines the properties for diagnostic utility
    */
   public Diagnostics(String propertyFile, DiagParameters diagParams, boolean startNewFile) {
      _propertyFile  = propertyFile;

      // Diagnostics Properties
      _params = diagParams;

      _lockThis = new Semaphore("Diagnostics.Diagnostics", Semaphore.CLASS_DIAGNOSTICS);
      _associationsTable = new Hashtable<>();

      beginDiagnostics(true, startNewFile);
   }

   /**
    * Creates an instance of this class (preferrably to be used for applets).
    * @param context          the path to locate the propertyFile
    * @param propertyFile     the filename that defines the properties for diagnostic utility
    * @param bAllowLogToFile  sets file logging
    */
   public Diagnostics(URL context, String propertyFile, boolean bAllowLogToFile, boolean startNewFile) {
      _propertyFile  = propertyFile;

      // Obtain parameters from _propertyFile
      _params = new DiagParameters(context, _propertyFile);

      _lockThis = new Semaphore("Diagnostics.Diagnostics", Semaphore.CLASS_DIAGNOSTICS);
      _associationsTable = new Hashtable<>();

      beginDiagnostics(bAllowLogToFile, startNewFile);
   }

   private void beginDiagnostics(boolean bAllowLogToFile, boolean startNewFile) {
      _endDiags = false;

      _logInfoToConsole = _params.logInfoToConsole.equalsIgnoreCase("yes");
      _logInfoToFile    = _params.logInfoToFile.equalsIgnoreCase("yes");
      enableFastLog(_params.fastLog.equalsIgnoreCase("yes"));

      _fileNumber                 = 1;        // _fileNumber starts from 1 to maxLogFiles
      _availableFileSizeRemaining = _params.maxLogFileSize * (long)1000;
      _filePath                   = null;
      _file                       = null;

      initLog(startNewFile);
      System.out.println(_params.diagLabel);
      /*
      String temp = "0123456789abcdef";
      System.out.println("line separator[0] == " + temp.charAt(lineSeparator.charAt(0) >> 4) + temp.charAt(lineSeparator.charAt(0) & 0xF));
      if (lineSeparator.length() > 1)
      System.out.println("line separator[1] == " + temp.charAt(lineSeparator.charAt(1) >> 4) + temp.charAt(lineSeparator.charAt(1) & 0xF));
      if (lineSeparator.length() > 2)
      System.out.println("line separator[2] == " + temp.charAt(lineSeparator.charAt(2) >> 4) + temp.charAt(lineSeparator.charAt(2) & 0xF));

      String lineSeparator2 = "\n";
      System.out.println("\\n[0] == " + temp.charAt(lineSeparator2.charAt(0) >> 4) + temp.charAt(lineSeparator2.charAt(0) & 0xF));
      if (lineSeparator2.length() > 1)
      System.out.println("\\n[1] == " + temp.charAt(lineSeparator2.charAt(1) >> 4) + temp.charAt(lineSeparator2.charAt(1) & 0xF));
      if (lineSeparator2.length() > 2)
      System.out.println("\\n[2] == " + temp.charAt(lineSeparator2.charAt(2) >> 4) + temp.charAt(lineSeparator2.charAt(2) & 0xF));
      */

      _queue = new MessageQueue(_params.messageQueueMaxSize);
      _myThread = new IOThread("DiagThread");
      _myThread.start();
   }

   @SuppressWarnings("serial")
   private class DiagnosticAssociationException extends java.lang.RuntimeException {
      public DiagnosticAssociationException() {
         super();
      }
   }

   // This class DOES NOT NEED ANY SYNCHRONIZED METHODS OR VOLATILE MEMBERS,
   // because each instance of one of these object is only ever accessed by the
   // thread that owns it. Therefore, there is no chance for concurrent access.
   private class DiagnosticAssociations implements Cloneable
   {
      // This string is used to reduce the number of times an association string is built.
      // Any time it is null, it must be rebuilt. Therefore, any time an entry in the
      // operationIDsList or the associationTable changes, we set it to null.
      private String    _prebuiltAssociations;
      private Hashtable<String, Hashtable<String, Integer>> _associationTable;
      Vector<String>    _operationIDsList;
      private Vector<String>    _operationNamesList;
      private boolean   _unnamedPartOfNamedOperation;

      public DiagnosticAssociations() {
         _prebuiltAssociations        = null;
         _associationTable            = new Hashtable<>();
         _operationIDsList            = new Vector<>();
         _operationNamesList          = new Vector<>();
         _unnamedPartOfNamedOperation = false;
      }
      @Override
      public Object clone() {
         DiagnosticAssociations clone = new DiagnosticAssociations();
         clone.setData(this);
         return clone;
      }
      @SuppressWarnings("unchecked")
      public void setData(DiagnosticAssociations newData) {
         if (newData != null) {
            _associationTable            = (Hashtable<String, Hashtable<String, Integer>>)(newData._associationTable.clone());
            _prebuiltAssociations        = (newData._prebuiltAssociations == null) ? null : new String(newData._prebuiltAssociations);
            _operationIDsList            = (Vector<String>)(newData._operationIDsList.clone());
            _operationNamesList          = (Vector<String>)(newData._operationNamesList.clone());
            _unnamedPartOfNamedOperation = newData._unnamedPartOfNamedOperation;
         }
         else {
            // Set to default value (as defined by constructor)
            setData(new DiagnosticAssociations());
         }
      }

      public void appendOperations(DiagnosticAssociations operationsSource) {
         // If there are no operationIDs in the list, do nothing.
         if (operationsSource._operationIDsList.size() > 0) {
            _prebuiltAssociations = null;
            String lastName = (operationsSource._operationNamesList.lastElement());
            String opID = null;
            String opName = null;
            for (int i=0 ; i<operationsSource._operationIDsList.size() ; i++) {
               opID   = (operationsSource._operationIDsList.elementAt(i));
               opName = (operationsSource._operationNamesList.elementAt(i));
               beginOperation(opID, opName, false/*printDiagnostic*/);
            }
            // If the last opName was null, then operationsSource was an Unnamed operation
            if ((opName == null) && (lastName != null)) {
               operationsSource._unnamedPartOfNamedOperation = true;
            }
         }
      }

      /*
                        Internal IDs   Internal Strings              Display
      ----------------------------------------------------------------------------------------------------
                        -              -                             -
      unknown start     1              null                          1
      logon start       1,2            log on, log on                1     Operation 'Log on' identified
      logon end         1              log on
      unknown end       -              -                             -     Operation 'Log on' ended

                        -              -                             -
      unknown start     1              null                          1
      logon start       1,2            log on, log on                1     Operation 'Log on' identified
      validate PW start 1,2,3          log on, log on, val PW        1,3   Operation 'Validate PW' started
      validate PW end   1,2            log on, log on                1,3   Operation 'Validate PW' ended
      logon end         1              log on
      unknown end       -              -                             -     Operation 'Log on' ended
      */
      public void beginOperation(String operationID, String operationName, boolean printDiagnostic)
      {
         boolean unNamedOperationIdentified = false;

         int operationNameCount = _operationNamesList.size();
         if (operationNameCount > 0) {
            if (_operationNamesList.elementAt(operationNameCount-1) == null) {
               // remove the previous 'null' entry, which indicated an unNamed operation
               // was inprogress, and we have now identified it.
               _operationNamesList.remove(operationNameCount-1);
               // replace the 'null' entry with the now named operation
               // This will create two IDENTICAL entries for the named entry.
               _operationNamesList.add(operationName);
               unNamedOperationIdentified = true;
            }
         }
         _operationIDsList.add(operationID);
         _operationNamesList.add(operationName);
         // clear out the pre-build association string so we force
         // the association string to be re-built next time its used.
         _prebuiltAssociations = null;

         if (printDiagnostic) {
            if (unNamedOperationIdentified) {
               logMessage(TYPE_OPERATIONS, "Operation " + operationName + " identified.");
            }
            else {
               logMessage(TYPE_OPERATIONS, "Operation " + operationName + " started.");
            }
         }
      }

      public void terminateOperation(String operationName, boolean ouputOperationEndedMessage)
      {
         boolean operationTerminatingNamedPortionOfUnNamedOperation = false;

         int operationNameCount = _operationNamesList.size();
         if (operationNameCount > 0) {
            String lastOperationNameInProgress = _operationNamesList.elementAt(operationNameCount-1);
            if (operationNameCount > 1) {
               // If Operation (N) IS Operation (N-1) (not just equal, but the same object),
               // then we are in a case were Operation (N-1) was unnamed, and Operation (N)
               // named that operation.
               // When Operation (N) terminates, we don't print anything out,
               // because that is not the true end of the operation. The true end of
               // that operation is when the unnamed operation (Operation (N)) terminate.
               operationTerminatingNamedPortionOfUnNamedOperation =
                  (lastOperationNameInProgress == _operationNamesList.elementAt(operationNameCount-2));
            }
            // check to ensure that the name specified in the parameter matches the
            // name that we just removed from the list. If the parameter is null,
            // then we don't report this as an error, so we claim that they match.
            if ((operationName != null) && (!operationName.equals(lastOperationNameInProgress))) {
               try {
                  throw new DiagnosticAssociationException();
               }
               catch (DiagnosticAssociationException ex) {
                  logMessage(TYPE_ERROR, "terminateOperation("+operationName+") called, but "+lastOperationNameInProgress+" was the last operation in progress.", ex);
               }
            }
            if (ouputOperationEndedMessage) {
               if (!operationTerminatingNamedPortionOfUnNamedOperation) {
                  if (lastOperationNameInProgress == null) {
                     // If the lastOperation is null, then it was an unnamed operation
                     // that never got a name before it completed. Since it therefore never
                     // had a diag of type TYPE_OPERATIONS, we need to make one now, so that
                     // it can be found by filtering on type OPERATIONS.
                     // However, if this operation was meerly one portion of a named operation
                     // that occurred on another thread, then we don't need to give it the
                     // type of TYPE_OPERATION
                     if (_unnamedPartOfNamedOperation) {
                        logMessage(TYPE_INFO, "Unnamed Operation ended.");
                     }
                     else {
                        logMessage(TYPE_OPERATIONS, "Unnamed Operation ended.");
                     }
                  }
                  else {
                     logMessage(TYPE_INFO, "Operation " + lastOperationNameInProgress + " ended.");
                  }
               }
            }
            // Don't change the operations until after we have printed anything we need to
            // to the diags, or those diags won't have the operation info in them.
            _operationIDsList.removeElementAt(operationNameCount-1);
            _operationNamesList.removeElementAt(operationNameCount-1);
            // clear out the pre-build association string so we force
            // the association string to be re-built next time its used.
            _prebuiltAssociations = null;
         }
         else {
            try {
               throw new DiagnosticAssociationException();
            }
            catch (DiagnosticAssociationException ex) {
               logMessage(TYPE_ERROR, "Operation " + operationName + " ended, but no operation was in progress.", ex);
            }
         }
      }

      public void associateItem(String itemName, String itemValue)
      {
//         boolean duplicateAssociation = false;

         if ((itemName != null) && (itemValue != null))
         {
            Hashtable<String, Integer> valueTable = _associationTable.get(itemName);
            Integer occuranceCount;
            if (valueTable == null) {
               valueTable = new Hashtable<>();
               _associationTable.put(itemName, valueTable);
               occuranceCount = null;
            }
            else {
               occuranceCount = valueTable.get(itemValue);
            }
            if (occuranceCount == null) {
               occuranceCount = Integer.valueOf(1);
               // If this is the first record of this value,
               // clear out the pre-build association string so we force
               // the association string to be re-built next time its used.
               _prebuiltAssociations = null;
            }
            else {
               occuranceCount = Integer.valueOf(occuranceCount.intValue() + 1);
//               duplicateAssociation = true;
            }
            valueTable.put(itemValue, occuranceCount);
         }
//         if (duplicateAssociation) {
//            try {
//               throw new DiagnosticAssociationException();
//            }
//            catch (DiagnosticAssociationException ex) {
//               logMessage(TYPE_ERROR, "Item associated "+occuranceCount.intValue()+" times. Item Name: " + itemName + ", Item Value: " + itemValue, ex);
//            }
//         }
      }

      public void unassociateItem(String itemName, String itemValue)
      {
         if ((itemName != null) && (itemValue != null))
         {
            Hashtable<String, Integer> valueTable = _associationTable.get(itemName);
            if (valueTable != null) {
               Integer occuranceCount = valueTable.get(itemValue);
               if (occuranceCount != null) {
                  if (occuranceCount.intValue() <= 1) {
                     valueTable.remove(itemValue);
                     // clear out the pre-build association string so we force
                     // the association string to be re-built next time its used.
                     _prebuiltAssociations = null;
                  }
                  else {
                     occuranceCount = Integer.valueOf(occuranceCount.intValue() - 1);
                     valueTable.put(itemValue, occuranceCount);
                  }
               }
               if (valueTable.size() == 0) {
                  _associationTable.remove(itemName);
               }
            }
         }
      }

      public String getAssociationsString()
      {
         // Check for a pre-build association string so that we don't have to
         // re-build this string again if nothing has changed since our last diag.
         if (_prebuiltAssociations == null) {
            StringBuffer assocBuffer = new StringBuffer();
            // Operation IDs are garuanteed to be unique, although more than one may exist.
            for (int i=0 ; i<_operationIDsList.size() ; i++) {
               assocBuffer.append('<');
               assocBuffer.append(ASSOCIATION_OPERATION_ID);
               assocBuffer.append('>');
               assocBuffer.append(_operationIDsList.elementAt(i));
               assocBuffer.append("</");
               assocBuffer.append(ASSOCIATION_OPERATION_ID);
               assocBuffer.append('>');
            }

            String itemName;
            String itemValue;
            Enumeration<String> itemNames;
            Enumeration<String> itemValues;
            Hashtable<String, Integer> itemValueTable;
            itemNames = _associationTable.keys();
            while (itemNames.hasMoreElements()) {
               itemName = itemNames.nextElement();
               if (itemName != null) {
                  itemValueTable = _associationTable.get(itemName);
                  if (itemValueTable != null) {
                     itemValues = itemValueTable.keys();
                     while (itemValues.hasMoreElements()) {
                        itemValue = itemValues.nextElement();
                        if (itemValue != null) {
                           // At this point we have a single associated pair of values
                           // put it into a '<x>y</x>' format
                           assocBuffer.append('<');
                           assocBuffer.append(itemName);
                           assocBuffer.append('>');
                           assocBuffer.append(itemValue);
                           assocBuffer.append("</");
                           assocBuffer.append(itemName);
                           assocBuffer.append('>');
                        }
                     }
                  }
               }
            }
            _prebuiltAssociations = assocBuffer.toString();
         }
         return _prebuiltAssociations;
      }

      public String getOperationsString() {
         StringBuffer results = new StringBuffer();
         results.append('[');
         boolean addComma = false;
         for (int i=0 ; i<_operationIDsList.size() ; i++) {
            if (addComma) {
               results.append(", ");
            }
            results.append(_operationIDsList.get(i));
            results.append(':');
            results.append(_operationNamesList.get(i));
            addComma = true;
         }
         results.append(']');
         return results.toString();
      }
   }

   private volatile int _nextOperationID;
   private final Hashtable<Thread, DiagnosticAssociations>    _associationsTable;

   public void beginUnNamedOperation()
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      String operationID = "" + _nextOperationID++;
      diagnosticAssociations.beginOperation(operationID, null/*operationName*/, false/*printDiagnostic*/);
   }

   public void terminateUnNamedOperation(boolean outputOperationEndedMessage)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      diagnosticAssociations.terminateOperation(null/*operationName*/, outputOperationEndedMessage);
   }

   public void beginOperation(String operationName)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      String operationID = "" + _nextOperationID++;
      diagnosticAssociations.beginOperation(operationID, operationName, true/*printDiagnostic*/);
   }

   public void terminateOperation(String operationName)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      diagnosticAssociations.terminateOperation(operationName, true/*outputOperationEndedMessage*/);
   }

   public Object getCurrentAssociations()
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      return diagnosticAssociations.clone();
   }

   public void restorePreviousAssociations(Object prevAssociationsObj)
   {
      /*DiagnosticAssociations previousAssoc;
      previousAssoc = */
      setCurrentAssociations(prevAssociationsObj, false/*preserveExistingOperations*/);
   }

   public Object setCurrentAssociations(Object newAssociationsObj, boolean preserveExistingOperations)
   {
      DiagnosticAssociations newAssociations = null;
      if ((newAssociationsObj != null) && (newAssociationsObj instanceof DiagnosticAssociations))
      {
         newAssociations = (DiagnosticAssociations)newAssociationsObj;
      }

      DiagnosticAssociations previousAssociations = setAssociationsForThisThread(newAssociations);
      // newAssociations is now the current Associations for this thread. Any modifications
      // to that object, will be remembered along with this thread.

      if ((preserveExistingOperations) && (newAssociations != null)) {
         // Keep the existing Operation associations.
         newAssociations.appendOperations(previousAssociations);
      }
      return previousAssociations;
   }

   public void associateItem(String itemName, String itemValue)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      diagnosticAssociations.associateItem(itemName, itemValue);
   }

   public void unassociateItem(String itemName, String itemValue)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      diagnosticAssociations.unassociateItem(itemName, itemValue);
   }

   private DiagnosticAssociations getAssociationsForThisThread() {
      return getAssociationsForThread(Thread.currentThread());
   }

   private DiagnosticAssociations getAssociationsForThread(Thread thread) {
      // It is always best if the threads that use this mechanism
      // are derived from TrackableThread, so that we can use
      // the Object contained in the Thread object itself for
      // the Associations. If they do not derive from that class,
      // then we must create our our Object for that thread,
      // and store that Object in our own Hashtable.
      // The problem with this is that it is difficult to clean up
      // our own Objects when a threads reference count goes to zero
      if (thread instanceof TrackableThread) {
         TrackableThread trackableThread = (TrackableThread) thread;
         if (trackableThread._diagnosticAssociations == null) {
            trackableThread._diagnosticAssociations = new DiagnosticAssociations();
         }
         return (DiagnosticAssociations)(trackableThread._diagnosticAssociations);
      }

      DiagnosticAssociations diagnosticAssociations = _associationsTable.get(thread);
      if (diagnosticAssociations == null) {
         diagnosticAssociations = new DiagnosticAssociations();
         _associationsTable.put(thread, diagnosticAssociations);
      }
      return diagnosticAssociations;
   }

   private DiagnosticAssociations setAssociationsForThisThread(DiagnosticAssociations newAssociations) {
      // It is always best if the threads that use this mechanism
      // are derived from TrackableThread, so that we can use
      // the Object contained in the Thread object itself for
      // the Associations. If they do not derive from that class,
      // then we must create our our Object for that thread,
      // and store that Object in our own Hashtable.
      // The problem with this is that it is difficult to clean up
      // our own Objects when a threads reference count goes to zero
      DiagnosticAssociations previousAssociations;
      Thread thisThread = Thread.currentThread();
      if (thisThread instanceof TrackableThread) {
         TrackableThread thisTrackableThread = (TrackableThread) thisThread;
         previousAssociations = (DiagnosticAssociations)(thisTrackableThread._diagnosticAssociations);

         thisTrackableThread._diagnosticAssociations = newAssociations;
      }
      else {
         previousAssociations = _associationsTable.get(thisThread);
         _associationsTable.put(thisThread, newAssociations);
      }

      if (previousAssociations == null) {
         previousAssociations = new DiagnosticAssociations();
      }
      return previousAssociations;
   }

   /*
   public void removeAssociationsForThisThread() {
      Thread thisThread = Thread.currentThread();
      if (thisThread instanceof TrackableThread) {
         TrackableThread thisTrackableThread = (TrackableThread) thisThread;
         thisTrackableThread._diagnosticAssociations = new DiagnosticAssociations();
         return;
      }

      _associationsTable.remove(thisThread);
   }
   */

   public String getAssociationsStringForThisThread()
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThisThread();
      return diagnosticAssociations.getAssociationsString();
   }

   public String getAssociationsStringForThread(Thread thread)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThread(thread);
      return diagnosticAssociations.getAssociationsString();
   }

   public String getOperationsStringForThread(Thread thread)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThread(thread);
      return diagnosticAssociations.getOperationsString();
   }

   public String getFirstOperationIDsForThread(Thread thread)
   {
      DiagnosticAssociations diagnosticAssociations = getAssociationsForThread(thread);
      return diagnosticAssociations._operationIDsList.elementAt(0);
   }

   /**
    * Ends diagnostic session. This terminates the normal message processing thread,
    * and the thread that called this method empties and displays the messages. Once
    * the message is empty, and all messages have been displayed, the method returns.
    * IMPORTANT:  This method has to be invoked for programs to terminate gracefully.
    */
   public void endDiagnostics() {
      Vector<DiagMsg> messageWaiting = new Vector<>();
      DiagMsg message = null;
      // Lock the message queue so no new messages will be accepted
      synchronized (_queue) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_queue._lockMessageQueue)) {

            _myThread = null;
            _endDiags = true;
            _queue.closeQueue();

            // flush any remaining messages out of the queue.
            while(_queue.getSize() > 0) {
               // Retrieve message from queue.
               message = _queue.getMessage();
               messageWaiting.add(message);
               // set this to null to help garbage collection reclaim the object more quickly
               message = null;
            }
         }
         // wake up any thread that is waiting for an object to enter the queue.
         _queue.notifyAll();
      }
      // display the message outside of the synchronized block to avoid a deadlock condition.
      while (!messageWaiting.isEmpty()){
         message = messageWaiting.remove(0);
         displayMessage(message);
         // set this to null to help garbage collection reclaim the object more quickly
         message = null;
      }
   }

   /**
    * Resets diagnostic properties. This is called any time 'resynch properties,diag' or
    * 'resynch properties,all' is entered on the command line.
    */
   public void resetDiagnostics(String propertyFile, DiagParameters diagParams, boolean startNewFile) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockThis)) {

            endDiagnostics();

            _propertyFile  = propertyFile;
            _params = diagParams;

            beginDiagnostics(true, startNewFile);

         }
      }
   }

   /**
    * Logs the message and exception.
    *
    * @param type     the type of message to log
    * @param message  the message to log
    * @param e        the exception to log
    */
   public void logMessage(String type, String message, Exception ex) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockThis)) {

            DiagMsg mesg = new DiagMsg(type, message,
                                       StringUtils.getTimeStamp(),
                                       Thread.currentThread().getName(), ex);
            _queue.addMessage(mesg);
            // set this to null to help garbage collection reclaim the object more quickly
            mesg = null;
         }
      }
   }

   /**
    * Logs the message.
    *
    * @param type     the type of message to log
    * @param message  the message to log
    */
   public void logMessage(String type, String message) {
      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockThis)) {

            DiagMsg mesg = new DiagMsg(type, message,
                                       StringUtils.getTimeStamp(),
                                       Thread.currentThread().getName(), null);

            _queue.addMessage(mesg);
            // set this to null to help garbage collection reclaim the object more quickly
            mesg = null;
         }
      }
   }

   /**
    * Logs the message.
    *
    * @param type     the type of message to log
    * @param message  the message to log
    */
   public void logMessage(int type, String message) {
      // If we are not suppose to write Info messages to either
      // the console or the log file, then we ignore this message.
      if (_logInfoToFile || _logInfoToConsole ) {
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockThis)) {

               DiagMsg mesg = new DiagMsg(TYPE_INFO_STRING, message,
                                          StringUtils.getTimeStamp(),
                                          Thread.currentThread().getName(), null);
               _queue.addMessage(mesg);
               // set this to null to help garbage collection reclaim the object more quickly
               mesg = null;

            }
         }
      }
   }

   /**
    * Enable fast logging.
    * @param enable  Enable or disable fast logging
    */
   public void enableFastLog(boolean enable)
   {
      _fastDiag = enable;
   }

   /**
    * Is fast logging enabled.
    * @return true    if fast logging is enabled
    *         false   otherwise
    */
   public boolean isEnabledFastLog()
   {
      return _fastDiag;
   }

   // This member is used to build the strings before they are placed into the log file,
   // so that we can write to the file as a single operation, which should reduce the
   // chance that a read on that file by an external app will read an incomplete file.
   private StringBuffer _completeDataString;
   private StringBuffer _emptyDataString;
   boolean _fileOverwrite = false;
   long _existingFileLength;

   /**
    * Logs the message to file.
    *
    * @param message  the message to log to file
    */
   private void logToFile(String messageAsString) {

      // If this message would make the log file too big, advance to the next file.
     if (_availableFileSizeRemaining < messageAsString.length()) {
       try {
      	if (_emptyDataString == null) {
            _emptyDataString = new StringBuffer();
         }
         else {
         	_emptyDataString.setLength(0);
         }
      	if(_availableFileSizeRemaining > 0){
            _emptyDataString.append(StringUtils.getSpaces((int)_availableFileSizeRemaining));
            _availableFileSizeRemaining = 0;
      	}
        //Fill the _availableFileSizeRemaining area with blank spaces.
        //This is remove any XML comments at the end of the file
        //If we have XML comments at the end, the next cycle of
        //overwriting the file would have errors in placing the XML comment tags.
      	if((_fileOverwrite==true) && (_file.length() <= _existingFileLength)){
      		_file.seek(_file.getFilePointer()-START_COMMENT_DATA.length());
      		_file.writeBytes(_emptyDataString.toString());
      	}
      	advanceFileNumber(false/*deleteFileBeforeUsing*/);//Pass the flag as false so that it does not delete the log file.
      	//if the file size is greater than the max file size allowed,
      	//then set the file size to the correct length
      	if(_file.length()>(_params.maxLogFileSize * (long)1000)) {
            _file.setLength((_params.maxLogFileSize * (long)1000));
         }
         if(_file.length()!=0){//This will tell if the file we are writing into is an existing file or a new file.
         		_file.seek(_file.length()-STOP_COMMENT_DATA.length());//Point to the existing "closing XML comment". We overwrite the closing XML comment because on multiple cycles, the closing XML comments should not keep adding to the file. This would be syntactically wrong.
         		_fileOverwrite=true;//If the file we writing into is an existing file then set this flag so that we can comment the previous cycles data
         		_file.writeBytes(STOP_COMMENT_DATA);//write the end comment XML sysntax into the log file at the end of the existing file
         		_existingFileLength = _file.length();//get the file's(file that is being overwritten) length
              	_file.seek(0);//Irrespective of whether the file is existing or new file set the file pointer to the begenning of the file.
             	_availableFileSizeRemaining = _params.maxLogFileSize * (long)1000;//Since this is a new file or a existing file that will completely be overwritten, set the available size to the maximum specified.
         	}
         	else {
         		_fileOverwrite=false;
         		_existingFileLength=0;
         	}
         }
         catch(IOException ex) {
            System.err.println ("Diagnostics: logToFile: Error proceeding to a new file " + _filePath);
            System.out.println (ex.getMessage());
         }
      }

      //if (_file == null)
        // setFilePath(_filePath, false);

      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockThis)) {

            try {
               // Write all the data as a single String, so that if anyone tries to
               // access the file while we are writting it, they will always see a
               // valid XML file.
               if (_completeDataString == null) {
                  _completeDataString = new StringBuffer();
               }
               else {
                  _completeDataString.setLength(0);
               }
               if (_file.length() == 0) {
                  _completeDataString.append(FILE_HEADER); // write the file header to this new file.
               }
               else if (_availableFileSizeRemaining == (_params.maxLogFileSize * (long)1000)){
                   // Set the write position so that we over-write the
                   // entire existing file with the newest message except for the header part.
                   // Later we will re-write the footer at the new EOF.
               	_file.seek(FILE_HEADER.length());//
               	_availableFileSizeRemaining -= FILE_HEADER.length();
               }
               else {
                  // Set the write position so that we over-write the
                  // existing file footer with the newest message.
                  // Later we will re-write the footer at the new EOF.
                  //_file.seek(_file.length() - _fileFooter.length());
               	_file.seek((_params.maxLogFileSize * (long)1000)-_availableFileSizeRemaining);
               }

               _completeDataString.append(messageAsString); // the message itself.
               _completeDataString.append(lineSeparator);   // write a CR-LF as defined for this system.
               // update the remaining file size.
               _availableFileSizeRemaining -= _completeDataString.length();
               _completeDataString.append(FILE_FOOTER);     // re-write the file footer

               //Comment the lines from the previous cycle if
               //the file we are writing is an existing file that is being over written.
               if (_fileOverwrite) {
               	//This check is important because if the current file length is greater than
               	//the file that was originally chosen to be overwritten,
               	//that means that the end comment data is gone(already has been overwritten).
               	//So in this case we should not add the starting comment line.
               	//To see what can happen, comment th below check(if condition) and increase the
               	//max log file size in the servicesserver.properties
               	if((((_params.maxLogFileSize * (long)1000)-_availableFileSizeRemaining)+FILE_FOOTER.length())<_existingFileLength) {
                     if((((_params.maxLogFileSize * (long)1000)-_availableFileSizeRemaining)+FILE_FOOTER.length()+START_COMMENT_DATA.length())<(_existingFileLength-STOP_COMMENT_DATA.length())) {
                        _completeDataString.append(START_COMMENT_DATA);
                     }
                     else if((((_params.maxLogFileSize * (long)1000)-_availableFileSizeRemaining)+FILE_FOOTER.length()+START_COMMENT_DATA.length())>(_existingFileLength-STOP_COMMENT_DATA.length())) {
                        _completeDataString.append("   ");//add empty spaces to fill up the space.
                     }
                     else {
                        _completeDataString.append(START_COMMENT_DATA).append(STOP_COMMENT_DATA);
                     }
                  }
               }
               // Append log messages to log file, with a CR-LF and the re-positioned footer
               _file.writeBytes(_completeDataString.toString());    // print message
            }
            catch(IOException ex) {
               System.err.println ("Diagnostics: logToFile: Error writing to file " + _filePath);
               System.out.println (ex.getMessage());
            }

            if (!_fastDiag)
            {
               RandomAccessFile raf = _file;
               _file = null;
               try {
                  raf.close();
                  raf = null;
               }
               catch (IOException ex) {
                  System.err.println ("Diagnostics: logToFile: Error closing file " + _filePath);
                  System.out.println (ex.getMessage());
               }
            }
         }
      }
   }

   /**
    * Obtain a log file name from the baseLogFile and indexLogFile.
    * @param baseLogFile
    * @param indexLogFile
    * @return log file name
    */
   private static String getLogFile(String baseLogFile, int indexLogFile) {
      DecimalFormat formatter = new DecimalFormat("0000");
      return baseLogFile + formatter.format(indexLogFile) + ".log";
   }

   /**
    * Initializes private variables to their appropriate values, _fileNumber
    * and _messageNumber.
    */
   private void initLog(boolean startNewFile) {
      File dir = new File(_params.logFileDir);

      // if directory NOT exist, create new directory
      if (!dir.isDirectory()) {
         dir.mkdir();
      }

      // Search for most recent logFile
      _fileNumber           = 1;
      String  fileName      = null;
      File    bestFileSoFar = null;
      boolean fileExists    = true;
      boolean useThisFile;

      File tempFile;
      // As soon as we find a file that doesn't exist, use it
      for (int i = 1; ((i <= _params.maxLogFiles) && fileExists); i++) {
         fileExists = false;
         fileName = getLogFile(_params.logFileBase, i);

         tempFile = new File(_params.logFileDir, fileName);
         try {
            fileExists = tempFile.exists();
         }
         catch (SecurityException ex) {
            System.err.println ("Security Exception occured while accessing " + tempFile.getAbsolutePath());
            System.out.println (ex.getMessage());
         }
         // We will use this file in three case:
         // 1) The file doesn't exits.
         // 2) We haven't selected any file yet.
         // 3) This file is old than the current oldest file.
         useThisFile = (!fileExists ||
                        (bestFileSoFar == null) ||
                        (tempFile.lastModified() > bestFileSoFar.lastModified()));

         if (useThisFile) {
            bestFileSoFar = tempFile;
            _fileNumber = i;
         }
      }

      _messageNumber = 1;  // set message to first message

      // If the file exists, advance to the next file,
      // which should be the oldest file.
      if (fileExists) {
         advanceFileNumber(startNewFile/*deleteFileBeforeUsing*/);
      }
      else {
         if (bestFileSoFar != null) {
            setFilePath(bestFileSoFar.getAbsolutePath(), startNewFile);
         }
      }
   }

   /**
    * Updates the file handle to point to the next file. The log file length
    * has to be less than equal to _params.maxLogFiles (in KBs) and the filename
    * is within _params.logFileBase1 to _params.logFileBase{maxLogFiles}  (inclusive).
    */
   private void advanceFileNumber(boolean deleteFileBeforeUsing) {
      // Start logging to the next log file
      _fileNumber                 = (_fileNumber % _params.maxLogFiles) + 1;
      String newFilePath = _params.logFileDir + "/" + getLogFile(_params.logFileBase, _fileNumber);

      setFilePath(newFilePath, deleteFileBeforeUsing);
   }

   private boolean setFilePath(String newPathName, boolean deleteFileBeforeUsing) {
      // Overwrite existing LogFile by deleting the old instance of it.
      File file = new File(newPathName);
      RandomAccessFile raf = null;
      long fileLength = 0;
      try {
         // If the the file exists, delete it or set our file length equal to its length.
         if (file.exists())
         {
            if (deleteFileBeforeUsing) {
               file.delete();
            }
            else {
               fileLength = file.length();
            }
         }
         raf = new RandomAccessFile(file, "rw");
         raf.seek(fileLength);
      }
      catch (SecurityException ex) {
         System.err.println ("Security Exception occured while accessing " + newPathName);
         System.out.println (ex.getMessage());
         return false;
      }
      catch (FileNotFoundException ex) {
         System.err.println ("Diagnostics: setFilePath: Error opening or creating file " + newPathName);
         System.out.println (ex.getMessage());
         return false;
      }
      catch (IOException ex) {
         System.err.println ("Diagnostics: setFilePath: Error seeking to the end of " + newPathName);
         System.out.println (ex.getMessage());
         return false;
      }

      try {
         if (_file != null)
         {
            _file.close();
            _file = null;
         }
      }
      catch (IOException ex) {
         System.err.println ("Diagnostics: setFilePath: Error closing " + _filePath);
         System.out.println (ex.getMessage());
      }

      synchronized (this) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockThis)) {

            _filePath  = newPathName;
            _file = raf;
            // Compute how much data we can write to it before we call it full.
            _availableFileSizeRemaining = _params.maxLogFileSize * (long)1000;
            // subtract the length of the file.
            _availableFileSizeRemaining -= fileLength;
         }
      }

      return true;
   }

   /**
    * Returns a formatted message as defined by MESSAGE_FORMAT.
    * @param type    the type of message
    * @param source  the source of this message
    * @param message the message to log
    * @return formatted log message
    */
   private static String formatMessageForFile(String msgNumber, DiagMsg msg) {

      if (msg._exception != null) {
         Object[] formatArgs = { msgNumber,                              // {0}
                                 msg._timeStamp,                         // {1}
                                 msg._type,                              // {2}
                                 msg._threadName,                        // {3}
                                 msg._message,                           // {4}
                                 msg._associations,                      // {5}
                                 msg._exception,                         // {6}
                                 StringUtils.getCallStack(msg._exception)// {7}
                               };
         return MSG_FORMAT_EXCEPTIONAL.format(formatArgs);
      }
      Object[] formatArgs = { msgNumber,                              // {0}
                              msg._timeStamp,                         // {1}
                              msg._type,                              // {2}
                              msg._threadName,                        // {3}
                              msg._message,                           // {4}
                              msg._associations                       // {5}
                            };
      return MSG_FORMAT_NORMAL.format(formatArgs);
   }

   private static String formatMessageForConsole(DiagMsg message) {
      //   4 characters       Type
      //   1 character        space
      //  21 characters       TimeStamp
      // + 1 character        space
      //  ---------------
      //  27 characters

      //  27 characters
      //  15 characters (max) ThreadName
      // + 1 character        space
      //  ---------------
      //  43 characters (max)
      StringBuffer messageBuffer = new StringBuffer(50);
      messageBuffer.append(message._type).append(' ');
      messageBuffer.append(message._timeStamp).append(' ');
      messageBuffer.append(message._threadName);
      int blankSpacesNeeded = 50 - messageBuffer.length();
      if (blankSpacesNeeded > 0) {
         messageBuffer.append(StringUtils.getSpaces(blankSpacesNeeded));
      }
      else {
         messageBuffer.append(' ');
      }
      messageBuffer.append(message._message);
      return messageBuffer.toString();
   }

   /**
    * Returns current message number as specified by DECIMAL_FORMAT.
    * @return current message number
    */
   private String getMessageNumber() {
      return DEC_FORMAT.format(_messageNumber++);
   }


   void displayMessage(DiagMsg message)
   {
      if (message != null) {
         if (message._type == TYPE_SYSTEM_OUT) {   // System Out
            System.out.print(message._message);
         }
         else {   // Formatted Message
            String msgNumber = getMessageNumber();
            String messageAsString;
            boolean manditory = ((message._type != TYPE_INFO_STRING) &&
                                 (message._type != TYPE_OPERATIONS)  &&
                                 (message._type != TYPE_STATISTICAL) &&
                                 (message._type != TYPE_MONITORS));
            if ( manditory || _logInfoToConsole) {
               messageAsString = formatMessageForConsole(message);
               System.out.println(messageAsString);
               if (message._exception != null) {
                  message._exception.printStackTrace();
               }
            }
            if ( manditory || _logInfoToFile) {
               messageAsString = formatMessageForFile(msgNumber, message);
               logToFile(messageAsString);
               // Check to see if this is a message that should be written
               // to the Debug File (currently this is Execptions.log file.)
               if (message._type == TYPE_DEBUG_TRAP) {
                  Vector<String> errors = new Vector<>();
                  errors.add(message._timeStamp);
                  //errors.add(StringUtils.getTimeStamp(true/*includeMilliseconds*/));
                  writeStringsToExceptionsFile(errors, message._message, message._threadName);
               }
            }
         }
      }
   }

   void writeStringsToExceptionsFile(Vector<String> messageLines, String message, String threadName)
   {
      // create the file, if it doesn't already exist
      File file = new File(_params.logFileDir, "Exceptions.log");

      // This String is used in case we have a IOExecption, it will tell us
      // what operation was being processed (open, write, or close)
      String ioOperation = "opening";
      // Append message to log file
      try (FileWriter fw = new FileWriter(file.getAbsolutePath(), true)) {
         ioOperation = "writing to";
         try {
            fw.write(lineSeparator);
            fw.write(threadName);
            fw.write(lineSeparator);
            fw.write(message);
            fw.write(lineSeparator);
            while (!messageLines.isEmpty()) {
               fw.write(messageLines.remove(0));
               fw.write(lineSeparator);
            }
            ioOperation = "closing";
         }
         catch (Exception ex) {
            try (PrintWriter pw = new PrintWriter(fw)) {
               ex.printStackTrace(pw);
            }
            fw.write(lineSeparator);
         }
      }
      catch(IOException ex) {
         System.err.println ("writeStringsToExceptionsFile: while " + ioOperation + " file " + file);
         System.err.println (ex.getMessage());
      }
   }

   /**
    * Method resumeOutput.
    * This method resumes the output of the diagnostics after a prior pause command
    */
   public void resumeOutput() {
      _myThread.resumeOutput();
   }

   /**
    * Method pauseOutput.
    * This method suspends the diagnostic output for the number of seconds in the parameter.
    * @param timeToPause
    */
   public void pauseOutput(int timeToPause) {
      _myThread.pauseOutput(timeToPause);
   }

   /**
    * IOThread simulates a low priority thread that logs the messages to file.
    *
    */
   private class IOThread extends TrackableThread {
      int _pauseTimer;
      /**
       * Creates an instance of this class.
       * @param name  assigns a name to this thread
       */
      public IOThread(String name) {
         super(name);
         super.setPriority(IOTHREAD_PRIORITY);
      }

      /**
       * Task perform by this thread.
       * While exist message in the queue, this thread will logs the message.
       */
      @Override
      public void run() {
         DiagMsg message = null;
         _pauseTimer = 0;
         while(!_endDiags || (_queue.getSize() > 0)) {
            // Retrieve message from queue.
            message = _queue.getMessage();
            while (_pauseTimer != 0) {
               try {
                  sleep(_pauseTimer);
                  _pauseTimer = 0;
               }
               catch (InterruptedException ex) {
                  // If we are interrupted, our pause timer
                  // may have been re-set to another value
                  // so we must continue in the while loop.
               }
            }
            displayMessage(message);
            // set this to null to help garbage collection reclaim the object more quickly
            message = null;
         }
      }
      /**
       * Method resumeOutput.
       */
      public void resumeOutput() {
         if (_pauseTimer != 0) {
            _pauseTimer = 0;
            interrupt();
         }
      }

      /**
       * Method pauseOutput.
       * @param timeToPause
       */
      public void pauseOutput(int timeToPause) {
         boolean alreadyPaused = (_pauseTimer != 0);
         _pauseTimer = timeToPause * 1000; // how may milliseconds should we wait?
         if (alreadyPaused) {
            // If we are already were paused, then we need to interrupt the current
            // wait, so that we are now waiting for the new
            interrupt();
         }
      }
   }

   /**
    * MessageQueue contains Diagnostic Messages.
    *
    */
   private class MessageQueue {
      private final Vector<DiagMsg> _messages;
      private boolean _stop = false;
      private final int _messageQueueMaxSize;

      final Semaphore _lockMessageQueue;
      /**
       * Creates an instance of this class.
       * Default constructor.
       */
      public MessageQueue(int messageQueueMaxSize) {
         _messages = new Vector<>();
         _messageQueueMaxSize = messageQueueMaxSize;
         _stop = false;
         _lockMessageQueue = new Semaphore("Diagnostics.MessageQueue", Semaphore.CLASS_MESSAGEQUEUE);
      }

      /**
       * Close the current queue.
       */
      public void closeQueue() {
         // notify & wait operation must occur within synchronized blocks
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockMessageQueue)) {

               _stop = true;
               notifyAll();

            }
         }
      }

      /**
       * Returns current queue size.
       * @return current queue size
       */
      public int getSize() {
         return _messages.size();
      }

      /**
       * Adds a message to the message queue.
       * @param msg  the message to add to the message queue.
       */
      public boolean addMessage(DiagMsg msg) {
         boolean messageAdded = false;
         // notify & wait operation must occur within synchronized blocks
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockMessageQueue)) {

               // Don't accept new messages if the queue has been stopped
               if (!_stop) {
                  // if there are too many entries waiting to be processed,
                  // stop accepting new diags.
                  if ((_messageQueueMaxSize > 0) && (_messages.size() > _messageQueueMaxSize)) {
                     closeQueue();
                     // Send out a message to all OhMyGod admins.
                     String errorMessage = "Message queue threshold of " + _messageQueueMaxSize +
                                           " exceeded! Diagnostic logging has been turned off." +
                                           " Try typing \"resynch properties,diag\" on the command line to restart it." +
                                           " If that doesn't help, then the server should be re-started to restore logging.";

                     // Since we are about the throw away the current message, and we need to
                     // create a new one to log this error, just canibalize the current one.
                     msg._type = TYPE_SEVERE_ERROR;
                     msg._message = errorMessage;

                     // Alter the first diag message, so we can see what was going on -
                     // assuming this diag makes it to the log file!
                     DiagMsg firstMsg = _messages.firstElement();
                     firstMsg._message = "first Message in queue when threshold of " + _messageQueueMaxSize +
                                         " exceeded ; " + firstMsg._type + " : " + firstMsg._message;
                     firstMsg._type = TYPE_SEVERE_ERROR;
                  }
                  _messages.addElement(msg);
                  messageAdded = true;
                  notify();
               }
            }
         }
         return messageAdded;
      }

      /**
       * Returns the first message from the message queue.
       * @return the first message from the message queue, null if
       *         message queue is empty.
       */
      public DiagMsg getMessage() {
         DiagMsg msg = null;
         // notify & wait operation must occur within synchronized blocks
         synchronized (this) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lockMessageQueue)) {

               while (_messages.isEmpty() && !_stop) {
                  try (SemaphoreAutoUntracker sau = new SemaphoreAutoUntracker(_lockMessageQueue)) {
                     wait();
                  }
                  catch (InterruptedException ex) {
                     System.out.println("Interrupted Exception");
                  }
               }

               if (!_messages.isEmpty()) {
                  msg = _messages.remove(0);
               }
            }
         }
         return msg;
      }
   }
}

