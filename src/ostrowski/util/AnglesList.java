package ostrowski.util;

import java.util.ArrayList;

public class AnglesList
{
   ArrayList<AnglePair> _list = new ArrayList<>();
   public AnglesList() {
   }

   public void add(AnglePair newAnglePairIn) {
      AnglePair newAnglePair = newAnglePairIn;
      // Look through the current collection to see if we can combine this new angle with any existing angles
      for (int i=0 ; i<_list.size() ; i++) {
         AnglePair pairI = _list.get(i);
         AnglePair union = newAnglePair.unionWithIfOverlapping(pairI, 0.01/*tollerance*/);
         if (union != null) {
            // if this had an overlap with the entry from the list,
            // so we no longer need pairI in this list.
            _list.remove(i);
            newAnglePair = union;
            --i;
         }
      }
      for (int i=0 ; i<_list.size() ; i++) {
         AnglePair pairI = _list.get(i);
         if (pairI._startAngle > newAnglePair._startAngle) {
            _list.add(i, newAnglePair);
            return;
         }
      }
      _list.add(newAnglePair);
   }

   public void add(AnglesList otherList) {
      for (AnglePair otherPair : otherList._list) {
         add(otherPair);
      }
   }

   public boolean overlapsWithAny(AnglePair anglePair) {
      for (AnglePair angle : _list) {
         if (angle.overlapsWith(angle)) {
            return true;
         }
      }
      return false;
   }

   public boolean containsCompletely(AnglePair anglePair) {
      for (AnglePair angle : _list) {
         if (angle.containsCompletely(anglePair)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public String toString() {
      return _list.toString();
   }
}
