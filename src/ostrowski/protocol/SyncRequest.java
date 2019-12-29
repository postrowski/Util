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
   protected static int                _nextMessageKey   = 1;
   protected int                       _syncKey          = _nextMessageKey++;
   protected String                    _message          = "";
   protected IRequestOption            _answer           = null;
   protected ArrayList<IRequestOption> _options          = new ArrayList<>();
   protected int                       _defaultID        = -1;
   protected List<SyncRequest>         _resultsQueue     = null;
   protected boolean                   _backupSelected   = false;

   public Semaphore _lockThis = new Semaphore("SyncRequest", Semaphore.CLASS_SYNCHREQUEST);

   public static final int OPT_CANCEL_ACTION  = -2;
   public static final int ACTION_NONE  = 0;

   public SyncRequest() {
       init();
   }

   public void init()
   {
   }

   public boolean isCancelable() {
      return getEnabledCount(true) != 1;
   }

   public boolean isCancel() {  return (_answer != null) && (_answer.getIntValue() == OPT_CANCEL_ACTION); }

   public void setResultsQueue(List<SyncRequest> resultsQueue) { _resultsQueue = resultsQueue;}
   public List<SyncRequest> getResultsQueue() { return _resultsQueue;}
   public void setSyncKey(int messageKey)    { _syncKey = messageKey; }
   //@Override from ISynchronizedRequest
   @Override
   public int  getSyncKey()                  { return _syncKey; }
   public String getMessage()                { return _message; }
   public void setMessage(String message)    { _message = message; }
   public void addOption(IRequestOption option) {
      // never add two options with the same ID (unless it is disabled, as is the case for the \n entries)
      if (option.getIntValue() != -1) {
         for (IRequestOption opt : _options) {
            if (opt.getIntValue() == option.getIntValue()) {
               if (opt.getIntValue() != OPT_CANCEL_ACTION) {
                  DebugBreak.debugBreak("Duplicate option added to SyncRequest.");
               }
               return;
            }
         }
      }
      _options.add(option);
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
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         _answer = _options.get(i);
      }
   }
   public synchronized void setCustAnswer(String answer)  {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         _answer = new RequestOption(answer, -1, true);
      }
   }
   public synchronized boolean setAnswerID(int answerID )    {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         for (IRequestOption reqOpt : _options) {
            if (reqOpt.getIntValue() == answerID) {
               if (reqOpt.isEnabled()) {
                  _answer = reqOpt;
                  return true;
               }
            }
         }
         IRequestOption singleEnabledOpt = null;
         for (IRequestOption reqOpt : _options) {
            if (reqOpt.isEnabled()) {
               if (singleEnabledOpt != null) {
                  // more than one option is enabled!
                  return false;
               }
               singleEnabledOpt = reqOpt;
            }
         }
         if (singleEnabledOpt != null) {
            _answer = singleEnabledOpt;
            return true;
         }
         return false;
      }
   }
   public void copyAnswer(SyncRequest source) {
      setAnswerID(source.getAnswerID());
   }
   public void setDefaultOption(IRequestOption defaultOpt) {
      _defaultID = (defaultOpt == null) ? -1 : defaultOpt.getIntValue();
   }
   @Deprecated
   public void setDefaultOption(int defaultID) { _defaultID = defaultID; }
   public String getAnswer()                   { return (_answer == null) ? null : _answer.getName(); }
   public int   getAnswerID()                  { return (_answer == null) ? -1 : _answer.getIntValue() ; }
   public boolean isAnswered()                 { return _answer != null;}
   public IRequestOption answer()              { return _answer;}
   public int   getDefaultIndex()
   {
      for (int i=0 ; i<_options.size() ; i++) {
         if (_defaultID == _options.get(i).getIntValue()) {
            return i;
         }
      }
      return -1;
   }
   public String[] getOptions()
   {
      String[] result = new String[_options.size()];
      for (int i=0 ; i<_options.size() ; i++) {
         result[i] = _options.get(i).getName();
      }
      return result;
   }
   public int[] getOptionIDs()
   {
      int[] result = new int[_options.size()];
      for (int i=0 ; i<_options.size() ; i++) {
         result[i] = _options.get(i).getIntValue();
      }
      return result;
   }
   public IRequestOption[] getReqOptions()
   {
      IRequestOption[] result = new IRequestOption[_options.size()];
      for (int i=0 ; i<_options.size() ; i++) {
         result[i] = _options.get(i);
      }
      return result;
   }
   public boolean[] getEnableds() {
      boolean[] result = new boolean[_options.size()];
      for (int i=0 ; i<_options.size() ; i++) {
         result[i] = _options.get(i).isEnabled();
      }
      return result;
   }

   public int getActionCount()
   {
      return _options.size();
   }
   public int getEnabledCount(boolean includeCancelAction) {
      int count = 0;
      for (IRequestOption reqOpt : _options) {
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
      for (IRequestOption reqOpt : _options) {
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
      for (IRequestOption reqOpt : _options) {
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
      _answer = singleEntry;
      return true;
   }

   @Override
   public void serializeToStream(DataOutputStream out)
   {
      try {
         writeToStream(_syncKey, out);
         writeToStream(_message, out);
         writeToStream(_options, out);
         writeToStream(_defaultID, out);
         writeToStream((_answer != null), out);
         if (_answer != null) {
            _answer.serializeToStream(out);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void serializeFromStream(DataInputStream in)
   {
      try {
         _syncKey         = readInt(in);
         _message         = readString(in);
         _options         = new ArrayList<>();
         ArrayList<SerializableObject> options = readIntoListSerializableObject(in);
         for (SerializableObject opt : options) {
            if (opt instanceof IRequestOption) {
               _options.add((IRequestOption)opt);
            }
         }
         _defaultID       = readInt(in);
         boolean hasAnswer = readBoolean(in);
         _answer = null;
         if (hasAnswer) {
            _answer.serializeFromStream(in);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public synchronized void copyDataInto(SyncRequest newObj)
   {
      try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(_lockThis)) {
         // TODO: deep copy of options?
         newObj._syncKey        = _syncKey;
         newObj._message        = _message;
         newObj._options        = new ArrayList<>();
         newObj._options.addAll(_options);
         newObj._defaultID      = _defaultID;
         newObj._answer         = _answer;
      }
   }
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("SyncRequest: ");
      sb.append("SyncKey: ").append(_syncKey);
      sb.append(", Message: ").append(getMessage());
      for (IRequestOption reqOpt : _options) {
         sb.append("\n   ").append((reqOpt).getIntValue());
         sb.append(": ").append(reqOpt.getName());
         if (!reqOpt.isEnabled()) {
            sb.append(" (disabled)");
         }
      }
      sb.append("\nAnswer: ").append((_answer == null) ? "-1" : _answer.toString());
      sb.append("\nDefault: ").append((_defaultID>=0) ? String.valueOf(_defaultID) : "-1");
      sb.append("\nresultsQueue=");
      if (_resultsQueue == null) {
         sb.append("null");
      }
      else {
         sb.append(_resultsQueue.hashCode());
      }

      return sb.toString();
   }

   //@Override from ISynchronizedRequest
   @Override
   public void setResponse(ISynchronizedResponse response)
   {
      if (response instanceof SyncRequest) {
         SyncRequest respAction = (SyncRequest) response;
         _answer = new RequestOption(respAction.getAnswer(), respAction.getAnswerID(), true);
      }
      else if (response instanceof Response) {
         Response resp = (Response) response;
         _answer = new RequestOption(resp.getAnswerStr(), resp.getFullAnswerID(), true);
      }
      // Notify any thread waiting for this response.
      synchronized (this) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(this._lockThis)) {
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
      for (int index = 0 ; index < _options.size() ; index++) {
         if (_options.get(index).getIntValue() == getFullAnswerID()) {
            return index;
         }
      }
      return -1;
   }

   public boolean is_backupSelected() {
      return _backupSelected;
   }

   public void set_backupSelected(boolean selected) {
      _backupSelected = selected;
      if (selected) {
         _answer = null;
      }
   }

   public boolean isSameQuestion(SyncRequest other) {
      // compare everything but the answers, because this is used by the backup
      // functionality to compare an unanswered question with an answered one.
      if (other == null) {
         return false;
      }
      if (!_message.equals(other._message)) {
         return false;
      }
      if (_options.size() != other._options.size()) {
         return false;
      }
      for (int i=0 ; i<_options.size() ; i++) {
         if (!_options.get(i).equals(other._options.get(i))) {
            return false;
         }
      }
      return true;
   }

   /**
    * this method is called to get a complete answer as a single ID, which
    * will be used to set the Response value, and will be copied into the
    * server-side Request object once its been received.
    * @return
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

   public HashMap<java.lang.Character, IRequestOption> _mapCharToOption = new HashMap<>();
   public HashMap<java.lang.Character, IRequestOption> _ctrlMapCharToOption = new HashMap<>();
   public HashMap<java.lang.Character, IRequestOption> _altMapCharToOption = new HashMap<>();

   public Integer getOptionIDForKeystroke(int key, int mask) {
      char charKey = (char)key;
      if (charKey == SWT.ESC) {
         for (IRequestOption reqOpt : _options) {
            if (reqOpt.getIntValue() == OPT_CANCEL_ACTION) {
               return OPT_CANCEL_ACTION;
            }
         }
      }
      if ((mask & SWT.SHIFT) != 0) {
         charKey = Character.toUpperCase(charKey);
      }
      if ((mask & SWT.CTRL) != 0) {
         if (_ctrlMapCharToOption.containsKey(charKey)) {
            return _ctrlMapCharToOption.get(charKey).getIntValue();
         }
      }
      else if ((mask & SWT.ALT) != 0) {
         if (_altMapCharToOption.containsKey(charKey)) {
            return _altMapCharToOption.get(charKey).getIntValue();
         }
      }
      else if (_mapCharToOption.containsKey(charKey)) {
         return _mapCharToOption.get(charKey).getIntValue();
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

         HashMap<java.lang.Character, IRequestOption> map = _mapCharToOption;
         if (ctrlKey) {
            map = _ctrlMapCharToOption;
         }
         else if (altKey) {
            map = _altMapCharToOption;
         }
         if (map.get(key) == null) {
            map.put(key, option);
            return true;
         }
      }
      return false;
   }

   public String getStringOfKeyStrokeAssignedToOption(int optionIndex) {
      IRequestOption option = _options.get(optionIndex);
      if (option.getIntValue() == OPT_CANCEL_ACTION) {
         return "Esc";
      }
      Set<Entry<Character, IRequestOption>> pairs = _mapCharToOption.entrySet();
      for (Entry<Character, IRequestOption> pair : pairs) {
         if (pair.getValue() == option) {
            return pair.getKey().toString();
         }
      }
      pairs = _altMapCharToOption.entrySet();
      for (Entry<Character, IRequestOption> pair : pairs) {
         if (pair.getValue() == option) {
            return "<alt>-" + pair.getKey();
         }
      }
      pairs = _ctrlMapCharToOption.entrySet();
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
      for (IRequestOption reqOpt : _options) {
         if (reqOpt.getIntValue() == value) {
            return reqOpt;
         }
      }
      return null;
   }
}
