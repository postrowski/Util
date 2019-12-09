package ostrowski.protocol;

import java.io.DataInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;

import ostrowski.DebugBreak;

public class SerializableFactory
{

   static HashMap<String, Class<?>> _classMap = new HashMap<>();
   static HashMap<Class<?>, String> _keyMap   = new HashMap<>();

   static {
      registerClass("ClinID", ClientID.class);
      registerClass("ObjChg", ObjectChanged.class);
      registerClass("ObjDel", ObjectDelete.class);
      registerClass("ObjInf", ObjectInfo.class);
      registerClass("ReqOpt", RequestOption.class);
      registerClass("Respns", Response.class);
   }

   public static void registerClass(String key, Class<?> cls) {
      boolean success = false;
      try {
         // make sure we can instantiate a new instance without any parameters.
         cls.getDeclaredConstructor().newInstance();
         if (_classMap.get(key) != null) {
            throw new IllegalArgumentException("Key " + key + " already used for " + _classMap.get(key));
         }
         if (_keyMap.get(cls) != null) {
            throw new IllegalArgumentException("class " + cls + " already mapped with key " + _keyMap.get(cls));
         }
         success = true;
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
         e.printStackTrace();
      }
      if (!success) {
         DebugBreak.debugBreak();
      }
      _classMap.put(key, cls);
      _keyMap.put(cls, key);
   }

   public static String getKey(SerializableObject serObj) {
      Class<?> serClass = serObj.getClass();
      String res = (_keyMap.get(serClass));
      while (res == null) {
         Type superClass = serClass.getGenericSuperclass();
         if (!(superClass instanceof Class) || superClass.equals(SerializableObject.class)) {
            String message = "class " + serObj.getClass().getName() + " is not registered in factory map.";
            DebugBreak.debugBreak(message);
            throw new UnsupportedOperationException(message);
         }
         serClass = (Class<?>) superClass;
         res = (_keyMap.get(serClass));
      }
      return res;
   }

   public static SerializableObject readObject(String eventID, DataInputStream inMsg)
   {
      Class<?> objClass = _classMap.get(eventID);
      if (objClass != null)
      {
         try {
            Object newObj = objClass.getDeclaredConstructor().newInstance();
            if (newObj instanceof SerializableObject) {
               SerializableObject newSerObj = (SerializableObject) newObj;
               newSerObj.serializeFromStream(inMsg);
               return newSerObj;
            }
            System.err.println("object " + newObj.getClass().toString()
                               + " is not derived from SerializableObject!");
         } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
         }
      }
      DebugBreak.debugBreak("unable to create " + eventID);
      return null;
   }

}
