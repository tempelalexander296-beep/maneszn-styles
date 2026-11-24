package com.maneszn.styles.hud;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.gui.HudEditScreen;
import com.maneszn.styles.gui.Theme;
import com.maneszn.styles.gui.Ui;
import com.maneszn.styles.perf.PieChart;
import com.maneszn.styles.tools.ArmorWarn;
import com.maneszn.styles.tools.Money;
import com.maneszn.styles.tools.Num;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

public final class HudManager {
   private static final Map<String, HudManager.Module> MODULES = new LinkedHashMap<>();
   private static final Map<String, ModConfig.HudPos> DEFAULTS = new LinkedHashMap<>();
   private static final Deque<Long> LEFT = new ArrayDeque<>();
   private static final Deque<Long> RIGHT = new ArrayDeque<>();
   private static boolean lastLeft;
   private static boolean lastRight;
   private static boolean sprintToggled;
   private static boolean lastSprintDown;
   private static final int BG = 1711276032;
   public static boolean editing;

   private HudManager() {
   }

   public static void register() {
      add(text("fps", mc -> opt("fps", "label", true) ? mc.getFps() + " FPS" : String.valueOf(mc.getFps()), null), true, 0.005, 0.01);
      add(
         text("cps", mc -> (opt("cps", "label", true) ? "CPS " : "") + cps(LEFT) + (opt("cps", "right", true) ? " | " + cps(RIGHT) : ""), null),
         true,
         0.005,
         0.05
      );
      add(text("coords", HudManager::coords, null), false, 0.005, 0.09);
      add(text("ping", HudManager::ping, mc -> "42 ms"), false, 0.005, 0.13);
      add(
         text("clock", mc -> LocalTime.now().format(DateTimeFormatter.ofPattern(opt("clock", "seconds", false) ? "HH:mm:ss" : "HH:mm")), null),
         false,
         0.005,
         0.17
      );
      add(
         text("sprint", mc -> ModConfig.get().toggleSprint && sprintToggled ? "[Sprinten (umgeschaltet)]" : null, mc -> "[Sprinten (umgeschaltet)]"),
         true,
         0.005,
         0.95
      );
      add(text("combo", mc -> PvpStats.combo() >= 2 ? "Combo " + PvpStats.combo() : null, mc -> "Combo 4"), false, 0.47, 0.6);
      add(
         text(
            "reach",
            mc -> PvpStats.reach() < 0.0 ? null : String.format(Locale.ROOT, "%.2f " + (de() ? "Blöcke" : "blocks"), PvpStats.reach()),
            mc -> "2.97 " + (de() ? "Blöcke" : "blocks")
         ),
         false,
         0.47,
         0.64
      );
      add(new HudManager.JumpReset(), true, 0.42, 0.68);
      add(new HudManager.Keystrokes(), true, 0.005, 0.55);
      add(new HudManager.Armor(), true, 0.92, 0.4);
      add(new HudManager.ItemCounter(), false, 0.92, 0.12);
      add(new HudManager.Effects(), false, 0.78, 0.01);
      add(text("money", mc -> {
         Double b = Money.balance();
         if (b == null) {
            return null;
         } else {
            double net = Money.sessionIn() - Money.sessionOut();
            return "$ " + Num.compact(b) + "  (" + Money.fmtSigned(net) + ")";
         }
      }, mc -> "$ 21.6M  (+125k)"), false, 0.005, 0.21);
      HudElementRegistry.addLast(ManesznStyles.id("hud"), (g, delta) -> render(g));
      HudElementRegistry.addLast(ManesznStyles.id("pie_chart"), (g, delta) -> PieChart.render(g));
      HudElementRegistry.addLast(ManesznStyles.id("armor_warn"), (g, delta) -> ArmorWarn.render(g));
      HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (g, delta) -> {
         ModConfig.CrosshairCfg c = ModConfig.get().crosshair;
         Minecraft mc = Minecraft.getInstance();
         if (c != null && c.on) {
            Hooks.crosshair = true;
            if (mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui) {
               Crosshair.draw(g, Crosshair.grid(c), g.guiWidth() / 2, g.guiHeight() / 2, 1, Crosshair.color(c), c.outline);
            }
         } else {
            original.render(g, delta);
         }
      });
   }

   static boolean de() {
      return Ui.langShort().equals("de");
   }

   private static void add(HudManager.Module m, boolean on, double x, double y) {
      MODULES.put(m.id(), m);
      DEFAULTS.put(m.id(), new ModConfig.HudPos(on, x, y));
   }

   public static List<HudManager.Module> modules() {
      return List.copyOf(MODULES.values());
   }

   public static int bg(String id) {
      ModConfig.HudPos p = pos(id);
      if (!p.background) {
         return 0;
      } else {
         int a = Math.max(0, Math.min(100, p.bgOpacity)) * 255 / 100;
         return a << 24 | p.bgColor & 16777215;
      }
   }

   public static int fg(String id, int def) {
      int c = pos(id).textColor;
      return c == 0 ? def : c | 0xFF000000;
   }

   public static boolean shadow(String id) {
      return pos(id).shadow;
   }

   public static boolean opt(String id, String key, boolean def) {
      Boolean b = pos(id).opts.get(key);
      return b == null ? def : b;
   }

   static void box(GuiGraphics g, String id, int x, int y, int w, int h, int accent) {
      int bg = bg(id);
      if (bg != 0) {
         g.fill(x, y, x + w, y + h, bg);
      }

      if (pos(id).accentBar) {
         g.fill(x, y, x + 1, y + h, accent);
      }
   }

   public static List<String[]> options(String id) {
      return switch (id) {
         case "keystrokes" -> List.<String[]>of(new String[]{"mouse", "true"}, new String[]{"space", "true"}, new String[]{"cps", "true"});
         case "cps" -> List.<String[]>of(new String[]{"right", "true"}, new String[]{"label", "true"});
         case "fps" -> List.<String[]>of(new String[]{"label", "true"});
         case "coords" -> List.<String[]>of(new String[]{"facing", "true"});
         case "clock" -> List.<String[]>of(new String[]{"seconds", "false"});
         case "jumpreset" -> List.<String[]>of(new String[]{"rate", "true"}, new String[]{"ticks", "true"});
         case "armor" -> List.<String[]>of(new String[]{"hand", "true"}, new String[]{"numbers", "true"});
         default -> List.of();
      };
   }

   public static void resetStyle(String id) {
      ModConfig.HudPos p = pos(id);
      ModConfig.HudPos d = new ModConfig.HudPos();
      p.textColor = d.textColor;
      p.background = d.background;
      p.bgColor = d.bgColor;
      p.bgOpacity = d.bgOpacity;
      p.accentBar = d.accentBar;
      p.shadow = d.shadow;
      p.opts.clear();
   }

   public static ModConfig.HudPos pos(String id) {
      ModConfig cfg = ModConfig.get();
      ModConfig.HudPos p = cfg.hud.get(id);
      if (p == null) {
         ModConfig.HudPos d = DEFAULTS.get(id);
         p = d == null ? new ModConfig.HudPos(false, 0.0, 0.0) : new ModConfig.HudPos(d.on, d.x, d.y);
         cfg.hud.put(id, p);
      }

      if (p.opts == null) {
         p.opts = new LinkedHashMap<>();
      }

      return p;
   }

   public static void tick(Minecraft mc) {
      PvpStats.tick(mc);
      if (mc.player != null) {
         boolean down = mc.options.keySprint.isDown();
         if (ModConfig.get().toggleSprint) {
            if (down && !lastSprintDown) {
               sprintToggled = !sprintToggled;
            }

            if (sprintToggled
               && mc.options.keyUp.isDown()
               && !mc.player.isShiftKeyDown()
               && !mc.player.isUsingItem()
               && mc.player.getFoodData().getFoodLevel() > 6) {
               mc.player.setSprinting(true);
            }
         } else {
            sprintToggled = false;
         }

         lastSprintDown = down;
      }
   }

   public static void render(GuiGraphics g) {
      Minecraft mc = Minecraft.getInstance();
      Hooks.hud = true;
      countClicks(mc);
      if (!mc.options.hideGui && mc.player != null) {
         if (!(mc.screen instanceof HudEditScreen)) {
            for (HudManager.Module m : MODULES.values()) {
               ModConfig.HudPos p = pos(m.id());
               if (p.on) {
                  int[] at = place(g, mc, m, p);
                  if (at != null) {
                     drawModule(g, mc, m, at, p.scale);
                  }
               }
            }
         }
      }
   }

   public static void drawModule(GuiGraphics g, Minecraft mc, HudManager.Module m, int[] at, double scale) {
      if (Math.abs(scale - 1.0) < 0.001) {
         m.draw(g, mc, at[0], at[1]);
      } else {
         Matrix3x2fStack pose = g.pose();
         pose.pushMatrix();
         pose.translate(at[0], at[1]);
         pose.scale((float)scale, (float)scale);
         m.draw(g, mc, 0, 0);
         pose.popMatrix();
      }
   }

   public static int[] scaledSize(Minecraft mc, HudManager.Module m, ModConfig.HudPos p) {
      int[] sz = m.size(mc);
      return sz == null ? null : new int[]{(int)Math.ceil(sz[0] * p.scale), (int)Math.ceil(sz[1] * p.scale)};
   }

   public static int[] place(GuiGraphics g, Minecraft mc, HudManager.Module m, ModConfig.HudPos p) {
      int[] sz = scaledSize(mc, m, p);
      if (sz == null) {
         return null;
      } else {
         int w = g.guiWidth();
         int h = g.guiHeight();
         int x = (int)Math.round(p.x * w);
         int y = (int)Math.round(p.y * h);
         x = Math.max(0, Math.min(w - sz[0], x));
         y = Math.max(0, Math.min(h - sz[1], y));
         return new int[]{x, y, sz[0], sz[1]};
      }
   }

   private static void countClicks(Minecraft mc) {
      long now = System.currentTimeMillis();
      boolean l = mc.options.keyAttack.isDown();
      boolean r = mc.options.keyUse.isDown();
      if (l && !lastLeft) {
         LEFT.add(now);
      }

      if (r && !lastRight) {
         RIGHT.add(now);
      }

      lastLeft = l;
      lastRight = r;

      while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000L) {
         LEFT.pollFirst();
      }

      while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000L) {
         RIGHT.pollFirst();
      }
   }

   static int cps(Deque<Long> d) {
      return d.size();
   }

   private static HudManager.Module text(String id, HudManager.Text t, HudManager.Text preview) {
      return new HudManager.Module() {
         @Override
         public String id() {
            return id;
         }

         private String value(Minecraft mc) {
            String s = mc.player == null ? null : t.get(mc);
            if (s == null && HudManager.editing) {
               s = preview != null ? preview.get(mc) : id.toUpperCase(Locale.ROOT);
            }

            return s;
         }

         @Override
         public int[] size(Minecraft mc) {
            String s = this.value(mc);
            return s == null ? null : new int[]{mc.font.width(s) + 8, 13};
         }

         @Override
         public void draw(GuiGraphics g, Minecraft mc, int x, int y) {
            String s = this.value(mc);
            if (s != null) {
               int w = mc.font.width(s) + 8;
               HudManager.box(g, id, x, y, w, 13, Theme.current().accent);
               g.drawString(mc.font, s, x + 5, y + 3, HudManager.fg(id, -1), HudManager.shadow(id));
            }
         }
      };
   }

   private static String coords(Minecraft mc) {
      LocalPlayer p = mc.player;
      if (p == null) {
         return null;
      } else {
         String dir = switch (p.getDirection()) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "O";
            case WEST -> "W";
            default -> "";
         };
         return "XYZ " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ() + (opt("coords", "facing", true) ? "  " + dir : "");
      }
   }

   private static String ping(Minecraft mc) {
      if (mc.getConnection() != null && mc.player != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         return info == null ? null : info.getLatency() + " ms";
      } else {
         return null;
      }
   }

   private static final class Armor implements HudManager.Module {
      private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{
         EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
      };

      @Override
      public String id() {
         return "armor";
      }

      private int slots() {
         return HudManager.opt(this.id(), "hand", true) ? SLOTS.length : SLOTS.length - 1;
      }

      @Override
      public int[] size(Minecraft mc) {
         return new int[]{HudManager.opt(this.id(), "numbers", true) ? 52 : 18, this.slots() * 18};
      }

      @Override
      public void draw(GuiGraphics g, Minecraft mc, int x, int y) {
         if (mc.player != null) {
            int[] sz = this.size(mc);
            int bg = HudManager.bg(this.id());
            if (bg != 0 && HudManager.pos(this.id()).bgOpacity > 0 && !HudManager.editing) {
               g.fill(x, y, x + sz[0], y + sz[1], bg);
            }

            for (int i = 0; i < this.slots(); i++) {
               ItemStack st = mc.player.getItemBySlot(SLOTS[i]);
               int yy = y + i * 18;
               if (!st.isEmpty()) {
                  g.renderItem(st, x, yy);
                  g.renderItemDecorations(mc.font, st, x, yy);
                  if (st.isDamageableItem() && HudManager.opt(this.id(), "numbers", true)) {
                     int left = st.getMaxDamage() - st.getDamageValue();
                     float frac = (float)left / st.getMaxDamage();
                     int col = frac > 0.5F ? -11141240 : (frac > 0.2F ? -10163 : -43691);
                     g.drawString(mc.font, String.valueOf(left), x + 19, yy + 5, col, true);
                  }
               }
            }
         }
      }
   }

   private static final class Effects implements HudManager.Module {
      private static final String[] ROMAN = new String[]{"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

      @Override
      public String id() {
         return "effects";
      }

      private static List<String[]> rows(Minecraft mc) {
         List<String[]> out = new ArrayList<>();
         if (mc.player != null) {
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
               String name = e.getEffect().value().getDisplayName().getString();
               int amp = e.getAmplifier();
               name = name + (amp >= 0 && amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1));
               String time;
               if (e.isInfiniteDuration()) {
                  time = "∞";
               } else {
                  int sec = e.getDuration() / 20;
                  time = sec / 60 + ":" + String.format("%02d", sec % 60);
               }

               int col = e.getEffect().value().getColor() | 0xFF000000;
               boolean low = !e.isInfiniteDuration() && e.getDuration() < 200;
               out.add(new String[]{name, time, String.valueOf(col), String.valueOf(low)});
            }
         }

         if (out.isEmpty() && HudManager.editing) {
            out.add(new String[]{HudManager.de() ? "Stärke II" : "Strength II", "1:24", String.valueOf(-14592), "false"});
            out.add(new String[]{HudManager.de() ? "Schnelligkeit II" : "Speed II", "0:08", String.valueOf(-13374465), "true"});
         }

         return out;
      }

      @Override
      public int[] size(Minecraft mc) {
         List<String[]> r = rows(mc);
         if (r.isEmpty()) {
            return null;
         } else {
            int w = 0;

            for (String[] s : r) {
               w = Math.max(w, mc.font.width(s[0]) + mc.font.width(s[1]) + 22);
            }

            return new int[]{w, r.size() * 12 + 1};
         }
      }

      @Override
      public void draw(GuiGraphics g, Minecraft mc, int x, int y) {
         List<String[]> r = rows(mc);
         int[] sz = this.size(mc);
         if (sz != null) {
            for (int i = 0; i < r.size(); i++) {
               String[] s = r.get(i);
               int yy = y + i * 12;
               int bgc = HudManager.bg(this.id());
               if (bgc != 0) {
                  g.fill(x, yy, x + sz[0], yy + 12, bgc);
               }

               g.fill(x + 3, yy + 3, x + 9, yy + 9, Integer.parseInt(s[2]));
               g.drawString(mc.font, s[0], x + 12, yy + 2, HudManager.fg(this.id(), -1), HudManager.shadow(this.id()));
               boolean blink = Boolean.parseBoolean(s[3]) && System.currentTimeMillis() / 400L % 2L == 0L;
               g.drawString(mc.font, s[1], x + sz[0] - mc.font.width(s[1]) - 3, yy + 2, blink ? -43691 : -4473925, true);
            }
         }
      }
   }

   private static final class ItemCounter implements HudManager.Module {
      private static final Item[] ITEMS = new Item[]{
         Items.TOTEM_OF_UNDYING,
         Items.ENDER_PEARL,
         Items.GOLDEN_APPLE,
         Items.ENCHANTED_GOLDEN_APPLE,
         Items.SPLASH_POTION,
         Items.POTION,
         Items.ARROW,
         Items.EXPERIENCE_BOTTLE,
         Items.END_CRYSTAL,
         Items.OBSIDIAN,
         Items.COBWEB,
         Items.WIND_CHARGE
      };

      @Override
      public String id() {
         return "items";
      }

      private static List<int[]> counts(Minecraft mc) {
         List<int[]> out = new ArrayList<>();
         int[] n = new int[ITEMS.length];
         if (mc.player != null) {
            Inventory inv = mc.player.getInventory();

            for (int i = 0; i < inv.getContainerSize(); i++) {
               ItemStack st = inv.getItem(i);
               if (!st.isEmpty()) {
                  for (int k = 0; k < ITEMS.length; k++) {
                     if (st.is(ITEMS[k])) {
                        n[k] += st.getCount();
                     }
                  }
               }
            }
         }

         for (int kx = 0; kx < ITEMS.length; kx++) {
            if (n[kx] > 0) {
               out.add(new int[]{kx, n[kx]});
            } else if (HudManager.editing && kx < 4) {
               out.add(new int[]{kx, 0});
            }
         }

         return out;
      }

      @Override
      public int[] size(Minecraft mc) {
         int rows = counts(mc).size();
         return rows == 0 ? null : new int[]{44, rows * 18};
      }

      @Override
      public void draw(GuiGraphics g, Minecraft mc, int x, int y) {
         List<int[]> c = counts(mc);

         for (int i = 0; i < c.size(); i++) {
            int yy = y + i * 18;
            HudManager.box(g, this.id(), x, yy, 44, 17, Theme.current().accent);
            g.renderItem(new ItemStack(ITEMS[((int[])c.get(i))[0]]), x + 1, yy);
            g.drawString(mc.font, String.valueOf(c.get(i)[1]), x + 20, yy + 5, HudManager.fg(this.id(), -1), HudManager.shadow(this.id()));
         }
      }
   }

   private static final class JumpReset implements HudManager.Module {
      @Override
      public String id() {
         return "jumpreset";
      }

      private String value() {
         String s = PvpStats.jumpResetText(HudManager.de(), HudManager.opt(this.id(), "ticks", true), HudManager.opt(this.id(), "rate", true));
         if (s == null && HudManager.editing) {
            s = HudManager.de() ? "Jump Reset: perfekt (75%)" : "Jump reset: perfect (75%)";
         }

         return s;
      }

      @Override
      public int[] size(Minecraft mc) {
         String s = this.value();
         return s == null ? null : new int[]{mc.font.width(s) + 8, 13};
      }

      @Override
      public void draw(GuiGraphics g, Minecraft mc, int x, int y) {
         String s = this.value();
         if (s != null) {
            int w = mc.font.width(s) + 8;
            int col = PvpStats.jumpResetText(HudManager.de(), true, true) == null ? -11141240 : PvpStats.jumpResetColor();
            HudManager.box(g, this.id(), x, y, w, 13, col);
            g.drawString(mc.font, s, x + 5, y + 3, HudManager.fg(this.id(), col), HudManager.shadow(this.id()));
         }
      }
   }

   private static final class Keystrokes implements HudManager.Module {
      @Override
      public String id() {
         return "keystrokes";
      }

      @Override
      public int[] size(Minecraft mc) {
         int h = 46;
         if (HudManager.opt(this.id(), "mouse", true)) {
            h += 24;
         }

         if (HudManager.opt(this.id(), "space", true)) {
            h += 12;
         }

         return new int[]{70, h};
      }

      @Override
      public void draw(GuiGraphics g, Minecraft mc, int x, int y) {
         Options o = mc.options;
         key(g, mc, x + 24, y, 22, 22, "W", o.keyUp.isDown());
         key(g, mc, x, y + 24, 22, 22, "A", o.keyLeft.isDown());
         key(g, mc, x + 24, y + 24, 22, 22, "S", o.keyDown.isDown());
         key(g, mc, x + 48, y + 24, 22, 22, "D", o.keyRight.isDown());
         int yy = y + 48;
         if (HudManager.opt(this.id(), "mouse", true)) {
            boolean c = HudManager.opt(this.id(), "cps", true);
            key(g, mc, x, yy, 34, 22, c ? "LMB " + HudManager.cps(HudManager.LEFT) : "LMB", o.keyAttack.isDown());
            key(g, mc, x + 36, yy, 34, 22, c ? "RMB " + HudManager.cps(HudManager.RIGHT) : "RMB", o.keyUse.isDown());
            yy += 24;
         }

         if (HudManager.opt(this.id(), "space", true)) {
            key(g, mc, x, yy, 70, 10, "▬▬▬", o.keyJump.isDown());
         }
      }

      private static void key(GuiGraphics g, Minecraft mc, int x, int y, int w, int h, String label, boolean down) {
         String id = "keystrokes";
         int text = HudManager.fg(id, -1);
         int accent = HudManager.pos(id).textColor == 0 ? Theme.current().accent : text;
         int bg = HudManager.bg(id);
         if (down) {
            g.fill(x, y, x + w, y + h, accent & 16777215 | -1442840576);
         } else if (bg != 0) {
            g.fill(x, y, x + w, y + h, bg);
         }

         Font f = mc.font;
         g.drawString(f, label, x + (w - f.width(label)) / 2, y + (h - 8) / 2, down ? -16777216 : text, !down && HudManager.shadow(id));
      }
   }

   public interface Module {
      String id();

      int[] size(Minecraft var1);

      void draw(GuiGraphics var1, Minecraft var2, int var3, int var4);
   }

   interface Text {
      String get(Minecraft var1);
   }
}
