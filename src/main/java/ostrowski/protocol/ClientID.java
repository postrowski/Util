/*
 * Created on Jun 1, 2006
 *
 */

package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientID extends SerializableObject
{
   private int id = -1;

   public ClientID() {}

   public ClientID(int id) { this.id = id;}

   public int getID() { return id;}

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         out.writeInt(id);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         id = in.readInt();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public String toString()
   {
      return "ClientID: " + id;
   }

}
