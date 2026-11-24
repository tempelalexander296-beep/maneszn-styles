package com.maneszn.styles.perf;

import com.maneszn.styles.mixinopt.ProfilerPieChartAccess;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;

public final class PieChart {
   private static final char SEP = '\u001e';
   private static boolean open;
   private static boolean navigated;
   private static int retry;

   private PieChart() {
   }

   public static boolean isOpen() {
      return open;
   }

   public static boolean collecting() {
      return open;
   }

   public static void toggle() {
      open = !open;
      navigated = false;
      retry = 0;
   }

   public static void tick(Minecraft mc) {
      if (open && mc.level != null) {
         if (!navigated || retry-- <= 0) {
            retry = 40;
            ProfilerPieChartAccess acc = (ProfilerPieChartAccess)mc.getDebugOverlay().getProfilerPieChart();
            ProfileResults res = acc.maneszn$results();
            if (res != null) {
               String cur = acc.maneszn$path();
               if (!navigated || cur == null || !cur.endsWith("\u001eblockEntities")) {
                  String path = find(res, "root", 0);
                  if (path != null) {
                     acc.maneszn$setPath(path);
                     navigated = true;
                  }
               }
            }
         }
      }
   }

   private static String find(ProfileResults res, String path, int depth) {
      if (depth > 6) {
         return null;
      } else {
         List<ResultField> list = res.getTimes(path);

         for (int i = 1; i < list.size(); i++) {
            String n = list.get(i).name;
            if ("blockEntities".equals(n)) {
               return path + "\u001e" + n;
            }
         }

         for (String prefer : new String[]{"gameRenderer", "level", "extract"}) {
            for (int ix = 1; ix < list.size(); ix++) {
               String n = list.get(ix).name;
               if (n != null && n.toLowerCase(Locale.ROOT).equals(prefer.toLowerCase(Locale.ROOT))) {
                  String r = find(res, path + "\u001e" + n, depth + 1);
                  if (r != null) {
                     return r;
                  }
               }
            }
         }

         return null;
      }
   }

   public static void render(GuiGraphics g) {
      if (open) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.options.hideGui && !mc.debugEntries.isOverlayVisible()) {
            mc.getDebugOverlay().getProfilerPieChart().render(g);
         }
      }
   }
}
