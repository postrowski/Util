package ostrowski.util;

public class SemaphoreAutoLocker implements AutoCloseable
{
   private final Semaphore _semaphore;
   public SemaphoreAutoLocker(Semaphore semaphore) {
      this._semaphore = semaphore;
      if (this._semaphore != null) {
         this._semaphore.lock();
      }
   }
   @Override
   public void close() {
      if (this._semaphore != null) {
         this._semaphore.unlock();
      }
   }
}
