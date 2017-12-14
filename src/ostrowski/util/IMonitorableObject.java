package ostrowski.util;

import java.util.Vector;

public interface IMonitorableObject
{
   public enum RegisterResults {
      Success,
      ThisAlreadyWatching,
      OthersAlsoWatching,
      ThisAndOthersAlreadyWatching,
      Failure
   }

   public enum UnRegisterResults {
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

   public final MonitoredObject _monitoredObj = new MonitoredObject("");

   default public void notifyWatchers(IMonitorableObject originalWatchedObject,
                              IMonitorableObject modifiedWatchedObject,
                              Object changeNotification,
                              Vector<IMonitoringObject> skipList,
                              Diagnostics diag)  {
      _monitoredObj.notifyWatchers(originalWatchedObject, modifiedWatchedObject, changeNotification, skipList, diag);
   }

   default public String getObjectIDString() {
      return _monitoredObj.getObjectIDString();
   }

   default public Vector<IMonitoringObject> getSnapShotOfWatchers() {
      return _monitoredObj.getSnapShotOfWatchers();
   }

   default public RegisterResults registerAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return _monitoredObj.registerAsWatcher(watcherObject, diag);
   }

   default public UnRegisterResults unregisterAsWatcher(IMonitoringObject watcherObject, Diagnostics diag) {
      return _monitoredObj.unregisterAsWatcher(watcherObject, diag);
   }

   default public UnRegisterResults unregisterAsWatcherAllInstances(IMonitoringObject watcherObject, Diagnostics diag) {
      return _monitoredObj.unregisterAsWatcherAllInstances(watcherObject, diag);
   }


}
