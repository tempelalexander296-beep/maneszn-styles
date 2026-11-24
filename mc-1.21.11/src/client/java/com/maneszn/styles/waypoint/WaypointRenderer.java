package com.maneszn.styles.waypoint;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.view.Freelook;
import com.maneszn.styles.view.Zoom;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class WaypointRenderer {
   private WaypointRenderer() {
   }

   public static void render(GuiGraphics g, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui) {
         List<Waypoints.Waypoint> list = Waypoints.visibleHere();
         if (!list.isEmpty()) {
            Hooks.waypoints = true;
            LocalPlayer p = mc.player;
            Vec3 eye = p.getEyePosition(partialTick);
            double yaw = Math.toRadians(Freelook.active() ? Freelook.yaw() : p.getViewYRot(partialTick));
            double pitch = Math.toRadians(Freelook.active() ? Freelook.pitch() : p.getViewXRot(partialTick));
            double fx = -Math.sin(yaw) * Math.cos(pitch);
            double fy = -Math.sin(pitch);
            double fz = Math.cos(yaw) * Math.cos(pitch);
            double rx = -Math.cos(yaw);
            double ry = 0.0;
            double rz = -Math.sin(yaw);
            double ux = ry * fz - rz * fy;
            double uy = rz * fx - rx * fz;
            double uz = rx * fy - ry * fx;
            if (!mc.options.getCameraType().isFirstPerson()) {
               double back = mc.options.getCameraType().isMirrored() ? -4.0 : 4.0;
               eye = eye.add(-fx * back, -fy * back, -fz * back);
               if (back < 0.0) {
                  fx = -fx;
                  fz = -fz;
                  fy = -fy;
                  rx = -rx;
                  rz = -rz;
               }
            }

            int w = g.guiWidth();
            int h = g.guiHeight();
            double scale = h / 2.0 / Math.tan(Math.toRadians(Math.max(5.0F, Zoom.lastFov)) / 2.0);

            for (Waypoints.Waypoint wp : list) {
               double dx = wp.x + 0.5 - eye.x;
               double dy = wp.y + 1.0 - eye.y;
               double dz = wp.z + 0.5 - eye.z;
               double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
               double z = dx * fx + dy * fy + dz * fz;
               double x = dx * rx + dy * ry + dz * rz;
               double y = dx * ux + dy * uy + dz * uz;
               boolean edge = false;
               double sx;
               double sy;
               if (z > 0.1) {
                  sx = w / 2.0 + x / z * scale;
                  sy = h / 2.0 - y / z * scale;
               } else {
                  sx = x >= 0.0 ? w - 1 : 0.0;
                  sy = h / 2.0 - Math.signum(y) * h * 0.25;
                  edge = true;
               }

               int margin = 12;
               if (sx < margin || sx > w - margin || sy < margin || sy > h - margin) {
                  edge = true;
               }

               sx = Math.max((double)margin, Math.min((double)(w - margin), sx));
               sy = Math.max((double)(margin + 10), Math.min((double)(h - margin - 10), sy));
               draw(g, mc, (int)sx, (int)sy, wp, dist, edge);
            }
         }
      }
   }

   private static void draw(GuiGraphics g, Minecraft mc, int x, int y, Waypoints.Waypoint wp, double dist, boolean edge) {
      int col = wp.color | 0xFF000000;

      for (int i = 0; i <= 4; i++) {
         g.fill(x - i, y - 4 + i, x + i + 1, y - 3 + i, col);
         g.fill(x - i, y + 4 - i, x + i + 1, y + 5 - i, col);
      }

      g.fill(x, y, x + 1, y + 1, -1);
      String name = wp.name;
      String d = dist >= 1000.0 ? String.format(Locale.ROOT, "%.1fk m", dist / 1000.0) : (int)dist + " m";
      Font f = mc.font;
      int alpha = edge ? 136 : 204;
      int nw = f.width(name);
      int dw = f.width(d);
      g.fill(x - nw / 2 - 2, y - 15, x + nw / 2 + 3, y - 6, alpha << 24);
      g.drawString(f, name, x - nw / 2, y - 14, col, false);
      g.fill(x - dw / 2 - 2, y + 7, x + dw / 2 + 3, y + 16, alpha << 24);
      g.drawString(f, d, x - dw / 2, y + 8, -1, false);
   }
}
