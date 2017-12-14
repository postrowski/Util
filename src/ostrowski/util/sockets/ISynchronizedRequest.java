/*
 * Created on Dec 15, 2006
 *
 */
package ostrowski.util.sockets;


public interface ISynchronizedRequest
{
   int getSyncKey();
   void setResponse(ISynchronizedResponse response);
// setResponse should look something like this:
//   public void setResponse(ISynchronizedResponse response)
//   {
//      if (response instanceof SyncRequest) {
//         SyncRequest respAction = (SyncRequest) response;
//         _answerID = respAction._answerID;
//         _answerStr = respAction._answerStr;
//      }
//      // Notify any thread waiting for this response.
//      synchronized (this) {
//         this._lockThis.lock();
//         try {
//            notifyAll();
//         }
//         finally {
//            this._lockThis.unlock();
//         }
//      }
//      // If a results queue has been set up, then put this object into it
//      // This method is used when one thread is waiting on multiple responses
//      // so the waiting thread waits on the queue instead.
//      List<SyncRequest> resultsQueue = getResultsQueue();
//      if (resultsQueue != null) {
//         synchronized (resultsQueue) {
//            resultsQueue.add(this);
//            resultsQueue.notifyAll();
//         }
//      }
//   }
}
