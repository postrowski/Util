package ostrowski.util;

import java.net.URL;
import java.util.Properties;

// ADDING A NEW PARAMETER
// If you want to add a new parameter, simply search for TO_DO in this
// file and complete each of the steps.

public class DiagParameters extends ParametersBase {

// TO_DO: Define a variable to store your parameter value.
   public String  diagLabel;
   public String  logFileBase;
   public String  logFileDir;
   public int     maxLogFileSize;
   public int     maxLogFiles;
   public String  fastLog;
   public String  logInfoToConsole;
   public String  logInfoToFile;
   public int     logEventLevel;
   public int     logErrorLevel;
   public int     logModuleMask;
   public int     traceProtocolMask;
   public int     traceProtocolDirection;
   public int     messageQueueMaxSize;

// TO_DO: Define a name for your new variable.
   private static final String DIAG_LABEL_NAME               = "DiagLabel";
   private static final String LOGFILE_BASE_NAME             = "LogFileBase";
   private static final String LOGFILE_DIR_NAME              = "LogFileDir";
   private static final String MAX_LOGFILE_SIZE_NAME         = "MaxLogFileSize";
   private static final String MAX_LOGFILES_NAME             = "MaxLogFiles";
   private static final String FAST_LOG_NAME                 = "FastLog";
   private static final String LOG_INFO_TO_CONSOLE_NAME      = "LogInfoToConsole";
   private static final String LOG_INTO_TO_FILE_NAME         = "LogInfoToFile";
   private static final String LOG_EVENT_LEVEL_NAME          = "LogEventLevel";
   private static final String LOG_ERROR_LEVEL_NAME          = "LogErrorLevel";
   private static final String LOG_MODULE_MASK_NAME          = "LogModuleMask";
   private static final String TRACE_PROTOCAL_MASK_NAME      = "TraceProtocolMask";
   private static final String TRACE_PROTOCAL_DIRECTION_NAME = "TraceProtocolDirection";
   private static final String MESSAGE_QUEUE_MAX_SIZE_NAME   = "MessageQueueMaxSize";

   public DiagParameters (String propertiesFileName) {
      super (propertiesFileName);
   }

   public DiagParameters (URL context, String propertiesFileName) {
      super (context, propertiesFileName);
   }

   @Override
   protected void setDefaults (Properties defaults) {
// TO_DO: Define a default value for your parameter.  The default must be in string form.
      defaults.put ( DIAG_LABEL_NAME,                 "Diagnostics");
      defaults.put ( LOGFILE_BASE_NAME,               "");
      defaults.put ( LOGFILE_DIR_NAME,                "logs");
      defaults.put ( MAX_LOGFILE_SIZE_NAME,            "100");
      defaults.put ( MAX_LOGFILES_NAME,               "10");
      defaults.put ( FAST_LOG_NAME,                   "yes");
      defaults.put ( LOG_INFO_TO_CONSOLE_NAME,          "no");
      defaults.put ( LOG_INTO_TO_FILE_NAME,             "no");
      defaults.put ( LOG_EVENT_LEVEL_NAME,             "0"); // All Events
      defaults.put ( LOG_ERROR_LEVEL_NAME,             "0"); // All Levels(INFO, WARN, ERR, DEBUG)
      defaults.put ( LOG_MODULE_MASK_NAME,             "0");
      defaults.put ( TRACE_PROTOCAL_MASK_NAME,         "0");
      defaults.put ( TRACE_PROTOCAL_DIRECTION_NAME,    "0");
      defaults.put ( MESSAGE_QUEUE_MAX_SIZE_NAME,       "3500");
   }

   @Override
   public void updateVarsFromFileSettings() {
      try {
// TO_DO: Look up your variable in the 'properties' object and set your variable to
// the value returned.  Note that the value returned is a string so you may need to
// convert it to the appropriate type.
         logErrorLevel          = Integer.parseInt( properties.getProperty(LOG_ERROR_LEVEL_NAME) );
         logModuleMask          = Integer.parseInt( properties.getProperty(LOG_MODULE_MASK_NAME) );
         traceProtocolMask      = Integer.parseInt( properties.getProperty(TRACE_PROTOCAL_MASK_NAME) );
         traceProtocolDirection = Integer.parseInt( properties.getProperty(TRACE_PROTOCAL_DIRECTION_NAME) );
         messageQueueMaxSize    = Integer.parseInt( properties.getProperty(MESSAGE_QUEUE_MAX_SIZE_NAME) );
         diagLabel              = properties.getProperty(DIAG_LABEL_NAME);
         logFileBase            = properties.getProperty(LOGFILE_BASE_NAME);
         logFileDir             = properties.getProperty(LOGFILE_DIR_NAME);
         maxLogFileSize         = Integer.parseInt(properties.getProperty(MAX_LOGFILE_SIZE_NAME));
         maxLogFiles            = Integer.parseInt(properties.getProperty(MAX_LOGFILES_NAME));
         fastLog                = properties.getProperty(FAST_LOG_NAME);
         logInfoToConsole       = properties.getProperty(LOG_INFO_TO_CONSOLE_NAME);
         logInfoToFile          = properties.getProperty(LOG_INTO_TO_FILE_NAME);
         logEventLevel          = Integer.parseInt( properties.getProperty(LOG_EVENT_LEVEL_NAME) );

      } catch (NumberFormatException e) {
          // we don't care if the property was of the wrong format,
          // they've all got default values. So catch the exception
          // and keep going.
      }
   }

   @Override
   protected void updateFileSettingsFromVars() {
      properties.put(DIAG_LABEL_NAME, diagLabel);
      properties.put(LOGFILE_BASE_NAME, logFileBase);
      properties.put(LOGFILE_DIR_NAME, logFileDir);
      properties.put(MAX_LOGFILE_SIZE_NAME, String.valueOf(maxLogFileSize));
      properties.put(MAX_LOGFILES_NAME, String.valueOf(maxLogFiles));
      properties.put(FAST_LOG_NAME, fastLog);
      properties.put(LOG_INFO_TO_CONSOLE_NAME, logInfoToConsole);
      properties.put(LOG_INTO_TO_FILE_NAME, logInfoToFile);
      properties.put(LOG_EVENT_LEVEL_NAME, String.valueOf(logEventLevel));
      properties.put(LOG_ERROR_LEVEL_NAME, String.valueOf(logErrorLevel));
      properties.put(LOG_MODULE_MASK_NAME, String.valueOf(logModuleMask));
      properties.put(TRACE_PROTOCAL_MASK_NAME, String.valueOf(traceProtocolMask));
      properties.put(TRACE_PROTOCAL_DIRECTION_NAME, String.valueOf(traceProtocolDirection));
      properties.put(MESSAGE_QUEUE_MAX_SIZE_NAME, String.valueOf(messageQueueMaxSize));
   }

   @Override
   public String toString() {
      return DIAG_LABEL_NAME              + ": " + diagLabel              + "\n" +
             LOGFILE_BASE_NAME            + ": " + logFileBase            + "\n" +
             LOGFILE_DIR_NAME             + ": " + logFileDir             + "\n" +
             MAX_LOGFILE_SIZE_NAME         + ": " + maxLogFileSize         + "\n" +
             MAX_LOGFILES_NAME            + ": " + maxLogFiles            + "\n" +
             FAST_LOG_NAME                + ": " + fastLog                + "\n" +
             LOG_INFO_TO_CONSOLE_NAME       + ": " + logInfoToConsole       + "\n" +
             LOG_INTO_TO_FILE_NAME          + ": " + logInfoToFile          + "\n" +
             LOG_EVENT_LEVEL_NAME          + ": " + logEventLevel          + "\n" +
             LOG_ERROR_LEVEL_NAME          + ": " + logErrorLevel          + "\n" +
             LOG_MODULE_MASK_NAME          + ": " + logModuleMask          + "\n" +
             TRACE_PROTOCAL_MASK_NAME      + ": " + traceProtocolMask      + "\n" +
             TRACE_PROTOCAL_DIRECTION_NAME + ": " + traceProtocolDirection + "\n" +
             MESSAGE_QUEUE_MAX_SIZE_NAME    + ": " + messageQueueMaxSize;
   }

   public String dump() {
      return "\n\t" + DIAG_LABEL_NAME              + ": " + diagLabel              +
             "\n\t" + LOGFILE_BASE_NAME            + ": " + logFileBase            +
             "\n\t" + LOGFILE_DIR_NAME             + ": " + logFileDir             +
             "\n\t" + MAX_LOGFILE_SIZE_NAME         + ": " + maxLogFileSize         +
             "\n\t" + MAX_LOGFILES_NAME            + ": " + maxLogFiles            +
             "\n\t" + FAST_LOG_NAME                + ": " + fastLog                +
             "\n\t" + LOG_INFO_TO_CONSOLE_NAME       + ": " + logInfoToConsole       +
             "\n\t" + LOG_INTO_TO_FILE_NAME          + ": " + logInfoToFile          +
             "\n\t" + LOG_EVENT_LEVEL_NAME          + ": " + logEventLevel          +
             "\n\t" + LOG_ERROR_LEVEL_NAME          + ": " + logErrorLevel          +
             "\n\t" + LOG_MODULE_MASK_NAME          + ": " + logModuleMask          +
             "\n\t" + TRACE_PROTOCAL_MASK_NAME      + ": " + traceProtocolMask      +
             "\n\t" + TRACE_PROTOCAL_DIRECTION_NAME + ": " + traceProtocolDirection +
             "\n\t" + MESSAGE_QUEUE_MAX_SIZE_NAME    + ": " + messageQueueMaxSize;

   }
}
