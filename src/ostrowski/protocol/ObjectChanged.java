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
   SerializableObject _originalObject;
   SerializableObject _modifiedObject;

   public ObjectChanged() {
   }

   public ObjectChanged(SerializableObject origObj, SerializableObject modObj) {
      _originalObject = origObj;
      _modifiedObject = modObj;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _originalObject = SerializableFactory.readObject(readString(in), in);
         _modifiedObject = SerializableFactory.readObject(readString(in), in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(SerializableFactory.getKey(_originalObject), out);
         _originalObject.serializeToStream(out);
         writeToStream(SerializableFactory.getKey(_modifiedObject), out);
         _modifiedObject.serializeToStream(out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public SerializableObject getOriginalObj() {
      return _originalObject;
   }

   public SerializableObject getModifiedObj() {
      return _modifiedObject;
   }

}
