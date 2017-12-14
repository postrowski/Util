package ostrowski.util;

public class SemaphoreAutoTracker implements AutoCloseable
{
   private final Semaphore _semaphore;
   public SemaphoreAutoTracker(Semaphore semaphore) {
      this._semaphore = semaphore;
      if (this._semaphore != null) {
         this._semaphore.track();
      }
   }
   @Override
   public void close() {
      if (this._semaphore != null) {
         this._semaphore.untrack();
      }
   }
}
