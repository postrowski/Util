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
   public String getObjectIDString();


   final MonitoringObject  _monitoringObj = new MonitoringObject("");


   default public Vector<IMonitorableObject> getSnapShotOfWatchedObjects() {
      return _monitoringObj.getSnapShotOfWatchedObjects();
   }

   default public void monitoredObjectChanged(IMonitorableObject originalWatchedObject, IMonitorableObject modifiedWatchedObject, Object changeNotification,
                                      Vector<IMonitoringObject> skipList, Diagnostics diag) {
      _monitoringObj.monitoredObjectChanged(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }

   default public boolean registerMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.registerMonitoredObject(watchedObject, diag);
   }

   default public boolean unregisterMonitoredObject(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.unregisterMonitoredObject(watchedObject, diag);
   }

   default public boolean unregisterMonitoredObjectAllInstances(IMonitorableObject watchedObject, Diagnostics diag) {
      return _monitoringObj.unregisterMonitoredObjectAllInstances(watchedObject, diag);
   }
}
