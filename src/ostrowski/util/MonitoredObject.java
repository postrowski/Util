package ostrowski.util;

// Java Imports
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class MonitoredObject implements IMonitorableObject, Cloneable
{
   // The IMonitoredObjectRegistrationWatcher interface can be used to let an object that
   // contains a MonitoredObject know when its contained monitored object have a change to
   // its watchers list.
   public interface IMonitoredObjectRegistrationWatcher
   {
      public void watcherRegistered(IMonitorableObject monitoredObject, IMonitoringObject newWatcher, RegisterResults registrationResults);
      public void watcherUnRegistered(IMonitorableObject monitoredObject, IMonitoringObject priorWatcher, UnRegisterResults unregistrationResults);
   }

   private Hashtable<IMonitoringObject, Integer> _watchers = new Hashtable<>();

   // This is used to lock access to all synchronized member variables:
   private Semaphore _lock_watchers;
   // The watchedProxy object allows this object to be contained within an
   // object that implements IMonitorableObject. If this object is contained,
   // then the constructor of this object should be passed a reference
   // to the container IMonitorableObject, so that when this object
   // register/unregisters itself with a IMonitoringObject, it can pass
   // the containing object, so that all notifications & registrations go
   // through the container first, before being passed down to this object
   // by the container.
   public IMonitorableObject _watchedProxy;

   private IMonitoredObjectRegistrationWatcher _registrationWatcher;
   // Turn this variable to 'true' if you want to see every registration & unregistration
   // of MonitoredObjects, along with every notification to those objects.
   private static final boolean EXCESSIVE_DIAGNOSTICS = false;

   public String _objectIDString;
   private void Init(String objectIDString) {
      _lock_watchers  = new Semaphore("MonitoredObject (" + objectIDString + ")",
                                      Semaphore.CLASS_MONITOREDOBJECT_watchers);
      _objectIDString = objectIDString;
      _registrationWatcher = null;
   }

   /**
   * Class constructor.
   */
   public MonitoredObject(String objectIDString) {
      Init(objectIDString);
      _watchedProxy        = this;
   }

   public MonitoredObject(String objectIDString, IMonitorableObject watchedProxy) {
      Init(objectIDString);
      _watchedProxy        = watchedProxy;
   }

   public void setRegistrationWatcher(IMonitoredObjectRegistrationWatcher registrationWatcher)
   {
      _registrationWatcher = registrationWatcher;
   }

   @Override
   @SuppressWarnings("unchecked")
   public Object clone() {
      MonitoredObject clone;
      if (_watchedProxy != this) {
         clone = new MonitoredObject(_objectIDString, _watchedProxy);
      }
      else {
         clone = new MonitoredObject(_objectIDString);
      }
      clone.setRegistrationWatcher(_registrationWatcher);

      clone._watchers = (Hashtable<IMonitoringObject, Integer>)(_watchers.clone());
      return clone;
   }

   @Override
   public void notifyWatchers(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification, Vector<IMonitoringObject> skipList, Diagnostics diag) {
      Vector<IMonitoringObject> recipients = new Vector<>();
      IMonitoringObject objectToNotify;
      synchronized (_watchers) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchers)) {
            Enumeration<IMonitoringObject> watcherIDs = _watchers.keys();
            while (watcherIDs.hasMoreElements()) {
               objectToNotify = watcherIDs.nextElement();
               // if a skipList is present, then we need to make sure that we don't send this
               // notification to any IMonitoringObjects in that list.
               boolean skipRecipient = false;
               if (skipList != null) {
                  skipRecipient = skipList.contains(objectToNotify);
               }
               if (!skipRecipient) {
                  recipients.add(objectToNotify);
               }
            }
         }
      }
      // release our lock before we send the notifications, to avoid a possible deadlock condition.
      while (!recipients.isEmpty()) {
         objectToNotify = recipients.remove(0);
         objectToNotify.monitoredObjectChanged(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
         if (EXCESSIVE_DIAGNOSTICS) {
            if (diag != null) {
               diag.logMessage(Diagnostics.TYPE_MONITORS, "notifyWatchers(" + objectToNotify + ") for MonitoredObject " + hashCode());
            }
         }
      }
   }

   @Override
   /**
    * Method registerAsWatcher. This method is called to register a monitoring object
    * as a watcher to all changes that affect this monitored object.
    * @param watcherObject. An IMonitoringObject that will receive notifications about
    * changes to this MonitoredObject.
    * @param diag. The diagnostic object on which to write all messages.
    * @return RegisterResults. This enum indicates the success, and tells if other watchers are active.
    */
   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      RegisterResults results = RegisterResults.Failure;
      if (watcherObject != null) {
         boolean otherWatchers;
         boolean alreadyWatching;
         Integer currentCount;
         synchronized (_watchers) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchers)) {

               currentCount = _watchers.get(watcherObject);
               if (currentCount == null) {
                  // This is the first time this watcher is registering as watching this object
                  currentCount = new Integer(1);
                  alreadyWatching = false;
               }
               else {
                  currentCount = new Integer(currentCount.intValue() + 1);
                  alreadyWatching = true;
               }
               _watchers.put(watcherObject, currentCount);

               // If the watchers size is greater than 1, then there must be
               // other watchers watching this object.
               otherWatchers = _watchers.size() > 1;
            }
         }
         if (EXCESSIVE_DIAGNOSTICS) {
            if (diag != null) {
               diag.logMessage(Diagnostics.TYPE_MONITORS, "registerAsWatcher(" + watcherObject + ") for MonitoredObject " + hashCode() + " count after = "+ currentCount);
            }
         }

         watcherObject.registerMonitoredObject(_watchedProxy, diag);

         // If we decide to return the total watcher count, don't forget to sum
         // up all the Integers that are the values of the _watchers Hashtable.
         if (alreadyWatching && otherWatchers) {
            return RegisterResults.ThisAndOthersAlreadyWatching;
         }
         if (alreadyWatching) {
            return RegisterResults.ThisAlreadyWatching;
         }
         if (otherWatchers) {
            return RegisterResults.OthersAlsoWatching;
         }

         return RegisterResults.Success;
      }
      if (_registrationWatcher != null) {
         _registrationWatcher.watcherRegistered(this, watcherObject, results);
      }
//      if (_excesiveDiagnostics) {
//         reportObjectWatcherRegistration(watcherObject, this, true, false, results);
//      }
      return results;
   }

   @Override
   /**
    * Method unregisterAsWatcher. This method is called to unregister a monitoring object
    * as a watcher to all changes that affect this monitored object.
    * @param watcherObject. An IMonitoringObject that will not longer recieve notification
    * about changes to this MonitoredObject.
    * @param diag. The diagnostic object on which to write all messages.
    * @return UnRegisterResults. This enum indicates if other watchers are still active,
    * or if the watcher object is still watching the monitored object (from a re-entrant watch).
    */
   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return unregisterAsWatcher(watcherObject, false/*removeAllInstancesOfWatcher*/, true/*reportFailureIfWatcherNotFound*/, diag);
   }
   public UnRegisterResults unregisterAsWatcherIfWatching(IMonitoringObject watcherObject, Diagnostics diag) {
      return unregisterAsWatcher(watcherObject, false/*removeAllInstancesOfWatcher*/, false/*reportFailureIfWatcherNotFound*/, diag);
   }

   @Override
   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag)
   {
      return unregisterAsWatcher(watcherObject, true/*removeAllInstancesOfWatcher*/,  true/*reportFailureIfWatcherNotFound*/, diag);
   };

   private UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, boolean removeAllInstancesOfWatcher, boolean reportFailureIfWatcherNotFound, Diagnostics diag) {
      UnRegisterResults results = UnRegisterResults.Failure;
      if (watcherObject != null) {
         boolean otherWatchers;
         boolean stillWatching;
         Integer currentCount;
         Exception caughtException = null;
         synchronized (_watchers) {
            try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchers)) {

               currentCount = _watchers.get(watcherObject);
               if ((currentCount == null) && reportFailureIfWatcherNotFound) {
                  // If we could not find an entry for this  watcher object, then
                  // it is likely that the object that is registered to listen for
                  // events is using a proxy object, and it did not declare itself
                  // as a proxy when the MonitoringObject was created.
                  // One example of this architecture is the ClientListenerThread
                  // object, which contains a MonitoringObject object named
                  // _watchedObjects. If the ClientListenerThread registers itself
                  // as a watcher of any Monitorable object, then the MonitorableObject
                  // calls registerMonitoredObject on the ClientListenerThread, which
                  // forwards this method call to the _watchedObjects member. This
                  // forwarding results in the contained object (_watchedObjects)
                  // being listed in the _watchers list for the MonitoredObject being
                  // watched. In this case, when we the ClientListenerThread calls
                  // unregisterAsWatcher on the object that it no longer wants to
                  // watch, we would be unable to find the ClientListenrThread,
                  // unless the _watchedObjects member knew that its _watchingProxy
                  // was the ClientListenerThread.
                  try {
                     throw new NullPointerException();
                  }
                  catch (NullPointerException e) {
                     caughtException = e;
                  }
               }
               if (currentCount != null) {

                  if (removeAllInstancesOfWatcher) {
                     currentCount = new Integer(0);
                  }
                  else {
                     currentCount = new Integer(currentCount.intValue() - 1);
                  }
                  if (currentCount.intValue() <= 0) {
                     // When the occurrence count goes to zero, remove it from the list
                     _watchers.remove(watcherObject);
                     stillWatching = false;
                  }
                  else {
                     _watchers.put(watcherObject, currentCount);
                     stillWatching = true;
                  }
                  if (stillWatching) {
                     // If the watchers size is greater than 1, then there must be
                     // watchers other that us that are watching this object.
                     otherWatchers = _watchers.size() > 1;
                  }
                  else {
                     // If the watchers size is greater than 0, then there must be
                     // other watchers watching this object. We are NOT watching it.
                     otherWatchers = _watchers.size() > 0;
                  }
                  if (stillWatching && otherWatchers) {
                     return UnRegisterResults.ThisAndOthersStillWatching;
                  }
                  if (stillWatching) {
                     return UnRegisterResults.ThisStillWatching;
                  }
                  if (otherWatchers) {
                     return UnRegisterResults.OthersStillWatching;
                  }
                  return UnRegisterResults.Success;
               }
            }
         }
         // If we threw an exception because we didn't find the object that was watching
         // us in our watchers list, we need to delay the printing of the diag until
         // after we release the _lock_watchers semaphore, or we could deadlock.
         if (diag != null) {
            if (caughtException != null) {
               diag.logMessage(Diagnostics.TYPE_SEVERE_ERROR, "unregisterAsWatcher(" + watcherObject + ") for MonitoredObject " + hashCode() + " unable to locate watcher! ", caughtException);
            }
            if (EXCESSIVE_DIAGNOSTICS) {
               diag.logMessage(Diagnostics.TYPE_MONITORS, "unregisterAsWatcher(" + watcherObject + ") for MonitoredObject " + hashCode() + " count after = "+ currentCount);
            }
         }
         if (removeAllInstancesOfWatcher) {
            watcherObject.unregisterMonitoredObjectAllInstances(_watchedProxy, diag);
         }
         else {
            watcherObject.unregisterMonitoredObject(_watchedProxy, diag);
         }
      }
      if (_registrationWatcher != null) {
         _registrationWatcher.watcherUnRegistered(this, watcherObject, results);
      }
//      if (_excesiveDiagnostics) {
//         reportObjectWatcherRegistration(watcherObject, this, false, removeAllInstancesOfWatcher, results);
//      }
      return results;
   }

   @Override
   public String getObjectIDString()
   {
      return _objectIDString;
   }

   @Override
   public Vector<IMonitoringObject> getSnapShotOfWatchers() {
      Vector<IMonitoringObject> retVect = new Vector<>();
      synchronized(_watchers) {
         try (SemaphoreAutoTracker sat = new SemaphoreAutoTracker(_lock_watchers)) {

            Enumeration<IMonitoringObject> watcherIDs = _watchers.keys();
            while (watcherIDs.hasMoreElements()) {
               retVect.add(watcherIDs.nextElement());
            }
         }
      }
      return retVect;
   }

   public int getWatchingCount(IMonitoringObject watcherObject) {
      _lock_watchers.check();
      Integer count = _watchers.get(watcherObject);
      if (count == null) {
         return 0;
      }
      return count.intValue();
   }

   public int getWatchersCount() {
      _lock_watchers.check();
      return _watchers.size();
   }

   @Override
   public String toString() {
      _lock_watchers.check();
      return " number of watchers: " + _watchers.size();
   }

   public String dump() {
      _lock_watchers.check();
      return "\n\t number of watchers: " + _watchers.size();
   }
}
