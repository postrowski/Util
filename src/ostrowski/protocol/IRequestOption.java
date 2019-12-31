package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface IRequestOption
{
   String getName();

   int getIntValue();

   boolean isEnabled();

   void setAnswerStr(String value);

   void setAnswerID(int i);

   void setEnabled(boolean b);

   void serializeToStream(DataOutputStream out);

   void serializeFromStream(DataInputStream in);
}
