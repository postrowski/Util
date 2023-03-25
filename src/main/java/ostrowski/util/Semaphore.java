package ostrowski.util;

// Java Imports
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class Semaphore {

   // Don't allow access to lockCount, because it must always be used
   // in an atomic operation to lock the object. Use tryLock() if you
   // want to fail (throw exception) if the call to lock() would block.
   // lockCount tells us how many times this Semaphore is locked by the
   // current thread. When this is zero, the Semaphore is unlocked.
   protected int lockCount;

   // This can be used to see how many threads are waiting for this lock
   public int waitCount;

   // The Semaphore object can also be used to track 'synchronize' blocks
   // without actually locking anything itself. When we use it in this manner,
   // we need to keep track of how many times 'track' and 'untrack' have
   // been called.
   public int trackCount;

   // This lets us identify which Thread owns this Semaphore
   public Thread ownerThread;

   // This Hashtable provides us with a mapping between the current thread
   // and a Vector that contains all the Semaphore objects that the thread
   // has locked.
   private static final Hashtable<Thread, Vector<Semaphore>> THREAD_HASH = new Hashtable<>(51); // assume about 50 threads may run in the server

   // This variable gives us a name we can display in the error cases, making
   // it easier to locate the violations
   public final String name;

   Semaphore peerLockAuthorityParent;

   // This member is used to keep track of the data output stream that this
   // Semaphore is protecting. We keep track of this, so we can use this
   // Semaphore object to close the socket if this blocking.
   final LockableDataOutputStream lockableDataOutputStream;

   // This is used to detect potential deadlocks. When multiple Semaphores
   // are locked, they MUST be locked in a descending order. Therefore, if
   // you lock a Semaphore that has an order of 4, and then while you still
   // have that object locked, you try to lock a Semaphore that has an order
   // of 4 or higher, you risk creating a deadlock condition. You also must
   // release Semaphores in the reverse order that they are locked in.
   // If a Semaphore is a simple lock object, meaning that no other objects
   // need to be locked while this Semaphore lock is maintained, then that
   // Semaphore should have an order of 1. If you have a Semaphore that must
   // lock simple Semaphores (order 1), then the Semaphore should have an
   // order of 2. Similarly, if another Semaphore need to lock this order 2
   // Semaphore, it should have an order of 3.
   public final int order;
   // To assign an order for a given Semaphore, you should
   // use one of the constants defined here (or define a new one here):

// order 1:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_MONITOREDOBJECT_watchers            = DependsOn(0);
   public static final int CLASS_MONITORINGOBJECT_watchedObjects     = DependsOn(0);
   public static final int CLASS_DIAGNOSTICS                         = DependsOn(0);
   public static final int CLASS_MESSAGEQUEUE                        = DependsOn(0);
   public static final int CLASS_LOCKABLEDATAOUTPUTSTREAM            = DependsOn(0);
   public static final int CLASS_SYNCHREQUEST                        = DependsOn(0);
   public static final int CLASS_SOCKETCONNECTOR                     = DependsOn(0);





   // These static Methods should be used to calculate the order of a Semaphore,
   // based upon what other Semaphore objects may be locked while the lock is maintained.
   static int DependsOn(int d1)                                         { return d1+1;}
   static int DependsOn(int d1, int d2)                                 { return Math.max(d1, d2)+1;}
   static int DependsOn(int d1, int d2, int d3)                         { return Math.max(Math.max(d1, d2), d3)+1;}
   static int DependsOn(int d1, int d2, int d3, int d4)                 { return Math.max(Math.max(Math.max(d1, d2), d3), d4)+1;}
   static int DependsOn(int d1, int d2, int d3, int d4, int d5)         { return Math.max(Math.max(Math.max(Math.max(d1, d2), d3), d4), d5)+1;}
   //static int DependsOn(int d1, int d2, int d3, int d4, int d5, int d6) { return Math.max(Math.max(Math.max(Math.max(Math.max(d1, d2), d3), d4), d5), d6)+1;}
   //static int DependsOn(int d1, int d2, int d3, int d4, int d5, int d6, int d7) { return Math.max(Math.max(Math.max(Math.max(Math.max(Math.max(d1, d2), d3), d4), d5), d6), d7)+1;}

   public Semaphore(String name, int order) {
      this.name = name + "[" + order + "]";
      this.order = order;
      lockCount = 0;
      waitCount = 0;
      trackCount = 0;
      peerLockAuthorityParent = null;
      lockableDataOutputStream = null;
   }

   public Semaphore(String name, int order, LockableDataOutputStream lockableDataOutStream) {
      this.name = name + "[" + order + "]";
      this.order = order;
      lockCount = 0;
      waitCount = 0;
      trackCount = 0;
      peerLockAuthorityParent = null;
      lockableDataOutputStream = lockableDataOutStream;
   }

   @Override
   protected void finalize() {
      // make sure that this object was released before it is deleted
      if ((lockCount != 0) || (waitCount != 0))
      {
         String strReason = "Semaphore " + name;
         strReason += " being destroyed while " + lockCount;
         strReason += " locks are held by thread " + ownerThread.getName();

         ReportPossibleDeadlockCondition(strReason);
      }
   }

   /** Call this method when you want to allow the Semaphore object to be locked while another
    * object of the same order is already locked. It will not report a deadlock possibility as
    * long as the parentLock Semaphore is locked before either peer object is locked.
    * @param parentLock The Semaphore that must be locked to lock both objects at the same time
    */
   public void setPeerLockAuthorityParent(Semaphore parentLock) {
      peerLockAuthorityParent = parentLock;
   }

   public synchronized void lock() {
      // If we already own this Semaphore, don't try to re-lock it.
      if (ownerThread != null)
      {
         if (ownerThread.equals(Thread.currentThread()))
         {
            ++lockCount;
            return;
         }
      }

      ++waitCount;
      while (lockCount > 0) {
         // we must be notified not interrupted,
         // so catch and ignore interruptions.
         try {
            // during this wait(), other threads may
            // enter any synchronized functions
            wait();
            // If we get here, then notify was called,
            // so unlock() was called, we succeeded.
            break;
         } catch (InterruptedException e) {
            continue;    // no unlock() called yet
         }
      }
      // we have acquired (locked) this Semaphore
      --waitCount;
      ++lockCount;
      ownerThread = Thread.currentThread();
      track();
   }

   public synchronized boolean unlock() {
      // Make sure that the current thread really owns this Semaphore

      // If ownerThread is null, then this will throw a RuntimeException
      // (NullPointerException), which is the desired behaviour (see below)
      if (!ownerThread.equals(Thread.currentThread()))
      {
         // this is really bad if this occurs. It means that we don't own this
         // object, but we are trying to unlock it.
         // I throw a RuntimeException, because I don't want to have the
         // enforcement with the whole 'throws NotOwnerException' garbage in
         // the function definition, which would force the programmer to have
         // to have a try/catch block around every unlock method call.
         throw new IllegalStateException();
      }

      if (--lockCount == 0)
      {
         // If this was the last lock held by the current thread
         // on this Semaphore, then the Semaphore is now unlocked.
         untrack();

         // clear the ownerThread, and then let another thread lock this.
         ownerThread = null;

         // This wakes up one thread, which may be in the lock() function.
         // Since both the unlock() and the lock() are synchronized, the
         // lock() function thread wont continue until this unlock() function exits.
         notify();
      }
      return true;
   }

   public SemaphoreAutoTracker trackAuto() {
      return new SemaphoreAutoTracker(this);
   }

   public synchronized void track() {
      // If the tackCount is non-zero, then we are already tracking
      // this object, so we do nothing except increment trackCount
      if (++trackCount > 1) {
         return;
      }

      // Now we need to update the Hashtable that contains a Vector
      // for each thread that owns a Semaphore.
      Vector<Semaphore> threadsLockedSemaphores = THREAD_HASH.get(Thread.currentThread());
      if (threadsLockedSemaphores == null)
      {
         threadsLockedSemaphores = new Vector<>();
         // Now put the new Vector into the Hashtable.
         THREAD_HASH.put(Thread.currentThread(), threadsLockedSemaphores);
         // Since this is an empty Vector, there is no need to checkForOrderViolations.
      }
      else
      {
         checkForOrderViolation(threadsLockedSemaphores, true/*unlockLockedObject*/);
      }
      // Add this Semaphore to the end of the Vector. It should
      // be the first one removed (a First-In, Last-Out Queue).
      threadsLockedSemaphores.add(this);
   }

   public synchronized void untrack() {
      // If the tackCount is non-zero, then we are still tracking
      // this object, so we do nothing except decrement trackCount
      if (--trackCount > 0) {
         return;
      }

      // We need to update the Hashtable that contains a Vector
      // for each thread that owns a Semaphore.
      Vector<Semaphore> threadsLockedSemaphores = THREAD_HASH.get(Thread.currentThread());
      if (threadsLockedSemaphores != null)
      {
         // When multiple Semaphores are unlocked, they must be
         // unlocked in the reverse of the order in which they
         // were locked, or a deadlock condition may result.
         // Therefore, the Semaphore at the end of the Vector
         // should be ourselves (this Semaphore)
         boolean objectRemoveFromList = false;
         if (!threadsLockedSemaphores.isEmpty()) {
            Semaphore lastLockedSem = threadsLockedSemaphores.lastElement();
            if (lastLockedSem != null)
            {
               if (lastLockedSem.equals(this)) {
                  // remove the last element (which is 'this') from the Vector
                  threadsLockedSemaphores.removeElementAt(threadsLockedSemaphores.size()-1);
                  objectRemoveFromList = true;
               }
               else {
                  // DEADLOCK CONDITION POSSIBLE!!!!!
                  // Find where in this Vector this Semaphore object exists and remove it.
                  int index = threadsLockedSemaphores.lastIndexOf(this);
                  if (index != -1) {
                     String strReason  = "Objects not unlocked in the reverse of the lock order. ";
                            strReason += "Object " + name + " is being unlocked, ";
                            strReason += "while object " + lastLockedSem.name + " was most recently locked.";

                     ReportPossibleDeadlockCondition(strReason);
                     // remove the 'this' from the Vector
                     threadsLockedSemaphores.removeElementAt(index);
                     objectRemoveFromList = true;
                  }
               }
               // now put the modified Vector back into the Hashtable
               THREAD_HASH.put(Thread.currentThread(), threadsLockedSemaphores);
            }
         }
         if (!objectRemoveFromList)
         {
            if (trackCount >= 0) {
               // 'This' did not exist in our lock list, report the error and do nothing.
               ReportPossibleDeadlockCondition("Object " + name + " unlocked that was not previously locked.");
            }
            else {
               // If the track count was below zero, AND this object was not removed from
               // the list of locked object, then this object was likely forcibly removed when
               // it was found to be tracked while a higher order lock was trying to be locked.
               trackCount = 0;
            }
         }
      }
      if (trackCount < 0)
      {
         // If the track count is now LESS than zero, report the error
         ReportPossibleDeadlockCondition("Object " + name + " untracked more often than it was tracked.");
      }
   }

   // The check method combines track() and untrack() to verify that an
   // object lock could be acquired without causing a deadlock, but does
   // not actually modify the threadHash Hashtable (for efficiency).
   public synchronized void check() {
      // If the tackCount is non-zero, then we are already tracking
      // this object for real, so everything is cool, do nothing.
      if (trackCount > 0) {
         return;
      }

      // Now we need to update the Hashtable that contains a Vector
      // for each thread that owns a Semaphore.
      Vector<Semaphore> threadsLockedSemaphores = THREAD_HASH.get(Thread.currentThread());
      // If there is no other locks held by this thread, then we are
      // certain no deadlock could occur by the locking of this Semaphore
      if (threadsLockedSemaphores == null) {
         return;
      }

      checkForOrderViolation(threadsLockedSemaphores, false/*unlockLockedObject*/);
   }

   boolean checkForOrderViolation(Vector<Semaphore> threadsLockedSemaphores, boolean unlockLockedObject)
   {
      // If other Semaphores are owned by this thread, we need
      // to make sure that the order of those Semaphores are
      // greater than the order of this Semaphore. If this is
      // not true, then a deadlock could occur.
      // Because the first Semaphore locked is at the top of the
      // Vector, and the most recently locked Semaphore is at
      // the bottom of the Vector, and because the order of the
      // Semaphore must always be decreasing, the lowest order
      // Semaphore should be at the bottom of the Vector.
      // Thus, we only need to compare the order of the Semaphore
      // at the end of the Vector to the order of this Semaphore.
      if (!threadsLockedSemaphores.isEmpty())
      {
         Semaphore lowestLockedSem = threadsLockedSemaphores.lastElement();
         if (lowestLockedSem.order <= order)
         {
            boolean deadlockPossible = true;
            if (lowestLockedSem.order == order) {
               // If we are locking two items of the same order, and they
               // both have the same peer locking authority parent, and
               // we have that parent locked, then this is not a deadlock condition.
               if (peerLockAuthorityParent != null) {
                  if (lowestLockedSem.peerLockAuthorityParent == peerLockAuthorityParent) {
                     // Check that we have the parent lock locked.
                     if (threadsLockedSemaphores.contains(peerLockAuthorityParent)) {
                        deadlockPossible = false;
                     }
                  }
               }
            }
            if (deadlockPossible) {
               // DEADLOCK CONDITION POSSIBLE!!!!!
               ReportPossibleDeadlockCondition("order violation: object " + lowestLockedSem.name +
                                               " locked, while attempting to lock object " + name + "." +
                                               (unlockLockedObject ? (lineSeparator + "Object will be unlocked manually.") : ""));
               if (unlockLockedObject) {
                  // Since we have already reported this error condition once, we
                  // untrack the offending Semaphore, since it may be that an Exception
                  // was thrown and not caught, which skipped the untrack() method.
                  // If this condition exists, then the lowestLockedSemaphore may
                  // produce hundreds of false deadlock conditions.
                  lowestLockedSem.untrack();
               }
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public synchronized String toString() {
      return "Name [order] = " + name + ", lock count = " + lockCount + ", wait count = " + waitCount + ", track count = " + trackCount;
   }

   public synchronized boolean tryLock() {
      if ((lockCount > 0) && (!ownerThread.equals(Thread.currentThread()))) {
         return false;
      }
      this.lock();
      return true;
   }

   static private final Vector<String> reportedViolations = new Vector<>();
   private void ReportPossibleDeadlockCondition(String strReason)
   {
      // Put the call stack into a String so we can check to make sure
      // that this deadlock condition hasn't already been reported.
      String callStack = getCallStack(new Exception());

      String prevViolation;
      // Now check it against our list of reported violations
      for (int i=0 ; i<reportedViolations.size() ; i++) {
         prevViolation = reportedViolations.elementAt(i);
         // If we find a match, don't report it again, just return (do nothing)
         if (prevViolation.equals(callStack)) {
            return;
         }
      }
      // if we couldn't find it in the list of reported Violations, add it now
      reportedViolations.add(callStack);

      // create the message that we will print in the log file
      String message = "Possible Deadlock condition found:";
      String time = StringUtils.getTimeStamp();
      System.err.println(time);
      System.err.println(message); // print message to console
      System.err.println("   "+strReason); // print message to console

      Vector<String> errors = new Vector<>();
      errors.add(time);           // print the timestamp
      errors.add(message);        // print message to file
      errors.add(strReason);      // print the reason
      errors.add(toString());     // output the object itself
      errors.add(callStack);      // now print the call stack.
      writeStringsToDeadlockFile(errors);
   }

   // This is a system-depended CR-LF that we should use any time we
   // want to output a newline character.
   public static final String lineSeparator = "\n";//(String) java.security.AccessController.doPrivileged(
                                             //    new sun.security.action.GetPropertyAction("line.separator"));

   private static void writeStringsToDeadlockFile(Vector<String> messageLines)
   {
      // create the file, if it doesn't already exist
      File file = new File("logs", "Deadlocks.log");

      // This String is used in case we have a IOException, it will tell us
      // what operation was being processed (open, write, or close)
      String ioOperation = "opening";
      // Append message to log file
      try (FileWriter fw = new FileWriter(file.getAbsolutePath(), true))
      {
         ioOperation = "writing to";
         try {
            fw.write(lineSeparator);
            while (!messageLines.isEmpty()) {
               fw.write(messageLines.remove(0));
               fw.write(lineSeparator);
            }
            ioOperation = "closing";
         }
         catch (Exception e) {
            try (PrintWriter pw = new PrintWriter(fw)) {
               fw.write(lineSeparator);
            }
         }
      }
      catch(IOException e) {
         System.err.println ("ReportPossibleDeadlockCondition: while " + ioOperation + " file " + file);
         System.err.println (e.getMessage());
      }
   }

   private static String getCallStack(Exception ex) {
      String callStack = null;
      try (StringWriter writer = new StringWriter();
           PrintWriter pw = new PrintWriter(writer)) {
         ex.printStackTrace(pw);
         callStack = writer.toString();
         // Remove the line that is the exception identifier
         callStack = removeFirstLine(callStack);
         // Remove the line in the call stack that is the call to ReportPossibleDeadlockCondition
         callStack = removeFirstLine(callStack);
      } catch (IOException e1) {
      }
      return callStack;
   }

   static final String CRLF = "\n";

   public static String removeFirstLine(String source) {
      int beginIndex = source.indexOf(CRLF);
      if (beginIndex < 0) {
         return source;
      }
      return source.substring(beginIndex + CRLF.length());
   }

   /**
    * class SortedStrings. This class is used to keep a list of strings that are
    * ordered by another string (perhaps a substring). Strings are always added
    * to this class by passing both the data string, and a key string. It is the
    * key string that is used to sort the element that hold the data strings. It
    * is allowable to have multiple Data strings be accessed by the same key string.
    *
    * @author pnostrow
    */
   // Define this class to be static, so that it can be instantiated from within
   // a static method of the Semaphore class
   public static class SortedStrings {
      final SortedStringsVector               sortedKeys;
      // This hashtable contains Vector objects, which contain all the strings
      // that are mapped by to by the same keyString. This allows us to accept
      // multiple data strings for a single key string.
      final Hashtable<String, Vector<String>> dataTable;

      public SortedStrings() {
         sortedKeys = new SortedStringsVector();
         dataTable = new Hashtable<>();
      }

      public void AddString(String keyString, String dataString) {
         sortedKeys.add(keyString);
         Vector<String> dataStrings = dataTable.get(keyString);
         if (dataStrings == null) {
            dataStrings = new Vector<>();
         }
         dataStrings.add(dataString);
         dataTable.put(keyString, dataStrings);
      }

      public String RemoveFirstString() {
         String firstKey = sortedKeys.firstElement();
         if (firstKey != null) {
            sortedKeys.remove(firstKey);
            Vector<String> dataStrings = dataTable.get(firstKey);
            if (dataStrings != null) {
               String firstDataString = dataStrings.remove(0);
               if (dataStrings.size() == 0) {
                  dataTable.remove(firstKey);
               }
               return firstDataString;
            }
         }
         return null;
      }
      /**
       * ConvertToSortedVectorOfDataStringsAndDestroy removes each of the
       * strings in the data array, in the order defined by the key strings,
       * and returns them in a Vector. It is necessary to remove the elements
       * from the data table as they are added to the results Vector, because
       * if there exist two different data strings that have the same key string
       * then we would never be able to access the second data string (unless we
       * remove the first string.)
       * @return A Vector of Strings
       */
      public Vector<String> ConvertToSortedVectorOfDataStringsAndDestroy() {
         Vector<String> results = new Vector<>();
         String firstDataString;
         while ((firstDataString = RemoveFirstString()) != null) {
            results.add(firstDataString);
         }
         return results;
      }
   }

   public static Vector<String> printLocks(Diagnostics diags) {
      SortedStrings sortedString = new SortedStrings();
      synchronized(THREAD_HASH) {
         Enumeration<Thread> enumThreads = THREAD_HASH.keys();
         Vector<Semaphore> threadsLockedSemaphores;
         Thread currentThread;
         StringBuilder threadData = new StringBuilder();
         String diagOperations;
         while(enumThreads.hasMoreElements()) {
            currentThread = (enumThreads.nextElement());
            threadsLockedSemaphores = THREAD_HASH.get(currentThread);
            if (threadsLockedSemaphores != null) {
               diagOperations   = "";
               // start with a fresh string buffer
               threadData.setLength(0);
               // print the thread name
               threadData.append("Thread ").append(currentThread);
               if (diags != null) {
                  // record the operations for this thread, so we can use it
                  // to sort with once we have the completed string buffer.
                  diagOperations = diags.getOperationsStringForThread(currentThread);
                  // print all the Diagnostic associations for this thread
                  threadData.append(" (Diag Assoc = ").append(diags.getAssociationsStringForThread(currentThread));
                  // print the operations (as names)
                  threadData.append(", Operations = ").append(diagOperations).append(")");
               }
               // print how many locks this thread has locked
               threadData.append(" has ").append(threadsLockedSemaphores.size()).append(" locks:");
               for (int i=0 ; i<threadsLockedSemaphores.size() ; i++) {
                  Semaphore lockedSemaphore = threadsLockedSemaphores.elementAt(i);
                  if (lockedSemaphore != null) {
                     // separate each lock with a CR-LF
                     threadData.append(Diagnostics.lineSeparator);
                     // the indent and print each semaphore that is locked.
                     threadData.append("    ").append(lockedSemaphore);
                  }
               }
               // put the thread data into the sortedString object, sorted by it operations.
               sortedString.AddString(diagOperations, threadData.toString());
            }
         }
      }
      return sortedString.ConvertToSortedVectorOfDataStringsAndDestroy();
   }

   public static boolean closeSocketOfLongestOperation(Diagnostics diags, long socketOutputTimeAllowanceInMilliseconds) {
      if (diags == null) {
         // If the diags parameter is null, we can not do anything, so return false.
         return false;
      }
      // This will hold the LockableDataOutputStream object that is currently locked by
      // the longest socket operation that has a LockableDataOutputStream object locked.
      // We will use this list after we release our lock on synchronized(_threadHash).
      LockableDataOutputStream dosWithLongestSocketLock = null;
      // To avoid writing diagnostics while we have a lock on synchronized(_threadHash),
      // we create this StringBuffer, and write all diagnostics to it. Then, once we have
      // exited the synchronized block, we can print the diags safely.
      StringBuilder diagOutput = new StringBuilder();
      diagOutput.append("Checking threads for blocking sockets");
      diagOutput.append(':').append(Diagnostics.lineSeparator);
      synchronized(THREAD_HASH) {
         Vector<Semaphore> threadsLockedSemaphores;
         Thread currentThread;
         long longestOperationTime = -1;
         // Go through all the threads that have objects locked,
         // looking for Semaphores that are blocking on socket sends.
         Enumeration<Thread> enumThreads = THREAD_HASH.keys();
         while(enumThreads.hasMoreElements()) {
            currentThread = enumThreads.nextElement();
            threadsLockedSemaphores = THREAD_HASH.get(currentThread);
            if (threadsLockedSemaphores != null) {
               // Look at all the Semaphores that this thread has locked, and
               // see if it has a Semaphore locked that has a data output stream.
               for (int i=0 ; i<threadsLockedSemaphores.size() ; i++) {
                  Semaphore lockedSemaphore = threadsLockedSemaphores.elementAt(i);
                  if (lockedSemaphore != null) {
                     if (lockedSemaphore.lockableDataOutputStream != null) {
                        // Determine how long this lock has been locked for.
                        long lockTimeLength = lockedSemaphore.lockableDataOutputStream.getLockTimeLengthInMilliseconds();
                        // If this thread does contain a locked Semaphore of the right
                        // type, then we check to see if it is the longest known operation.
                        diagOutput.append("Thread ").append(currentThread);
                        diagOutput.append(" contains a locked semaphore for a data output stream,");
                        diagOutput.append(" operationID = ").append(diags.getFirstOperationIDsForThread(currentThread));
                        diagOutput.append(" locked for ").append(lockTimeLength).append(" milliseconds");
                        if ((longestOperationTime == -1) || (longestOperationTime > lockTimeLength)) {
                           if (longestOperationTime == -1) {
                              diagOutput.append(" (first, longest operation)");
                           }
                           else {
                              diagOutput.append(" (replacing previous longest operation length of ").append(longestOperationTime).append(")");
                           }
                           // This is the new longest operation time.
                           // Check to see if the socket has been locked for a period of time longer than
                           // the time allowed by the socketOutputTimeAllowanceInMilliseconds parameter.
                           if (lockTimeLength > socketOutputTimeAllowanceInMilliseconds) {
                              // record the thread, and the operationID
                              longestOperationTime = lockTimeLength;
                              dosWithLongestSocketLock = lockedSemaphore.lockableDataOutputStream;
                           }
                           else {
                              diagOutput.append(", but socket has not been locked for the minimum ");
                              diagOutput.append(socketOutputTimeAllowanceInMilliseconds).append(" milliseconds.");
                           }
                           diagOutput.append(Diagnostics.lineSeparator);
                           // Since we found this to be true, we don't need to continue
                           // searching the other Semaphore objects.
                           break;
                        }
                     }
                  }
               }
            } // end if (threadsLockedSemaphores != null)
         } // end while(enumThreads.hasMoreElements())
      } // end synchronized(_threadHash)

      // Print the results into the log file and/or to the console.
      diags.logMessage(Diagnostics.TYPE_INFO, diagOutput.toString());
      // Now shut down the locked LockableDataOutputStream object.
      if (dosWithLongestSocketLock != null) {
         try {
            diags.logMessage(Diagnostics.TYPE_SEVERE_ERROR, "Shutting down LockableDataOutputStream: "
                                                            + dosWithLongestSocketLock);
            dosWithLongestSocketLock.close();
            // return true if we successfully shut down a socket.
            return true;
         } catch (IOException ex) {
            diags.logMessage(Diagnostics.TYPE_SEVERE_ERROR, "Exception caught while shutting down LockableDataOutputStream: "
                                                            + dosWithLongestSocketLock, ex);
         }
      }
      // return false if we did not successfully shut down any sockets.
      return false;
   }

   public static synchronized boolean addNewThreadToTrackTable() {
      // Update the Hashtable that contains a Vector
      // for each thread that owns a Semaphore.
      Vector<Semaphore> threadsLockedSemaphores = THREAD_HASH.get(Thread.currentThread());
      // If the thread is already in our threadHash table, do nothing.
      if (threadsLockedSemaphores == null)
      {
         threadsLockedSemaphores = new Vector<>();
         // Now put the new Vector into the Hashtable.
         THREAD_HASH.put(Thread.currentThread(), threadsLockedSemaphores);
         return true;
      }
      return false;
   }

   public static boolean removeDyingThread(Thread threadToRemove) {
      Vector<Semaphore> threadsLockedSemaphores = THREAD_HASH.remove(threadToRemove);
      if (threadsLockedSemaphores == null) {
         return false;
      }
      if (!threadsLockedSemaphores.isEmpty()) {
         Vector<String> errors = new Vector<>();
         errors.add(StringUtils.getTimeStamp());
         errors.add("Thread dying before all its object locks have been released:");
         errors.add(threadToRemove.toString());
         errors.add(threadsLockedSemaphores.toString());
         errors.add(getCallStack(new Exception()));

         writeStringsToDeadlockFile(errors);
      }
      return true;
   }
}
