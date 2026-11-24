package com.maneszn.styles.tools;

import java.util.Locale;

public final class Num {
   private Num() {
   }

   public static Double parse(String raw) {
      if (raw == null) {
         return null;
      } else {
         String s = raw.trim().toLowerCase(Locale.ROOT).replace("$", "").replace(" ", "").replace(" ", "");
         double mul = 1.0;
         String[][] suf = new String[][]{{"mrd", "1e9"}, {"mio", "1e6"}, {"k", "1e3"}, {"m", "1e6"}, {"b", "1e9"}};

         for (String[] x : suf) {
            if (s.endsWith(x[0])) {
               mul = Double.parseDouble(x[1]);
               s = s.substring(0, s.length() - x[0].length());
               break;
            }
         }

         if (s.isEmpty()) {
            return null;
         } else {
            boolean dot = s.contains(".");
            boolean comma = s.contains(",");
            if (mul != 1.0) {
               s = s.replace(",", ".");
               if (s.indexOf(46) != s.lastIndexOf(46)) {
                  s = s.replace(".", "");
               }
            } else if (dot && comma) {
               if (s.lastIndexOf(44) > s.lastIndexOf(46)) {
                  s = s.replace(".", "").replace(",", ".");
               } else {
                  s = s.replace(",", "");
               }
            } else if (dot) {
               if (s.matches("\\d{1,3}(\\.\\d{3})+")) {
                  s = s.replace(".", "");
               }
            } else if (comma) {
               if (s.matches("\\d{1,3}(,\\d{3})+")) {
                  s = s.replace(",", "");
               } else {
                  s = s.replace(",", ".");
               }
            }

            try {
               return Double.parseDouble(s) * mul;
            } catch (NumberFormatException var9) {
               return null;
            }
         }
      }
   }

   public static String compact(double v) {
      double a = Math.abs(v);
      String sign = v < 0.0 ? "-" : "";
      if (a >= 1.0E9) {
         return sign + trim(a / 1.0E9) + "B";
      } else if (a >= 1000000.0) {
         return sign + trim(a / 1000000.0) + "M";
      } else {
         return a >= 1000.0 ? sign + trim(a / 1000.0) + "k" : sign + trim(a);
      }
   }

   public static String full(double v) {
      return Math.abs(v - Math.rint(v)) < 1.0E-9 ? String.format(Locale.GERMANY, "%,d", (long)Math.rint(v)) : String.format(Locale.GERMANY, "%,.2f", v);
   }

   private static String trim(double v) {
      String s = String.format(Locale.ROOT, v >= 100.0 ? "%.0f" : (v >= 10.0 ? "%.1f" : "%.2f"), v);
      if (s.contains(".")) {
         s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
      }

      return s;
   }
}
