package ostrowski.util;

import ostrowski.DebugBreak;

public class AnglePair implements Comparable<AnglePair> {
   public static final double TWO_PI = 2 * Math.PI;
   private static final double NINETY_DEGREES = Math.PI / 2;
   public double _startAngle;
   public double _stopAngle;

   public AnglePair(double startAngle, double stopAngle) {
      _startAngle = startAngle;
      _stopAngle = stopAngle;
      ensureNormalized();
   }

   protected void ensureNormalized() {
      _startAngle = normalizeAngle(_startAngle, TWO_PI);
      _stopAngle  = normalizeAngle(_stopAngle,  TWO_PI);
      while (_stopAngle < _startAngle) {
         _stopAngle += TWO_PI;
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
//      if (stopAngle < _startAngle) return false;
//      if (startAngle > _stopAngle) return false;
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
      // _startAngle must always be between 0 and 2PI
      // _stopAngle must always be higher than _startAngle
      double angle = normalizeAngle(angleIn, TWO_PI);

      double angleFromStart = normalizeAngle(_startAngle - angle, TWO_PI);
      double angleFromStop  = normalizeAngle(angle - _stopAngle, TWO_PI);

      if (angleFromStart < angleFromStop) {
         // becomes the new start
         _startAngle = angle;
      }
      else {
         // becomes the new stop
         _stopAngle = angle;
      }
      //ensureAcute();
      ensureNormalized();
   }

   public double width() {
      return _stopAngle - _startAngle;
   }

   public boolean includesAngle(double angleIn, double tolerance) {
      double angle = normalizeAngle(angleIn, TWO_PI);
      if (angle < _startAngle) {
         angle += TWO_PI;
      }
      return (angle <= _stopAngle);
   }

   @Override
   public int compareTo(AnglePair arg) {
      return Double.compare(_startAngle, arg._startAngle);
   }

   public boolean containsCompletely(AnglePair other) {
      return (includesAngle(other._startAngle, 0.0/*tolerance*/) &&
              includesAngle(other._stopAngle, 0.0/*tolerance*/));
   }

   public boolean overlapsWith(AnglePair other) {
      return (includesAngle(other._startAngle, 0.0/*tolerance*/) ||
              includesAngle(other._stopAngle, 0.0/*tolerance*/)  ||
              other.includesAngle(_startAngle, 0.0/*tolerance*/) ||
              other.includesAngle(_stopAngle, 0.0/*tolerance*/));
   }

   // This method will expand the size of the angle by adding the two angles together, IF they overlap.
   // If they don't overlap, then the union can not be contained within a single AnglePair, so 'null' is returned.
   public AnglePair unionWithIfOverlapping(AnglePair other, double tolerance) {
      if (this.includesAngle(other._startAngle, tolerance)) {
         double stopAngle;
         if (this.includesAngle(other._stopAngle, tolerance)) {
            // this AnglePair fully encloses the other AnglePair, or these angles complete a full circle
            if (this.width() < other.width()) {
               return new AnglePair(0, TWO_PI); // fill circle
            }
            stopAngle = _stopAngle;
         }
         else {
            stopAngle = other._stopAngle;
            while (stopAngle < _startAngle) {
               stopAngle += (2 * Math.PI);
            }
         }
         return new AnglePair(_startAngle, stopAngle);
      }
      if (other.includesAngle(this._startAngle, tolerance)) {
         return other.unionWithIfOverlapping(this, tolerance);
      }
      // These angles don't overlap.
      // check if they are very very close.
//      if (Math.abs(_stopAngle - other._startAngle) < 0.01) {
//         return new AnglePair(_startAngle, other._stopAngle);
//      }
//      if (Math.abs(other._stopAngle - _startAngle) < 0.01) {
//         return new AnglePair(other._startAngle, _stopAngle);
//      }
      return null;
   }

   // this method will return a smaller angle than this angle.
   // If these angles don't overlap at all, it will return null.
   public AnglePair intersectionWith(AnglePair other) {
      if (overlapsWith(other)) {
         double startAngle = Math.max(_startAngle, other._startAngle);
         double stopAngle  = Math.min(_stopAngle,  other._stopAngle);
         return new AnglePair(startAngle, stopAngle);
      }
      return null;
   }

   public AnglePair adjustWidth(double angleRatio) {
      double center = (_startAngle + _stopAngle)/2;
      double width = _stopAngle - _startAngle;
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
      return (this._startAngle == otherAP._startAngle) && (this._stopAngle == otherAP._stopAngle);
   }

   @Override
   public int hashCode() {
      return Double.valueOf(this._startAngle).hashCode() ^ Double.valueOf(this._stopAngle).hashCode();
   }

   public double diff(AnglePair otherAP) {
      return Math.abs(this._startAngle - otherAP._startAngle) +
             Math.abs(this._stopAngle  - otherAP._stopAngle);
   }

   @Override
   public String toString() {
      // always display in degrees
      return "[" + (Math.round((_startAngle * 36000) / TWO_PI) / 100.0) +
               ", " + (Math.round((_stopAngle * 36000) / TWO_PI) / 100.0) + "]";
   }

}

//public class AnglePair implements Comparable<AnglePair> {
//   public static double TWO_PI = 2 * Math.PI;
//   public double _startAngle;
//   public double _width;
//   public double _stopAngle;
//   public AnglePair(double startAngle, double stopAngle) {
//      _startAngle = normalizeAngle(startAngle, TWO_PI);
//      _stopAngle = stopAngle;
//      _width = normalizeAngle(stopAngle - startAngle, TWO_PI);
//\   }
//   protected void ensureNormalized() {
//      _startAngle = normalizeAngle(_startAngle, TWO_PI);
//      _width = normalizeAngle(_width, TWO_PI);
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
//      // _startAngle must always be between 0 and 2PI
//      // _stopAngle must always be higher than _startAngle
//      angle = normalizeAngle(angle, TWO_PI);
//
//      double angleFromStart = normalizeAngle(_startAngle - angle, TWO_PI);
//      double angleFromStop  = normalizeAngle(angle - _stopAngle, TWO_PI);
//
//      if (angleFromStart < angleFromStop) {
//         // becomes the new start
//         _startAngle = angle;
//      }
//      else {
//         // becomes the new stop
//         _stopAngle = angle;
//      }
//      ensureAcute();
//   }
//   public double width() {
//      return _width;
//   }
//
//   public boolean includesAngle(double angle) {
//      angle = normalizeAngle(angle, TWO_PI);
//      if (angle < _startAngle) {
//         angle += TWO_PI;
//      }
//      return (angle <= (_startAngle + _width));
//   }
//   public boolean containsCompletely(AnglePair other) {
//      return (includesAngle(other._startAngle) &&
//              includesAngle(other._startAngle + other._width));
//   }
//   // This method will expand the size of the angle by adding the two angles together, IF they overlap.
//   // If they don't overlap, then the union can not be contained within a single AnglePair, so 'null' is returned.
//   public AnglePair unionWithIfOverlapping(AnglePair other) {
//      if (this._startAngle > other._startAngle) {
//         return other.unionWithIfOverlapping(this);
//      }
//      // At this point, we know that this._startAngle <= other._startAngle
//
//      double tolerance = 0.0;
//      if (((this._startAngle + this._width) + tolerance) > other._startAngle) {
//         // no overlapping regions
//         return null;
//      }
//
//      // regions overlap. New AnglePairs endpoint is the same as the others endpoint
//      return new AnglePair(_startAngle, (other._startAngle + other._width));
//   }
//
//   // this method will return a smaller angle than this angle.
//   // If these angles don't overlap at all, it will return null.
//   public AnglePair intersectionWith(AnglePair other) {
//      if (this._startAngle > other._startAngle) {
//         return other.intersectionWith(this);
//      }
//      // At this point, we know that this._startAngle <= other._startAngle
//
//      if (overlapsWith(other)) {
//         double startAngle = Math.max(_startAngle, other._startAngle);
//         double stopAngle  = Math.min(_stopAngle,  other._stopAngle);
//         return new AnglePair(startAngle, stopAngle);
//      }
//      return null;
//   }
//
//}
