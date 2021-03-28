package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestOption extends SerializableObject implements IRequestOption
{
   private String  name;
   private int     value;
   private boolean enabled;

   public RequestOption() {}

   public RequestOption(String name, int value, boolean enabled) {
      this.name = name;
      this.value = value;
      this.enabled = enabled;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public int getIntValue() {
      return this.value;
   }

   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(name, out);
         writeToStream(value, out);
         writeToStream(enabled, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         name = readString(in);
         value = readInt(in);
         enabled = readBoolean(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void setAnswerStr(String name) {
      this.name = name;
   }

   @Override
   public void setAnswerID(int value) {
      this.value = value;
   }

   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
