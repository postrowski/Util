package ostrowski.protocol;

import java.io.DataInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;

import ostrowski.DebugBreak;

public class SerializableFactory
{

   static final HashMap<String, Class<? extends SerializableObject>> CLASS_MAP = new HashMap<>();
   static final HashMap<Class<? extends SerializableObject>, String> KEY_MAP   = new HashMap<>();

   static {
      registerClass("ClinID", ClientID.class);
      registerClass("ObjChg", ObjectChanged.class);
      registerClass("ObjDel", ObjectDelete.class);
      registerClass("ObjInf", ObjectInfo.class);
      registerClass("ReqOpt", RequestOption.class);
      registerClass("Respns", Response.class);
   }

   public static void registerClass(String key, Class<? extends SerializableObject> cls) {
      boolean success = false;
      try {
         // make sure we can instantiate a new instance without any parameters.
         cls.getDeclaredConstructor().newInstance();
         if (CLASS_MAP.get(key) != null) {
            throw new IllegalArgumentException("Key " + key + " already used for " + CLASS_MAP.get(key));
         }
         if (KEY_MAP.get(cls) != null) {
            throw new IllegalArgumentException("class " + cls + " already mapped with key " + KEY_MAP.get(cls));
         }
         success = true;
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
         e.printStackTrace();
      }
      if (!success) {
         DebugBreak.debugBreak();
      }
      CLASS_MAP.put(key, cls);
      KEY_MAP.put(cls, key);
   }

   public static String getKey(SerializableObject serObj) {
      Class<? extends SerializableObject> serClass = serObj.getClass();
      String res = (KEY_MAP.get(serClass));
      while (res == null) {
         Type superClass = serClass.getGenericSuperclass();
         if (!(superClass instanceof Class) || superClass.equals(SerializableObject.class)) {
            String message = "class " + serClass.getName() + " is not registered in factory map.";
            DebugBreak.debugBreak(message);
            throw new UnsupportedOperationException(message);
         }
         serClass = (Class<? extends SerializableObject>) superClass;
         res = (KEY_MAP.get(serClass));
      }
      return res;
   }

   public static SerializableObject readObject(String eventID, DataInputStream inMsg)
   {
      Class<?> objClass = CLASS_MAP.get(eventID);
      if (objClass != null)
      {
         try {
            Object newObj = objClass.getDeclaredConstructor().newInstance();
            if (newObj instanceof SerializableObject) {
               SerializableObject newSerObj = (SerializableObject) newObj;
               newSerObj.serializeFromStream(inMsg);
               return newSerObj;
            }
            System.err.println("object " + newObj.getClass()
                               + " is not derived from SerializableObject!");
         } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
         }
      }
      DebugBreak.debugBreak("unable to create " + eventID);
      return null;
   }

}
