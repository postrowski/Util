package ostrowski.util;

// Java Imports


public class TrackableThread extends Thread {
   public Object diagnosticAssociations;
   public TrackableThread()                                                 { super();                    initialize();}

   public TrackableThread(Runnable target)                                  { super(target);              initialize();}

   public TrackableThread(Runnable target, String name)                     { super(target, name);        initialize();}

   public TrackableThread(String name)                                      { super(name);                initialize();}

   public TrackableThread(ThreadGroup group, Runnable target)               { super(group, target);       initialize();}

   public TrackableThread(ThreadGroup group, Runnable target, String name)  { super(group, target, name); initialize();}

   public TrackableThread(ThreadGroup group, String name)                   { super(group, name);         initialize();}

   private void initialize() {
      diagnosticAssociations = null;
      // Make certain that we are placed in the Semaphores thread tracking table,
      // so that we can see this thread from diagnostic dumps ('dump locks').
      Semaphore.addNewThreadToTrackTable();
   }

   @Override
   protected void finalize() {
      // When we reach this point, the thread is dead, so we should remove
      // any reference to it from the Semaphores global thread hashtable.
      // This needs to be the last operation this thread does so that
      // it doesn't re-add itself back to the list of active threads,
      // which would happen any time a Diag is printed or a lock is acquired.
      Semaphore.removeDyingThread(this);
      // NOTE: the above call WILL NOT DO ANYTHING until the Semaphore
      //       object starts using WeakReferences to this thread, or uses
      //       WeakHashMaps containing this thread. Otherwise, if the
      //       Semaphore references this thread, finalize on this thread
      //       will never be called.
      //       Therefore,
   }

}
