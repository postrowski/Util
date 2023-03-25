/*
 * Created on Dec 1, 2006
 *
 */
package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectInfo extends SerializableObject
{
   SerializableObject object;

   public ObjectInfo() {
   }

   public ObjectInfo(SerializableObject obj) {
      object = obj;
   }

   public SerializableObject getObject() {
      return object;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         String key = readString(in);
         object = SerializableFactory.readObject(key, in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      String key = SerializableFactory.getKey(object);
      try {
         writeToStream(key, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
      object.serializeToStream(out);
   }

}
