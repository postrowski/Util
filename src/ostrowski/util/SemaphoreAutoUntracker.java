package ostrowski.util;

public class SemaphoreAutoUntracker implements AutoCloseable
{
   private final Semaphore _semaphore;
   public SemaphoreAutoUntracker(Semaphore semaphore) {
      this._semaphore = semaphore;
      if (this._semaphore != null) {
         this._semaphore.untrack();
      }
   }
   @Override
   public void close() {
      if (this._semaphore != null) {
         this._semaphore.track();
      }
   }
}
