/*
 * Created on May 10, 2006
 *
 */
package ostrowski.util.sockets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

import ostrowski.protocol.Response;
import ostrowski.protocol.SerializableFactory;
import ostrowski.protocol.SerializableObject;
import ostrowski.protocol.SyncRequest;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

public abstract class SocketConnector extends Thread
{
   public abstract void processReceivedObject(SerializableObject inObj);

   public abstract void handleDisconnect(SocketConnector diconnectedConnection);

   public abstract void handleConnect(SocketConnector connectedConnection);

   public abstract void diag(String message);

   final boolean connected;
   boolean          running;
   Socket           socket       = null;
   DataInputStream  inputStream  = null;
   DataOutputStream outputStream = null;
   static final HashMap<Integer, SyncRequest> syncMap          = new HashMap<>();
   final        Semaphore                     lock_inputStream = new Semaphore("SocketConnector", Semaphore.CLASS_SOCKETCONNECTOR);

   public SocketConnector(String threadName)
   {
      super(threadName);
      connected = false;
      running = false;
   }

   public void connect(String ipAddress, int port)
   {
      try {
         socket = new Socket(ipAddress, port);
         initStreams();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void setSocket(Socket socket)
   {
      this.socket = socket;
      initStreams();
   }

   public void initStreams()
   {
      if (socket != null) {
         diag("connected to " + socket.getInetAddress().getHostAddress());
         try {
            socket.setSoLinger(true, 10/*linger_timeout_in_seconds*/);
            outputStream = new DataOutputStream(socket.getOutputStream());
            inputStream = new DataInputStream(socket.getInputStream());
         } catch (IOException e) {
            e.printStackTrace();
         }
         handleConnect(this);
      }
      else {
         handleDisconnect(this);
      }
   }

   public void shutdown()
   {
      running = false;
      try {
         socket.shutdownInput();
         socket.shutdownOutput();
         socket.close();
         inputStream.close();
         synchronized (inputStream) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_inputStream)) {
               inputStream.notifyAll();
            }
         }
         this.interrupt();
      } catch (IOException e) {
      }
   }

   @Override
   public void run()
   {
      running = true;
      diag("running in thread " + getName());
      if (socket != null) {
         try {
            while (running) {
               readAndProcessMessageFromSocket();
            }
         } catch (IOException e) {
         }
      }
      handleDisconnect(this);
      diag("terminating thread " + getName());
   }

   static boolean FULL_BUFFER_DUMP_TO_DIAG = false;

   private void readAndProcessMessageFromSocket() throws IOException
   {
      // Receive the ID of the incoming event from the client and
      // create an event object of the appropriate type with data
      // from the stream
      int msgSize = inputStream.readInt();
      byte[] msgBuf = new byte[msgSize];
      inputStream.readFully(msgBuf);
      byte[] diagBuf = new byte[msgSize+4];
      diagBuf[0] = (byte)(msgSize >>> 24);
      diagBuf[1] = (byte)(msgSize >>> 16);
      diagBuf[2] = (byte)(msgSize >>>  8);
      diagBuf[3] = (byte)(msgSize >>>  0);
      System.arraycopy(msgBuf, 0, diagBuf, 4, msgSize);
      if (FULL_BUFFER_DUMP_TO_DIAG) {
         StringBuilder sb = new StringBuilder();
         sb.append("received object:");
         appendByteBufferDump(sb, diagBuf);
         diag(sb.toString());
      }

      try (ByteArrayInputStream inBuf = new ByteArrayInputStream(msgBuf);
           DataInputStream inStream = new DataInputStream(inBuf))
      {
         // Receive the ID of the incoming event from the client and
         // create an event object of the appropriate type with data
         // from the stream
         String eventID = SerializableObject.readString(inStream);
         SerializableObject inObj = SerializableFactory.readObject(eventID, inStream);
         if (inObj != null) {
            diag("received object: " + inObj);
            boolean handled = false;
            SyncRequest origObj = null;
            if (inObj instanceof Response) {
               Response response = (Response) inObj;
               origObj = syncMap.remove(response.getSyncKey());
               origObj.setFullAnswerID(response.getFullAnswerID());
            }
            if (inObj instanceof SyncRequest) {
               SyncRequest newReq = (SyncRequest) inObj;
               origObj = syncMap.remove(newReq.getSyncKey());
               if (origObj != null) {
                  diag("original Object found.");
                  origObj.copyAnswer(newReq);
                  origObj.setResponse(newReq);
               }
            }
            if (origObj != null) {
               synchronized (origObj) {
                  origObj.notifyAll();
               }
               List<SyncRequest> resultsQueue = origObj.getResultsQueue();
               if (resultsQueue != null) {
                  synchronized (resultsQueue) {
                     diag("about to lock results Queue:" + resultsQueue);
                     diag("locked results Queue:" + resultsQueue);
                     resultsQueue.add(origObj);
                     resultsQueue.notifyAll();
                     diag("about to release lock of results Queue ");
                  }
               }
               else {
                  diag("no results Queue found.");
               }
               handled = true;
            }
            if (!handled) {
               processReceivedObject(inObj);
            }
         }
      }
   }

   public boolean sendObject(SerializableObject objToSend, String target)
   {
      // Get everything were going to send into a ByteBuffer first
      // so that we can send it all in one TCP/IP block, which
      // reduces the overhead of sending the data.
      try (ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
           DataOutputStream outStream = new DataOutputStream(outBuf))
      {
         String key = SerializableFactory.getKey(objToSend);
         SerializableObject.writeToStream(key, outStream);
         if (objToSend instanceof SyncRequest) {
            SyncRequest actReq = (SyncRequest) objToSend;
            // Only track the request message, not the response going back.
            if (!actReq.isAnswered()) {
               syncMap.put(actReq.getSyncKey(), actReq);
            }
         }
         objToSend.serializeToStream(outStream);
         byte[] dataArray = outBuf.toByteArray();
         int msgSize = dataArray.length;
         byte[] newBuf = new byte[msgSize+4];
         newBuf[0] = (byte)(msgSize >>> 24);
         newBuf[1] = (byte)(msgSize >>> 16);
         newBuf[2] = (byte)(msgSize >>>  8);
         newBuf[3] = (byte)(msgSize >>>  0);
         System.arraycopy(dataArray, 0, newBuf, 4, msgSize);
         if (FULL_BUFFER_DUMP_TO_DIAG) {
            StringBuilder sb = new StringBuilder();
            sb.append("sending to ").append(target).append(":");
            appendByteBufferDump(sb, newBuf);
            diag(sb.toString());
         }

         long timeStart = System.currentTimeMillis();
         outputStream.write(newBuf);
         long duration = System.currentTimeMillis() - timeStart;
         diag("sent object: (" + duration + "ms) to " + target + ": " + objToSend);
         return true;
      } catch (IOException e) {
         e.printStackTrace();
      }
      return false;
   }

   static final String NUMBERS = "0123456789ABCDEF";
   private static void appendByteBufferDump(StringBuilder sb, byte[] dataArray)
   {
      StringBuilder ascii = new StringBuilder();
      for (int i=0 ; ((i<dataArray.length) || ((i%32) != 0)) ; i++) {
         if ((i%4) == 0) {
            sb.append(' ');
            if ((i%8) == 0) {
               sb.append(' ');
               if ((i%32) == 0) {
                  sb.append(ascii).append("\n");
                  ascii.setLength(0);
               }
               ascii.append(' ');
            }
         }
         if (i<dataArray.length) {
            if ((dataArray[i]>=0x20) && (dataArray[i] <=0x7F)) {
               ascii.append((char)(dataArray[i]));
            }
            else {
               ascii.append('-');
            }
            sb.append(NUMBERS.charAt((dataArray[i] >> 4) & 0x0F));
            sb.append(NUMBERS.charAt(dataArray[i] & 0x0F));
         }
         else {
            sb.append("  ");
         }
//       if (i%512 == 0) {
//       sb.append("...");
//       break;
//       }
      }
      sb.append("  ").append(ascii);
   }
}
