package ostrowski.util;

import ostrowski.DebugBreak;

public class AnglePair implements Comparable<AnglePair> {
   public static final  double TWO_PI         = 2 * Math.PI;
   private static final double NINETY_DEGREES = Math.PI / 2;
   public               double startAngle;
   public               double stopAngle;

   public AnglePair(double startAngle, double stopAngle) {
      this.startAngle = startAngle;
      this.stopAngle = stopAngle;
      ensureNormalized();
   }

   protected void ensureNormalized() {
      startAngle = normalizeAngle(startAngle, TWO_PI);
      stopAngle = normalizeAngle(stopAngle, TWO_PI);
      while (stopAngle < startAngle) {
         stopAngle += TWO_PI;
      }
   }

   protected void ensureAcute() {
      ensureNormalized();
      double width = width();
      if (width > NINETY_DEGREES) {
         DebugBreak.debugBreak("angle must initially be acute.");
      }
   }

//   public boolean intersectsAngle(double startAngle, double stopAngle) {
//      if (stopAngle < startAngle) return false;
//      if (startAngle > stopAngle) return false;
//      return true;
//   }

   public static double normalizeAngleRadians(double angle) {
      // normalize angle between 0-2PI:
      return normalizeAngle(angle, 2*Math.PI);
   }

   public static double normalizeAngle(double angleIn, double maxAngle) {
      // normalize angle between 0-2PI:
      double angle = angleIn;
      while (angle > maxAngle) {
         angle -= maxAngle;
      }
      while (angle < 0) {
         angle += maxAngle;
      }
      return angle;
   }

   // 0-80    +50  = 0-80
   // 90-170  +50  = 50-170
   // 0-80   -310  = 0-80
   // 330-350 +20  = 330-370
   // 320-400 +50  = 320-410
   public void adjustToIncludeAngle(double angleIn) {
      if (includesAngle(angleIn, 0.0/*tolerance*/)) {
         return;
      }
      // startAngle must always be between 0 and 2PI
      // stopAngle must always be higher than startAngle
      double angle = normalizeAngle(angleIn, TWO_PI);

      double angleFromStart = normalizeAngle(startAngle - angle, TWO_PI);
      double angleFromStop  = normalizeAngle(angle - stopAngle, TWO_PI);

      if (angleFromStart < angleFromStop) {
         // becomes the new start
         startAngle = angle;
      }
      else {
         // becomes the new stop
         stopAngle = angle;
      }
      //ensureAcute();
      ensureNormalized();
   }

   public double width() {
      return stopAngle - startAngle;
   }

   public boolean includesAngle(double angleIn, double tolerance) {
      double angle = normalizeAngle(angleIn, TWO_PI);
      if (angle < startAngle) {
         angle += TWO_PI;
      }
      return (angle <= stopAngle);
   }

   @Override
   public int compareTo(AnglePair arg) {
      return Double.compare(startAngle, arg.startAngle);
   }

   public boolean containsCompletely(AnglePair other) {
      return (includesAngle(other.startAngle, 0.0/*tolerance*/) &&
              includesAngle(other.stopAngle, 0.0/*tolerance*/));
   }

   public boolean overlapsWith(AnglePair other) {
      return (includesAngle(other.startAngle, 0.0/*tolerance*/) ||
              includesAngle(other.stopAngle, 0.0/*tolerance*/) ||
              other.includesAngle(startAngle, 0.0/*tolerance*/) ||
              other.includesAngle(stopAngle, 0.0/*tolerance*/));
   }

   // This method will expand the size of the angle by adding the two angles together, IF they overlap.
   // If they don't overlap, then the union can not be contained within a single AnglePair, so 'null' is returned.
   public AnglePair unionWithIfOverlapping(AnglePair other, double tolerance) {
      if (this.includesAngle(other.startAngle, tolerance)) {
         double stopAngle;
         if (this.includesAngle(other.stopAngle, tolerance)) {
            // this AnglePair fully encloses the other AnglePair, or these angles complete a full circle
            if (this.width() < other.width()) {
               return new AnglePair(0, TWO_PI); // fill circle
            }
            stopAngle = this.stopAngle;
         }
         else {
            stopAngle = other.stopAngle;
            while (stopAngle < startAngle) {
               stopAngle += (2 * Math.PI);
            }
         }
         return new AnglePair(startAngle, stopAngle);
      }
      if (other.includesAngle(this.startAngle, tolerance)) {
         return other.unionWithIfOverlapping(this, tolerance);
      }
      // These angles don't overlap.
      // check if they are very very close.
//      if (Math.abs(stopAngle - other.startAngle) < 0.01) {
//         return new AnglePair(startAngle, other.stopAngle);
//      }
//      if (Math.abs(other.stopAngle - startAngle) < 0.01) {
//         return new AnglePair(other.startAngle, stopAngle);
//      }
      return null;
   }

   // this method will return a smaller angle than this angle.
   // If these angles don't overlap at all, it will return null.
   public AnglePair intersectionWith(AnglePair other) {
      if (overlapsWith(other)) {
         double startAngle = Math.max(this.startAngle, other.startAngle);
         double stopAngle  = Math.min(this.stopAngle, other.stopAngle);
         return new AnglePair(startAngle, stopAngle);
      }
      return null;
   }

   public AnglePair adjustWidth(double angleRatio) {
      double center = (startAngle + stopAngle) / 2;
      double width = stopAngle - startAngle;
      double newWidth = width * angleRatio;
      double startAngle = center - (newWidth/2);
      double stopAngle  = center + (newWidth/2);
      return new AnglePair(startAngle, stopAngle);
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof AnglePair)) {
         return false;
      }
      AnglePair otherAP = (AnglePair) other;
      return (this.startAngle == otherAP.startAngle) && (this.stopAngle == otherAP.stopAngle);
   }

   @Override
   public int hashCode() {
      return Double.valueOf(this.startAngle).hashCode() ^ Double.valueOf(this.stopAngle).hashCode();
   }

   public double diff(AnglePair otherAP) {
      return Math.abs(this.startAngle - otherAP.startAngle) +
             Math.abs(this.stopAngle - otherAP.stopAngle);
   }

   @Override
   public String toString() {
      // always display in degrees
      return "[" + (Math.round((startAngle * 36000) / TWO_PI) / 100.0) +
             ", " + (Math.round((stopAngle * 36000) / TWO_PI) / 100.0) + "]";
   }

}

//public class AnglePair implements Comparable<AnglePair> {
//   public static double TWO_PI = 2 * Math.PI;
//   public double startAngle;
//   public double width;
//   public double stopAngle;
//   public AnglePair(double startAngle, double stopAngle) {
//      startAngle = normalizeAngle(startAngle, TWO_PI);
//      stopAngle = stopAngle;
//      width = normalizeAngle(stopAngle - startAngle, TWO_PI);
//\   }
//   protected void ensureNormalized() {
//      startAngle = normalizeAngle(_startAngle, TWO_PI);
//      width = normalizeAngle(_width, TWO_PI);
//   }
//   protected void ensureAcute() {
//      ensureNormalized();
//      if (width() > (TWO_PI / 4)) {
//         DebugBreak.debugBreak("angle must initially be acute.");
//      }
//   }
//   public static double normalizeAngleRadians(double angle) {
//      // normalize angle between 0-2PI:
//      return normalizeAngle(angle, 2*Math.PI);
//   }
//   public static double normalizeAngle(double angle, double maxAngle) {
//      // normalize angle between 0-2PI:
//      while (angle > maxAngle) {
//         angle -= maxAngle;
//      }
//      while (angle < 0) {
//         angle += maxAngle;
//      }
//      return angle;
//   }
//   // 0-80    +50  = 0-80
//   // 90-170  +50  = 50-170
//   // 0-80   -310  = 0-80
//   // 330-350 +20  = 330-370
//   // 320-400 +50  = 320-410
//   public void adjustToIncludeAngle(double angle) {
//      if (includesAngle(angle)) {
//         return;
//      }
//      // startAngle must always be between 0 and 2PI
//      // stopAngle must always be higher than startAngle
//      angle = normalizeAngle(angle, TWO_PI);
//
//      double angleFromStart = normalizeAngle(startAngle - angle, TWO_PI);
//      double angleFromStop  = normalizeAngle(angle - stopAngle, TWO_PI);
//
//      if (angleFromStart < angleFromStop) {
//         // becomes the new start
//         startAngle = angle;
//      }
//      else {
//         // becomes the new stop
//         stopAngle = angle;
//      }
//      ensureAcute();
//   }
//   public double width() {
//      return width;
//   }
//
//   public boolean includesAngle(double angle) {
//      angle = normalizeAngle(angle, TWO_PI);
//      if (angle < startAngle) {
//         angle += TWO_PI;
//      }
//      return (angle <= (startAngle + width));
//   }
//   public boolean containsCompletely(AnglePair other) {
//      return (includesAngle(other.startAngle) &&
//              includesAngle(other.startAngle + other._width));
//   }
//   // This method will expand the size of the angle by adding the two angles together, IF they overlap.
//   // If they don't overlap, then the union can not be contained within a single AnglePair, so 'null' is returned.
//   public AnglePair unionWithIfOverlapping(AnglePair other) {
//      if (this.startAngle > other.startAngle) {
//         return other.unionWithIfOverlapping(this);
//      }
//      // At this point, we know that this.startAngle <= other.startAngle
//
//      double tolerance = 0.0;
//      if (((this.startAngle + this.width) + tolerance) > other.startAngle) {
//         // no overlapping regions
//         return null;
//      }
//
//      // regions overlap. New AnglePairs endpoint is the same as the others endpoint
//      return new AnglePair(startAngle, (other.startAngle + other.width));
//   }
//
//   // this method will return a smaller angle than this angle.
//   // If these angles don't overlap at all, it will return null.
//   public AnglePair intersectionWith(AnglePair other) {
//      if (this.startAngle > other.startAngle) {
//         return other.intersectionWith(this);
//      }
//      // At this point, we know that this._startAngle <= other.startAngle
//
//      if (overlapsWith(other)) {
//         double startAngle = Math.max(startAngle, other.startAngle);
//         double stopAngle  = Math.min(stopAngle,  other.stopAngle);
//         return new AnglePair(startAngle, stopAngle);
//      }
//      return null;
//   }
//
//}
