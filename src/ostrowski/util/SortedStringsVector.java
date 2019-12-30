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
   Vector<String> _data;
   public SortedStringsVector() {
      _data = new Vector<>();
   }

   public void add(String newString) {
      synchronized(_data) {
         String element;
         for (int i=_data.size()-1 ; i>=0 ; i--) {
            element = _data.elementAt(i);
            if (element.compareTo(newString) <= 0) {
               _data.add(i+1, newString);
               return;
            }
         }
         _data.add(0, newString);
      }
   }

   public void remove(String strData) {
      _data.remove(strData);
   }

   public void clear() {
      _data.clear();
   }

   public String firstElement() {
      synchronized(_data) {
         if (_data.size() == 0) { return null; }
         return _data.firstElement();
      }
   }

   public int size() {
      return _data.size();
   }
}
