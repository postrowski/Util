package ostrowski.util;

// Java Imports
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
// Inter-Tel Imports

// This class is intented to be contained by a class that itself
// implements IMonitoringObject. That class can then forward the
// calls to registerMonitoredObject & unregisterMonitoredObject to
// this contained object.
public class MonitoringObject implements IMonitoringObject
{
   private Hashtable<IMonitorableObject, Integer> _watchedObjects;
   private Semaphore _lock_watchedObjects;
   // The watchingProxy object allows this object to be contained within an
   // object that implements IMonitoringObject. If this object is contained,
   // then the constructor of this object should be passed a reference
   // to the container IMonitoringObject, so that when this object
   // registers/unregisters itself with a IMonitorableObject, it can pass
   // the containing object, so that all notifications & registrations go
   // through the container first, before being passed down to this object
   // by the container.
   public IMonitoringObject _watchingProxy;

   // The _forwardObject allows a MonitoringObject to directly forward all
   // changes to this object to the watcher of the _forwardObject
   public MonitoredObject _forwardObject;

   public String _objectIDString;

   private void Init(String objectIDString) {
      _watchedObjects      = new Hashtable<>();
      _lock_watchedObjects = new Semaphore("MonitoringObject (" + objectIDString + ")",
                                           Semaphore.CLASS_MONITORINGOBJECT_watchedObjects);
      _watchingProxy       = this;
      _forwardObject       = null;
      _objectIDString      = objectIDString;
   }

   public MonitoringObject(String objectIDString)
   {
      Init(objectIDString);
   }
   public MonitoringObject(String objectIDString, IMonitoringObject watchingProxy) {
      Init(objectIDString);
      _watchingProxy = watchingProxy;
   }
   public MonitoringObject(String objectIDString, MonitoredObject forwardObject) {
      Init(objectIDString);
      _forwardObject = forwardObject;
   }

   @Override
   public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag)
   {
      synchronized (_watchedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchedObjects)) {

            Integer currentCount = _watchedObjects.get(watchedObject);
            if (currentCount == null) {
               // This is the first time we are registering as watching this watchedObject
               currentCount = new Integer(1);
            }
            else {
               currentCount = new Integer(currentCount.intValue() + 1);
            }
            _watchedObjects.put(watchedObject, currentCount);
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
      synchronized (_watchedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchedObjects)) {

            Integer currentCount = _watchedObjects.get(watchedObject);
            if (currentCount != null) {
               objectFound = true;

               if (removeAllInstancesOfWatched) {
                  _watchedObjects.remove(watchedObject);
               }
               else {
                  currentCount = new Integer(currentCount.intValue() - 1);

                  if (currentCount.intValue() <= 0) {
                     // When the occurance count goes to zero, remove it from the list
                     _watchedObjects.remove(watchedObject);
                  }
                  else {
                     _watchedObjects.put(watchedObject, currentCount);
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
      return _objectIDString;
   }

   @Override
   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag)
   {
      // If a forwarding object has been specified, then forward this
      // notification on to that object.
      if (_forwardObject != null)
      {
         _forwardObject.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
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
            // the _watchedObjects list, so double-check that this object is still
            // being watched, or we will report an 'unable to locate watcher' exception.
            if (_watchedObjects.containsKey(watchedObject)) {
               // Tell the watched object that we aren't watching it anymore,
               // even if we called registerAsWatcher multiple times.
               watchedObject.unregisterAsWatcherAllInstances(_watchingProxy, diag);
               // The call to unregisterAsWatcherAllInstances should have removed
               // that element from our _watchedObjects list.
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
         watchedObject.unregisterAsWatcher(_watchingProxy, diag);
      }
   }

   @Override
   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      Vector<IMonitorableObject> retVect = new Vector<>();
      synchronized(_watchedObjects) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchedObjects)) {

            Enumeration<IMonitorableObject> objects = _watchedObjects.keys();
            while (objects.hasMoreElements()) {
               retVect.add(objects.nextElement());
            }

         }
      }
      return retVect;
   }
}
