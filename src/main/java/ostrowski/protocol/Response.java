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
   private int    synchID    = -1;
   private int    answerID  = -1;
   private String answerStr = "";

   public Response() {}

   public Response(ISynchronizedRequest request) {
      synchID = request.getSyncKey();
      if (request instanceof SyncRequest) {
         SyncRequest syncReq = (SyncRequest) request;
         answerID = syncReq.getFullAnswerID();
         answerStr = syncReq.getAnswer();
      }
   }

   public void setAnswerKey(Integer answer) {
      answerID = answer;
   }

   public void setAnswerStr(String answer) {
      answerStr =  answer;
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         synchID = readInt(in);
         answerID = readInt(in);
         answerStr = readString(in);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(synchID, out);
         writeToStream(answerID, out);
         writeToStream(answerStr, out);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   //@Override from ISynchronizedResponse
   @Override
   public int getSyncKey() {
      return synchID;
   }

   public int getFullAnswerID() {
      return answerID;
   }

   public void setFullAnswerID(int answerID) {
      this.answerID = answerID;
   }

   public String getAnswerStr() {
      return answerStr;
   }

   @Override
   public String toString() {
      return "Response: SynchKey=" + synchID + ", answerID=" + answerID + ", answerStr=" + answerStr;
   }

}
