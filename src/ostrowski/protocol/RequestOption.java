package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RequestOption extends SerializableObject implements IRequestOption
{
   private String  _name;
   private int     _value;
   private boolean _enabled;

   public RequestOption() {}

   public RequestOption(String name, int value, boolean enabled) {
      this._name = name;
      this._value = value;
      this._enabled = enabled;
   }

   @Override
   public String getName() {
      return this._name;
   }

   @Override
   public int getIntValue() {
      return this._value;
   }

   @Override
   public boolean isEnabled() {
      return this._enabled;
   }

   @Override
   public void serializeToStream(DataOutputStream out) {
      try {
         writeToStream(_name, out);
         writeToStream(_value, out);
         writeToStream(_enabled, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in) {
      try {
         _name         = readString(in);
         _value        = readInt(in);
         _enabled      = readBoolean(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void setAnswerStr(String name) {
      this._name = name;
   }

   @Override
   public void setAnswerID(int value) {
      this._value = value;
   }

   @Override
   public void setEnabled(boolean enabled) {
      this._enabled = enabled;
   }
}
