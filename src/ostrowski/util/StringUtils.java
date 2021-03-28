package ostrowski.util;

// Java Imports
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;


public class StringUtils {
   // NOTE: If you ever change the list of delimiters, and remove the carat ('^') from the front of the
   //       list, there will be problems, because we made an assumption in the database that any time a
   //       contact is linked to a device, its delimiter would be the carat ('^'), and as such, we hard-
   //       code the search to look for "^D^<node>:<ext>". This allows us to avoid a wildcard search,
   //       so it is much more efficient.
   // Basically, it is never safe to alter the order of this list, because an entry in the database may have
   // been created with the old order, and thus would not match the same list built using the new order.
   static private final String DELIMITERS = "^|~`!@#$%&*()_+/<>?;:,.-=[]\"'{}¤¶§" +
                                            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890" +
                                            "•¦ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜ¢£¥ƒáíóúñÑªº¿¬½¼¡«»¯ßµ±÷˜°·² ";
   static public char findDelimiter(Vector<String> stringList) {
      boolean found;
      String testcase;
      char testDelimiter;
      for (int indexI = 0; indexI < DELIMITERS.length(); indexI++) {
         found = false;
         testDelimiter = DELIMITERS.charAt(indexI);
         for (int indexJ = 0; indexJ < stringList.size(); indexJ++)
         {
            testcase = stringList.elementAt(indexJ);
            if (testcase != null) {
               if (testcase.indexOf(testDelimiter) != -1) {
                  found = true;
                  break;
               }
            }
         }
         if (!found) {
            return DELIMITERS.charAt(indexI);
         }
      }

      // We couldn't find any character that isn't being used, so return NULL,
      // and hope that NULL doesn't exist in the string.
      return ' ';
   }

   static public String delimitList(Vector<String> stringList) {
      StringBuilder str = new StringBuilder();
      char delimiter = findDelimiter(stringList);
      for (int i = 0; i < stringList.size(); i++) {
         str.append(delimiter);
         str.append(stringList.elementAt(i));
      }

      return str.toString();
   }

   static public String joinList(Vector<String> stringList, String delimString)
   {
      StringBuilder str = new StringBuilder();
      for (int i = 0; i < stringList.size(); ++i) {
         str.append(delimString);
         str.append(stringList.elementAt(i));
      }
      return str.toString();
   }

   static public String joinList(Vector<String> stringList)
   {
      return joinList(stringList, "");
   }

   static public Vector<String> removeDelimiter(String delimitedStringIn, boolean allowEmptyTerminator) {
      Vector<String> stringList = new Vector<>();
      int index;
      char delimiter;

      if (delimitedStringIn != null) {
         String delimitedString = delimitedStringIn;
         if (delimitedString.length() > 0) {
            delimiter = delimitedString.charAt(0);
            delimitedString = delimitedString.substring(1); // remove the first delimiter;
            index = delimitedString.indexOf(delimiter);
            while (index != -1)
            {
               stringList.add(delimitedString.substring(0, index)); // make a copy of the first element
               delimitedString = delimitedString.substring(index+1);  // remove that element & the next delimiter
               index = delimitedString.indexOf(delimiter);
            }

            if ((delimitedString.length() > 0) || allowEmptyTerminator) {
               stringList.add(delimitedString);
            }
         }
      }
      return stringList;
   }

   static public String addStringToDelimitedList(String delimitedString, boolean allowEmptyTerminator, String newStringToAppend) {
      if (delimitedString != null) {
         if (delimitedString.length() > 0) {
            // Get the current delimiter.
            char delimiter = delimitedString.charAt(0);
            if (newStringToAppend.indexOf(delimiter) == -1) {
               // In this case, the current delimiter is valid to use with the new string
               return delimitedString + delimiter + newStringToAppend;
            }
         }
      }
      // If the current delimiter exists in the new string, then we must
      // start from scratch and find a new delimiter.
      Vector<String> list = removeDelimiter(delimitedString, allowEmptyTerminator);
      list.add(newStringToAppend);
      return delimitList(list);
   }

   static public String stripAndReplace(String before, String strip, char replace) {
      char temp;
      StringBuilder after = new StringBuilder();
      for (int i = 0; i < before.length(); i++) {
         temp = before.charAt(i);
         if ((strip.indexOf(temp)) == -1) {
            after.append(temp);
         }
         else {
            after.append(replace);
         }
      }
      return after.toString();
   }

   static public String base64Encode(byte[] data) {

      int j = 0;
      int dataLength = data.length;
      byte[] encodedData =  new byte[dataLength*2];
      for (byte datum : data) {
         encodedData[j++] = (byte) (((datum & 0xF0) >> 4) + 'a');
         encodedData[j++] = (byte) ((datum & 0x0F) + 'a');
      }

      return new String (encodedData);
   }

   public static byte[] base64Decode(String stringToDecode) {

      byte[] data = stringToDecode.getBytes();

      int j = 0;
      int dataLength = data.length;
      byte[] decodedData =  new byte[dataLength/2];
      for (int i = 0; i < dataLength;) {
         decodedData[j] = (byte)((data[i++] - 'a') << 4);
         decodedData[j++] |= (byte)(data[i++] - 'a');
      }

      return decodedData;
   }

   // Get the Universal time zone (GMT) for getting all timeStamps
   private static final TimeZone UNIVERSAL_TIMEZONE      = TimeZone.getTimeZone("GMT");
   // This SimpleDateFormat object is used by the static timeStamp() method.
   // Since the DateFormatter never changes, we make it static, to avoid
   // construction/destruction of this object.
   // Date Format
   public static final  String   DATE_FORMAT_WITH_MILLIS = "MM/dd/yy HH:mm:ss.SSS"; // used by Diagnostics & Semaphore
                                                                                 // SerializableObject to serialize Date objects as strings
   // Date Formatters. Although these are static, we initialize them
   // to null so that before they can be used, they are created, and
   // have their TimeZones set to the UniversalTimeZone (GMT).
   private static SimpleDateFormat DATE_FORMATTER = null;

   /**
    * Method ensureDateFormaterIsValid. This method makes sure that the date formatter
    * are initialized, and they have their timezones set to GMT. It should be called any
    * time we are about to use a date formatter, and we find that either one is null. It
    * should probably not be called every time before we use a date formatter, because the
    * synchronized access on universalTimeZone might be expensive.
    * Instead, call it like this:
    *       if (_dateFormater == null) {
    *          ensureDateFormaterIsValid();
    *       }
    */
   private static void ensureDateFormaterIsValid() {
      // Make sure that only one thread can be here at a time by synchronizing
      // on a static object. The choice of synchronizing on the universalTimeZone
      // was arbitrary. It could be any static, initialized, object.
      synchronized(UNIVERSAL_TIMEZONE) {
         if (DATE_FORMATTER == null) {
            DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT_WITH_MILLIS);
            DATE_FORMATTER.setTimeZone(UNIVERSAL_TIMEZONE);
         }
      }
   }

   /**
    * Method getCalendar. This method
    * @return Calendar
    */
   public static Calendar getCalendar() {
      // make sure that only one thread can be here at a time
      synchronized(UNIVERSAL_TIMEZONE) {
         return Calendar.getInstance(UNIVERSAL_TIMEZONE);
      }
   }

   /**
    * Method getTimeStamp. Returns string of current time, formatted with the
    * string specified by DATE_FORMAT_WITH_MILLIS.
    * @return String Formatted time stamp string.
    */
   public static String getTimeStamp() {
      return getTimeStamp(getCalendar().getTime());
   }
   /**
    * Method getTimeStamp. Returns string of time specified in the parameter,
    * with the format specified by DATE_FORMAT_WITH_MILLIS.
    * @param date to date to format into a string.
    * @return String Formatted time stamp string.
    */
   public static String getTimeStamp(Date date) {
      if (DATE_FORMATTER == null) {
         ensureDateFormaterIsValid();
      }
      return DATE_FORMATTER.format(date);
   }

   /**
    * This method takes an Exception, and extracts the call stack into a String object suitable
    * for printing in the Diagnostics, or the command line.
    * @param e The Exception that was thrown.
    * @return String detailing the call stack that created the exception.
    */
   public static String getCallStack(Throwable e) {
      try (StringWriter writer = new StringWriter();
           PrintWriter pw = new PrintWriter(writer))
      {
         e.printStackTrace(pw);
         return writer.toString();
      } catch (IOException e1) {
      }
      return null;
   }

   private static final String BLANKS = "                                                                   ";
   public static String getSpaces(int spacesNeeded) {
      if( spacesNeeded <= BLANKS.length() )
      {
         return BLANKS.substring( 0, spacesNeeded );
      }
      char[] array = new char[ spacesNeeded ];
      Arrays.fill( array, ' ' );
      return new String( array );
   }
}

