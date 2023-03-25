package ostrowski.util;

import java.util.Vector;

public interface IMonitoringObject
{
//   public boolean registerMonitoredObject(IMonitorableObject watchedObject,
//                                          Diagnostics diag);
//   public boolean unregisterMonitoredObject(IMonitorableObject watchedObject,
//                                            Diagnostics diag);
//   public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject,
//                                                        Diagnostics diag);
//   public void monitoredObjectChanged(IMonitorableObject originalWatchedObject,
//                                      IMonitorableObject modifiedWatchedObject,
//                                      Object changeNotification,
//                                      Vector<IMonitoringObject> skipList,
//                                      Diagnostics diag);
//   public Vector<IMonitorableObject> getSnapShotOfWatchedObjects();
   String getObjectIDString();

   MonitoringObject  _monitoringObj = new MonitoringObject("");


   default Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      return _monitoringObj.getSnapShotOfWatchedObjects();
   }

   default void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                                       Vector<IMonitoringObject> skipList, Diagnostics diag) {
      _monitoringObj.monitoredObjectChanged(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }

   default boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.registerMonitoredObject(watchedObject, diag);
   }

   default boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.unregisterMonitoredObject(watchedObject, diag);
   }

   default boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
}
