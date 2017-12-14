package ostrowski.util;

// Java Imports
import java.io.DataOutputStream;
import java.io.OutputStream;

public class LockableDataOutputStream extends DataOutputStream{

   public Semaphore _lock;
   private long _lockTimeMillis;

   public LockableDataOutputStream(OutputStream out)
   {
      super(out);
      _lock = new Semaphore("LockableDataOutputStream", Semaphore.CLASS_LOCKABLEDATAOUTPUTSTREAM, this);
      _lockTimeMillis = 0;
   }

   public void lock()
   {
      _lock.lock();
   }

   public void unlock()
   {
      _lock.unlock();
   }

   public void track()
   {
      _lock.track();
      // If this is the first time we have tracked this
      // LockableDataOutputStream, record the timestamp.
      if (_lock._trackCount == 1) {
         _lockTimeMillis = System.currentTimeMillis();
      }
   }

   public void untrack()
   {
      // If we will no longer have this object tracked,
      // discard the timestamp.
      if (_lock._trackCount == 1) {
         _lockTimeMillis = 0;
      }
      _lock.untrack();
   }

   public void check()
   {
      _lock.check();
   }

   public long getLockTimeLengthInMilliseconds() {
      if (_lockTimeMillis == 0) {
         return -1;
      }
      return System.currentTimeMillis() - _lockTimeMillis;
   }
}
