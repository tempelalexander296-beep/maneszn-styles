package com.maneszn.styles.net;

import java.lang.reflect.Method;
import java.util.UUID;

public final class Profiles {
   private static volatile Method idM;
   private static volatile Method nameM;

   private Profiles() {
   }

   public static UUID id(Object profile) {
      if (profile == null) {
         return null;
      } else {
         try {
            if (idM == null) {
               idM = find(profile.getClass(), "id", "getId");
            }

            return idM.invoke(profile) instanceof UUID u ? u : null;
         } catch (Exception var3) {
            return null;
         }
      }
   }

   public static String name(Object profile) {
      if (profile == null) {
         return "";
      } else {
         try {
            if (nameM == null) {
               nameM = find(profile.getClass(), "name", "getName");
            }

            Object v = nameM.invoke(profile);
            return v == null ? "" : v.toString();
         } catch (Exception var2) {
            return "";
         }
      }
   }

   private static Method find(Class<?> c, String... names) throws NoSuchMethodException {
      for (String n : names) {
         try {
            Method m = c.getMethod(n);
            m.setAccessible(true);
            return m;
         } catch (NoSuchMethodException var7) {
         }
      }

      throw new NoSuchMethodException(String.join("/", names));
   }
}
