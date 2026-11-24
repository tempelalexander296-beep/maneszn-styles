package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.emote.Emote;
import com.maneszn.styles.emote.EmoteManager;
import com.maneszn.styles.emote.Emotes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class EmoteScreen extends Screen {
   private int hovered = -1;
   private static int page;
   private final Ui.Zones zones = new Ui.Zones();
   private static final Map<String, Optional<Identifier>> ICONS = new HashMap<>();

   public EmoteScreen() {
      super(Component.translatable("maneszn_styles.emote.title"));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -2013265920);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      Theme th = Theme.current();
      this.zones.clear();
      page = Math.floorMod(page, Emotes.pages());
      List<Emote> all = Emotes.page(page);
      int cx = this.width / 2;
      int cy = this.height / 2;
      int r = Math.max(60, Math.min(this.width, this.height) / 2 - 40);
      int cw = 76;
      int ch = 26;
      double dx = mouseX - cx;
      double dy = mouseY - cy;
      this.hovered = -1;
      if (dx * dx + dy * dy > 676.0) {
         double ang = Math.atan2(dy, dx) + (Math.PI / 2);
         this.hovered = Math.floorMod(Math.round(ang / (Math.PI * 2) * all.size()), all.size());
      }

      String lang = Ui.langShort();

      for (int i = 0; i < all.size(); i++) {
         Emote e = all.get(i);
         double a = (-Math.PI / 2) + i * 2 * Math.PI / all.size();
         int x = (int)(cx + Math.cos(a) * r) - cw / 2;
         int y = (int)(cy + Math.sin(a) * r) - ch / 2;
         boolean hov = i == this.hovered;
         Ui.rect(g, x, y, cw, ch, hov ? th.accentDim() : -15462116);
         Ui.border(g, x, y, cw, ch, hov ? th.accent : -13752510);
         boolean open = e.unlocked();
         int icol = hov ? th.accent : -6646866;
         Identifier itex = open ? iconTexture(e.id()) : null;
         if (itex != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, itex, x + cw / 2 - 5, y + 2, 0.0F, 0.0F, 10, 10, 32, 32, 32, 32, icol | 0xFF000000);
         } else {
            Ui.textCentered(g, open ? e.icon() : "✖", x + cw / 2, y + 4, icol);
         }

         Ui.textCentered(g, Ui.trim(e.displayName(lang), cw - 6), x + cw / 2, y + 15, open ? -1185282 : -9739136);
         if (e.requires() != null) {
            String tag = e.requires().toUpperCase(Locale.ROOT);
            String var29 = e.requires();

            int col = switch (var29) {
               case "vip" -> -11141240;
               case "legend" -> -45715;
               case "owner" -> -5085441;
               default -> -54742;
            };
            Ui.textRight(g, tag.length() > 3 ? tag.substring(0, 3) : tag, x + cw - 3, y + 3, col);
         }

         Ui.text(g, String.valueOf((i + 1) % 10), x + 3, y + 3, -9739136);
      }

      Ui.rect(g, cx - 44, cy - 16, 88, 32, -15001818);
      Ui.border(g, cx - 44, cy - 16, 88, 32, th.accent);
      String title = this.hovered >= 0 ? all.get(this.hovered).displayName(lang) : Ui.tr("maneszn_styles.emote.title");
      Ui.textCentered(g, Ui.trim(title, 84), cx, cy - 10, -1185282);
      String hint = Ui.tr("maneszn_styles.emote.hint", ManesznStyles.emoteKey.getTranslatedKeyMessage().getString());
      Ui.textCentered(g, Ui.trim(hint, 84), cx, cy + 3, -9739136);
      String pg = Ui.tr("maneszn_styles.emote.page", page + 1, Emotes.pages());
      int pw = Ui.width(pg) + 50;
      int py = cy + 22;
      Ui.rect(g, cx - pw / 2, py, pw, 14, -15462116);
      Ui.textCentered(g, pg, cx, py + 3, -1185282);
      boolean hl = Ui.hover(mouseX, mouseY, cx - pw / 2, py, 18, 14);
      boolean hr = Ui.hover(mouseX, mouseY, cx + pw / 2 - 18, py, 18, 14);
      Ui.text(g, "◀", cx - pw / 2 + 5, py + 3, hl ? th.accent : -6646866);
      Ui.textRight(g, "▶", cx + pw / 2 - 5, py + 3, hr ? th.accent : -6646866);
      this.zones.add(cx - pw / 2, py, 18, 14, () -> page--);
      this.zones.add(cx + pw / 2 - 18, py, 18, 14, () -> page++);
      Ui.textCentered(g, Ui.tr("maneszn_styles.emote.stop_hint"), cx, this.height - 16, -6646866);
      Ui.textCentered(g, Ui.tr("maneszn_styles.emote.page_hint"), cx, this.height - 28, -9739136);
   }

   private static Identifier iconTexture(String id) {
      return ICONS.computeIfAbsent(id, k -> {
         Identifier rid = ManesznStyles.id("textures/gui/emote/" + k + ".png");
         boolean ok = Minecraft.getInstance().getResourceManager().getResource(rid).isPresent();
         return ok ? Optional.of(rid) : Optional.empty();
      }).orElse(null);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY != 0.0) {
         page += scrollY < 0.0 ? 1 : -1;
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.zones.click(event.x(), event.y(), event.button())) {
         this.hovered = -1;
         return true;
      } else if (event.button() == 0 && this.hovered >= 0) {
         this.play(this.hovered);
         return true;
      } else if (event.button() == 1) {
         this.onClose();
         return true;
      } else {
         return super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      int key = event.key();
      if (key >= 49 && key <= 57) {
         this.play(key - 49);
         return true;
      } else if (key == 48) {
         this.play(9);
         return true;
      } else if (key == 262 || key == 69) {
         page++;
         return true;
      } else if (key == 263 || key == 81) {
         page--;
         return true;
      } else if (ManesznStyles.emoteKey.matches(event)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   private void play(int index) {
      List<Emote> list = Emotes.page(Math.floorMod(page, Emotes.pages()));
      if (index >= 0 && index < list.size()) {
         Ui.clickSound();
         this.onClose();
         EmoteManager.play(list.get(index).id());
      }
   }
}
