/*
 * Created on May 10, 2006
 *
 */
package ostrowski.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

import ostrowski.DebugBreak;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;
import ostrowski.util.sockets.ISynchronizedRequest;
import ostrowski.util.sockets.ISynchronizedResponse;

public abstract class SyncRequest extends SerializableObject implements ISynchronizedRequest, ISynchronizedResponse
{
   protected static int                  nextMessageKey = 1;
   protected        int                  syncKey        = nextMessageKey++;
   protected        String               message        = "";
   protected        IRequestOption       answer         = null;
   protected        List<IRequestOption> options        = new ArrayList<>();
   protected        int                  defaultID      = -1;
   protected        List<SyncRequest>    resultsQueue   = null;
   protected        boolean              backupSelected = false;

   public final Semaphore lockThis = new Semaphore("SyncRequest", Semaphore.CLASS_SYNCHREQUEST);

   public static final int OPT_CANCEL_ACTION = -2;
   public static final int ACTION_NONE       = 0;

   public SyncRequest() {
       init();
   }

   public void init()
   {
   }

   public boolean isCancelable() {
      return getEnabledCount(true) != 1;
   }

   public boolean isCancel() {  return (answer != null) && (answer.getIntValue() == OPT_CANCEL_ACTION); }

   public void setResultsQueue(List<SyncRequest> resultsQueue) { this.resultsQueue = resultsQueue;}
   public List<SyncRequest> getResultsQueue() { return resultsQueue;}
   public void setSyncKey(int messageKey)    { syncKey = messageKey; }
   //@Override from ISynchronizedRequest
   @Override
   public int  getSyncKey()                  { return syncKey; }
   public String getMessage()                { return message; }
   public void setMessage(String message)    { this.message = message; }
   public void addOption(IRequestOption option) {
      // never add two options with the same ID (unless it is disabled, as is the case for the \n entries)
      if (option.getIntValue() != -1) {
         for (IRequestOption opt : options) {
            if (opt.getIntValue() == option.getIntValue()) {
               if (opt.getIntValue() != OPT_CANCEL_ACTION) {
                  DebugBreak.debugBreak("Duplicate option added to SyncRequest.");
               }
               return;
            }
         }
      }
      options.add(option);
      if (option.getIntValue() != -1) {
         String allowedKeyStrokes = getAllowedKeyStrokesForOption(option.getIntValue());
         if (allowedKeyStrokes != null) {
            setAnswerIdForKeyStroke(allowedKeyStrokes, option);
         }
      }
   }
   @Deprecated
   public void addOption(int optionID, String optionStr, boolean enabled) {
      addOption(new RequestOption(optionStr, optionID, enabled));
   }
   public void addSeparatorOption() {
      addOption(new RequestOption("\n", -1, false));
   }

   public synchronized void setAnswerByOptionIndex(int i) {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         answer = options.get(i);
      }
   }
   public synchronized void setCustAnswer(String answer)  {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         this.answer = new RequestOption(answer, -1, true);
      }
   }
   public synchronized boolean setAnswerID(int answerID )    {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         for (IRequestOption reqOpt : options) {
            if (reqOpt.getIntValue() == answerID) {
               if (reqOpt.isEnabled()) {
                  answer = reqOpt;
                  return true;
               }
            }
         }
         IRequestOption singleEnabledOpt = null;
         for (IRequestOption reqOpt : options) {
            if (reqOpt.isEnabled()) {
               if (singleEnabledOpt != null) {
                  // more than one option is enabled!
                  return false;
               }
               singleEnabledOpt = reqOpt;
            }
         }
         if (singleEnabledOpt != null) {
            answer = singleEnabledOpt;
            return true;
         }
         return false;
      }
   }
   public void copyAnswer(SyncRequest source) {
      setAnswerID(source.getAnswerID());
   }
   public void setDefaultOption(IRequestOption defaultOpt) {
      defaultID = (defaultOpt == null) ? -1 : defaultOpt.getIntValue();
   }
   @Deprecated
   public void setDefaultOption(int defaultID) { this.defaultID = defaultID; }
   public String getAnswer()                   { return (answer == null) ? null : answer.getName(); }
   public int   getAnswerID()                  { return (answer == null) ? -1 : answer.getIntValue() ; }
   public boolean isAnswered()                 { return answer != null;}
   public IRequestOption answer()              { return answer;}
   public int   getDefaultIndex()
   {
      for (int i = 0; i < options.size() ; i++) {
         if (defaultID == options.get(i).getIntValue()) {
            return i;
         }
      }
      return -1;
   }
   public String[] getOptions()
   {
      String[] result = new String[options.size()];
      for (int i = 0; i < options.size() ; i++) {
         result[i] = options.get(i).getName();
      }
      return result;
   }
   public int[] getOptionIDs()
   {
      int[] result = new int[options.size()];
      for (int i = 0; i < options.size() ; i++) {
         result[i] = options.get(i).getIntValue();
      }
      return result;
   }
   public IRequestOption[] getReqOptions()
   {
      IRequestOption[] result = new IRequestOption[options.size()];
      for (int i = 0; i < options.size() ; i++) {
         result[i] = options.get(i);
      }
      return result;
   }
   public boolean[] getEnableds() {
      boolean[] result = new boolean[options.size()];
      for (int i = 0; i < options.size() ; i++) {
         result[i] = options.get(i).isEnabled();
      }
      return result;
   }

   public int getActionCount()
   {
      return options.size();
   }
   public int getEnabledCount(boolean includeCancelAction) {
      int count = 0;
      for (IRequestOption reqOpt : options) {
         if (reqOpt.isEnabled()) {
            if (includeCancelAction || (reqOpt.getIntValue() != OPT_CANCEL_ACTION)) {
               count++;
            }
         }
      }
      return count;
   }
   public String getSingleEnabledAction() {
      String name = null;
      for (IRequestOption reqOpt : options) {
         if (reqOpt.isEnabled()) {
            if (reqOpt.getIntValue() != OPT_CANCEL_ACTION) {
               if (name != null) {
                  return null; // multiple enable answers!
               }
               name = reqOpt.getName();
            }
         }
      }
      return name;
   }
   public boolean selectSingleEnabledEntry(boolean ignoreCancel) {
      IRequestOption singleEntry = null;
      for (IRequestOption reqOpt : options) {
         if (reqOpt.isEnabled()) {
            if (!ignoreCancel || (reqOpt.getIntValue() != OPT_CANCEL_ACTION)) {
               if (singleEntry != null) {
                  // more than one entry is enabled
                  return false;
               }
               singleEntry = reqOpt;
            }
         }
      }
      if (singleEntry == null) {
         // No entries are enabled!
         // Add a 'do nothing' option, and select it
         addOption(new RequestOption("no action", ACTION_NONE, true));
         setAnswerByOptionIndex(0);
         return true;
      }
      // exactly one is enabled, select it
      answer = singleEntry;
      return true;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(syncKey, out);
         writeToStream(message, out);
         writeToStream(options, out);
         writeToStream(defaultID, out);
         writeToStream((answer instanceof SerializableObject), out);
         if (answer != null) {
            writeToStream(SerializableFactory.getKey((SerializableObject) answer), out);
            answer.serializeToStream(out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         syncKey = readInt(in);
         message = readString(in);
         options = new ArrayList<>();
         List<SerializableObject> options = readIntoListSerializableObject(in);
         for (SerializableObject opt : options) {
            if (opt instanceof IRequestOption) {
               this.options.add((IRequestOption)opt);
            }
         }
         defaultID = readInt(in);
         boolean hasAnswer = readBoolean(in);
         answer = null;
         if (hasAnswer) {
            answer = (IRequestOption) SerializableFactory.readObject(readString(in), in);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public synchronized void copyDataInto(SyncRequest newObj)
   {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lockThis)) {
         // TODO: deep copy of options?
         newObj.syncKey = syncKey;
         newObj.message = message;
         newObj.options = new ArrayList<>();
         newObj.options.addAll(options);
         newObj.defaultID = defaultID;
         newObj.answer = answer;
      }
   }
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("SyncRequest: ");
      sb.append("SyncKey: ").append(syncKey);
      sb.append(", Message: ").append(getMessage());
      for (IRequestOption reqOpt : options) {
         sb.append("\n   ").append((reqOpt).getIntValue());
         sb.append(": ").append(reqOpt.getName());
         if (!reqOpt.isEnabled()) {
            sb.append(" (disabled)");
         }
      }
      sb.append("\nAnswer: ").append((answer == null) ? "-1" : answer.toString());
      sb.append("\nDefault: ").append((defaultID >= 0) ? String.valueOf(defaultID) : "-1");
      sb.append("\nresultsQueue=");
      if (resultsQueue == null) {
         sb.append("null");
      }
      else {
         sb.append(resultsQueue.hashCode());
      }

      return sb.toString();
   }

   //@Override from ISynchronizedRequest
   @Override
   public void setResponse(ISynchronizedResponse response)
   {
      if (response instanceof SyncRequest) {
         SyncRequest respAction = (SyncRequest) response;
         answer = new RequestOption(respAction.getAnswer(), respAction.getAnswerID(), true);
      }
      else if (response instanceof Response) {
         Response resp = (Response) response;
         answer = new RequestOption(resp.getAnswerStr(), resp.getFullAnswerID(), true);
      }
      // Notify any thread waiting for this response.
      synchronized (this) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(this.lockThis)) {
            notifyAll();
         }
      }
      // If a results queue has been set up, then put this object into it
      // This method is used when one thread is waiting on multiple responses
      // so the waiting thread waits on the queue instead.
      List<SyncRequest> resultsQueue = getResultsQueue();
      if (resultsQueue != null) {
         synchronized (resultsQueue) {
            resultsQueue.add(this);
            resultsQueue.notifyAll();
         }
      }
   }

   public int getAnswerIndex()
   {
      for (int index = 0; index < options.size() ; index++) {
         if (options.get(index).getIntValue() == getFullAnswerID()) {
            return index;
         }
      }
      return -1;
   }

   public boolean is_backupSelected() {
      return backupSelected;
   }

   public void set_backupSelected(boolean selected) {
      backupSelected = selected;
      if (selected) {
         answer = null;
      }
   }

   public boolean isSameQuestion(SyncRequest other) {
      // compare everything but the answers, because this is used by the backup
      // functionality to compare an unanswered question with an answered one.
      if (other == null) {
         return false;
      }
      if (!message.equals(other.message)) {
         return false;
      }
      if (options.size() != other.options.size()) {
         return false;
      }
      for (int i = 0; i < options.size() ; i++) {
         if (!options.get(i).equals(other.options.get(i))) {
            return false;
         }
      }
      return true;
   }

   /**
    * this method is called to get a complete answer as a single ID, which
    * will be used to set the Response value, and will be copied into the
    * server-side Request object once its been received.
    * @return The integer answer ID
    */
   public int getFullAnswerID() {
      return getAnswerID();
   }

   public void setFullAnswerID(int answerKey) {
      setAnswerID(answerKey);
   }

   public boolean keyPressed(KeyEvent arg0) {
      Integer value = getOptionIDForKeystroke(arg0.keyCode, arg0.stateMask);
      if (value == null) {
         return false;
      }
      return setAnswerID(value);
   }

   public final HashMap<java.lang.Character, IRequestOption> mapCharToOption     = new HashMap<>();
   public final HashMap<java.lang.Character, IRequestOption> ctrlMapCharToOption = new HashMap<>();
   public final HashMap<java.lang.Character, IRequestOption> altMapCharToOption  = new HashMap<>();

   public Integer getOptionIDForKeystroke(int key, int mask) {
      char charKey = (char)key;
      if (charKey == SWT.ESC) {
         for (IRequestOption reqOpt : options) {
            if (reqOpt.getIntValue() == OPT_CANCEL_ACTION) {
               return OPT_CANCEL_ACTION;
            }
         }
      }
      if ((mask & SWT.SHIFT) != 0) {
         charKey = Character.toUpperCase(charKey);
      }
      if ((mask & SWT.CTRL) != 0) {
         if (ctrlMapCharToOption.containsKey(charKey)) {
            return ctrlMapCharToOption.get(charKey).getIntValue();
         }
      }
      else if ((mask & SWT.ALT) != 0) {
         if (altMapCharToOption.containsKey(charKey)) {
            return altMapCharToOption.get(charKey).getIntValue();
         }
      }
      else if (mapCharToOption.containsKey(charKey)) {
         return mapCharToOption.get(charKey).getIntValue();
      }
      return null;
   }

   public boolean setAnswerIdForKeyStroke(String allowedCharsIn, IRequestOption option) {
      char key;
      boolean ctrlKey = false;
      boolean altKey = false;
      String allowedChars = allowedCharsIn;
      while (allowedChars.length() > 0) {
         if (allowedChars.toLowerCase().startsWith("<alt>")) {
            allowedChars = allowedChars.substring("<alt>".length());
            altKey = true;
         }
         if (allowedChars.toLowerCase().startsWith("<ctrl>")) {
            allowedChars = allowedChars.substring("<ctrl>".length());
            ctrlKey = true;
         }
         key = allowedChars.charAt(0);
         allowedChars = allowedChars.substring(1);

         HashMap<java.lang.Character, IRequestOption> map = mapCharToOption;
         if (ctrlKey) {
            map = ctrlMapCharToOption;
         }
         else if (altKey) {
            map = altMapCharToOption;
         }
         if (map.get(key) == null) {
            map.put(key, option);
            return true;
         }
      }
      return false;
   }

   public String getStringOfKeyStrokeAssignedToOption(int optionIndex) {
      IRequestOption option = options.get(optionIndex);
      if (option.getIntValue() == OPT_CANCEL_ACTION) {
         return "Esc";
      }
      Set<Entry<Character, IRequestOption>> pairs = mapCharToOption.entrySet();
      for (Entry<Character, IRequestOption> pair : pairs) {
         if (pair.getValue() == option) {
            return pair.getKey().toString();
         }
      }
      pairs = altMapCharToOption.entrySet();
      for (Entry<Character, IRequestOption> pair : pairs) {
         if (pair.getValue() == option) {
            return "<alt>-" + pair.getKey();
         }
      }
      pairs = ctrlMapCharToOption.entrySet();
      for (Entry<Character, IRequestOption> pair : pairs) {
         if (pair.getValue() == option) {
            return "<ctrl>-" + pair.getKey();
         }
      }
      return "";
   }

   protected String getAllowedKeyStrokesForOption(int optionID) {
      return null;
   }

   public IRequestOption getRequestOptionByIntValue(int value) {
      for (IRequestOption reqOpt : options) {
         if (reqOpt.getIntValue() == value) {
            return reqOpt;
         }
      }
      return null;
   }
}
