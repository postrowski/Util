package ostrowski;

public class DebugBreak
{
   static public void debugBreak() {
      debugBreak("");
   }

   public static void debugBreakIf(boolean test) {
      if (test) {
         debugBreak("");
      }
   }

   public static void debugBreakIf(boolean test, String reason) {
      if (test) {
         debugBreak(reason);
      }
   }

   public static void debugBreak(String reason) {
      int n = 0;
      if ((reason != null) && (reason.length() > 0)) {
         System.out.println("Rules.debugBreak - " + reason);
      }
      n = n + 1;
   }

}
