package ostrowski.util;

// Java Imports
import java.io.DataOutputStream;
import java.io.OutputStream;

public class LockableDataOutputStream extends DataOutputStream{

   public final Semaphore lock;
   private      long      lockTimeMillis;

   public LockableDataOutputStream(OutputStream out)
   {
      super(out);
      lock = new Semaphore("LockableDataOutputStream", Semaphore.CLASS_LOCKABLEDATAOUTPUTSTREAM, this);
      lockTimeMillis = 0;
   }

   public void lock()
   {
      lock.lock();
   }

   public void unlock()
   {
      lock.unlock();
   }

   public void track()
   {
      lock.track();
      // If this is the first time we have tracked this
      // LockableDataOutputStream, record the timestamp.
      if (lock.trackCount == 1) {
         lockTimeMillis = System.currentTimeMillis();
      }
   }

   public void untrack()
   {
      // If we will no longer have this object tracked,
      // discard the timestamp.
      if (lock.trackCount == 1) {
         lockTimeMillis = 0;
      }
      lock.untrack();
   }

   public void check()
   {
      lock.check();
   }

   public long getLockTimeLengthInMilliseconds() {
      if (lockTimeMillis == 0) {
         return -1;
      }
      return System.currentTimeMillis() - lockTimeMillis;
   }
}
