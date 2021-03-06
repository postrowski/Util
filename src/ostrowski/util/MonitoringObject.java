package ostrowski.util;

// Java Imports
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

// This class is intended to be contained by a class that itself
// implements IMonitoringObject. That class can then forward the
// calls to registerMonitoredObject & unregisterMonitoredObject to
// this contained object.
public class MonitoringObject implements IMonitoringObject
{
   private Hashtable<IMonitorableObject, Integer> watchedObjects;
   private Semaphore                              lock_watchedObjects;
   // The watchingProxy object allows this object to be contained within an
   // object that implements IMonitoringObject. If this object is contained,
   // then the constructor of this object should be passed a reference
   // to the container IMonitoringObject, so that when this object
   // registers/unregisters itself with a IMonitorableObject, it can pass
   // the containing object, so that all notifications & registrations go
   // through the container first, before being passed down to this object
   // by the container.
   public IMonitoringObject                       watchingProxy;

   // The forwardObject allows a MonitoringObject to directly forward all
   // changes to this object to the watcher of the forwardObject
   public MonitoredObject forwardObject;

   public String objectIDString;

   private void Init(String objectIDString) {
      watchedObjects = new Hashtable<>();
      lock_watchedObjects = new Semaphore("MonitoringObject (" + objectIDString + ")",
                                          Semaphore.CLASS_MONITORINGOBJECT_watchedObjects);
      watchingProxy = this;
      forwardObject = null;
      this.objectIDString = objectIDString;
   }

   public MonitoringObject(String objectIDString)
   {
      Init(objectIDString);
   }
   public MonitoringObject(String objectIDString, IMonitoringObject watchingProxy) {
      Init(objectIDString);
      this.watchingProxy = watchingProxy;
   }
   public MonitoringObject(String objectIDString, MonitoredObject forwardObject) {
      Init(objectIDString);
      this.forwardObject = forwardObject;
   }

   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      synchronized (watchedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_watchedObjects)) {

            Integer currentCount = watchedObjects.get(watchedObject);
            if (currentCount == null) {
               // This is the first time we are registering as watching this watchedObject
               currentCount = 1;
            }
            else {
               currentCount = currentCount + 1;
            }
            watchedObjects.put(watchedObject, currentCount);
         }
      }
      return true;
   }

   @Override
   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return unregisterMonitoredObject(watchedObject, false, diag);
   }
   @Override
   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag)
   {
      return unregisterMonitoredObject(watchedObject, true, diag);
   }
   private boolean unregisterMonitoredObject(IMonitorableObject watchedObject, boolean removeAllInstancesOfWatched, Diagnostics diag)
   {
      boolean objectFound = false;
      synchronized (watchedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_watchedObjects)) {

            Integer currentCount = watchedObjects.get(watchedObject);
            if (currentCount != null) {
               objectFound = true;

               if (removeAllInstancesOfWatched) {
                  watchedObjects.remove(watchedObject);
               }
               else {
                  currentCount = currentCount - 1;

                  if (currentCount <= 0) {
                     // When the occurrance count goes to zero, remove it from the list
                     watchedObjects.remove(watchedObject);
                  }
                  else {
                     watchedObjects.put(watchedObject, currentCount);
                  }
               }
            }
         }
      }
      return objectFound;
   }
   @Override
   public String getObjectIDString()
   {
      return objectIDString;
   }

   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag)
   {
      // If a forwarding object has been specified, then forward this
      // notification on to that object.
      if (forwardObject != null)
      {
         forwardObject.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
         return;
      }
      // If not, then we can do nothing in this base implementation,
      // because we know nothing about the objects that changed.
      throw new UnsupportedOperationException();
   }

   /**
    * This method does not come from the IMonitoringObject interface.
    * It allows the containing class to clear all the watched objects
    * from its list, and tells each watched object that its no longer
    * being watched.
    */
   public void stopMonitoringAllObjects(Diagnostics diag)
   {
      IMonitorableObject watchedObject;
      Vector<IMonitorableObject> watchedObjects = getSnapShotOfWatchedObjects();
      while (watchedObjects.size() > 0){
         // Pull the last element out of the list. Pulling from the end is
         // more efficient than pulling from the front, because it doesn't
         // require a shift of all the remaining element in the Vector.
         watchedObject = watchedObjects.remove(watchedObjects.size()-1);
         if (watchedObject != null) {
            // If two threads call stopMonitoringAllObjects at the same time, then
            // the object we just pulled out of the snapshot may no longer be in
            // the watchedObjects list, so double-check that this object is still
            // being watched, or we will report an 'unable to locate watcher' exception.
            if (this.watchedObjects.containsKey(watchedObject)) {
               // Tell the watched object that we aren't watching it anymore,
               // even if we called registerAsWatcher multiple times.
               watchedObject.unregisterAsWatcherAllInstances(watchingProxy, diag);
               // The call to unregisterAsWatcherAllInstances should have removed
               // that element from our watchedObjects list.
            }
         }
      }
      // It is possible that at this point there are objects that this object is watching,
      // because they may have been added since the call to getSnapShotOfWatchedObjects().
      // However, it will always be the case that objects could be added to the list between
      // the time we release any locks and we return, so don't bother preventing this case.
   }
   /**
    * This method does not come from the IMonitoringObject interface.
    * It allows the containing class to decrement the watched count on
    * all objects in the watchedObject list.
    */
   public void decMonitorCountAllObjects(Diagnostics diag)
   {
      IMonitorableObject watchedObject;
      Vector<IMonitorableObject> objects = getSnapShotOfWatchedObjects();
      while (objects.size() > 0) {
         watchedObject = objects.remove(0);
         // Tell the watched object that we aren't watching it anymore,
         // but only decrement one count, not all Instances.
         watchedObject.unregisterAsWatcher(watchingProxy, diag);
      }
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      Vector<IMonitorableObject> retVect = new Vector<>();
      synchronized(watchedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(lock_watchedObjects)) {

            Enumeration<IMonitorableObject> objects = watchedObjects.keys();
            while (objects.hasMoreElements()) {
               retVect.add(objects.nextElement());
            }

         }
      }
      return retVect;
   }
}
