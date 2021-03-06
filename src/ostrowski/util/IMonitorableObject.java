package ostrowski.util;

import java.util.Vector;

public interface IMonitorableObject
{
   enum RegisterResults {
      Success,
      ThisAlreadyWatching,
      OthersAlsoWatching,
      ThisAndOthersAlreadyWatching,
      Failure
   }

   enum UnRegisterResults {
      Success,
      ThisStillWatching,
      ThisAndOthersStillWatching,
      OthersStillWatching,
      Failure
   }

//   public void notifyWatchers(IMonitorableObject originalWatchedObject,
//                              IMonitorableObject modifiedWatchedObject,
//                              Object changeNotification,
//                              Vector<IMonitoringObject> skipList,
//                              Diagnostics diag);
//   public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag);
//   public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag);
//   public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag);
//   public Vector<IMonitoringObject> getSnapShotOfWatchers();
//   public String getObjectIDString();

   MonitoredObject monitoredObj = new MonitoredObject("");

   default void notifyWatchers(IMonitorableObject originalWatchedObject,
                               IMonitorableObject modifiedWatchedObject,
                               Object changeNotification,
                               Vector<IMonitoringObject> skipList,
                               Diagnostics diag)  {
      monitoredObj.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }

   default String getObjectIDString() {
      return monitoredObj.getObjectIDString();
   }

   default Vector<IMonitoringObject> getSnapShotOfWatchers() {
      return monitoredObj.getSnapShotOfWatchers();
   }

   default RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return monitoredObj.registerAsWatcher(watcherObject, diag);
   }

   default UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return monitoredObj.unregisterAsWatcher(watcherObject, diag);
   }

   default UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag) {
      return monitoredObj.unregisterAsWatcherAllInstances(watcherObject, diag);
   }


}
