package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.cosmetic.CosmeticManager;
import com.maneszn.styles.cosmetic.CosmeticRegistry;
import com.maneszn.styles.cosmetic.CustomCapes;
import com.maneszn.styles.cosmetic.SkinFetcher;
import com.maneszn.styles.name.NameDecorator;
import com.maneszn.styles.name.NameEffects;
import com.maneszn.styles.net.ApiClient;
import com.maneszn.styles.pack.SoundChoices;
import com.maneszn.styles.pack.StylePackBuilder;
import com.maneszn.styles.perf.FpsBoost;
import com.maneszn.styles.perf.PieChart;
import com.maneszn.styles.perf.PingOptimizer;
import com.maneszn.styles.render.CosmeticScenes;
import com.maneszn.styles.render.ItemScales;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.Group;
import com.maneszn.styles.style.Style;
import com.maneszn.styles.style.StyleRegistry;
import com.maneszn.styles.tools.CalcScreen;
import com.maneszn.styles.tools.MapartScreen;
import com.maneszn.styles.tools.MarketScreen;
import com.maneszn.styles.tools.TrimScreen;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class StylesScreen extends Screen {
   private static final Identifier LOGO = ManesznStyles.id("textures/gui/logo.png");
   private static final int HEADER_H = 24;
   private static final int BOTTOM_H = 22;
   private static final int GAP = 6;
   private static String lastCategory;
   private static StylesScreen.Tab lastTab = StylesScreen.Tab.COLLECTIONS;
   private static final Set<String> COLLAPSED = new HashSet<>();
   private static Cosmetic.Type cosType = Cosmetic.Type.CAPE;
   private static CosmeticScenes.BuddySpot buddySpot = CosmeticScenes.BuddySpot.LEFT_SHOULDER;
   private static final Cosmetic.Type[] COS_TYPES = new Cosmetic.Type[]{
      Cosmetic.Type.CAPE,
      Cosmetic.Type.HAT,
      Cosmetic.Type.FACE,
      Cosmetic.Type.BACK,
      Cosmetic.Type.HIP,
      Cosmetic.Type.SHOES,
      Cosmetic.Type.WINGS,
      Cosmetic.Type.AURA,
      Cosmetic.Type.BUDDY,
      Cosmetic.Type.PET,
      Cosmetic.Type.SKINFX,
      Cosmetic.Type.NAMECOLOR,
      Cosmetic.Type.NAMETAG,
      Cosmetic.Type.FRAME,
      Cosmetic.Type.LOGO
   };
   private static boolean outfitView;
   private int hoveredOutfit = -1;
   private String hoveredCos;
   private final Screen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private EditBox search;
   private EditBox skinBox;
   private EditBox outfitBox;
   private StylesScreen.Tab tab = lastTab;
   private StylesScreen.Filter filter = StylesScreen.Filter.ALL;
   private String categoryId;
   private String selectedStyle;
   private String hoveredStyle;
   private String zoomStyle;
   private double leftScroll;
   private double gridScroll;
   private int leftMaxScroll;
   private int gridMaxScroll;
   private String toast;
   private long toastUntil;
   private long resetArmedUntil;
   private boolean swallowSlash;
   private double spinYaw;
   private long lastFrameMs;
   private long lastDragMs;
   private boolean draggingPreview;
   private int previewX;
   private int previewY;
   private int previewSize;
   private int leftW;
   private int rightW;
   private int midX;
   private int midW;
   private int top;
   private int bottom;
   private int listY;
   private int listH;
   private int gridY;
   private int gridH;
   private static final int MAX_OUTFITS = 18;
   private long removeAllArmedUntil;
   private static final String[] EXTRAS_PAGES = new String[]{"general", "trading", "fps", "ping", "items", "look", "tools", "online"};
   private static String extrasPage = "general";
   private int clipTop = Integer.MIN_VALUE;
   private int clipBottom = Integer.MAX_VALUE;
   private static final int[] OUTLINE_COLORS = new int[]{0, -872415232, -1, -5745153, -50373, -11665558, -13377281, -7859, -41006, -12878849};
   private static final int[] HIT_COLORS = new int[]{0, -5745153, -12878849, -11665558, -7859, -24806, -41006, -13377281, -1, -16777216};
   private static final Item[] QUICK_ITEMS = new Item[]{
      Items.TOTEM_OF_UNDYING,
      Items.NETHERITE_SWORD,
      Items.DIAMOND_SWORD,
      Items.MACE,
      Items.ENDER_PEARL,
      Items.GOLDEN_APPLE,
      Items.END_CRYSTAL,
      Items.RESPAWN_ANCHOR,
      Items.SHIELD,
      Items.BOW,
      Items.SPLASH_POTION,
      Items.WIND_CHARGE
   };
   private boolean soundsDirty;
   private static final String[] GROUND_STYLES = new String[]{"vanilla", "2d", "flat", "static"};

   public StylesScreen(Screen parent) {
      super(Component.literal("Maneszn Styles"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.leftW = clamp(this.width * 22 / 100, 118, 170);
      this.rightW = clamp(this.width * 24 / 100, 118, 180);
      this.midX = 4 + this.leftW + 6;
      this.midW = this.width - this.midX - this.rightW - 6 - 4;
      this.top = 24;
      this.bottom = this.height - 22;
      String old = this.search == null ? "" : this.search.getValue();
      int sw = Math.max(60, Math.min(120, this.midW / 2 - 10));
      this.search = new EditBox(this.font, this.midX + this.midW - sw - 6, this.top + 8, sw, 12, Component.translatable("maneszn_styles.ui.search"));
      this.search.setMaxLength(40);
      this.search.setHint(Component.translatable("maneszn_styles.ui.search_hint"));
      this.search.setValue(old);
      this.search.setResponder(s -> this.gridScroll = 0.0);
      this.addRenderableWidget(this.search);
      String oldSkin = this.skinBox == null ? CosmeticManager.extra(buddySpot.skinKey()) : this.skinBox.getValue();
      this.skinBox = new EditBox(this.font, 0, 0, 80, 12, Component.translatable("maneszn_styles.buddy.skin_name"));
      this.skinBox.setMaxLength(16);
      this.skinBox.setHint(Component.translatable("maneszn_styles.buddy.skin_hint"));
      this.skinBox.setValue(oldSkin == null ? "" : oldSkin);
      this.skinBox.visible = false;
      this.addRenderableWidget(this.skinBox);
      String oldOutfit = this.outfitBox == null ? "" : this.outfitBox.getValue();
      this.outfitBox = new EditBox(this.font, 0, 0, 80, 12, Component.translatable("maneszn_styles.outfit.name"));
      this.outfitBox.setMaxLength(24);
      this.outfitBox.setHint(Component.translatable("maneszn_styles.outfit.name_hint"));
      this.outfitBox.setValue(oldOutfit);
      this.outfitBox.visible = false;
      this.addRenderableWidget(this.outfitBox);
      if (this.categoryId == null) {
         List<Category> vis = StyleRegistry.get().visibleCategories();
         Category last = lastCategory == null ? null : StyleRegistry.get().category(lastCategory);
         if (last != null && vis.contains(last)) {
            this.categoryId = last.id();
         } else if (!vis.isEmpty()) {
            this.categoryId = vis.get(0).id();
         }
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void onClose() {
      ModConfig.save();
      if (this.soundsDirty) {
         this.soundsDirty = false;
         StylePackBuilder.applyAndReload();
      }

      lastTab = this.tab;
      lastCategory = this.categoryId;
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -234092014);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.zones.clear();
      this.hoveredStyle = null;
      this.hoveredCos = null;
      this.hoveredOutfit = -1;
      Theme th = Theme.current();
      boolean zoom = this.zoomStyle != null;
      int mx = zoom ? -9999 : mouseX;
      int my = zoom ? -9999 : mouseY;
      this.renderHeader(g, mx, my, th);
      this.renderLeft(g, mx, my, th);
      switch (this.tab) {
         case COLLECTIONS:
            this.renderGrid(g, mx, my, th);
            break;
         case COSMETICS:
            if (outfitView) {
               this.renderOutfits(g, mx, my, th);
            } else {
               this.renderCosmetics(g, mx, my, th);
            }
            break;
         case EXTRAS:
            this.renderExtras(g, mx, my, th);
      }

      this.renderInspector(g, mx, my, th);
      this.renderBottom(g, mx, my, th);
      HolidayDecor.draw(g, th, this.width, this.height);
      this.search.visible = this.tab == StylesScreen.Tab.COLLECTIONS;
      if (this.tab != StylesScreen.Tab.COSMETICS || cosType != Cosmetic.Type.BUDDY || outfitView) {
         this.skinBox.visible = false;
      }

      if (this.tab != StylesScreen.Tab.COSMETICS || !outfitView) {
         this.outfitBox.visible = false;
      }

      super.render(g, mouseX, mouseY, partialTick);
      if (zoom) {
         this.renderZoom(g, th);
      }

      this.renderToast(g, th);
   }

   private void renderHeader(GuiGraphics g, int mx, int my, Theme th) {
      Ui.rect(g, 0, 0, this.width, 24, -15462116);
      Ui.rect(g, 0, 23, this.width, 1, -13752510);
      g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 6, 3, 0.0F, 0.0F, 18, 18, 64, 64, 64, 64);
      Ui.textShadow(g, "Maneszn", 29, 8, th.accent);
      Ui.text(g, " Styles", 29 + Ui.width("Maneszn"), 8, -1185282);
      ApiClient api = ApiClient.get();
      int n = api.online();
      String count = n >= 0 ? String.valueOf(n) : "–";

      String online = switch (api.status()) {
         case ONLINE -> Ui.tr("maneszn_styles.ui.online", count);
         case ANONYMOUS -> Ui.tr("maneszn_styles.ui.online_anon", count);
         case CONNECTING -> Ui.tr("maneszn_styles.ui.connecting");
         case AUTH_FAILED -> Ui.tr("maneszn_styles.ui.auth_failed");
         case OFFLINE -> Ui.tr("maneszn_styles.ui.offline");
         case DISABLED -> Ui.tr("maneszn_styles.ui.api_off");
      };

      int dotColor = switch (api.status()) {
         case ONLINE -> -14498466;
         case ANONYMOUS -> th.accent;
         case CONNECTING -> -340971;
         default -> -1096636;
      };
      int right = this.width - 8;
      Ui.textRight(g, online, right, 8, -6646866);
      int dotX = right - Ui.width(online) - 9;
      Ui.rect(g, dotX, 9, 5, 5, dotColor);
      if (StylePackBuilder.isReloading()) {
         Ui.textRight(g, Ui.tr("maneszn_styles.ui.reloading"), dotX - 10, 8, th.accent);
      }
   }

   private void renderLeft(GuiGraphics g, int mx, int my, Theme th) {
      int x = 4;
      int y = this.top + 4;
      int w = this.leftW;
      Ui.rect(g, x, y, w, this.bottom - y - 4, -15462116);
      StylesScreen.Tab[] tabs = StylesScreen.Tab.values();
      int tw = (w - 4) / tabs.length;

      for (int i = 0; i < tabs.length; i++) {
         StylesScreen.Tab t = tabs[i];
         int tx = x + 2 + i * tw;
         boolean sel = this.tab == t;
         boolean hov = Ui.hover(mx, my, tx, y + 2, tw - 2, 16);
         Ui.rect(g, tx, y + 2, tw - 2, 16, sel ? th.accentDim() : (hov ? -14015424 : -15001818));
         if (sel) {
            Ui.rect(g, tx, y + 17, tw - 2, 1, th.accent);
         }

         String label = Ui.trim(Ui.tr("maneszn_styles.tab." + t.name().toLowerCase(Locale.ROOT)), tw - 6);
         Ui.textCentered(g, label, tx + (tw - 2) / 2, y + 6, sel ? -1185282 : -6646866);
         this.zones.add(tx, y + 2, tw - 2, 16, () -> {
            this.tab = t;
            lastTab = t;
            this.gridScroll = 0.0;
            this.leftScroll = 0.0;
         });
      }

      this.listY = y + 22;
      this.listH = this.bottom - 8 - this.listY;
      switch (this.tab) {
         case COLLECTIONS:
            this.renderCategoryList(g, x, w, mx, my, th);
            break;
         case COSMETICS:
            this.renderCosmeticTypes(g, x, w, mx, my, th);
            break;
         case EXTRAS:
            this.renderExtrasPages(g, x, w, mx, my, th);
      }
   }

   private void renderCategoryList(GuiGraphics g, int x, int w, int mx, int my, Theme th) {
      StyleRegistry reg = StyleRegistry.get();
      ModConfig cfg = ModConfig.get();
      String lang = Ui.langShort();
      List<Category> visible = reg.visibleCategories();
      g.enableScissor(x, this.listY, x + w, this.listY + this.listH);
      int cy = this.listY - (int)this.leftScroll;

      for (Group grp : reg.groups()) {
         List<Category> cats = new ArrayList<>();

         for (Category c : visible) {
            if (c.group().equals(grp.id())) {
               cats.add(c);
            }
         }

         if (!cats.isEmpty()) {
            boolean col = COLLAPSED.contains(grp.id());
            boolean hovG = this.inList(mx, my) && Ui.hover(mx, my, x, cy, w, 14);
            Ui.text(g, (col ? "▸ " : "▾ ") + grp.displayName(lang), x + 6, cy + 3, hovG ? -1185282 : -9739136);
            Ui.textRight(g, String.valueOf(cats.size()), x + w - 6, cy + 3, -9739136);
            this.zones.addClipped(x, cy, w, 14, this.listY, this.listY + this.listH, () -> {
               if (!COLLAPSED.remove(grp.id())) {
                  COLLAPSED.add(grp.id());
               }
            }, null);
            cy += 15;
            if (!col) {
               for (Category cx : cats) {
                  boolean sel = cx.id().equals(this.categoryId);
                  boolean hov = this.inList(mx, my) && Ui.hover(mx, my, x + 2, cy, w - 4, 26);
                  if (sel || hov) {
                     Ui.rect(g, x + 2, cy, w - 4, 26, sel ? th.accentDim() : -14015424);
                  }

                  if (sel) {
                     Ui.rect(g, x + 2, cy, 2, 26, th.accent);
                  }

                  String activeId = cfg.activeStyle(cx.id());
                  Style active = "vanilla".equals(activeId) ? null : reg.style(cx.id(), activeId);
                  int iconY = cy + 5;
                  TextureCache.icon(cx, active).ifPresent(t -> Ui.texture(g, t, x + 7, iconY, 16, 16));
                  String count = String.valueOf(reg.styles(cx.id()).size());
                  int textW = w - 34 - Ui.width(count) - 6;
                  Ui.text(g, Ui.trim(cx.displayName(lang), textW), x + 28, cy + 4, -1185282);
                  String activeName = active == null ? "Vanilla" : active.name();
                  Ui.text(g, Ui.trim(activeName, textW), x + 28, cy + 15, sel ? th.accent : -6646866);
                  Ui.textRight(g, count, x + w - 7, cy + 9, -9739136);
                  this.zones.addClipped(x + 2, cy, w - 4, 26, this.listY, this.listY + this.listH, () -> this.selectCategory(cx.id()), null);
                  cy += 27;
               }

               cy += 3;
            }
         }
      }

      g.disableScissor();
      int contentH = cy + (int)this.leftScroll - this.listY;
      this.leftMaxScroll = Math.max(0, contentH - this.listH);
      this.leftScroll = Math.max(0.0, Math.min(this.leftScroll, (double)this.leftMaxScroll));
      this.drawScrollbar(g, x + w - 3, this.listY, this.listH, this.leftScroll, this.leftMaxScroll, contentH, th);
   }

   private void renderStaticList(GuiGraphics g, int x, int w, Theme th, String[] keys) {
      int cy = this.listY;

      for (String k : keys) {
         boolean first = k.equals(keys[0]);
         if (first) {
            Ui.rect(g, x + 2, cy, w - 4, 20, th.accentDim());
            Ui.rect(g, x + 2, cy, 2, 20, th.accent);
         }

         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.side." + k), w - 14), x + 9, cy + 6, first ? -1185282 : -6646866);
         cy += 21;
      }
   }

   private boolean inList(int mx, int my) {
      return my >= this.listY && my < this.listY + this.listH;
   }

   private void selectCategory(String id) {
      this.categoryId = id;
      lastCategory = id;
      this.selectedStyle = null;
      this.gridScroll = 0.0;
   }

   private void renderGrid(GuiGraphics g, int mx, int my, Theme th) {
      int x = this.midX;
      int y = this.top + 4;
      Ui.rect(g, x, y, this.midW, this.bottom - y - 4, -15462116);
      StyleRegistry reg = StyleRegistry.get();
      ModConfig cfg = ModConfig.get();
      Category c = this.categoryId == null ? null : reg.category(this.categoryId);
      if (c == null) {
         Ui.textCentered(g, Ui.tr("maneszn_styles.ui.empty"), x + this.midW / 2, y + 40, -6646866);
      } else {
         String lang = Ui.langShort();
         String activeId = cfg.activeStyle(c.id());
         Style activeStyle = "vanilla".equals(activeId) ? null : reg.style(c.id(), activeId);
         List<Style> all = reg.styles(c.id());
         int titleW = this.midW - this.search.getWidth() - 20;
         Ui.textShadow(g, Ui.trim(c.displayName(lang), titleW), x + 8, y + 6, -1185282);
         String sub = Ui.tr("maneszn_styles.ui.count", all.size() + 1) + " · " + (activeStyle == null ? "Vanilla" : activeStyle.name());
         Ui.text(g, Ui.trim(sub, titleW), x + 8, y + 18, -6646866);
         int favCount = 0;
         if (cfg.isFavorite(c.id(), "vanilla")) {
            favCount++;
         }

         for (Style s : all) {
            if (cfg.isFavorite(c.id(), s.id())) {
               favCount++;
            }
         }

         String[] labels = new String[]{
            Ui.tr("maneszn_styles.filter.all"), "★ " + Ui.tr("maneszn_styles.filter.favorites") + " (" + favCount + ")", Ui.tr("maneszn_styles.filter.new")
         };
         int fx = x + this.midW - 6;

         for (int i = labels.length - 1; i >= 0; i--) {
            StylesScreen.Filter f = StylesScreen.Filter.values()[i];
            int bw = Ui.width(labels[i]) + 10;
            fx -= bw;
            boolean sel = this.filter == f;
            boolean hov = Ui.hover(mx, my, fx, y + 22, bw, 11);
            Ui.rect(g, fx, y + 22, bw, 11, sel ? th.accent : (hov ? -14015424 : -15001818));
            Ui.text(g, labels[i], fx + 5, y + 24, sel ? -16054254 : -6646866);
            this.zones.add(fx, y + 22, bw, 11, () -> {
               this.filter = f;
               this.gridScroll = 0.0;
            });
            fx -= 3;
         }

         List<StylesScreen.Entry> entries = new ArrayList<>();
         entries.add(new StylesScreen.Entry("vanilla", null));
         String q = this.search.getValue().trim().toLowerCase(Locale.ROOT);

         for (Style sx : all) {
            if ((q.isEmpty() || sx.name().toLowerCase(Locale.ROOT).contains(q) || sx.sourceLabel(lang).toLowerCase(Locale.ROOT).contains(q))
               && (this.filter != StylesScreen.Filter.FAVORITES || cfg.isFavorite(c.id(), sx.id()))
               && (this.filter != StylesScreen.Filter.NEW || sx.isNew())) {
               entries.add(new StylesScreen.Entry(sx.id(), sx));
            }
         }

         this.gridY = y + 38;
         this.gridH = this.bottom - 8 - this.gridY;
         int inner = this.midW - 12;
         int cols = inner >= 242 ? 4 : Math.max(1, (inner + 6) / 62);
         int cardW = (inner - (cols - 1) * 6) / cols;
         int pv = cardW - 8;
         int cardH = pv + 30;
         g.enableScissor(x + 2, this.gridY, x + this.midW - 2, this.gridY + this.gridH);
         long now = System.currentTimeMillis();

         for (int i = 0; i < entries.size(); i++) {
            StylesScreen.Entry e = entries.get(i);
            int cx = x + 6 + i % cols * (cardW + 6);
            int cy = this.gridY + i / cols * (cardH + 6) - (int)this.gridScroll;
            if (cy <= this.gridY + this.gridH && cy + cardH >= this.gridY) {
               boolean visibleMouse = my >= this.gridY && my < this.gridY + this.gridH;
               boolean hov = visibleMouse && Ui.hover(mx, my, cx, cy, cardW, cardH);
               if (hov) {
                  this.hoveredStyle = e.id();
               }

               boolean isActive = e.id().equals(activeId);
               Ui.rect(g, cx, cy, cardW, cardH, hov ? -14015424 : -14607314);
               Ui.rect(g, cx + 4, cy + 4, pv, pv, -15001818);
               Optional<TextureCache.Tex> tex = TextureCache.preview(c, e.style());
               if (tex.isPresent()) {
                  Ui.texture(g, tex.get(), cx + 8, cy + 8, pv - 8, pv - 8);
               } else {
                  Ui.textCentered(g, "?", cx + 4 + pv / 2, cy + 4 + pv / 2 - 4, -9739136);
               }

               String name = e.style() == null ? "Vanilla" : e.style().name();
               String src = e.style() == null ? "Minecraft" : e.style().sourceLabel(lang);
               Ui.text(g, Ui.trim(name, cardW - 8), cx + 4, cy + pv + 8, -1185282);
               Ui.text(g, Ui.trim(src, cardW - 8), cx + 4, cy + pv + 18, -9739136);
               if (e.style() != null && e.style().isNew()) {
                  int bw = Ui.width("NEW") + 4;
                  Ui.rect(g, cx + 4, cy + 4, bw, 10, th.accent);
                  Ui.text(g, "NEW", cx + 6, cy + 5, -16054254);
               }

               if (cfg.isFavorite(c.id(), e.id())) {
                  Ui.textRight(g, "★", cx + cardW - 5, cy + 6, -340971);
               }

               if (isActive) {
                  int a = 120 + (int)(100.0 * (0.5 + 0.5 * Math.sin(now / 280.0)));
                  Ui.border(g, cx - 1, cy - 1, cardW + 2, cardH + 2, th.accentAlpha(a));
                  Ui.border(g, cx, cy, cardW, cardH, th.accent);
                  Ui.rect(g, cx + cardW - 9, cy + cardH - 9, 5, 5, -14498466);
               } else if (hov) {
                  Ui.border(g, cx, cy, cardW, cardH, -13752510);
               }

               String id = e.id();
               this.zones.addClipped(cx, cy, cardW, cardH, this.gridY, this.gridY + this.gridH, () -> this.apply(c, id), () -> this.zoomStyle = id);
            }
         }

         g.disableScissor();
         int rows = (entries.size() + cols - 1) / cols;
         int contentH = rows * (cardH + 6);
         this.gridMaxScroll = Math.max(0, contentH - this.gridH);
         this.gridScroll = Math.max(0.0, Math.min(this.gridScroll, (double)this.gridMaxScroll));
         this.drawScrollbar(g, x + this.midW - 4, this.gridY, this.gridH, this.gridScroll, this.gridMaxScroll, contentH, th);
      }
   }

   private void renderOutfits(GuiGraphics g, int mx, int my, Theme th) {
      int x = this.midX;
      int y = this.top + 4;
      Ui.rect(g, x, y, this.midW, this.bottom - y - 4, -15462116);
      ModConfig cfg = ModConfig.get();
      int clearW = Ui.width(Ui.tr("maneszn_styles.cosmetics.remove_all")) + 12;
      Ui.textShadow(g, Ui.trim(Ui.tr("maneszn_styles.side.outfits"), this.midW - 24 - clearW), x + 8, y + 6, -1185282);
      this.renderRemoveAll(g, mx, my, th, x + this.midW - 8 - clearW, y + 5, clearW);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.outfit.help"), this.midW - 16), x + 8, y + 18, -6646866);
      int saveW = Ui.width(Ui.tr("maneszn_styles.outfit.save")) + 12;
      this.outfitBox.visible = true;
      this.outfitBox.setX(x + 8);
      this.outfitBox.setY(y + 30);
      this.outfitBox.setWidth(this.midW - 16 - saveW - 4);
      boolean hs = Ui.hover(mx, my, x + this.midW - 8 - saveW, y + 29, saveW, 14);
      boolean canSave = CosmeticManager.equippedCount() > 0;
      Ui.button(g, x + this.midW - 8 - saveW, y + 29, saveW, 14, Ui.tr("maneszn_styles.outfit.save"), hs && canSave, canSave, th);
      if (canSave) {
         this.zones.add(x + this.midW - 8 - saveW, y + 29, saveW, 14, this::saveOutfit);
      }

      this.gridY = y + 48;
      this.gridH = this.bottom - 8 - this.gridY;
      int inner = this.midW - 12;
      int cols = inner >= 242 ? 4 : Math.max(1, (inner + 6) / 62);
      int cardW = (inner - (cols - 1) * 6) / cols;
      int pv = cardW - 8;
      int cardH = pv + 20;
      List<ModConfig.Outfit> list = cfg.outfits;
      if (list.isEmpty()) {
         Ui.textCentered(g, Ui.tr("maneszn_styles.outfit.empty"), x + this.midW / 2, this.gridY + 20, -9739136);
      }

      g.enableScissor(x + 2, this.gridY, x + this.midW - 2, this.gridY + this.gridH);
      long now = System.currentTimeMillis();

      for (int i = 0; i < list.size(); i++) {
         ModConfig.Outfit o = list.get(i);
         int cx = x + 6 + i % cols * (cardW + 6);
         int cy = this.gridY + i / cols * (cardH + 6) - (int)this.gridScroll;
         if (cy <= this.gridY + this.gridH && cy + cardH >= this.gridY) {
            boolean visibleMouse = my >= this.gridY && my < this.gridY + this.gridH;
            boolean hov = visibleMouse && Ui.hover(mx, my, cx, cy, cardW, cardH);
            if (hov) {
               this.hoveredOutfit = i;
            }

            boolean active = o.cosmetics.equals(withoutSkins(cfg.cosmetics)) || o.cosmetics.equals(cfg.cosmetics);
            Ui.rect(g, cx, cy, cardW, cardH, hov ? -14015424 : -14607314);
            Ui.rect(g, cx + 4, cy + 4, pv, pv, -15001818);
            CosmeticPreviews.outfit(o).ifPresent(t -> Ui.texture(g, t, cx + 6, cy + 6, pv - 4, pv - 4));
            Ui.text(g, Ui.trim(o.name, cardW - 8), cx + 4, cy + pv + 8, -1185282);
            if (active) {
               int a = 120 + (int)(100.0 * (0.5 + 0.5 * Math.sin(now / 280.0)));
               Ui.border(g, cx - 1, cy - 1, cardW + 2, cardH + 2, th.accentAlpha(a));
               Ui.border(g, cx, cy, cardW, cardH, th.accent);
            } else if (hov) {
               Ui.border(g, cx, cy, cardW, cardH, -13752510);
            }

            int idx = i;
            this.zones.addClipped(cx, cy, cardW, cardH, this.gridY, this.gridY + this.gridH, () -> this.wearOutfit(idx), null);
            if (hov) {
               int bx = cx + cardW - 15;
               Ui.button(g, bx, cy + 3, 12, 12, "✕", Ui.hover(mx, my, bx, cy + 3, 12, 12), false, th);
               this.zones.addClipped(bx, cy + 3, 12, 12, this.gridY, this.gridY + this.gridH, () -> {
                  cfg.outfits.remove(idx);
                  ModConfig.save();
                  this.toast(Ui.tr("maneszn_styles.toast.outfit_deleted", o.name));
               }, null);
               int ux = bx - 14;
               Ui.button(g, ux, cy + 3, 12, 12, "⟳", Ui.hover(mx, my, ux, cy + 3, 12, 12), false, th);
               this.zones.addClipped(ux, cy + 3, 12, 12, this.gridY, this.gridY + this.gridH, () -> {
                  o.cosmetics = withoutSkins(cfg.cosmetics);
                  keepSkins(o.cosmetics, cfg.cosmetics);
                  ModConfig.save();
                  CosmeticPreviews.clearOutfits();
                  this.toast(Ui.tr("maneszn_styles.toast.outfit_updated", o.name));
               }, null);
            }
         }
      }

      g.disableScissor();
      int rows = (list.size() + cols - 1) / cols;
      int contentH = rows * (cardH + 6);
      this.gridMaxScroll = Math.max(0, contentH - this.gridH);
      this.gridScroll = Math.max(0.0, Math.min(this.gridScroll, (double)this.gridMaxScroll));
      this.drawScrollbar(g, x + this.midW - 4, this.gridY, this.gridH, this.gridScroll, this.gridMaxScroll, contentH, th);
   }

   private static Map<String, String> withoutSkins(Map<String, String> m) {
      Map<String, String> out = new LinkedHashMap<>();

      for (Map.Entry<String, String> e : m.entrySet()) {
         if (!e.getKey().startsWith("skin_")) {
            out.put(e.getKey(), e.getValue());
         }
      }

      return out;
   }

   private static void keepSkins(Map<String, String> outfit, Map<String, String> from) {
      for (CosmeticScenes.BuddySpot sp : CosmeticScenes.BuddySpot.values()) {
         if (outfit.containsKey(sp.key()) && from.containsKey(sp.skinKey())) {
            outfit.put(sp.skinKey(), from.get(sp.skinKey()));
         }
      }
   }

   private void saveOutfit() {
      ModConfig cfg = ModConfig.get();
      String name = this.outfitBox.getValue().trim();
      if (name.isEmpty()) {
         name = "Outfit #" + (cfg.outfits.size() + 1);
      }

      Map<String, String> m = withoutSkins(cfg.cosmetics);
      keepSkins(m, cfg.cosmetics);

      for (ModConfig.Outfit o : cfg.outfits) {
         if (o.name.equalsIgnoreCase(name)) {
            o.cosmetics = m;
            ModConfig.save();
            CosmeticPreviews.clearOutfits();
            this.toast(Ui.tr("maneszn_styles.toast.outfit_updated", o.name));
            this.outfitBox.setValue("");
            return;
         }
      }

      if (cfg.outfits.size() >= 18) {
         this.toast(Ui.tr("maneszn_styles.toast.outfit_full", 18));
      } else {
         cfg.outfits.add(new ModConfig.Outfit(name, m));
         ModConfig.save();
         this.outfitBox.setValue("");
         this.outfitBox.setFocused(false);
         this.setFocused(null);
         this.toast(Ui.tr("maneszn_styles.toast.outfit_saved", name));
      }
   }

   private void wearOutfit(int idx) {
      ModConfig cfg = ModConfig.get();
      if (idx >= 0 && idx < cfg.outfits.size()) {
         ModConfig.Outfit o = cfg.outfits.get(idx);
         Map<String, String> m = new LinkedHashMap<>(o.cosmetics);

         for (Map.Entry<String, String> e : cfg.cosmetics.entrySet()) {
            if (e.getKey().startsWith("skin_")) {
               m.putIfAbsent(e.getKey(), e.getValue());
            }
         }

         CosmeticManager.applyLoadout(m);
         TextureCache.clear();
         this.toast(Ui.tr("maneszn_styles.toast.outfit_worn", o.name));
      }
   }

   private static String typeKey(Cosmetic.Type t) {
      return switch (t) {
         case CAPE -> "capes";
         case HAT -> "hats";
         case FACE -> "faces";
         case BACK -> "backs";
         case HIP -> "hips";
         case SHOES -> "shoes";
         case BELT -> "belts";
         case JEWELRY -> "jewelry";
         case KILLFX -> "killfx";
         case NAMEBG -> "namebg";
         case WINGS -> "wings";
         case AURA -> "auras";
         case BUDDY -> "buddies";
         case PET -> "pets";
         case SKINFX -> "skinfx";
         case NAMECOLOR -> "nametag";
         case NAMETAG -> "nametag_anim";
         case FRAME -> "frames";
         case LOGO -> "logos";
      };
   }

   private void renderCosmeticTypes(GuiGraphics g, int x, int w, int mx, int my, Theme th) {
      String lang = Ui.langShort();
      g.enableScissor(x, this.listY, x + w, this.listY + this.listH);
      int cy = this.listY - (int)this.leftScroll;

      for (Cosmetic.Type t : COS_TYPES) {
         boolean sel = t == cosType && !outfitView;
         boolean hov = this.inList(mx, my) && Ui.hover(mx, my, x + 2, cy, w - 4, 24);
         if (sel || hov) {
            Ui.rect(g, x + 2, cy, w - 4, 24, sel ? th.accentDim() : -14015424);
         }

         if (sel) {
            Ui.rect(g, x + 2, cy, 2, 24, th.accent);
         }

         String count = String.valueOf(CosmeticRegistry.get().list(t).size());
         int textW = w - 20 - Ui.width(count);
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.side." + typeKey(t)), textW), x + 9, cy + 3, -1185282);
         String eqName;
         if (t == Cosmetic.Type.BUDDY) {
            int n = CosmeticManager.buddies(ModConfig.get().cosmetics).size();
            eqName = n == 0 ? Ui.tr("maneszn_styles.cosmetics.none") : Ui.tr("maneszn_styles.buddy.count", n);
         } else {
            Cosmetic eq = CosmeticRegistry.get().get(t, CosmeticManager.equipped(t));
            eqName = eq == null ? Ui.tr("maneszn_styles.cosmetics.none") : eq.displayName(lang);
         }

         Ui.text(g, Ui.trim(eqName, textW), x + 9, cy + 13, sel ? th.accent : -6646866);
         Ui.textRight(g, count, x + w - 7, cy + 8, -9739136);
         this.zones.addClipped(x + 2, cy, w - 4, 24, this.listY, this.listY + this.listH, () -> {
            cosType = t;
            outfitView = false;
            this.gridScroll = 0.0;
         }, null);
         cy += 25;
      }

      boolean selx = outfitView;
      boolean hovx = this.inList(mx, my) && Ui.hover(mx, my, x + 2, cy, w - 4, 24);
      if (selx || hovx) {
         Ui.rect(g, x + 2, cy, w - 4, 24, selx ? th.accentDim() : -14015424);
      }

      if (selx) {
         Ui.rect(g, x + 2, cy, 2, 24, th.accent);
      }

      String count = String.valueOf(ModConfig.get().outfits.size());
      int textW = w - 20 - Ui.width(count);
      Ui.text(g, Ui.trim("\ud83d\udc55 " + Ui.tr("maneszn_styles.side.outfits"), textW), x + 9, cy + 3, -1185282);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.outfit.sub"), textW), x + 9, cy + 13, selx ? th.accent : -6646866);
      Ui.textRight(g, count, x + w - 7, cy + 8, -9739136);
      this.zones.addClipped(x + 2, cy, w - 4, 24, this.listY, this.listY + this.listH, () -> {
         outfitView = true;
         this.gridScroll = 0.0;
      }, null);
      cy += 25;
      selx = this.inList(mx, my) && Ui.hover(mx, my, x + 2, cy, w - 4, 24);
      Ui.rect(g, x + 2, cy, w - 4, 24, selx ? -14015424 : -14607314);
      Ui.rect(g, x + 2, cy, 2, 24, -12486);
      Ui.text(g, Ui.trim("\ud83c\udf81 " + Ui.tr("maneszn_styles.code.title"), w - 14), x + 9, cy + 3, -12486);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.code.sub"), w - 14), x + 9, cy + 13, -6646866);
      this.zones.addClipped(x + 2, cy, w - 4, 24, this.listY, this.listY + this.listH, () -> this.minecraft.setScreen(new CodeScreen(this)), null);
      cy += 25;
      cy += 4;
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.cosmetics.visual_only"), w - 12), x + 6, cy, -9739136);
      String emoteKey = ManesznStyles.emoteKey.getTranslatedKeyMessage().getString();
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.cosmetics.emote_hint", emoteKey), w - 12), x + 6, cy + 11, th.accent);
      g.disableScissor();
      int leftContentH = cy + 24 + (int)this.leftScroll - this.listY;
      this.leftMaxScroll = Math.max(0, leftContentH - this.listH);
      this.leftScroll = Math.max(0.0, Math.min(this.leftScroll, (double)this.leftMaxScroll));
      this.drawScrollbar(g, x + w - 3, this.listY, this.listH, this.leftScroll, this.leftMaxScroll, leftContentH, th);
   }

   private void renderCosmetics(GuiGraphics g, int mx, int my, Theme th) {
      int x = this.midX;
      int y = this.top + 4;
      Ui.rect(g, x, y, this.midW, this.bottom - y - 4, -15462116);
      String lang = Ui.langShort();
      List<Cosmetic> all = new ArrayList<>(CosmeticRegistry.get().list(cosType));
      all.removeIf(CosmeticManager::hidden);
      String equipped = equippedId(cosType);
      Cosmetic eq = CosmeticRegistry.get().get(cosType, equipped);
      int clearW = Ui.width(Ui.tr("maneszn_styles.cosmetics.remove_all")) + 12;
      Ui.textShadow(g, Ui.trim(Ui.tr("maneszn_styles.side." + typeKey(cosType)), this.midW - 24 - clearW), x + 8, y + 6, -1185282);
      this.renderRemoveAll(g, mx, my, th, x + this.midW - 8 - clearW, y + 5, clearW);
      if (cosType == Cosmetic.Type.CAPE) {
         String up = Ui.tr("maneszn_styles.cape.upload");
         int uw = Ui.width(up) + 12;
         int ux = x + this.midW - 8 - clearW - 4 - uw;
         boolean hu = Ui.hover(mx, my, ux, y + 5, uw, 13);
         Ui.button(g, ux, y + 5, uw, 13, up, hu, true, th);
         this.zones.add(ux, y + 5, uw, 13, this::pickCape);
      }

      String sub = Ui.tr("maneszn_styles.ui.count", all.size() + 1)
         + " · "
         + (
            cosType == Cosmetic.Type.BUDDY
               ? Ui.tr("maneszn_styles.buddy.count", CosmeticManager.buddies(ModConfig.get().cosmetics).size())
               : (eq == null ? Ui.tr("maneszn_styles.cosmetics.none") : eq.displayName(lang))
         );
      Ui.text(g, Ui.trim(sub, this.midW - 16), x + 8, y + 18, -6646866);
      Ui.text(g, Ui.trim(this.syncLine(), this.midW - 16), x + 8, y + 28, this.syncColor(th));
      this.gridY = y + 40;
      this.gridH = this.bottom - 8 - this.gridY;
      int inner = this.midW - 12;
      int cols = inner >= 242 ? 4 : Math.max(1, (inner + 6) / 62);
      int cardW = (inner - (cols - 1) * 6) / cols;
      int pv = cardW - 8;
      int cardH = pv + 20;
      String selfName = this.minecraft.getUser() == null ? "Steve" : this.minecraft.getUser().getName();
      List<Cosmetic> entries = new ArrayList<>();
      entries.add(null);
      entries.addAll(all);
      g.enableScissor(x + 2, this.gridY, x + this.midW - 2, this.gridY + this.gridH);
      long now = System.currentTimeMillis();

      for (int i = 0; i < entries.size(); i++) {
         Cosmetic c = entries.get(i);
         int cx = x + 6 + i % cols * (cardW + 6);
         int cy = this.gridY + i / cols * (cardH + 6) - (int)this.gridScroll;
         if (cy <= this.gridY + this.gridH && cy + cardH >= this.gridY) {
            boolean visibleMouse = my >= this.gridY && my < this.gridY + this.gridH;
            boolean hov = visibleMouse && Ui.hover(mx, my, cx, cy, cardW, cardH);
            if (hov) {
               this.hoveredCos = c == null ? "" : c.id();
            }

            boolean active = cosType == Cosmetic.Type.BUDDY
               ? (c == null ? CosmeticManager.buddies(ModConfig.get().cosmetics).isEmpty() : CosmeticManager.buddyEquipped(c))
               : (c == null ? equipped == null : c.id().equals(equipped));
            Ui.rect(g, cx, cy, cardW, cardH, hov ? -14015424 : -14607314);
            Ui.rect(g, cx + 4, cy + 4, pv, pv, -15001818);
            if (c == null) {
               Ui.textCentered(g, "∅", cx + 4 + pv / 2, cy + 4 + pv / 2 - 4, -9739136);
            } else if (c.type() == Cosmetic.Type.NAMECOLOR) {
               Component name = NameDecorator.colorize(Component.literal(selfName), selfName, c);
               int tw = Ui.font().width(name);
               Ui.component(g, name, cx + 4 + (pv - tw) / 2, cy + 4 + pv / 2 - 4, -1);
            } else if (c.type() == Cosmetic.Type.NAMETAG) {
               this.renderNametagSample(g, c, selfName, cx + 4, cy + 4, pv);
            } else if (c.type() == Cosmetic.Type.LOGO) {
               MutableComponent lg = NameDecorator.logo(c);
               g.pose().pushMatrix();
               g.pose().translate(cx + 4 + pv / 2.0F, cy + 4 + pv / 2.0F);
               g.pose().scale(3.0F, 3.0F);
               Ui.component(g, lg, -5, -5, -1);
               g.pose().popMatrix();
            } else if (c.type() == Cosmetic.Type.FRAME) {
               Component name = NameDecorator.frame(
                  NameDecorator.colorize(
                     Component.literal(selfName),
                     selfName,
                     CosmeticRegistry.get().get(Cosmetic.Type.NAMECOLOR, CosmeticManager.equipped(Cosmetic.Type.NAMECOLOR))
                  ),
                  c
               );
               int tw = Ui.font().width(name);
               Ui.component(g, name, cx + 4 + (pv - tw) / 2, cy + 4 + pv / 2 - 4, -1);
            } else {
               Optional<TextureCache.Tex> tex = CosmeticPreviews.card(c);
               if (tex.isPresent()) {
                  Ui.texture(g, tex.get(), cx + 6, cy + 6, pv - 4, pv - 4);
               }

               if (c.frames() > 1) {
                  int bw = Ui.width("ANIM") + 4;
                  Ui.rect(g, cx + 4, cy + 4, bw, 10, th.accent);
                  Ui.text(g, "ANIM", cx + 6, cy + 5, -16054254);
               }
            }

            String name = c == null ? Ui.tr("maneszn_styles.cosmetics.none") : c.displayName(lang);
            Ui.text(g, Ui.trim(name, cardW - 8), cx + 4, cy + pv + 8, -1185282);
            if (c != null && c.requires() != null) {
               boolean lock = CosmeticManager.locked(c);
               String badge = c.requires();

               int col = switch (badge) {
                  case "vip" -> -12486;
                  case "legend" -> -45715;
                  case "owner" -> -5085441;
                  default -> -54742;
               };
               badge = c.requires().toUpperCase(Locale.ROOT);
               int bw = Ui.width(badge) + 4;
               Ui.rect(g, cx + cardW - 4 - bw, cy + 4, bw, 10, col);
               Ui.text(g, badge, cx + cardW - 2 - bw, cy + 5, -16054254);
               if (lock) {
                  Ui.rect(g, cx + 4, cy + 4, pv, pv, -1610284278);
                  Ui.textCentered(g, "\ud83d\udd12", cx + 4 + pv / 2, cy + 4 + pv / 2 - 4, -1);
               }
            }

            if (active) {
               int a = 120 + (int)(100.0 * (0.5 + 0.5 * Math.sin(now / 280.0)));
               Ui.border(g, cx - 1, cy - 1, cardW + 2, cardH + 2, th.accentAlpha(a));
               Ui.border(g, cx, cy, cardW, cardH, th.accent);
               Ui.rect(g, cx + cardW - 9, cy + cardH - 9, 5, 5, -14498466);
            } else if (hov) {
               Ui.border(g, cx, cy, cardW, cardH, -13752510);
            }

            String id = c == null ? null : c.id();
            this.zones
               .addClipped(
                  cx, cy, cardW, cardH, this.gridY, this.gridY + this.gridH, () -> this.equip(cosType, id), c == null ? null : () -> this.equip(cosType, null)
               );
            if (hov && c != null && CustomCapes.isCustom(c.id())) {
               int bx = cx + cardW - 15;
               Ui.button(g, bx, cy + 3, 12, 12, "✕", Ui.hover(mx, my, bx, cy + 3, 12, 12), false, th);
               this.zones.addClipped(bx, cy + 3, 12, 12, this.gridY, this.gridY + this.gridH, () -> {
                  if (id.equals(CosmeticManager.equipped(Cosmetic.Type.CAPE))) {
                     CosmeticManager.equip(Cosmetic.Type.CAPE, null);
                  }

                  CustomCapes.delete(id);
                  this.toast(Ui.tr("maneszn_styles.toast.cape_deleted"));
               }, null);
            }
         }
      }

      g.disableScissor();
      int rows = (entries.size() + cols - 1) / cols;
      int contentH = rows * (cardH + 6);
      this.gridMaxScroll = Math.max(0, contentH - this.gridH);
      this.gridScroll = Math.max(0.0, Math.min(this.gridScroll, (double)this.gridMaxScroll));
      this.drawScrollbar(g, x + this.midW - 4, this.gridY, this.gridH, this.gridScroll, this.gridMaxScroll, contentH, th);
   }

   private String syncLine() {
      return switch (ApiClient.get().status()) {
         case ONLINE -> Ui.tr("maneszn_styles.cosmetics.sync_on");
         case ANONYMOUS -> Ui.tr("maneszn_styles.cosmetics.sync_anon");
         case CONNECTING -> Ui.tr("maneszn_styles.cosmetics.sync_connecting");
         default -> Ui.tr("maneszn_styles.cosmetics.sync_off");
         case DISABLED -> Ui.tr("maneszn_styles.cosmetics.sync_disabled");
      };
   }

   private int syncColor(Theme th) {
      return switch (ApiClient.get().status()) {
         case ONLINE -> -14498466;
         case ANONYMOUS, CONNECTING -> th.accent;
         default -> -9739136;
      };
   }

   private static String equippedId(Cosmetic.Type type) {
      if (type == Cosmetic.Type.BUDDY) {
         for (CosmeticManager.BuddyAt b : CosmeticManager.buddies(ModConfig.get().cosmetics)) {
            if (b.spot() == buddySpot) {
               return b.buddy().id();
            }
         }

         return null;
      } else {
         return CosmeticManager.equipped(type);
      }
   }

   private void equip(Cosmetic.Type type, String id) {
      if (type == Cosmetic.Type.BUDDY) {
         if (id == null) {
            for (CosmeticScenes.BuddySpot sp : CosmeticScenes.BuddySpot.values()) {
               if (ModConfig.get().cosmetics.containsKey(sp.key())) {
                  CosmeticManager.equipBuddy(sp, null);
               }
            }

            this.toast(Ui.tr("maneszn_styles.toast.cos_removed"));
         } else {
            Cosmetic c = CosmeticRegistry.get().get(type, id);
            if (c != null) {
               buddySpot = CosmeticManager.spotOf(c);
               boolean was = CosmeticManager.buddyEquipped(c);
               CosmeticManager.toggleBuddy(c);
               String v = CosmeticManager.extra(buddySpot.skinKey());
               this.skinBox.setValue(v == null ? "" : v);
               this.toast(was ? Ui.tr("maneszn_styles.toast.cos_removed") : Ui.tr("maneszn_styles.toast.cos_equipped", c.displayName(Ui.langShort())));
            }
         }
      } else {
         String before = equippedId(type);
         if (id == null ? before != null : !id.equals(before)) {
            Cosmetic want = CosmeticRegistry.get().get(type, id);
            if (CosmeticManager.locked(want)) {
               this.toast(Ui.tr("maneszn_styles.code.locked", want.requires().toUpperCase(Locale.ROOT)));
               this.minecraft.setScreen(new CodeScreen(this));
            } else {
               CosmeticManager.equip(type, id);
               Cosmetic c = CosmeticRegistry.get().get(type, id);
               this.toast(c == null ? Ui.tr("maneszn_styles.toast.cos_removed") : Ui.tr("maneszn_styles.toast.cos_equipped", c.displayName(Ui.langShort())));
            }
         }
      }
   }

   private void renderRemoveAll(GuiGraphics g, int mx, int my, Theme th, int bx, int by, int bw) {
      boolean armed = System.currentTimeMillis() < this.removeAllArmedUntil;
      boolean hov = Ui.hover(mx, my, bx, by, bw, 13);
      boolean any = CosmeticManager.equippedCount() > 0;
      Ui.button(g, bx, by, bw, 13, Ui.tr(armed ? "maneszn_styles.cosmetics.remove_all_confirm" : "maneszn_styles.cosmetics.remove_all"), hov && any, armed, th);
      if (any) {
         this.zones.add(bx, by, bw, 13, () -> {
            if (System.currentTimeMillis() < this.removeAllArmedUntil) {
               CosmeticManager.unequipAll();
               this.removeAllArmedUntil = 0L;
               this.toast(Ui.tr("maneszn_styles.toast.all_removed"));
            } else {
               this.removeAllArmedUntil = System.currentTimeMillis() + 3000L;
            }
         });
      }
   }

   private Cosmetic inspectedCosmetic() {
      if (this.hoveredCos != null) {
         return this.hoveredCos.isEmpty() ? null : CosmeticRegistry.get().get(cosType, this.hoveredCos);
      } else {
         return CosmeticRegistry.get().get(cosType, equippedId(cosType));
      }
   }

   private void renderCosmeticInspector(GuiGraphics g, int mx, int my, Theme th, int x, int y, int w, int px, int pw) {
      String lang = Ui.langShort();
      Cosmetic shown = this.inspectedCosmetic();
      boolean showNone = this.hoveredCos != null && this.hoveredCos.isEmpty();
      Map<Cosmetic.Type, Cosmetic> worn = new EnumMap<>(Cosmetic.Type.class);

      for (Cosmetic.Type t : Cosmetic.Type.values()) {
         if (t != Cosmetic.Type.BUDDY) {
            Cosmetic c = this.pick(t, shown, showNone);
            if (c != null) {
               worn.put(t, c);
            }
         }
      }

      List<CosmeticManager.BuddyAt> buddies = new ArrayList<>();
      CosmeticScenes.BuddySpot shownSpot = cosType == Cosmetic.Type.BUDDY && shown != null ? CosmeticManager.spotOf(shown) : null;

      for (CosmeticManager.BuddyAt b : CosmeticManager.buddies(ModConfig.get().cosmetics)) {
         if ((cosType != Cosmetic.Type.BUDDY || !showNone) && b.spot() != shownSpot) {
            buddies.add(b);
         }
      }

      if (shownSpot != null) {
         buddies.add(new CosmeticManager.BuddyAt(shownSpot, shown, CosmeticManager.extra(shownSpot.skinKey())));
      }

      this.previewX = px;
      this.previewY = y + 8;
      this.previewSize = pw;
      long now = System.currentTimeMillis();
      if (this.lastFrameMs != 0L && !this.draggingPreview && now - this.lastDragMs > 2500L) {
         this.spinYaw = this.spinYaw + (now - this.lastFrameMs) * 0.045;
      }

      this.lastFrameMs = now;
      String selfName = this.minecraft.getUser() == null ? "Steve" : this.minecraft.getUser().getName();
      if (cosType == Cosmetic.Type.AURA && shown != null && shown.parts().isEmpty()) {
         CosmeticPreviews.card(shown).ifPresent(tx -> Ui.texture(g, tx, px + 8, y + 16, pw - 16, pw - 16));
      } else if (cosType == Cosmetic.Type.NAMECOLOR) {
         MutableComponent name = NameDecorator.logo().append(NameDecorator.colorize(Component.literal(selfName), selfName, shown));
         int tw = Ui.font().width(name);
         Ui.component(g, name, px + (pw - tw) / 2, y + 8 + pw / 2 - 4, -1);
      } else if (cosType == Cosmetic.Type.NAMETAG) {
         this.renderNametagSample(g, shown, selfName, px, y + 8, pw);
      } else if (cosType == Cosmetic.Type.LOGO) {
         MutableComponent name = NameDecorator.logo(shown)
            .append(NameDecorator.colorize(Component.literal(selfName), selfName, worn.get(Cosmetic.Type.NAMECOLOR)));
         int tw = Ui.font().width(name);
         g.pose().pushMatrix();
         g.pose().translate(px + pw / 2.0F, y + 8 + pw / 2.0F);
         g.pose().scale(2.0F, 2.0F);
         Ui.component(g, name, -tw / 2, -4, -1);
         g.pose().popMatrix();
      } else if (cosType == Cosmetic.Type.FRAME) {
         MutableComponent name = NameDecorator.logo()
            .append(NameDecorator.frame(NameDecorator.colorize(Component.literal(selfName), selfName, worn.get(Cosmetic.Type.NAMECOLOR)), shown));
         int tw = Ui.font().width(name);
         Ui.component(g, name, px + (pw - tw) / 2, y + 8 + pw / 2 - 4, -1);
      } else {
         CosmeticPreviews.figure(worn, buddies, this.spinYaw, CosmeticManager.ticks()).ifPresent(tx -> Ui.texture(g, tx, px + 4, y + 12, pw - 8, pw - 8));
         Ui.textCentered(g, Ui.tr("maneszn_styles.ui.drag_rotate"), px + pw / 2, y + 8 + pw - 10, -9739136);
      }

      int ty = y + 8 + pw + 8;
      Ui.textShadow(g, Ui.trim(shown == null ? Ui.tr("maneszn_styles.cosmetics.none") : shown.displayName(lang), w - 16), x + 8, ty, -1185282);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.side." + typeKey(cosType)), w - 16), x + 8, ty + 12, -6646866);
      String eqId = equippedId(cosType);
      String shownId = shown == null ? null : shown.id();
      boolean isActive = cosType == Cosmetic.Type.BUDDY
         ? (shown == null ? CosmeticManager.buddies(ModConfig.get().cosmetics).isEmpty() : CosmeticManager.buddyEquipped(shown))
         : (shownId == null ? eqId == null : shownId.equals(eqId));
      int by = ty + 26;
      boolean hovA = Ui.hover(mx, my, x + 8, by, w - 16, 16);
      String label;
      if (cosType == Cosmetic.Type.BUDDY && shown != null) {
         label = Ui.tr(isActive ? "maneszn_styles.cosmetics.remove" : "maneszn_styles.cosmetics.equip");
      } else {
         label = isActive
            ? Ui.tr("maneszn_styles.ui.active")
            : (shown == null ? Ui.tr("maneszn_styles.cosmetics.remove") : Ui.tr("maneszn_styles.cosmetics.equip"));
      }

      boolean clickable = !isActive || cosType == Cosmetic.Type.BUDDY && shown != null;
      Ui.button(g, x + 8, by, w - 16, 16, label, hovA, clickable, th);
      if (clickable) {
         this.zones.add(x + 8, by, w - 16, 16, () -> this.equip(cosType, shownId));
      }

      by += 20;
      if (cosType == Cosmetic.Type.BUDDY) {
         by = this.renderBuddyOptions(g, mx, my, th, x, by, w, shown);
      }

      if (by + 30 < this.bottom - 6) {
         this.renderThemes(g, x, by + 6, w, th);
      }
   }

   private int renderBuddyOptions(GuiGraphics g, int mx, int my, Theme th, int x, int by, int w, Cosmetic shown) {
      if (shown != null && shown.spot() != null) {
         CosmeticScenes.BuddySpot sp = CosmeticManager.spotOf(shown);
         if (sp != buddySpot && !this.skinBox.isFocused()) {
            buddySpot = sp;
            String v = CosmeticManager.extra(sp.skinKey());
            this.skinBox.setValue(v == null ? "" : v);
         }
      }

      Cosmetic cur = CosmeticManager.miniMeFor(buddySpot);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.buddy.skin_for", cur == null ? "?" : cur.displayName(Ui.langShort())), w - 16), x + 8, by, -9739136);
      by += 10;
      this.skinBox.visible = true;
      int okW = Ui.width("OK") + 10;
      this.skinBox.setX(x + 8);
      this.skinBox.setY(by);
      this.skinBox.setWidth(w - 16 - okW - 3);
      boolean hov = Ui.hover(mx, my, x + w - 8 - okW, by - 1, okW, 14);
      Ui.button(g, x + w - 8 - okW, by - 1, okW, 14, "OK", hov, true, th);
      this.zones.add(x + w - 8 - okW, by - 1, okW, 14, this::applySkinName);
      by += 16;
      String name = CosmeticManager.skinNameOr(CosmeticManager.extra(buddySpot.skinKey()));
      String status = SkinFetcher.loading(name)
         ? Ui.tr("maneszn_styles.buddy.loading")
         : (SkinFetcher.failed(name) ? Ui.tr("maneszn_styles.buddy.not_found", name) : Ui.tr("maneszn_styles.buddy.skin_of", name));
      Ui.text(g, Ui.trim(status, w - 16), x + 8, by, SkinFetcher.failed(name) ? -1096636 : -6646866);
      by += 12;
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.buddy.multi_hint"), w - 16), x + 8, by, -9739136);
      return by + 10;
   }

   private void applySkinName() {
      String v = this.skinBox.getValue().trim();
      if (!v.isEmpty() && !SkinFetcher.validName(v)) {
         this.toast(Ui.tr("maneszn_styles.toast.skin_invalid"));
      } else {
         CosmeticManager.setExtra(buddySpot.skinKey(), v.toLowerCase(Locale.ROOT));
         SkinFetcher.get(CosmeticManager.skinNameOr(v));
         TextureCache.clear();
         this.skinBox.setFocused(false);
         this.setFocused(null);
         this.toast(Ui.tr("maneszn_styles.toast.skin_set", v.isEmpty() ? "Maneszn" : v));
      }
   }

   private void renderNametagSample(GuiGraphics g, Cosmetic tag, String name, int x, int y, int size) {
      MutableComponent base = Component.literal(name);
      Cosmetic col = CosmeticRegistry.get().get(Cosmetic.Type.NAMECOLOR, CosmeticManager.equipped(Cosmetic.Type.NAMECOLOR));
      MutableComponent text = NameDecorator.logo()
         .append(tag == null ? NameDecorator.colorize(base, name, col) : NameEffects.apply(base, name, col, tag.text(), ApiClient.selfId()));
      int dy = 0;
      int dx = 0;
      if (tag != null) {
         double[] m = motionSample(tag.motion());
         dx = (int)Math.round(m[0] * 40.0);
         dy = (int)Math.round(-m[1] * 40.0);
      }

      int tw = Ui.font().width(text);
      int tx = x + (size - tw) / 2 + dx;
      int ty = y + size / 2 - 4 + dy;
      Ui.rect(g, tx - 2, ty - 1, tw + 4, 10, 1426063360);
      Ui.component(g, text, tx, ty, -1);
   }

   private static double[] motionSample(String motion) {
      double t = CosmeticManager.ticks();
      String var3 = motion == null ? "none" : motion;

      return switch (var3) {
         case "bounce" -> new double[]{0.0, Math.abs(Math.sin(t * 0.22)) * 0.28};
         case "float" -> new double[]{0.0, Math.sin(t * 0.08) * 0.12};
         case "wiggle" -> new double[]{Math.sin(t * 0.6) * 0.04, Math.abs(Math.sin(t * 0.9)) * 0.06};
         case "shake" -> new double[]{(Math.floorMod((long)t * 7919L, 97) / 97.0 - 0.5) * 0.06, (Math.floorMod((long)t * 104729L, 89) / 89.0 - 0.5) * 0.06};
         case "wave" -> new double[]{Math.sin(t * 0.12) * 0.12, Math.sin(t * 0.24) * 0.06};
         default -> new double[]{0.0, 0.0};
      };
   }

   private Cosmetic pick(Cosmetic.Type t, Cosmetic shown, boolean showNone) {
      return t != cosType || shown == null && !showNone ? CosmeticRegistry.get().get(t, CosmeticManager.equipped(t)) : shown;
   }

   private void renderExtrasPages(GuiGraphics g, int x, int w, int mx, int my, Theme th) {
      int cy = this.listY;

      for (String k : EXTRAS_PAGES) {
         boolean sel = k.equals(extrasPage);
         boolean hov = Ui.hover(mx, my, x + 2, cy, w - 4, 20);
         if (sel || hov) {
            Ui.rect(g, x + 2, cy, w - 4, 20, sel ? th.accentDim() : -14015424);
         }

         if (sel) {
            Ui.rect(g, x + 2, cy, 2, 20, th.accent);
         }

         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.extras.page." + k), w - 14), x + 9, cy + 6, sel ? -1185282 : -6646866);
         this.zones.add(x + 2, cy, w - 4, 20, () -> {
            extrasPage = k;
            this.gridScroll = 0.0;
         });
         cy += 22;
      }
   }

   private void renderExtras(GuiGraphics g, int mx, int my, Theme th) {
      int x = this.midX;
      int y = this.top + 4;
      Ui.rect(g, x, y, this.midW, this.bottom - y - 4, -15462116);
      ModConfig cfg = ModConfig.get();
      StyleRegistry reg = StyleRegistry.get();
      Ui.textShadow(g, Ui.tr("maneszn_styles.extras.page." + extrasPage), x + 10, y + 7, -1185282);
      Ui.rect(g, x + 8, y + 19, this.midW - 16, 1, -13752510);
      this.gridY = y + 24;
      this.gridH = this.bottom - 8 - this.gridY;
      this.clipTop = this.gridY;
      this.clipBottom = this.gridY + this.gridH;
      g.enableScissor(x + 2, this.gridY, x + this.midW - 2, this.gridY + this.gridH);
      int ry = this.gridY + 4 - (int)this.gridScroll;
      String contentH = extrasPage;

      ry = switch (contentH) {
         case "fps" -> {
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.fps_boost", cfg.fpsBoost, () -> {
               FpsBoost.set(!cfg.fpsBoost);
               this.toast(Ui.tr(cfg.fpsBoost ? "maneszn_styles.toast.fps_on" : "maneszn_styles.toast.fps_off"));
            });
            ry += 6;
            Ui.text(g, Ui.tr("maneszn_styles.extras.fps_now", this.minecraft.getFps()), x + 12, ry, th.accent);
            ry += 14;

            for (int i = 1; i <= 6; i++) {
               Ui.text(g, Ui.trim("• " + Ui.tr("maneszn_styles.extras.fps_info" + i), this.midW - 28), x + 12, ry, -6646866);
               ry += 11;
            }

            ry += 4;
            Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.extras.fps_restore"), this.midW - 28), x + 12, ry, -9739136);
            yield ry + 12;
         }
         case "ping" -> {
            for (int i = 1; i <= 3; i++) {
               Ui.text(g, Ui.trim("• " + Ui.tr("maneszn_styles.ping.info" + i), this.midW - 28), x + 12, ry, -6646866);
               ry += 11;
            }

            ry += 6;
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.ping.open", () -> this.minecraft.setScreen(new PingScreen(this)));
            PingOptimizer.Result best = PingOptimizer.best();
            if (best != null) {
               ry += 4;
               Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.ping.last", best.region(), best.ping()), this.midW - 28), x + 12, ry, -14498466);
               ry += 12;
            }
            yield ry;
         }
         case "items" -> {
            ry = this.section(g, x, ry, "maneszn_styles.extras.sec_held");
            ry = this.scaleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.held_scale", cfg.heldItemScale, 0.3, 2.0, v -> cfg.heldItemScale = v);
            ry = this.section(g, x, ry + 6, "maneszn_styles.extras.sec_ground_look");
            ry = this.groundStyleRow(g, mx, my, th, x, ry);
            ry = this.actionRow(
               g,
               mx,
               my,
               th,
               x,
               ry,
               "maneszn_styles.extras.ground_video",
               () -> {
                  cfg.droppedItemStyle = "2d";

                  for (String id : new String[]{
                     "minecraft:totem_of_undying",
                     "minecraft:splash_potion",
                     "minecraft:potion",
                     "minecraft:ender_pearl",
                     "minecraft:golden_apple",
                     "minecraft:enchanted_golden_apple",
                     "minecraft:cobweb",
                     "minecraft:end_crystal",
                     "minecraft:experience_bottle",
                     "minecraft:wind_charge"
                  }) {
                     cfg.itemScales.putIfAbsent(id, 1.6);
                  }

                  ModConfig.save();
                  this.toast(Ui.tr("maneszn_styles.toast.ground_video"));
               }
            );
            ry = this.section(g, x, ry + 6, "maneszn_styles.extras.sec_dropped");
            yield this.itemScaleRows(g, mx, my, th, x, ry);
         }
         case "look" -> {
            ry = this.section(g, x, ry, "maneszn_styles.extras.sec_colors");
            ry = this.colorRow(
               g, mx, my, th, x, ry, "maneszn_styles.extras.outline_color", cfg.blockOutlineColor, OUTLINE_COLORS, cx -> cfg.blockOutlineColor = cx
            );
            ry = this.colorRow(g, mx, my, th, x, ry, "maneszn_styles.extras.hit_color", cfg.hitColor, HIT_COLORS, cx -> cfg.hitColor = cx);
            ry = this.section(g, x, ry + 6, "maneszn_styles.extras.sec_sounds");

            for (SoundChoices.Slot slot : SoundChoices.SLOTS) {
               ry = this.soundRow(g, mx, my, th, x, ry, slot);
            }
            yield ry;
         }
         case "trading" -> {
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.trade.open_market", () -> this.minecraft.setScreen(new MarketScreen(this, "market")));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.trade.open_spawner", () -> this.minecraft.setScreen(new MarketScreen(this, "spawner")));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.trade.open_alarms", () -> this.minecraft.setScreen(new MarketScreen(this, "alarms")));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.trade.open_money", () -> this.minecraft.setScreen(new MarketScreen(this, "money")));
            ry += 6;

            for (int i = 1; i <= 3; i++) {
               Ui.text(g, Ui.trim("• " + Ui.tr("maneszn_styles.trade.info" + i), this.midW - 28), x + 12, ry, -6646866);
               ry += 11;
            }
            yield ry;
         }
         case "tools" -> {
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.calc.open", () -> this.minecraft.setScreen(new CalcScreen(this)));
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.calc.button_toggle", cfg.calcButton, () -> {
               cfg.calcButton = !cfg.calcButton;
               ModConfig.save();
            });
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.trim.open", () -> this.minecraft.setScreen(new TrimScreen(this)));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.mapart.open", () -> this.minecraft.setScreen(new MapartScreen(this)));
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.duraguard.toggle", cfg.duraGuard, () -> {
               cfg.duraGuard = !cfg.duraGuard;
               ModConfig.save();
            });
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.duraguard.percent", () -> {
               int[] steps = new int[]{1, 2, 3, 5, 10, 15, 20};
               int i = 0;

               while (i < steps.length && steps[i] <= cfg.duraGuardPercent) {
                  i++;
               }

               cfg.duraGuardPercent = steps[i % steps.length];
               ModConfig.save();
               this.toast(Ui.tr("maneszn_styles.duraguard.now", cfg.duraGuardPercent));
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.duraguard.all", cfg.duraGuardAllTools, () -> {
               cfg.duraGuardAllTools = !cfg.duraGuardAllTools;
               ModConfig.save();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.armorwarn.toggle", cfg.armorWarn, () -> {
               cfg.armorWarn = !cfg.armorWarn;
               ModConfig.save();
            });
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.armorwarn.percent", () -> {
               int[] steps = new int[]{5, 10, 15, 20, 25, 30};
               int i = 0;

               while (i < steps.length && steps[i] <= cfg.armorWarnPercent) {
                  i++;
               }

               cfg.armorWarnPercent = steps[i % steps.length];
               ModConfig.save();
               this.toast(Ui.tr("maneszn_styles.armorwarn.now", cfg.armorWarnPercent));
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.tpa.toggle", cfg.autoTpa, () -> {
               cfg.autoTpa = !cfg.autoTpa;
               ModConfig.save();
            });
            Ui.text(
               g,
               Ui.trim(Ui.tr("maneszn_styles.tpa.info", cfg.tpaFriends.isEmpty() ? "–" : String.join(", ", cfg.tpaFriends)), this.midW - 28),
               x + 12,
               ry,
               -9739136
            );
            ry += 12;
            Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.tpa.info2"), this.midW - 28), x + 12, ry, -9739136);
            ry += 16;
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.pie_chart", () -> {
               this.minecraft.setScreen(null);
               PieChart.toggle();
            });
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.open_hud", () -> this.minecraft.setScreen(new HudEditScreen(this)));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.open_crosshair", () -> this.minecraft.setScreen(new CrosshairScreen(this)));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.open_custom", () -> Util.getPlatform().openPath(ModPaths.customDir()));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.open_presets", () -> Util.getPlatform().openPath(ModPaths.presetsDir()));
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.rescan", this::reloadRegistry);
            int builtin = 0;
            int custom = 0;
            int packs = 0;

            for (Category c : reg.categories()) {
               for (Style s : reg.styles(c.id())) {
                  switch (s.source()) {
                     case BUILTIN:
                        builtin++;
                        break;
                     case CUSTOM:
                        custom++;
                        break;
                     case PACK:
                        packs++;
                  }
               }
            }

            ry += 8;
            Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.extras.stats", builtin, custom, packs, reg.scannedPackCount()), this.midW - 16), x + 8, ry, -9739136);
            ry += 8;
            Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.extras.stats", builtin, custom, packs, reg.scannedPackCount()), this.midW - 16), x + 10, ry, -9739136);
            yield ry + 14;
         }
         case "online" -> {
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.paste_api", () -> {
               String clip = this.minecraft.keyboardHandler.getClipboard().trim();
               if (!clip.startsWith("http://") && !clip.startsWith("https://")) {
                  this.toast(Ui.tr("maneszn_styles.toast.api_invalid"));
               } else {
                  cfg.apiUrl = clip;
                  ModConfig.save();
                  ApiClient.get().reconnect();
                  this.toast(Ui.tr("maneszn_styles.toast.api_set"));
               }
            });
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.reconnect", () -> {
               ApiClient.get().reconnect();
               this.toast(Ui.tr("maneszn_styles.toast.reconnect"));
            });
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.reconnect", () -> {
               ApiClient.get().reconnect();
               this.toast(Ui.tr("maneszn_styles.toast.reconnect"));
            });
            ry += 8;
            String url = cfg.apiUrl != null && !cfg.apiUrl.isBlank() ? cfg.apiUrl : Ui.tr("maneszn_styles.extras.api_none");
            Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.extras.api", url), this.midW - 16), x + 10, ry, -9739136);
            ry += 12;
            String err = ApiClient.get().lastError();
            if (err != null && !err.isBlank()) {
               Ui.text(g, Ui.trim("⚠ " + err, this.midW - 16), x + 10, ry, -1096636);
               ry += 12;
            }
            yield ry;
         }
         default -> {
            ry = this.actionRow(
               g,
               mx,
               my,
               th,
               x,
               ry,
               "maneszn_styles.extras.ores_yellow",
               () -> {
                  for (String cx : new String[]{
                     "erz_diamant", "erz_smaragd", "erz_gold", "erz_eisen", "erz_redstone", "erz_lapis", "erz_kohle", "erz_kupfer", "antiker_schrott"
                  }) {
                     if (reg.style(cx, "maneszn_gelb") != null) {
                        cfg.active.put(cx, "maneszn_gelb");
                     }
                  }

                  ModConfig.save();
                  StylePackBuilder.applyAndReload();
                  this.toast(Ui.tr("maneszn_styles.toast.ores_yellow"));
               }
            );
            ry = this.actionRow(g, mx, my, th, x, ry, "maneszn_styles.extras.ores_vanilla", () -> {
               cfg.active.keySet().removeIf(k -> k.startsWith("erz_") || k.equals("antiker_schrott"));
               ModConfig.save();
               StylePackBuilder.applyAndReload();
               this.toast(Ui.tr("maneszn_styles.toast.ores_vanilla"));
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.scan_packs", cfg.scanResourcePacks, () -> {
               cfg.scanResourcePacks = !cfg.scanResourcePacks;
               ModConfig.save();
               this.reloadRegistry();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.anonymous", cfg.anonymous, () -> {
               cfg.anonymous = !cfg.anonymous;
               ModConfig.save();
               ApiClient.get().reconnect();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.show_logo", cfg.showLogo, () -> {
               cfg.showLogo = !cfg.showLogo;
               ModConfig.save();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.show_others", cfg.showOthersCosmetics, () -> {
               cfg.showOthersCosmetics = !cfg.showOthersCosmetics;
               ModConfig.save();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.show_custom_capes", cfg.showCustomCapes, () -> {
               cfg.showCustomCapes = !cfg.showCustomCapes;
               ModConfig.save();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.show_auras", cfg.showAuras, () -> {
               cfg.showAuras = !cfg.showAuras;
               ModConfig.save();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.cape_physics", cfg.capePhysics, () -> {
               cfg.capePhysics = !cfg.capePhysics;
               ModConfig.save();
            });
            ry = this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.elytra_swap", cfg.elytraSwap, () -> {
               cfg.elytraSwap = !cfg.elytraSwap;
               ModConfig.save();
               StylePackBuilder.applyAndReload();
            });
            yield this.toggleRow(g, mx, my, th, x, ry, "maneszn_styles.extras.emote_sounds", cfg.emoteSounds, () -> {
               cfg.emoteSounds = !cfg.emoteSounds;
               ModConfig.save();
            });
         }
      };
      g.disableScissor();
      int extrasH = ry + 8 + (int)this.gridScroll - this.gridY;
      this.gridMaxScroll = Math.max(0, extrasH - this.gridH);
      this.gridScroll = Math.max(0.0, Math.min(this.gridScroll, (double)this.gridMaxScroll));
      this.drawScrollbar(g, x + this.midW - 4, this.gridY, this.gridH, this.gridScroll, this.gridMaxScroll, extrasH, th);
      this.clipTop = Integer.MIN_VALUE;
      this.clipBottom = Integer.MAX_VALUE;
   }

   private int section(GuiGraphics g, int x, int y, String key) {
      Ui.text(g, Ui.tr(key), x + 10, y + 2, Theme.current().accent);
      return y + 13;
   }

   private int scaleRow(GuiGraphics g, int mx, int my, Theme th, int x, int y, String key, double value, double min, double max, DoubleConsumer set) {
      int w = this.midW - 16;
      Ui.rect(g, x + 8, y, w, 18, -14607314);
      Ui.text(g, Ui.trim(Ui.tr(key), w - 90), x + 14, y + 5, -1185282);
      int bw = 14;
      int plusX = x + 8 + w - bw - 4;
      int minusX = plusX - bw - 40;
      boolean hm = Ui.hover(mx, my, minusX, y + 2, bw, 14);
      boolean hp = Ui.hover(mx, my, plusX, y + 2, bw, 14);
      Ui.button(g, minusX, y + 2, bw, 14, "−", hm, false, th);
      Ui.textCentered(g, Math.round(value * 100.0) + "%", minusX + bw + 20, y + 5, th.accent);
      Ui.button(g, plusX, y + 2, bw, 14, "+", hp, false, th);
      this.zones.addClipped(minusX, y + 2, bw, 14, this.clipTop, this.clipBottom, () -> {
         set.accept(Math.max(min, Math.round((value - 0.1) * 10.0) / 10.0));
         ModConfig.save();
      }, null);
      this.zones.addClipped(plusX, y + 2, bw, 14, this.clipTop, this.clipBottom, () -> {
         set.accept(Math.min(max, Math.round((value + 0.1) * 10.0) / 10.0));
         ModConfig.save();
      }, null);
      this.zones.addClipped(minusX + bw, y + 2, 40, 14, this.clipTop, this.clipBottom, () -> {
         set.accept(1.0);
         ModConfig.save();
      }, null);
      return y + 21;
   }

   private int itemScaleRows(GuiGraphics g, int mx, int my, Theme th, int x, int y) {
      ModConfig cfg = ModConfig.get();
      int w = this.midW - 16;
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.extras.item_single"), w - 4), x + 10, y + 2, -9739136);
      y += 12;
      int ix = x + 10;

      for (Item it : QUICK_ITEMS) {
         ItemStack st = new ItemStack(it);
         String id = ItemScales.idOf(st);
         boolean has = id != null && cfg.itemScales.containsKey(id);
         boolean hov = Ui.hover(mx, my, ix, y, 18, 18);
         Ui.rect(g, ix, y, 18, 18, has ? th.accentDim() : (hov ? -14015424 : -14607314));
         g.renderItem(st, ix + 1, y + 1);
         this.zones.addClipped(ix, y, 18, 18, this.clipTop, this.clipBottom, () -> {
            if (id != null) {
               cfg.itemScales.putIfAbsent(id, 1.5);
            }

            ModConfig.save();
         }, null);
         ix += 20;
         if (ix + 18 > x + 8 + w - 90) {
            break;
         }
      }

      String handLabel = Ui.tr("maneszn_styles.extras.item_add_hand");
      int hw = Ui.width(handLabel) + 10;
      int hx = x + 8 + w - hw;
      Ui.button(g, hx, y + 2, hw, 14, handLabel, Ui.hover(mx, my, hx, y + 2, hw, 14), false, th);
      this.zones.addClipped(hx, y + 2, hw, 14, this.clipTop, this.clipBottom, () -> {
         if (this.minecraft.player != null) {
            String idx = ItemScales.idOf(this.minecraft.player.getMainHandItem());
            if (idx == null) {
               this.toast(Ui.tr("maneszn_styles.toast.item_hand_empty"));
            } else {
               cfg.itemScales.putIfAbsent(idx, 1.5);
               ModConfig.save();
            }
         }
      }, null);
      y += 22;

      for (String id : new ArrayList<>(cfg.itemScales.keySet())) {
         double v = cfg.itemScales.get(id);
         Ui.rect(g, x + 8, y, w, 20, -14607314);
         ItemStack st = stackOf(id);
         if (st != null) {
            g.renderItem(st, x + 11, y + 2);
         }

         String name = st != null ? st.getHoverName().getString() : id;
         Ui.text(g, Ui.trim(name, w - 130), x + 32, y + 6, -1185282);
         int bw = 14;
         int xX = x + 8 + w - 16;
         int plusX = xX - 4 - bw;
         int minusX = plusX - bw - 40;
         Ui.button(g, minusX, y + 3, bw, 14, "−", Ui.hover(mx, my, minusX, y + 3, bw, 14), false, th);
         Ui.textCentered(g, Math.round(v * 100.0) + "%", minusX + bw + 20, y + 6, th.accent);
         Ui.button(g, plusX, y + 3, bw, 14, "+", Ui.hover(mx, my, plusX, y + 3, bw, 14), false, th);
         Ui.button(g, xX, y + 3, 14, 14, "✕", Ui.hover(mx, my, xX, y + 3, 14, 14), false, th);
         this.zones.addClipped(minusX, y + 3, bw, 14, this.clipTop, this.clipBottom, () -> {
            cfg.itemScales.put(id, Math.max(0.3, Math.round((v - 0.1) * 10.0) / 10.0));
            ModConfig.save();
         }, null);
         this.zones.addClipped(plusX, y + 3, bw, 14, this.clipTop, this.clipBottom, () -> {
            cfg.itemScales.put(id, Math.min(4.0, Math.round((v + 0.1) * 10.0) / 10.0));
            ModConfig.save();
         }, null);
         this.zones.addClipped(xX, y + 3, 14, 14, this.clipTop, this.clipBottom, () -> {
            cfg.itemScales.remove(id);
            ModConfig.save();
         }, null);
         y += 22;
      }

      return y;
   }

   private static ItemStack stackOf(String id) {
      try {
         Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
         return item == null ? null : new ItemStack(item);
      } catch (Exception var2) {
         return null;
      }
   }

   private int colorRow(GuiGraphics g, int mx, int my, Theme th, int x, int y, String key, int value, int[] colors, IntConsumer set) {
      int w = this.midW - 16;
      Ui.rect(g, x + 8, y, w, 30, -14607314);
      Ui.text(g, Ui.trim(Ui.tr(key), w - 12), x + 14, y + 3, -1185282);
      int sx = x + 14;

      for (int c : colors) {
         boolean sel = c == value;
         Ui.rect(g, sx, y + 14, 12, 12, sel ? -1 : -13752510);
         if (c == 0) {
            Ui.rect(g, sx + 1, y + 15, 10, 10, -15001818);
            Ui.textCentered(g, "V", sx + 6, y + 16, -6646866);
         } else {
            Ui.rect(g, sx + 1, y + 15, 10, 10, c | 0xFF000000);
         }

         this.zones.addClipped(sx, y + 14, 12, 12, this.clipTop, this.clipBottom, () -> {
            set.accept(c);
            ModConfig.save();
         }, null);
         sx += 15;
         if (sx + 12 > x + 8 + w) {
            break;
         }
      }

      return y + 33;
   }

   private int soundRow(GuiGraphics g, int mx, int my, Theme th, int x, int y, SoundChoices.Slot slot) {
      ModConfig cfg = ModConfig.get();
      int w = this.midW - 16;
      boolean hov = Ui.hover(mx, my, x + 8, y, w - 20, 18);
      Ui.rect(g, x + 8, y, w, 18, hov ? -14015424 : -14607314);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.sound.slot." + slot.id()), w / 2), x + 14, y + 5, -1185282);
      String cur = cfg.sounds.get(slot.id());
      String label = cur == null ? "Vanilla" : Ui.tr("maneszn_styles.sound.choice." + cur);
      Ui.textRight(g, Ui.trim(label, w / 2 - 30) + " ›", x + 8 + w - 24, y + 5, cur == null ? -6646866 : th.accent);
      this.zones.addClipped(x + 8, y, w - 20, 18, this.clipTop, this.clipBottom, () -> {
         String next = SoundChoices.next(cfg.sounds.get(slot.id()));
         if (next == null) {
            cfg.sounds.remove(slot.id());
         } else {
            cfg.sounds.put(slot.id(), next);
         }

         ModConfig.save();
         SoundChoices.preview(SoundChoices.choice(next));
         this.soundsDirty = true;
      }, () -> {
         cfg.sounds.remove(slot.id());
         ModConfig.save();
         this.soundsDirty = true;
      });
      int px = x + 8 + w - 18;
      Ui.button(g, px, y + 2, 16, 14, "▶", Ui.hover(mx, my, px, y + 2, 16, 14), false, th);
      this.zones.addClipped(px, y + 2, 16, 14, this.clipTop, this.clipBottom, () -> SoundChoices.preview(SoundChoices.choice(cfg.sounds.get(slot.id()))), null);
      return y + 21;
   }

   private static String mark(boolean b) {
      return b ? "✓" : "–";
   }

   private int toggleRow(GuiGraphics g, int mx, int my, Theme th, int x, int y, String key, boolean on, Runnable r) {
      int w = this.midW - 16;
      boolean hov = Ui.hover(mx, my, x + 8, y, w, 18);
      Ui.rect(g, x + 8, y, w, 18, hov ? -14015424 : -14607314);
      Ui.text(g, Ui.trim(Ui.tr(key), w - 50), x + 14, y + 5, -1185282);
      String state = Ui.tr(on ? "maneszn_styles.ui.on" : "maneszn_styles.ui.off");
      int sw = Ui.width(state) + 10;
      Ui.rect(g, x + 8 + w - sw - 4, y + 3, sw, 12, on ? th.accent : -15001818);
      Ui.text(g, state, x + 8 + w - sw + 1, y + 5, on ? -16054254 : -6646866);
      this.zones.addClipped(x + 8, y, w, 18, this.clipTop, this.clipBottom, r, null);
      return y + 21;
   }

   private int groundStyleRow(GuiGraphics g, int mx, int my, Theme th, int x, int y) {
      ModConfig cfg = ModConfig.get();
      int w = this.midW - 16;
      int n = GROUND_STYLES.length;
      int gap = 3;
      int bw = (w - gap * (n - 1)) / n;

      for (int i = 0; i < n; i++) {
         String st = GROUND_STYLES[i];
         int bx = x + 8 + i * (bw + gap);
         boolean sel = st.equals(cfg.droppedItemStyle == null ? "vanilla" : cfg.droppedItemStyle);
         boolean hov = Ui.hover(mx, my, bx, y, bw, 30);
         Ui.rect(g, bx, y, bw, 30, sel ? th.accentDim() : (hov ? -14015424 : -14607314));
         if (sel) {
            Ui.border(g, bx, y, bw, 30, th.accent);
         }

         Ui.textCentered(g, Ui.trim(Ui.tr("maneszn_styles.ground." + st), bw - 4), bx + bw / 2, y + 6, sel ? -1185282 : -6646866);
         Ui.textCentered(g, Ui.trim(Ui.tr("maneszn_styles.ground." + st + ".desc"), bw - 4), bx + bw / 2, y + 18, -9739136);
         this.zones.addClipped(bx, y, bw, 30, this.clipTop, this.clipBottom, () -> {
            cfg.droppedItemStyle = st;
            ModConfig.save();
         }, null);
      }

      return y + 34;
   }

   private int actionRow(GuiGraphics g, int mx, int my, Theme th, int x, int y, String key, Runnable r) {
      int w = this.midW - 16;
      boolean hov = Ui.hover(mx, my, x + 8, y, w, 18);
      Ui.rect(g, x + 8, y, w, 18, hov ? -14015424 : -14607314);
      Ui.text(g, Ui.trim(Ui.tr(key), w - 20), x + 14, y + 5, hov ? th.accent : -1185282);
      Ui.textRight(g, "›", x + 8 + w - 6, y + 5, -9739136);
      this.zones.addClipped(x + 8, y, w, 18, this.clipTop, this.clipBottom, r, null);
      return y + 21;
   }

   private void renderInspector(GuiGraphics g, int mx, int my, Theme th) {
      int x = this.width - this.rightW - 4;
      int y = this.top + 4;
      int w = this.rightW;
      Ui.rect(g, x, y, w, this.bottom - y - 4, -15462116);
      StyleRegistry reg = StyleRegistry.get();
      ModConfig cfg = ModConfig.get();
      Category c = this.categoryId == null ? null : reg.category(this.categoryId);
      String lang = Ui.langShort();
      int maxPw = this.bottom - y - 4 - 150;
      int pw = Math.max(40, Math.min(w - 16, maxPw));
      int px = x + (w - pw) / 2;
      Ui.rect(g, px, y + 8, pw, pw, -15001818);
      Ui.border(g, px, y + 8, pw, pw, -13752510);
      int ty = y + 8 + pw + 8;
      if (c != null && this.tab == StylesScreen.Tab.COLLECTIONS) {
         String activeId = cfg.activeStyle(c.id());
         String inspId = this.hoveredStyle != null ? this.hoveredStyle : (this.selectedStyle != null ? this.selectedStyle : activeId);
         Style s = "vanilla".equals(inspId) ? null : reg.style(c.id(), inspId);
         if (s == null) {
            inspId = "vanilla";
         }

         Style active = "vanilla".equals(activeId) ? null : reg.style(c.id(), activeId);
         this.previewX = px;
         this.previewY = y + 8;
         this.previewSize = pw;
         if (TextureCache.has3d(c, s)) {
            long now = System.currentTimeMillis();
            if (this.lastFrameMs != 0L && !this.draggingPreview && now - this.lastDragMs > 2500L) {
               this.spinYaw = this.spinYaw + (now - this.lastFrameMs) * 0.045;
            }

            this.lastFrameMs = now;
            int frame = (int)Math.round(this.spinYaw / 10.0);
            TextureCache.spin(c, s, frame).ifPresent(t -> Ui.texture(g, t, px + 4, y + 12, pw - 8, pw - 8));
            Ui.textCentered(g, Ui.tr("maneszn_styles.ui.drag_rotate"), px + pw / 2, y + 8 + pw - 10, -9739136);
         } else {
            TextureCache.preview(c, s).ifPresent(t -> Ui.texture(g, t, px + 8, y + 16, pw - 16, pw - 16));
         }

         Ui.textShadow(g, Ui.trim(s == null ? "Vanilla" : s.name(), w - 16), x + 8, ty, -1185282);
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.ui.from", s == null ? "Minecraft" : s.sourceLabel(lang)), w - 16), x + 8, ty + 12, -6646866);
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.ui.now", active == null ? "Vanilla" : active.name()), w - 16), x + 8, ty + 23, -6646866);
         boolean isActive = inspId.equals(activeId);
         int by = ty + 37;
         String note = c.note(lang);
         if (note != null) {
            Ui.text(g, Ui.trim("ⓘ " + note, w - 16), x + 8, ty + 34, th.accent);
            by += 11;
         }

         if (c.kind() == Category.Kind.SOUND) {
            boolean hovL = Ui.hover(mx, my, x + 8, by, w - 16, 14);
            Ui.button(g, x + 8, by, w - 16, 14, "▶ " + Ui.tr("maneszn_styles.ui.listen"), hovL, false, th);
            this.zones.add(x + 8, by, w - 16, 14, () -> playCategorySound(c));
            by += 17;
         }

         boolean hovA = Ui.hover(mx, my, x + 8, by, w - 16, 16);
         Ui.button(g, x + 8, by, w - 16, 16, Ui.tr(isActive ? "maneszn_styles.ui.active" : "maneszn_styles.ui.apply"), hovA, !isActive, th);
         String target = inspId;
         if (!isActive) {
            this.zones.add(x + 8, by, w - 16, 16, () -> this.apply(c, target));
         }

         by += 19;
         boolean fav = cfg.isFavorite(c.id(), inspId);
         boolean hovF = Ui.hover(mx, my, x + 8, by, w - 16, 14);
         Ui.button(g, x + 8, by, w - 16, 14, (fav ? "★ " : "☆ ") + Ui.tr("maneszn_styles.ui.favorite"), hovF, false, th);
         this.zones.add(x + 8, by, w - 16, 14, () -> cfg.toggleFavorite(c.id(), target));
         ty = by + 22;
      } else if (this.tab == StylesScreen.Tab.COSMETICS && outfitView) {
         this.previewX = px;
         this.previewY = y + 8;
         this.previewSize = pw;
         long now = System.currentTimeMillis();
         if (this.lastFrameMs != 0L && !this.draggingPreview && now - this.lastDragMs > 2500L) {
            this.spinYaw = this.spinYaw + (now - this.lastFrameMs) * 0.045;
         }

         this.lastFrameMs = now;
         List<ModConfig.Outfit> list = cfg.outfits;
         ModConfig.Outfit o = this.hoveredOutfit >= 0 && this.hoveredOutfit < list.size() ? list.get(this.hoveredOutfit) : null;
         Map<String, String> lo = o != null ? o.cosmetics : cfg.cosmetics;
         CosmeticPreviews.loadoutFigure(lo, this.spinYaw, CosmeticManager.ticks()).ifPresent(t -> Ui.texture(g, t, px + 4, y + 12, pw - 8, pw - 8));
         Ui.textCentered(g, Ui.tr("maneszn_styles.ui.drag_rotate"), px + pw / 2, y + 8 + pw - 10, -9739136);
         Ui.textShadow(g, Ui.trim(o != null ? o.name : Ui.tr("maneszn_styles.outfit.current"), w - 16), x + 8, ty, -1185282);
         int n = 0;

         for (String k : lo.keySet()) {
            if (!k.startsWith("skin_")) {
               n++;
            }
         }

         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.outfit.items", n), w - 16), x + 8, ty + 12, -6646866);
         ty += 30;
      } else {
         if (this.tab == StylesScreen.Tab.COSMETICS) {
            this.renderCosmeticInspector(g, mx, my, th, x, y, w, px, pw);
            return;
         }

         g.blit(RenderPipelines.GUI_TEXTURED, LOGO, px + 8, y + 16, 0.0F, 0.0F, pw - 16, pw - 16, 64, 64, 64, 64);
         Ui.textShadow(g, "Maneszn Styles", x + 8, ty, -1185282);
         ty += 40;
      }

      this.renderThemes(g, x, ty, w, th);
   }

   private void renderThemes(GuiGraphics g, int x, int ty, int w, Theme th) {
      Ui.text(g, "Theme", x + 8, ty, -9739136);
      int sx = x + 8;
      int sy = ty + 11;

      for (Theme t : Theme.values()) {
         boolean sel = t == th;
         Ui.rect(g, sx, sy, 12, 12, t.accent);
         if (t.accent2 != 0) {
            Ui.rect(g, sx + 6, sy + 6, 6, 6, t.accent2);
         }

         if (sel) {
            Ui.border(g, sx - 1, sy - 1, 14, 14, -1);
         }

         this.zones.add(sx, sy, 12, 12, () -> {
            ModConfig.get().theme = t.id;
            ModConfig.save();
            this.toast(Ui.tr("maneszn_styles.toast.theme", t.label(Ui.lang())));
         });
         sx += 16;
         if (sx + 12 > x + w - 4) {
            sx = x + 8;
            sy += 16;
         }
      }
   }

   private void renderBottom(GuiGraphics g, int mx, int my, Theme th) {
      int y = this.bottom;
      Ui.rect(g, 0, y, this.width, 22, -15462116);
      Ui.rect(g, 0, y, this.width, 1, -13752510);
      boolean armed = System.currentTimeMillis() < this.resetArmedUntil;
      String[] labels = new String[]{
         Ui.tr("maneszn_styles.ui.import"),
         Ui.tr("maneszn_styles.ui.packs"),
         Ui.tr("maneszn_styles.ui.presets"),
         Ui.tr(armed ? "maneszn_styles.ui.reset_confirm" : "maneszn_styles.ui.reset")
      };
      Runnable[] actions = new Runnable[]{this::startImport, this::reloadRegistry, this::openPresets, this::reset};
      int bx = 6;

      for (int i = 0; i < labels.length; i++) {
         int bw = Ui.width(labels[i]) + 12;
         boolean hov = Ui.hover(mx, my, bx, y + 3, bw, 16);
         Ui.button(g, bx, y + 3, bw, 16, labels[i], hov, i == 0, th);
         this.zones.add(bx, y + 3, bw, 16, actions[i]);
         bx += bw + 4;
      }

      String keyName = ManesznStyles.openMenuKey.getTranslatedKeyMessage().getString();
      String open = Ui.tr("maneszn_styles.ui.open", keyName);
      int openW = Ui.width(open);
      Ui.textRight(g, open, this.width - 8, y + 7, -6646866);
      String hint = Ui.tr("maneszn_styles.ui.hint");
      int space = this.width - 8 - openW - 10 - bx;
      if (space > 40) {
         Ui.textCentered(g, Ui.trim(hint, space), bx + space / 2 + 4, y + 7, -9739136);
      }
   }

   private void renderZoom(GuiGraphics g, Theme th) {
      g.nextStratum();
      this.zones.clear();
      Ui.rect(g, 0, 0, this.width, this.height, -671088640);
      Category c = this.categoryId == null ? null : StyleRegistry.get().category(this.categoryId);
      if (c != null) {
         Style s = "vanilla".equals(this.zoomStyle) ? null : StyleRegistry.get().style(c.id(), this.zoomStyle);
         int size = Math.max(32, Math.min(this.width, this.height) - 80);
         int zx = (this.width - size) / 2;
         int zy = (this.height - size) / 2 - 8;
         Ui.rect(g, zx - 8, zy - 8, size + 16, size + 34, -15462116);
         Ui.border(g, zx - 8, zy - 8, size + 16, size + 34, th.accent);
         if (TextureCache.has3d(c, s)) {
            long now = System.currentTimeMillis();
            int frame = (int)(now / 60L % 36L);
            TextureCache.spin(c, s, frame).ifPresent(t -> Ui.texture(g, t, zx, zy, size, size));
         } else {
            TextureCache.preview(c, s).ifPresent(t -> Ui.texture(g, t, zx, zy, size, size));
         }

         Ui.textCentered(g, s == null ? "Vanilla" : s.name(), this.width / 2, zy + size + 8, -1185282);
         Ui.textCentered(g, Ui.tr("maneszn_styles.ui.zoom_close"), this.width / 2, zy + size + 18, -9739136);
      }

      this.zones.add(0, 0, this.width, this.height, () -> this.zoomStyle = null, () -> this.zoomStyle = null);
   }

   private void renderToast(GuiGraphics g, Theme th) {
      if (this.toast != null && System.currentTimeMillis() <= this.toastUntil) {
         g.nextStratum();
         int w = Ui.width(this.toast) + 16;
         int x = (this.width - w) / 2;
         int y = this.bottom - 24;
         Ui.rect(g, x, y, w, 16, -15001818);
         Ui.border(g, x, y, w, 16, th.accent);
         Ui.textCentered(g, this.toast, this.width / 2, y + 4, -1185282);
      }
   }

   private void drawScrollbar(GuiGraphics g, int x, int y, int h, double scroll, int max, int content, Theme th) {
      if (max > 0) {
         int barH = Math.max(12, h * h / Math.max(1, content));
         int by = y + (int)((h - barH) * (scroll / max));
         Ui.rect(g, x, y, 2, h, -13752510);
         Ui.rect(g, x, by, 2, barH, th.accent);
      }
   }

   private void apply(Category c, String styleId) {
      ModConfig cfg = ModConfig.get();
      this.selectedStyle = styleId;
      if (!cfg.activeStyle(c.id()).equals(styleId)) {
         if ("vanilla".equals(styleId)) {
            cfg.active.remove(c.id());
         } else {
            cfg.active.put(c.id(), styleId);
         }

         ModConfig.save();
         StylePackBuilder.applyAndReload();
         Style s = StyleRegistry.get().style(c.id(), styleId);
         this.toast(Ui.tr("maneszn_styles.toast.applied", s == null ? "Vanilla" : s.name(), c.displayName(Ui.langShort())));
      }
   }

   private static void playCategorySound(Category c) {
      String cid = c.id();

      String ev = switch (cid) {
         case "hit_sound" -> "entity.player.hurt";
         case "totem_sound" -> "item.totem.use";
         case "pearl_sound" -> "entity.player.teleport";
         case "explode_sound" -> "entity.generic.explode";
         default -> null;
      };
      if (ev != null) {
         SoundEvent sound = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(ev));
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
      }
   }

   private void startImport() {
      this.toast(Ui.tr("maneszn_styles.toast.pick_file"));
      FileDialogs.openImage(Ui.tr("maneszn_styles.ui.import")).thenAccept(p -> this.minecraft.execute(() -> p.ifPresent(this::openImport)));
   }

   private void openImport(Path file) {
      ModConfig.get().lastImportDir = file.getParent() == null ? "" : file.getParent().toString();
      ModConfig.save();
      this.minecraft.setScreen(new ImportScreen(this, file, this.categoryId));
   }

   private void openPresets() {
      this.minecraft.setScreen(new PresetsScreen(this));
   }

   private void reset() {
      long now = System.currentTimeMillis();
      if (now > this.resetArmedUntil) {
         this.resetArmedUntil = now + 3000L;
      } else {
         this.resetArmedUntil = 0L;
         ModConfig.get().active.clear();
         ModConfig.save();
         StylePackBuilder.applyAndReload();
         this.toast(Ui.tr("maneszn_styles.toast.reset"));
      }
   }

   void reloadRegistry() {
      StyleRegistry.get().reload();
      TextureCache.clear();
      if (this.categoryId != null && StyleRegistry.get().styles(this.categoryId).isEmpty()) {
         List<Category> vis = StyleRegistry.get().visibleCategories();
         this.categoryId = vis.isEmpty() ? null : vis.get(0).id();
      }

      boolean affected = false;

      for (Map.Entry<String, String> e : ModConfig.get().active.entrySet()) {
         Style s = StyleRegistry.get().style(e.getKey(), e.getValue());
         if (s == null || s.source() != Style.Source.BUILTIN) {
            affected = true;
         }
      }

      if (affected) {
         StylePackBuilder.applyAndReload();
      }

      this.toast(Ui.tr("maneszn_styles.toast.rescanned", StyleRegistry.get().styleCount(), StyleRegistry.get().scannedPackCount()));
   }

   void onImported(String catId, String styleId) {
      StyleRegistry.get().reload();
      TextureCache.clear();
      this.tab = StylesScreen.Tab.COLLECTIONS;
      this.categoryId = catId;
      this.selectedStyle = styleId;
      this.filter = StylesScreen.Filter.ALL;
   }

   void toast(String msg) {
      this.toast = msg;
      this.toastUntil = System.currentTimeMillis() + 2500L;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      double mx = event.x();
      double my = event.y();
      if (this.zoomStyle == null && this.search.visible && this.search.isMouseOver(mx, my)) {
         return super.mouseClicked(event, doubleClick);
      } else if (this.zoomStyle == null && this.skinBox.visible && this.skinBox.isMouseOver(mx, my)) {
         this.setFocused(this.skinBox);
         return super.mouseClicked(event, doubleClick);
      } else if (this.zoomStyle == null && this.outfitBox.visible && this.outfitBox.isMouseOver(mx, my)) {
         this.setFocused(this.outfitBox);
         return super.mouseClicked(event, doubleClick);
      } else {
         if (this.outfitBox.isFocused()) {
            this.outfitBox.setFocused(false);
            this.setFocused(null);
         }

         if (this.skinBox.isFocused()) {
            this.skinBox.setFocused(false);
            this.setFocused(null);
         }

         if (this.search.isFocused()) {
            this.search.setFocused(false);
            this.setFocused(null);
         }

         if (this.zoomStyle == null
            && (this.tab == StylesScreen.Tab.COLLECTIONS || this.tab == StylesScreen.Tab.COSMETICS)
            && event.button() == 0
            && Ui.hover((int)mx, (int)my, this.previewX, this.previewY, this.previewSize, this.previewSize)) {
            this.draggingPreview = true;
            return true;
         } else {
            return this.zones.click(mx, my, event.button()) ? true : super.mouseClicked(event, doubleClick);
         }
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      if (this.draggingPreview) {
         this.spinYaw += dragX * 2.5;
         this.lastDragMs = System.currentTimeMillis();
         return true;
      } else {
         return super.mouseDragged(event, dragX, dragY);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.draggingPreview) {
         this.draggingPreview = false;
         this.lastDragMs = System.currentTimeMillis();
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.zoomStyle != null) {
         return true;
      } else if (mouseX < 4 + this.leftW && mouseY >= this.listY && mouseY < this.listY + this.listH && this.tab != StylesScreen.Tab.EXTRAS) {
         this.leftScroll = Math.max(0.0, Math.min((double)this.leftMaxScroll, this.leftScroll - scrollY * 18.0));
         return true;
      } else if (mouseX >= this.midX && mouseX < this.midX + this.midW && mouseY >= this.gridY && mouseY < this.gridY + this.gridH) {
         this.gridScroll = Math.max(0.0, Math.min((double)this.gridMaxScroll, this.gridScroll - scrollY * 24.0));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      int key = event.key();
      if (this.zoomStyle != null) {
         if (key != 256 && key != 32 && key != 257) {
            return true;
         } else {
            this.zoomStyle = null;
            return true;
         }
      } else if (this.outfitBox.isFocused()) {
         if (key == 257 || key == 335) {
            this.saveOutfit();
            return true;
         } else if (key == 256) {
            this.outfitBox.setFocused(false);
            this.setFocused(null);
            return true;
         } else {
            return super.keyPressed(event);
         }
      } else if (this.skinBox.isFocused()) {
         if (key == 257 || key == 335) {
            this.applySkinName();
            return true;
         } else if (key == 256) {
            this.skinBox.setFocused(false);
            this.setFocused(null);
            return true;
         } else {
            return super.keyPressed(event);
         }
      } else if (this.search.isFocused()) {
         if (key != 256 && key != 257) {
            return super.keyPressed(event);
         } else {
            this.search.setFocused(false);
            this.setFocused(null);
            return true;
         }
      } else if (this.tab != StylesScreen.Tab.COLLECTIONS || key != 47 && key != 331) {
         if (key == 70 && this.tab == StylesScreen.Tab.COLLECTIONS && this.categoryId != null) {
            String target = this.hoveredStyle != null
               ? this.hoveredStyle
               : (this.selectedStyle != null ? this.selectedStyle : ModConfig.get().activeStyle(this.categoryId));
            ModConfig.get().toggleFavorite(this.categoryId, target);
            boolean fav = ModConfig.get().isFavorite(this.categoryId, target);
            this.toast(Ui.tr(fav ? "maneszn_styles.toast.fav_on" : "maneszn_styles.toast.fav_off"));
            return true;
         } else if (ManesznStyles.openMenuKey.matches(event)) {
            this.onClose();
            return true;
         } else {
            return super.keyPressed(event);
         }
      } else {
         this.focusSearch();
         return true;
      }
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.swallowSlash && event.codepoint() == 47) {
         this.swallowSlash = false;
         return true;
      } else {
         this.swallowSlash = false;
         if (!this.search.isFocused()
            && !this.skinBox.isFocused()
            && !this.outfitBox.isFocused()
            && this.zoomStyle == null
            && this.tab == StylesScreen.Tab.COLLECTIONS
            && event.codepoint() == 47) {
            this.focusSearch();
            this.swallowSlash = false;
            return true;
         } else {
            return super.charTyped(event);
         }
      }
   }

   private void focusSearch() {
      this.setFocused(this.search);
      this.search.setFocused(true);
      this.swallowSlash = true;
   }

   private void pickCape() {
      FileDialogs.openImage(Ui.tr("maneszn_styles.cape.upload")).thenAccept(opt -> opt.ifPresent(p -> this.minecraft.execute(() -> this.importCape(p))));
   }

   private void importCape(Path p) {
      CustomCapes.Result r = CustomCapes.importFile(p);
      if (r.id() == null) {
         this.toast(Ui.tr("maneszn_styles.toast.cape_error." + r.error()));
      } else {
         CosmeticManager.equip(Cosmetic.Type.CAPE, r.id());
         TextureCache.clear();
         this.toast(Ui.tr(ApiClient.get().status() == ApiClient.Status.ONLINE ? "maneszn_styles.toast.cape_uploaded" : "maneszn_styles.toast.cape_local"));
      }
   }

   @Override
   public void onFilesDrop(List<Path> files) {
      if (this.tab == StylesScreen.Tab.COSMETICS && cosType == Cosmetic.Type.CAPE && !outfitView) {
         for (Path p : files) {
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
            if (n.endsWith(".png") || n.endsWith(".gif") || n.endsWith(".jpg") || n.endsWith(".jpeg")) {
               this.importCape(p);
               return;
            }
         }
      }

      for (Path px : files) {
         String n = px.getFileName().toString().toLowerCase(Locale.ROOT);
         if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".bmp")) {
            this.openImport(px);
            return;
         }
      }

      this.toast(Ui.tr("maneszn_styles.toast.no_image"));
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private record Entry(String id, Style style) {
   }

   static enum Filter {
      ALL,
      FAVORITES,
      NEW;
   }

   static enum Tab {
      COLLECTIONS,
      COSMETICS,
      EXTRAS;
   }
}
