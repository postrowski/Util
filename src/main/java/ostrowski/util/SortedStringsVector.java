/*
 * Created on Jan 21, 2004
 *
 */
package ostrowski.util;

import java.util.Vector;

/**
 * This class contains a Vector of String objects, and it keeps them sorted, based
 * on the compareTo operation of the String class.
 * @author pnostrow
 */

public class SortedStringsVector {
   final Vector<String> data;
   public SortedStringsVector() {
      data = new Vector<>();
   }

   public void add(String newString) {
      synchronized(data) {
         String element;
         for (int i = data.size() - 1; i >= 0 ; i--) {
            element = data.elementAt(i);
            if (element.compareTo(newString) <= 0) {
               data.add(i + 1, newString);
               return;
            }
         }
         data.add(0, newString);
      }
   }

   public void remove(String strData) {
      data.remove(strData);
   }

   public void clear() {
      data.clear();
   }

   public String firstElement() {
      synchronized(data) {
         if (data.size() == 0) { return null; }
         return data.firstElement();
      }
   }

   public int size() {
      return data.size();
   }
}
