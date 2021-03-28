/*
 * Created on Dec 1, 2006
 *
 */

package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectChanged extends SerializableObject
{
   SerializableObject originalObject;
   SerializableObject modifiedObject;

   public ObjectChanged() {
   }

   public ObjectChanged(SerializableObject origObj, SerializableObject modObj) {
      originalObject = origObj;
      modifiedObject = modObj;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         originalObject = SerializableFactory.readObject(readString(in), in);
         modifiedObject = SerializableFactory.readObject(readString(in), in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(SerializableFactory.getKey(originalObject), out);
         originalObject.serializeToStream(out);
         writeToStream(SerializableFactory.getKey(modifiedObject), out);
         modifiedObject.serializeToStream(out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public SerializableObject getOriginalObj() {
      return originalObject;
   }

   public SerializableObject getModifiedObj() {
      return modifiedObject;
   }

   @Override
   public String toString() {
      return "ObjectChanged. originalObject = '" + originalObject +
             "'\n modifiedObject = '" + modifiedObject + "'";
   }
}
