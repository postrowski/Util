package ostrowski.util;

public class SemaphoreAutoLocker implements AutoCloseable
{
   private final Semaphore semaphore;
   public SemaphoreAutoLocker(Semaphore semaphore) {
      this.semaphore = semaphore;
      if (this.semaphore != null) {
         this.semaphore.lock();
      }
   }
   @Override
   public void close() {
      if (this.semaphore != null) {
         this.semaphore.unlock();
      }
   }
}
