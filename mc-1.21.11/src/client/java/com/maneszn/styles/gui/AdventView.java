package com.maneszn.styles.gui;

import dev.maneszn.advent.client.CalendarScreen;
import dev.maneszn.advent.client.ClientAdvent;
import dev.maneszn.advent.core.PlayerAdvent;
import dev.maneszn.advent.core.Reward;
import dev.maneszn.advent.core.Rewards;
import dev.maneszn.advent.net.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Die 24 Adventskalender-Cosmetics im normalen Cosmetics-Menü: freigeschaltete lassen sich wie alle anderen
 * per Klick anlegen/ablegen (Rechtsklick = ablegen), gesperrte zeigen, welches Türchen sie bringt.
 */
public final class AdventView {
   private AdventView() {
   }

   private static PlayerAdvent state() {
      return ClientAdvent.localState();
   }

   public static int owned() {
      return state().claimedCount();
   }

   public static boolean owns(String id) {
      Reward r = Rewards.byId(id);
      return r != null && state().isClaimed(r.day());
   }

   private static boolean equipped(Reward r) {
      return r.id().equals(state().equipped().get(r.slot()));
   }

   /** Die Welt bzw. der Server hat die Mod (sonst gibt es dort keinen Adventskalender). */
   public static boolean connected() {
      return Minecraft.getInstance().player != null && ClientPlayNetworking.canSend(Payloads.Equip.TYPE);
   }

   public static String statusLine() {
      return Ui.tr(connected() ? "maneszn_styles.advent.hint" : "maneszn_styles.advent.offline");
   }

   public static String equippedLine() {
      int n = state().equipped().size();
      return n == 0 ? Ui.tr("maneszn_styles.advent.none") : Ui.tr("maneszn_styles.advent.worn", n);
   }

   public static void toggle(Reward r) {
      var sm = Minecraft.getInstance().getSoundManager();
      if (!connected() || !owns(r.id())) {
         sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6F));
         return;
      }

      ClientPlayNetworking.send(new Payloads.Equip(r.slot().ordinal(), equipped(r) ? "" : r.id()));
      sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public static void unequip(Reward r) {
      if (connected() && equipped(r)) {
         ClientPlayNetworking.send(new Payloads.Equip(r.slot().ordinal(), ""));
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
      }
   }

   private static int dim(int argb, float k) {
      int r = (int)((argb >> 16 & 255) * k), g = (int)((argb >> 8 & 255) * k), b = (int)((argb & 255) * k);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   /** Karte im Raster: Geschenk in den Türfarben mit Schleife, Tag-Nummer, Seltenheit, Name. */
   public static void card(GuiGraphics g, Theme th, Reward r, int cx, int cy, int cardW, int cardH, int pv, boolean hov) {
      boolean own = owns(r.id());
      boolean eq = own && equipped(r);
      Ui.rect(g, cx, cy, cardW, cardH, hov ? -14015424 : -14607314);
      int bx = cx + 4, by = cy + 4;
      float k = own ? 1.0F : 0.45F;
      g.fillGradient(bx, by, bx + pv, by + pv, dim(r.doorColorB(), k), dim(r.doorColorA(), k * 0.8F));
      int rib = dim(r.ribbonColor(), k);
      int rw = Math.max(2, pv / 9);
      g.fill(bx + (pv - rw) / 2, by, bx + (pv + rw) / 2, by + pv, rib);
      g.fill(bx, by + (pv - rw) / 2, bx + pv, by + (pv + rw) / 2, rib);
      int bow = Math.max(3, pv / 7);
      g.fill(bx + pv / 2 - bow, by + pv / 2 - bow / 2 - rw, bx + pv / 2 - 1, by + pv / 2 + bow / 2 - rw, rib);
      g.fill(bx + pv / 2 + 1, by + pv / 2 - bow / 2 - rw, bx + pv / 2 + bow, by + pv / 2 + bow / 2 - rw, rib);
      g.fillGradient(bx, by, bx + pv, by + pv / 2, 0x30FFFFFF, 0x00FFFFFF);
      String day = String.valueOf(r.day());
      g.pose().pushMatrix();
      float sc = Math.max(1.5F, pv / 28.0F);
      g.pose().translate(bx + 4, by + 4);
      g.pose().scale(sc, sc);
      g.drawString(Ui.font(), day, 0, 0, own ? -1 : -6646866, true);
      g.pose().popMatrix();
      String rar = r.rarity().label.toUpperCase(java.util.Locale.ROOT);
      int bw = Ui.width(rar) + 4;
      Ui.rect(g, cx + cardW - 4 - bw, cy + 4, bw, 10, own ? r.rarity().color : -12960172);
      Ui.text(g, rar, cx + cardW - 2 - bw, cy + 5, -16054254);
      Ui.border(g, bx, by, pv, pv, own ? r.rarity().color : -13752510);
      if (!own) {
         Ui.textCentered(g, "🔒", bx + pv / 2, by + pv / 2 - 4, -1);
         Ui.textCentered(g, Ui.tr("maneszn_styles.advent.door", r.day()), bx + pv / 2, by + pv - 12, -1185282);
      }

      Ui.text(g, Ui.trim(own ? r.name() : "???", cardW - 8), cx + 4, cy + pv + 8, own ? (eq ? -1 : r.rarity().glow) : -9739136);
      if (eq) {
         long now = System.currentTimeMillis();
         int a = 120 + (int)(100.0 * (0.5 + 0.5 * Math.sin(now / 280.0)));
         Ui.border(g, cx - 1, cy - 1, cardW + 2, cardH + 2, th.accentAlpha(a));
         Ui.border(g, cx, cy, cardW, cardH, th.accent);
         Ui.rect(g, cx + cardW - 9, cy + cardH - 9, 5, 5, -14498466);
      } else if (hov) {
         Ui.border(g, cx, cy, cardW, cardH, -13752510);
      }
   }

   /** Rechte Spalte: dein Spieler in 3D mit dem Cosmetic (Vorschau beim Drüberfahren) + Infos + Knöpfe. */
   public static void inspector(GuiGraphics g, Ui.Zones zones, int mx, int my, Theme th, int x, int y, int w, int px, int pw, String hoveredId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 2, y + 10, px + pw - 2, y + 6 + pw, Math.max(20, (int)(pw * 0.36F)), 0.0625F, mx, my, mc.player);
      }

      Reward r = hoveredId != null ? Rewards.byId(hoveredId) : null;
      if (r == null) {
         for (Reward c : Rewards.ALL) {
            if (equipped(c)) {
               r = c;
            }
         }
      }

      int ty = y + 8 + pw + 8;
      if (r == null) {
         Ui.textShadow(g, Ui.trim("🎄 " + Ui.tr("maneszn_styles.advent.side"), w - 16), x + 8, ty, -1185282);
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.advent.sub", owned()), w - 16), x + 8, ty + 12, -6646866);
         ty += 30;
      } else {
         boolean own = owns(r.id());
         boolean eq = own && equipped(r);
         Ui.textShadow(g, Ui.trim(own ? r.name() : "???", w - 16), x + 8, ty, own ? r.rarity().glow : -9739136);
         Ui.text(g, Ui.trim(r.rarity().label + " · " + r.slot().label + " · " + Ui.tr("maneszn_styles.advent.door", r.day()), w - 16), x + 8, ty + 12, r.rarity().color);
         Ui.text(g, Ui.trim(own ? r.description() : Ui.tr("maneszn_styles.advent.locked", r.day()), w - 16), x + 8, ty + 23, -6646866);
         int by = ty + 37;
         if (own && connected()) {
            final Reward fr = r;
            boolean h = Ui.hover(mx, my, x + 8, by, w - 16, 16);
            Ui.button(g, x + 8, by, w - 16, 16, Ui.tr(eq ? "maneszn_styles.advent.unequip" : "maneszn_styles.advent.equip"), h, !eq, th);
            zones.add(x + 8, by, w - 16, 16, () -> toggle(fr));
            by += 19;
         }

         ty = by + 3;
      }

      boolean hc = Ui.hover(mx, my, x + 8, ty, w - 16, 16);
      Ui.button(g, x + 8, ty, w - 16, 16, Ui.tr("maneszn_styles.advent.open"), hc, false, th);
      zones.add(x + 8, ty, w - 16, 16, () -> mc.setScreen(new CalendarScreen()));
   }
}
