/*
 * Created on Dec 1, 2006
 *
 */

package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectDelete extends SerializableObject
{
   SerializableObject _object;

   public ObjectDelete() {
   }

   public ObjectDelete(SerializableObject obj) {
      _object = obj;
   }

   public SerializableObject getObject() {
      return _object;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         String key = readString(in);
         _object = SerializableFactory.readObject(key, in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      String key = SerializableFactory.getKey(_object);
      try {
         writeToStream(key, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
      _object.serializeToStream(out);
   }
}
