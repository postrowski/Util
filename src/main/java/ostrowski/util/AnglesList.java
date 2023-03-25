package ostrowski.util;

import java.util.ArrayList;
import java.util.List;

public class AnglesList
{
   final List<AnglePair> list = new ArrayList<>();
   public AnglesList() {
   }

   public void add(AnglePair newAnglePairIn) {
      AnglePair newAnglePair = newAnglePairIn;
      // Look through the current collection to see if we can combine this new angle with any existing angles
      for (int i = 0; i < list.size() ; i++) {
         AnglePair pairI = list.get(i);
         AnglePair union = newAnglePair.unionWithIfOverlapping(pairI, 0.01/*tolerance*/);
         if (union != null) {
            // if this had an overlap with the entry from the list,
            // so we no longer need pairI in this list.
            list.remove(i);
            newAnglePair = union;
            --i;
         }
      }
      for (int i = 0; i < list.size() ; i++) {
         AnglePair pairI = list.get(i);
         if (pairI.startAngle > newAnglePair.startAngle) {
            list.add(i, newAnglePair);
            return;
         }
      }
      list.add(newAnglePair);
   }

   public void add(AnglesList otherList) {
      for (AnglePair otherPair : otherList.list) {
         add(otherPair);
      }
   }

   public boolean overlapsWithAny(AnglePair anglePair) {
      for (AnglePair angle : list) {
         if (angle.overlapsWith(angle)) {
            return true;
         }
      }
      return false;
   }

   public boolean containsCompletely(AnglePair anglePair) {
      for (AnglePair angle : list) {
         if (angle.containsCompletely(anglePair)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public String toString() {
      return list.toString();
   }
}
