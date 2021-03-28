package ostrowski.util;

public class SemaphoreAutoTracker implements AutoCloseable
{
   private final Semaphore semaphore;
   public SemaphoreAutoTracker(Semaphore semaphore) {
      this.semaphore = semaphore;
      if (this.semaphore != null) {
         this.semaphore.track();
      }
   }
   @Override
   public void close() {
      if (this.semaphore != null) {
         this.semaphore.untrack();
      }
   }
}
