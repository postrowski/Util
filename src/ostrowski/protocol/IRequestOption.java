package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface IRequestOption
{
   public String getName();

   public int getIntValue();

   public boolean isEnabled();

   public void setAnswerStr(String value);

   public void setAnswerID(int i);

   public void setEnabled(boolean b);

   public void serializeToStream(DataOutputStream out);

   public void serializeFromStream(DataInputStream in);
}
