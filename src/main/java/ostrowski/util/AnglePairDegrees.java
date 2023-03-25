package ostrowski.util;

import ostrowski.DebugBreak;

public class AnglePairDegrees extends AnglePair {

   public AnglePairDegrees(double startAngle, double stopAngle) {
      super(Math.toRadians(startAngle), Math.toRadians(stopAngle));
   }

   public static void unionTest(double start1, double end1,
                                double start2, double end2,
                                double resStart, double resEnd) {
      AnglePairDegrees pair1 = new AnglePairDegrees(start1, end1);
      AnglePairDegrees pair2 = new AnglePairDegrees(start2, end2);
      AnglePairDegrees res   = new AnglePairDegrees(resStart, resEnd);
      AnglePair unionA = pair1.unionWithIfOverlapping(pair2, 0.0/*tolerance*/);
      AnglePair unionB = pair2.unionWithIfOverlapping(pair1, 0.0/*tolerance*/);
      DebugBreak.debugBreakIf(unionA.diff(unionB) > 0.00001, "angles dont match");
      DebugBreak.debugBreakIf(unionA.diff(res) > 0.00001, "angles dont match");

   }
   public static void test() {
      unionTest(300, 360, 0, 30, 300, 390);
      // pair1 = 30-50
      AnglePair pair1 = new AnglePairDegrees(30, 50);
      // pair2 = 330-390 (-30 - 30)
      AnglePair pair2 = new AnglePairDegrees(330, 30);
      DebugBreak.debugBreakIf((Math.abs(pair1.width() - Math.toRadians(20)) > 0.0001));
      DebugBreak.debugBreakIf((Math.abs(pair2.width() - Math.toRadians(60)) > 0.0001));
      pair1.adjustToIncludeAngle(Math.toRadians(30));
      // pair1 = 30-50
      DebugBreak.debugBreakIf((Math.abs(pair1.width() - Math.toRadians(20)) > 0.0001));
      pair1.adjustToIncludeAngle(Math.toRadians(10));
      // pair1 = 10-50
      DebugBreak.debugBreakIf((Math.abs(pair1.width() - Math.toRadians(40)) > 0.0001));
      pair1.adjustToIncludeAngle(Math.toRadians(340));
      DebugBreak.debugBreakIf((Math.abs(pair1.width() - Math.toRadians(70))  > 0.0001));
      // pair1 = 340-410 (-20 - 50)
      DebugBreak.debugBreakIf(!pair1.includesAngle(Math.toRadians(10  ), 0.0/*tolerance*/));
      DebugBreak.debugBreakIf( pair1.includesAngle(Math.toRadians(50.1), 0.0/*tolerance*/));
      DebugBreak.debugBreakIf(!pair1.includesAngle(Math.toRadians(49.9), 0.0/*tolerance*/));
      DebugBreak.debugBreakIf( pair1.includesAngle(Math.toRadians(180 ), 0.0/*tolerance*/));
      DebugBreak.debugBreakIf( pair1.includesAngle(Math.toRadians(339 ), 0.0/*tolerance*/));
      DebugBreak.debugBreakIf(!pair1.includesAngle(Math.toRadians(341 ), 0.0/*tolerance*/));
   }
}
