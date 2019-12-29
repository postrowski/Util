/*
 * Created on Dec 24, 2006
 *
 */

package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ostrowski.util.sockets.ISynchronizedRequest;
import ostrowski.util.sockets.ISynchronizedResponse;

public class Response extends SerializableObject implements ISynchronizedResponse
{
   private int _synchID      = -1;
   private int _answerID     = -1;
   private String _answerStr = "";

   public Response() {}

   public Response(ISynchronizedRequest request) {
      _synchID = request.getSyncKey();
      if (request instanceof SyncRequest) {
         SyncRequest syncReq = (SyncRequest) request;
         _answerID  = syncReq.getFullAnswerID();
         _answerStr = syncReq.getAnswer();
      }
   }

   public void setAnswerKey(Integer answer) {
      _answerID = answer;
   }

   public void setAnswerStr(String answer) {
      _answerStr =  answer;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _synchID = readInt(in);
         _answerID = readInt(in);
         _answerStr = readString(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_synchID, out);
         writeToStream(_answerID, out);
         writeToStream(_answerStr, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   //@Override from ISynchronizedResponse
   @Override
   public int getSyncKey() {
      return _synchID;
   }

   public int getFullAnswerID() {
      return _answerID;
   }

   public void setFullAnswerID(int answerID) {
      _answerID = answerID;
   }

   public String getAnswerStr() {
      return _answerStr;
   }

   @Override
   public String toString() {
      return "Response: SynchKey=" + _synchID+ ", answerID="+_answerID + ", answerStr="+_answerStr;
   }

}
