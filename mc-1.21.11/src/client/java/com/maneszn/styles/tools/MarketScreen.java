package com.maneszn.styles.tools;

import com.maneszn.styles.gui.Theme;
import com.maneszn.styles.gui.Ui;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MarketScreen extends ToolScreen {
   private static final String[] TABS = new String[]{"market", "spawner", "alarms", "money"};
   private static String tab = "market";
   private EditBox search;
   private EditBox alarmBox;
   private Market.Entry editing;
   private double scroll;

   public MarketScreen(Screen parent) {
      super("maneszn_styles.trade.title", parent);
   }

   public MarketScreen(Screen parent, String startTab) {
      this(parent);
      tab = startTab;
   }

   @Override
   protected int maxWidth() {
      return 420;
   }

   @Override
   protected void init() {
      super.init();
      String old = this.search != null ? this.search.getValue() : "";
      this.search = new EditBox(this.font, this.px + 10, this.py + 44, this.pw - 20, 16, Component.translatable("maneszn_styles.trade.search"));
      this.search.setHint(Component.translatable("maneszn_styles.trade.search"));
      this.search.setValue(old);
      this.addRenderableWidget(this.search);
      this.alarmBox = new EditBox(this.font, this.px + 10, this.py + this.ph - 24, 90, 16, Component.literal("alarm"));
      this.alarmBox.setHint(Component.literal("z. B. 25k"));
      this.alarmBox.visible = false;
      this.addRenderableWidget(this.alarmBox);
   }

   @Override
   protected void draw(GuiGraphics g, int mx, int my, Theme th) {
      int tx = this.px + 10;

      for (String t : TABS) {
         String label = Ui.tr("maneszn_styles.trade.tab." + t);
         int w = Ui.width(label) + 14;
         boolean sel = t.equals(tab);
         boolean hov = Ui.hover(mx, my, tx, this.py + 22, w, 16);
         Ui.rect(g, tx, this.py + 22, w, 16, sel ? th.accentDim() : (hov ? -14015424 : -14607314));
         Ui.textCentered(g, label, tx + w / 2, this.py + 26, sel ? -1185282 : -6646866);
         this.zones.add(tx, this.py + 22, w, 16, () -> {
            tab = t;
            this.scroll = 0.0;
            this.editing = null;
         });
         tx += w + 3;
      }

      this.search.visible = !tab.equals("money");
      this.alarmBox.visible = this.editing != null;
      if (tab.equals("money")) {
         this.drawMoney(g, th);
      } else {
         this.drawList(g, mx, my, th);
      }
   }

   private void drawList(GuiGraphics g, int mx, int my, Theme th) {
      String q = this.search.getValue().trim().toLowerCase(Locale.ROOT);
      List<Market.Entry> list = new ArrayList<>();

      for (Market.Entry e : Market.all().values()) {
         if ((!tab.equals("spawner") || e.spawner())
            && (!tab.equals("alarms") || !(e.alarm <= 0.0))
            && (q.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(q) || e.item.contains(q))) {
            list.add(e);
         }
      }

      list.sort(Comparator.comparingLong(ex -> -ex.seen));
      int y0 = this.py + 66;
      int bottom = this.py + this.ph - (this.editing != null ? 32 : 8);
      if (list.isEmpty()) {
         Ui.text(
            g,
            Ui.trim(Ui.tr(tab.equals("alarms") ? "maneszn_styles.trade.no_alarms" : "maneszn_styles.trade.empty"), this.pw - 20),
            this.px + 10,
            y0 + 4,
            -6646866
         );
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.trade.how"), this.pw - 20), this.px + 10, y0 + 18, -9739136);
      } else {
         String[] cols = Ui.tr("maneszn_styles.trade.col").split("\\|");
         int hx = this.px + this.pw - 210;
         Ui.text(g, cols[0], this.px + 14, y0, -9739136);

         for (int i = 1; i < cols.length; i++) {
            Ui.text(g, cols[i], hx + (i - 1) * 48, y0, -9739136);
         }

         int y = y0 + 12 - (int)this.scroll;
         g.enableScissor(this.px, y0 + 10, this.px + this.pw, bottom);

         for (Market.Entry ex : list) {
            if (y > bottom) {
               break;
            }

            if (y + 16 >= y0 + 10) {
               boolean hov = Ui.hover(mx, my, this.px + 10, y, this.pw - 20, 16);
               Ui.rect(g, this.px + 10, y, this.pw - 20, 16, ex == this.editing ? th.accentDim() : (hov ? -14015424 : -14607314));
               Ui.text(g, Ui.trim(ex.name, this.pw - 230), this.px + 14, y + 4, -1185282);
               int cx = this.px + this.pw - 210;
               Ui.text(g, Num.compact(ex.min), cx, y + 4, -14498466);
               Ui.text(g, Num.compact(ex.avg()), cx + 48, y + 4, -7859);
               Ui.text(g, Num.compact(ex.last), cx + 96, y + 4, -6646866);
               Ui.text(g, ex.alarm > 0.0 ? "⏰ " + Num.compact(ex.alarm) : "⏰", cx + 144, y + 4, ex.alarm > 0.0 ? th.accent : -9739136);
               this.zones.addClipped(this.px + 10, y, this.pw - 20, 16, y0 + 10, bottom, () -> {
                  this.editing = ex;
                  this.alarmBox.setValue(ex.alarm > 0.0 ? Num.compact(ex.alarm) : "");
                  this.setFocused(this.alarmBox);
               }, null);
            }

            y += 18;
         }

         g.disableScissor();
         int content = list.size() * 18;
         this.scroll = Math.max(0.0, Math.min(this.scroll, (double)Math.max(0, content - (bottom - y0 - 12))));
         if (this.editing != null) {
            int by = this.py + this.ph - 24;
            Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.trade.alarm_for", this.editing.name), this.pw - 230), this.px + 106, by + 4, -6646866);
            int bx = this.px + this.pw - 120;
            boolean h1 = Ui.hover(mx, my, bx, by, 54, 16);
            boolean h2 = Ui.hover(mx, my, bx + 58, by, 52, 16);
            Ui.button(g, bx, by, 54, 16, Ui.tr("maneszn_styles.trade.save"), h1, true, th);
            Ui.button(g, bx + 58, by, 52, 16, Ui.tr("maneszn_styles.trade.off"), h2, false, th);
            this.zones.add(bx, by, 54, 16, () -> {
               Double v = Num.parse(this.alarmBox.getValue());
               this.editing.alarm = v == null ? 0.0 : v;
               Market.save();
               this.editing = null;
            });
            this.zones.add(bx + 58, by, 52, 16, () -> {
               this.editing.alarm = 0.0;
               Market.save();
               this.editing = null;
            });
         }
      }
   }

   private void drawMoney(GuiGraphics g, Theme th) {
      int y = this.py + 46;
      Double bal = Money.balance();
      Ui.textShadow(g, Ui.tr("maneszn_styles.trade.balance", bal == null ? "–" : Num.full(bal) + " (" + Num.compact(bal) + ")"), this.px + 10, y, -1185282);
      y += 16;
      double[] t = Money.today();
      Ui.text(g, Ui.tr("maneszn_styles.trade.today"), this.px + 10, y, -6646866);
      Ui.text(g, "+" + Num.compact(t[0]), this.px + 110, y, -14498466);
      Ui.text(g, "-" + Num.compact(t[1]), this.px + 170, y, -1096636);
      Ui.text(g, "= " + Money.fmtSigned(t[0] - t[1]), this.px + 230, y, t[0] >= t[1] ? -14498466 : -1096636);
      y += 12;
      Ui.text(g, Ui.tr("maneszn_styles.trade.session"), this.px + 10, y, -6646866);
      Ui.text(g, "+" + Num.compact(Money.sessionIn()), this.px + 110, y, -14498466);
      Ui.text(g, "-" + Num.compact(Money.sessionOut()), this.px + 170, y, -1096636);
      y += 18;
      Ui.text(g, Ui.tr("maneszn_styles.trade.week"), this.px + 10, y, -9739136);
      y += 12;
      double max = 1.0;

      for (int i = 6; i >= 0; i--) {
         double[] d = Money.data().days.getOrDefault(LocalDate.now().minusDays(i).toString(), new double[2]);
         max = Math.max(max, Math.max(d[0], d[1]));
      }

      int bw = (this.pw - 20) / 7;

      for (int i = 6; i >= 0; i--) {
         LocalDate day = LocalDate.now().minusDays(i);
         double[] d = Money.data().days.getOrDefault(day.toString(), new double[2]);
         int x = this.px + 10 + (6 - i) * bw;
         int hIn = (int)(40.0 * d[0] / max);
         int hOut = (int)(40.0 * d[1] / max);
         Ui.rect(g, x + 4, y + 40 - hIn, bw / 2 - 5, hIn, -14498466);
         Ui.rect(g, x + bw / 2, y + 40 - hOut, bw / 2 - 5, hOut, -1096636);
         Ui.textCentered(g, day.getDayOfMonth() + ".", x + bw / 2, y + 43, -9739136);
      }

      y += 58;
      Ui.text(g, Ui.tr("maneszn_styles.trade.recent"), this.px + 10, y, -9739136);
      y += 12;
      List<Money.Change> ch = Money.data().changes;
      SimpleDateFormat f = new SimpleDateFormat("dd.MM. HH:mm");

      for (int i = ch.size() - 1; i >= 0 && y < this.py + this.ph - 12; i--) {
         Money.Change c = ch.get(i);
         Ui.text(g, f.format(new Date(c.time)), this.px + 14, y, -9739136);
         Ui.text(g, Money.fmtSigned(c.delta), this.px + 100, y, c.delta >= 0.0 ? -14498466 : -1096636);
         Ui.text(g, "→ " + Num.compact(c.balance), this.px + 170, y, -6646866);
         y += 11;
      }

      if (bal == null) {
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.trade.no_balance"), this.pw - 20), this.px + 10, this.py + this.ph - 14, -9739136);
      }
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double sx, double sy) {
      this.scroll -= sy * 18.0;
      if (this.scroll < 0.0) {
         this.scroll = 0.0;
      }

      return true;
   }
}
