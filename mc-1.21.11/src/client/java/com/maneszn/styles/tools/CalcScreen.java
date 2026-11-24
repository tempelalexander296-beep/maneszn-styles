package com.maneszn.styles.tools;

import com.maneszn.styles.gui.Theme;
import com.maneszn.styles.gui.Ui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class CalcScreen extends ToolScreen {
   private static final List<String> HISTORY = new ArrayList<>();
   private static final String[][] KEYS = new String[][]{
      {"7", "8", "9", "/", "C"}, {"4", "5", "6", "*", "⌫"}, {"1", "2", "3", "-", "("}, {"0", ".", "k", "+", ")"}, {"m", "b", "%", "=", "×64"}
   };
   private EditBox input;

   public CalcScreen(Screen parent) {
      super("maneszn_styles.calc.title", parent);
   }

   @Override
   protected int maxWidth() {
      return 220;
   }

   @Override
   protected void init() {
      super.init();
      this.ph = Math.min(this.ph, 250);
      this.py = (this.height - this.ph) / 2;
      String old = this.input != null ? this.input.getValue() : "";
      this.input = new EditBox(this.font, this.px + 10, this.py + 24, this.pw - 20, 18, Component.literal("calc"));
      this.input.setMaxLength(200);
      this.input.setValue(old);
      this.addRenderableWidget(this.input);
      this.setFocused(this.input);
      this.input.setFocused(true);
   }

   @Override
   protected void draw(GuiGraphics g, int mx, int my, Theme th) {
      Double r = eval(this.input.getValue());
      int y = this.py + 46;
      Ui.textShadow(g, r == null ? "= –" : "= " + Num.full(r), this.px + 10, y, r == null ? -6646866 : -1185282);
      if (r != null) {
         Ui.textRight(g, "(" + Num.compact(r) + ")", this.px + this.pw - 10, y, th.accent);
      }

      y += 16;
      int bw = (this.pw - 20 - 12) / 5;
      int bh = 18;

      for (String[] row : KEYS) {
         for (int i = 0; i < row.length; i++) {
            String k = row[i];
            int bx = this.px + 10 + i * (bw + 3);
            boolean hov = Ui.hover(mx, my, bx, y, bw, bh);
            Ui.button(g, bx, y, bw, bh, k, hov, k.equals("="), th);
            this.zones.add(bx, y, bw, bh, () -> this.press(k));
         }

         y += bh + 3;
      }

      y += 4;
      Ui.text(g, Ui.tr("maneszn_styles.calc.history"), this.px + 10, y, -9739136);
      y += 11;

      for (int i = HISTORY.size() - 1; i >= 0 && y < this.py + this.ph - 10; i--) {
         String h = HISTORY.get(i);
         boolean hov = Ui.hover(mx, my, this.px + 10, y - 1, this.pw - 20, 10);
         Ui.text(g, Ui.trim(h, this.pw - 20), this.px + 10, y, hov ? -1185282 : -6646866);
         String res = h.substring(h.lastIndexOf("= ") + 2);
         this.zones.add(this.px + 10, y - 1, this.pw - 20, 10, () -> this.input.setValue(res.replace(".", "").replace(",", ".")));
         y += 10;
      }
   }

   private void press(String k) {
      String v = this.input.getValue();
      switch (k) {
         case "C":
            this.input.setValue("");
            break;
         case "⌫":
            this.input.setValue(v.isEmpty() ? v : v.substring(0, v.length() - 1));
            break;
         case "=":
            this.commit();
            break;
         case "×64":
            this.input.setValue(v.isEmpty() ? "64" : "(" + v + ")*64");
            break;
         default:
            this.input.setValue(v + k);
      }

      this.setFocused(this.input);
   }

   private void commit() {
      Double r = eval(this.input.getValue());
      if (r != null) {
         HISTORY.add(this.input.getValue() + " = " + Num.full(r));

         while (HISTORY.size() > 20) {
            HISTORY.remove(0);
         }

         this.input.setValue(Num.full(r).replace(".", "").replace(",", "."));
      }
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      if (e.key() != 257 && e.key() != 335) {
         return super.keyPressed(e);
      } else {
         this.commit();
         return true;
      }
   }

   public static Double eval(String s) {
      if (s != null && !s.isBlank()) {
         try {
            CalcScreen.Parser p = new CalcScreen.Parser(s.toLowerCase().replace("×", "*").replace("x", "*").replace(":", "/").replace(" ", ""));
            double v = p.expr();
            return p.i == p.s.length() && !Double.isNaN(v) && !Double.isInfinite(v) ? v : null;
         } catch (RuntimeException var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static final class Parser {
      final String s;
      int i;

      Parser(String s) {
         this.s = s;
      }

      double expr() {
         double v = this.term();

         while (this.i < this.s.length()) {
            char c = this.s.charAt(this.i);
            if (c == '+') {
               this.i++;
               v += this.term();
            } else {
               if (c != '-') {
                  break;
               }

               this.i++;
               v -= this.term();
            }
         }

         return v;
      }

      double term() {
         double v = this.factor();

         while (this.i < this.s.length()) {
            char c = this.s.charAt(this.i);
            if (c == '*') {
               this.i++;
               v *= this.factor();
            } else {
               if (c != '/') {
                  break;
               }

               this.i++;
               v /= this.factor();
            }
         }

         return v;
      }

      double factor() {
         if (this.i < this.s.length() && this.s.charAt(this.i) == '-') {
            this.i++;
            return -this.factor();
         } else {
            double v;
            if (this.i < this.s.length() && this.s.charAt(this.i) == '(') {
               this.i++;
               v = this.expr();
               if (this.i < this.s.length() && this.s.charAt(this.i) == ')') {
                  this.i++;
               }
            } else {
               int st = this.i;

               while (this.i < this.s.length() && (Character.isDigit(this.s.charAt(this.i)) || this.s.charAt(this.i) == '.' || this.s.charAt(this.i) == ',')) {
                  this.i++;
               }

               if (st == this.i) {
                  throw new IllegalArgumentException();
               }

               v = Double.parseDouble(this.s.substring(st, this.i).replace(',', '.'));
            }

            while (this.i < this.s.length()) {
               char c = this.s.charAt(this.i);
               if (c == 'k') {
                  v *= 1000.0;
                  this.i++;
               } else if (c == 'm') {
                  v *= 1000000.0;
                  this.i++;
               } else if (c == 'b') {
                  v *= 1.0E9;
                  this.i++;
               } else {
                  if (c != '%') {
                     break;
                  }

                  v /= 100.0;
                  this.i++;
               }
            }

            return v;
         }
      }
   }
}
