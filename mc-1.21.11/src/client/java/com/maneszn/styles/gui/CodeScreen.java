package com.maneszn.styles.gui;

import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.cosmetic.CosmeticRegistry;
import com.maneszn.styles.net.ApiClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CodeScreen extends Screen {
   private final Screen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private EditBox nameBox;
   private EditBox codeBox;
   private String message = "";
   private int messageColor = -6646866;
   private boolean busy;
   private String reward = "vip";
   private int count = 1;
   private int uses = 1;
   private final List<String> newCodes = new ArrayList<>();

   public CodeScreen(Screen parent) {
      super(Component.translatable("maneszn_styles.code.title"));
      this.parent = parent;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private int pw() {
      return Math.min(this.width - 20, 330);
   }

   @Override
   protected void init() {
      int pw = this.pw();
      int px = (this.width - pw) / 2;
      String self = this.minecraft.getUser() == null ? "" : this.minecraft.getUser().getName();
      String oldName = this.nameBox == null ? self : this.nameBox.getValue();
      String oldCode = this.codeBox == null ? "" : this.codeBox.getValue();
      this.nameBox = new EditBox(this.font, px + 10, 52, pw - 20, 16, Component.translatable("maneszn_styles.code.name"));
      this.nameBox.setMaxLength(16);
      this.nameBox.setValue(oldName);
      this.addRenderableWidget(this.nameBox);
      this.codeBox = new EditBox(this.font, px + 10, 86, pw - 20, 16, Component.translatable("maneszn_styles.code.code"));
      this.codeBox.setMaxLength(19);
      this.codeBox.setValue(oldCode);
      this.codeBox.setResponder(v -> {
         String f = format(v);
         if (!f.equals(v)) {
            this.codeBox.setValue(f);
         }
      });
      this.addRenderableWidget(this.codeBox);
      this.setFocused(this.codeBox);
      this.codeBox.setFocused(true);
   }

   static String format(String raw) {
      StringBuilder sb = new StringBuilder();
      int n = 0;

      for (char ch : raw.toUpperCase(Locale.ROOT).toCharArray()) {
         if (Character.isLetterOrDigit(ch) && ch <= 127) {
            if (n == 16) {
               break;
            }

            if (n > 0 && n % 4 == 0) {
               sb.append('-');
            }

            sb.append(ch);
            n++;
         }
      }

      return sb.toString();
   }

   private static String plain(String code) {
      return code.replace("-", "").replace(" ", "");
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -1442512118);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float partialTick) {
      this.zones.clear();
      Theme th = Theme.current();
      ApiClient api = ApiClient.get();
      int pw = this.pw();
      int px = (this.width - pw) / 2;
      int py = 10;
      int ph = this.height - 20;
      Ui.rect(g, px, py, pw, ph, -15462116);
      Ui.border(g, px, py, pw, ph, -12486);
      Ui.textShadow(g, "\ud83c\udf81 " + Ui.tr("maneszn_styles.code.title"), px + 10, py + 7, -12486);
      String rank = api.isOwner() ? "OWNER" : (api.isAdmin() ? "ADMIN" : (api.entitled("legend") ? "LEGEND" : (api.entitled("vip") ? "VIP" : "–")));
      Ui.textRight(
         g,
         Ui.tr("maneszn_styles.code.rank", rank),
         px + pw - 10,
         py + 7,
         api.isOwner() ? -5085441 : (api.isAdmin() ? -54742 : (api.entitled("legend") ? -45715 : (api.entitled("vip") ? -12486 : -9739136)))
      );
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.code.hint"), pw - 20), px + 10, py + 19, -6646866);
      Ui.text(g, Ui.tr("maneszn_styles.code.name"), px + 10, 42, -1185282);
      Ui.text(g, Ui.tr("maneszn_styles.code.code"), px + 10, 76, -1185282);
      int len = plain(this.codeBox.getValue()).length();
      Ui.textRight(g, len + "/16", px + pw - 10, 76, len == 16 ? -14498466 : -9739136);
      super.render(g, mx, my, partialTick);
      boolean ready = len == 16 && !this.nameBox.getValue().isBlank() && !this.busy && api.loggedIn();
      int by = 108;
      boolean hov = Ui.hover(mx, my, px + 10, by, pw - 20, 18);
      Ui.button(g, px + 10, by, pw - 20, 18, Ui.tr(this.busy ? "maneszn_styles.code.checking" : "maneszn_styles.code.redeem"), hov && ready, ready, th);
      if (ready) {
         this.zones.add(px + 10, by, pw - 20, 18, this::redeem);
      }

      String msg = !api.loggedIn() ? Ui.tr("maneszn_styles.code.offline") : this.message;
      Ui.text(g, Ui.trim(msg, pw - 20), px + 10, by + 23, !api.loggedIn() ? -34953 : this.messageColor);
      int y = by + 38;

      for (String tier : new String[]{"vip", "legend"}) {
         int col = tier.equals("vip") ? -12486 : -45715;
         int n = 0;

         for (Cosmetic c : CosmeticRegistry.get().all()) {
            if (tier.equals(c.requires())) {
               n++;
            }
         }

         boolean has = api.entitled(tier);
         Ui.rect(g, px + 10, y, pw - 20, 14, -14607314);
         Ui.rect(g, px + 10, y, 2, 14, col);
         Ui.text(g, tier.toUpperCase(Locale.ROOT), px + 16, y + 3, col);
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.code.tier_" + tier, n), pw - 110), px + 58, y + 3, -6646866);
         Ui.textRight(g, has ? "✔" : "\ud83d\udd12", px + pw - 14, y + 3, has ? -14498466 : -9739136);
         y += 16;
      }

      if (api.isAdmin()) {
         y = this.renderAdmin(g, mx, my, th, px, pw, y + 6, py + ph - 26);
      }

      int dy = py + ph - 22;
      boolean hd = Ui.hover(mx, my, px + 10, dy, pw - 20, 16);
      Ui.button(g, px + 10, dy, pw - 20, 16, Ui.tr("maneszn_styles.hud.done"), hd, false, th);
      this.zones.add(px + 10, dy, pw - 20, 16, this::onClose);
   }

   private int renderAdmin(GuiGraphics g, int mx, int my, Theme th, int px, int pw, int y, int maxY) {
      ApiClient api = ApiClient.get();
      Ui.rect(g, px + 10, y, pw - 20, 1, -54742);
      y += 5;
      Ui.text(g, "⚑ " + Ui.tr("maneszn_styles.code.admin"), px + 10, y, -54742);
      y += 12;
      int x = px + 10;
      String[] tiers = api.isOwner() ? new String[]{"vip", "legend", "admin"} : new String[]{"vip", "legend"};

      for (String r : tiers) {
         int w = tiers.length > 2 ? 42 : 50;
         boolean hv = Ui.hover(mx, my, x, y, w, 14);
         Ui.button(g, x, y, w, 14, r.toUpperCase(Locale.ROOT), hv, r.equals(this.reward), th);
         this.zones.add(x, y, w, 14, () -> this.reward = r);
         x += w + 3;
      }

      x = this.stepper(
         g,
         mx,
         my,
         th,
         x + 4,
         y,
         Ui.tr("maneszn_styles.code.count", this.count),
         () -> this.count = Math.max(1, this.count - 1),
         () -> this.count = Math.min(20, this.count + 1)
      );
      this.stepper(
         g,
         mx,
         my,
         th,
         x + 4,
         y,
         Ui.tr("maneszn_styles.code.uses", this.uses),
         () -> this.uses = Math.max(1, this.uses - 1),
         () -> this.uses = Math.min(100, this.uses + 1)
      );
      y += 18;
      int half = (pw - 24) / 2;
      boolean h1 = Ui.hover(mx, my, px + 10, y, half, 16);
      Ui.button(g, px + 10, y, half, 16, Ui.tr("maneszn_styles.code.create"), h1 && !this.busy, !this.busy, th);
      if (!this.busy) {
         this.zones.add(px + 10, y, half, 16, this::createCodes);
      }

      int gx = px + 14 + half;
      int gw = (half - 3) / 2;
      boolean h2 = Ui.hover(mx, my, gx, y, gw, 16);
      boolean h3 = Ui.hover(mx, my, gx + gw + 3, y, gw, 16);
      Ui.button(g, gx, y, gw, 16, Ui.tr("maneszn_styles.code.grant"), h2, false, th);
      Ui.button(g, gx + gw + 3, y, gw, 16, Ui.tr("maneszn_styles.code.revoke"), h3, false, th);
      this.zones.add(gx, y, gw, 16, () -> this.grant(false));
      this.zones.add(gx + gw + 3, y, gw, 16, () -> this.grant(true));
      y += 20;
      if (!this.newCodes.isEmpty()) {
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.code.copy_hint"), pw - 20), px + 10, y, -9739136);
         y += 11;

         for (String c : this.newCodes) {
            if (y + 13 > maxY) {
               break;
            }

            boolean hv = Ui.hover(mx, my, px + 10, y, pw - 20, 12);
            Ui.rect(g, px + 10, y, pw - 20, 12, hv ? -14015424 : -14607314);
            Ui.text(g, c, px + 14, y + 2, -1);
            Ui.textRight(g, "⧉", px + pw - 14, y + 2, th.accent);
            this.zones.add(px + 10, y, pw - 20, 12, () -> {
               this.minecraft.keyboardHandler.setClipboard(c);
               this.say(Ui.tr("maneszn_styles.code.copied", c), -14498466);
            });
            y += 13;
         }
      }

      return y;
   }

   private int stepper(GuiGraphics g, int mx, int my, Theme th, int x, int y, String label, Runnable minus, Runnable plus) {
      Ui.button(g, x, y, 12, 14, "−", Ui.hover(mx, my, x, y, 12, 14), false, th);
      this.zones.add(x, y, 12, 14, minus);
      int lw = Ui.width(label) + 6;
      Ui.text(g, label, x + 15, y + 3, -1185282);
      Ui.button(g, x + 15 + lw, y, 12, 14, "+", Ui.hover(mx, my, x + 15 + lw, y, 12, 14), false, th);
      this.zones.add(x + 15 + lw, y, 12, 14, plus);
      return x + 27 + lw;
   }

   private void say(String msg, int color) {
      this.message = msg;
      this.messageColor = color;
   }

   private void redeem() {
      this.busy = true;
      this.say("", -6646866);
      ApiClient.get().redeem(plain(this.codeBox.getValue()), this.nameBox.getValue().trim(), (ok, res) -> {
         this.busy = false;
         if (ok) {
            this.say(Ui.tr("maneszn_styles.code.success", res[1], res[0].toUpperCase(Locale.ROOT)), -14498466);
            this.codeBox.setValue("");
         } else {
            this.say("offline".equals(res[0]) ? Ui.tr("maneszn_styles.code.offline") : res[0], -34953);
         }
      });
   }

   private void createCodes() {
      this.busy = true;
      ApiClient.get().adminCodes(this.reward, this.count, this.uses, (codes, err) -> {
         this.busy = false;
         if (codes == null) {
            this.say(err, -34953);
         } else {
            this.newCodes.addAll(0, codes);
            this.minecraft.keyboardHandler.setClipboard(String.join("\n", codes));
            this.say(Ui.tr("maneszn_styles.code.created", codes.size()), -14498466);
         }
      });
   }

   private void grant(boolean revoke) {
      String name = this.nameBox.getValue().trim();
      if (!name.isEmpty()) {
         ApiClient.get().adminGrant(name, this.reward, revoke, err -> {
            if (!err.isEmpty()) {
               this.say(err, -34953);
            } else {
               this.say(Ui.tr(revoke ? "maneszn_styles.code.revoked" : "maneszn_styles.code.granted", name, this.reward.toUpperCase(Locale.ROOT)), -14498466);
            }
         });
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (!this.nameBox.isMouseOver(event.x(), event.y()) && !this.codeBox.isMouseOver(event.x(), event.y())) {
         return this.zones.click(event.x(), event.y(), event.button()) ? true : super.mouseClicked(event, doubleClick);
      } else {
         return super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }
}
