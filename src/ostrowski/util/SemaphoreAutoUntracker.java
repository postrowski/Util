package ostrowski.util;

public class SemaphoreAutoUntracker implements AutoCloseable
{
   private final Semaphore semaphore;
   public SemaphoreAutoUntracker(Semaphore semaphore) {
      this.semaphore = semaphore;
      if (this.semaphore != null) {
         this.semaphore.untrack();
      }
   }
   @Override
   public void close() {
      if (this.semaphore != null) {
         this.semaphore.track();
      }
   }
}
