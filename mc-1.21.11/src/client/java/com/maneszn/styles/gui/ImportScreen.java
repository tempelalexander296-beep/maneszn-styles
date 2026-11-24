package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.image.StyleImporter;
import com.maneszn.styles.pack.StylePackBuilder;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.Group;
import com.maneszn.styles.style.StyleRegistry;
import com.maneszn.styles.style.Target;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ImportScreen extends Screen {
   private static final int[] RESOLUTIONS = new int[]{0, 16, 32, 64};
   private final StylesScreen parent;
   private final Path file;
   private final Ui.Zones zones = new Ui.Zones();
   private final List<Category> categories = new ArrayList<>();
   private BufferedImage source;
   private String error;
   private EditBox nameBox;
   private int catIndex;
   private int targetIndex = -1;
   private Optional<TextureCache.Tex> original = Optional.empty();
   private Optional<TextureCache.Tex> result = Optional.empty();
   private String resultInfo = "";
   private double listScroll;
   private int listMax;
   private int listY;
   private int listH;

   public ImportScreen(StylesScreen parent, Path file, String preferredCategory) {
      super(Component.translatable("maneszn_styles.import.title"));
      this.parent = parent;
      this.file = file;
      String lang = Ui.langShort();

      for (Group g : StyleRegistry.get().groups()) {
         for (Category c : StyleRegistry.get().categories()) {
            if (c.group().equals(g.id()) && c.kind() != Category.Kind.SOUND) {
               this.categories.add(c);
            }
         }
      }

      for (int i = 0; i < this.categories.size(); i++) {
         if (this.categories.get(i).id().equals(preferredCategory)) {
            this.catIndex = i;
         }
      }

      String fn = file.getFileName().toString().toLowerCase(Locale.ROOT);

      for (int ix = 0; ix < this.categories.size(); ix++) {
         Category cx = this.categories.get(ix);
         if (fn.contains(cx.id()) || fn.contains(cx.displayName(lang).toLowerCase(Locale.ROOT))) {
            this.catIndex = ix;
            break;
         }
      }

      try (InputStream in = Files.newInputStream(file)) {
         this.source = ImageIO.read(in);
         if (this.source == null) {
            this.error = Ui.tr("maneszn_styles.import.unreadable");
         }
      } catch (Exception var11) {
         this.error = Ui.tr("maneszn_styles.import.unreadable") + " (" + var11.getMessage() + ")";
      }
   }

   @Override
   protected void init() {
      String old = this.nameBox == null ? this.defaultName() : this.nameBox.getValue();
      int rightW = Math.max(130, Math.min(190, this.width / 4));
      int rx = this.width - rightW - 8;
      this.nameBox = new EditBox(this.font, rx + 8, 58, rightW - 16, 14, Component.translatable("maneszn_styles.import.name"));
      this.nameBox.setMaxLength(32);
      this.nameBox.setValue(old);
      this.addRenderableWidget(this.nameBox);
      if (this.original.isEmpty() && this.source != null) {
         ImageProcessor.Img img = ImageProcessor.Img.of(this.source);
         int m = Math.max(img.w(), img.h());
         if (m > 256) {
            img = ImageProcessor.resizeArea(img, Math.max(1, img.w() * 256 / m), Math.max(1, img.h() * 256 / m));
         }

         this.original = TextureCache.bytes("import_original", png(img));
         this.recompute();
      }
   }

   private String defaultName() {
      String n = this.file.getFileName().toString();
      int dot = n.lastIndexOf(46);
      if (dot > 0) {
         n = n.substring(0, dot);
      }

      n = n.replace('_', ' ').replace('-', ' ').trim();
      return n.isEmpty() ? "Mein Style" : Character.toUpperCase(n.charAt(0)) + n.substring(1);
   }

   private Category category() {
      return this.categories.isEmpty() ? null : this.categories.get(this.catIndex);
   }

   private Target target() {
      Category c = this.category();
      return c != null && this.targetIndex >= 0 && this.targetIndex < c.targets().size() ? c.targets().get(this.targetIndex) : null;
   }

   private void recompute() {
      Category c = this.category();
      if (this.source != null && c != null) {
         Target ref = this.target() != null ? this.target() : c.previewTargetOrFirst();
         ImageProcessor.Img out = ImageProcessor.process(this.source, ref.kindOr(c.kind()), ref.width(), ref.height(), ModConfig.get().importResolution);
         this.result = TextureCache.bytes("import_result", png(out));
         this.resultInfo = out.w() + "×" + out.h() + " px";
      }
   }

   private static byte[] png(ImageProcessor.Img img) {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ImageIO.write(img.toBuffered(), "png", bos);
         return bos.toByteArray();
      } catch (Exception var2) {
         return new byte[0];
      }
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Ui.background(g, this.width, this.height);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      this.zones.clear();
      Theme th = Theme.current();
      String lang = Ui.langShort();
      Ui.rect(g, 0, 0, this.width, 24, -15462116);
      Ui.rect(g, 0, 23, this.width, 1, -13752510);
      Ui.textShadow(g, Ui.tr("maneszn_styles.import.title"), 8, 8, th.accent);
      Ui.textRight(g, Ui.trim(this.file.getFileName().toString(), this.width / 2), this.width - 8, 8, -6646866);
      int leftW = Math.max(120, Math.min(170, this.width / 5));
      int rightW = Math.max(130, Math.min(190, this.width / 4));
      int rx = this.width - rightW - 8;
      int lx = 4;
      int ly = 28;
      Ui.rect(g, lx, ly, leftW, this.height - ly - 4, -15462116);
      Ui.text(g, Ui.tr("maneszn_styles.import.category"), lx + 6, ly + 5, -9739136);
      this.listY = ly + 16;
      this.listH = this.height - this.listY - 8;
      g.enableScissor(lx, this.listY, lx + leftW, this.listY + this.listH);
      int cy = this.listY - (int)this.listScroll;

      for (int i = 0; i < this.categories.size(); i++) {
         Category c = this.categories.get(i);
         final int idx = i;
         boolean sel = i == this.catIndex;
         boolean hov = my >= this.listY && my < this.listY + this.listH && Ui.hover(mx, my, lx + 2, cy, leftW - 4, 14);
         if (sel || hov) {
            Ui.rect(g, lx + 2, cy, leftW - 4, 14, sel ? th.accentDim() : -14015424);
         }

         Ui.text(g, Ui.trim(c.displayName(lang), leftW - 14), lx + 8, cy + 3, sel ? -1185282 : -6646866);
         this.zones.addClipped(lx + 2, cy, leftW - 4, 14, this.listY, this.listY + this.listH, () -> {
            this.catIndex = idx;
            this.targetIndex = -1;
            this.recompute();
         }, null);
         cy += 15;
      }

      g.disableScissor();
      this.listMax = Math.max(0, this.categories.size() * 15 - this.listH);
      int mxL = lx + leftW + 6;
      int mw = rx - mxL - 6;
      Ui.rect(g, mxL, ly, mw, this.height - ly - 4, -15462116);
      if (this.error != null) {
         Ui.textCentered(g, this.error, mxL + mw / 2, ly + 40, -1096636);
      } else {
         int box = Math.max(32, Math.min((mw - 30) / 2, this.height - ly - 60));
         int bx1 = mxL + (mw - box * 2 - 18) / 2;
         int by = ly + 22;
         int bx2 = bx1 + box + 18;
         Ui.text(g, Ui.tr("maneszn_styles.import.original"), bx1, ly + 8, -9739136);
         Ui.text(g, Ui.tr("maneszn_styles.import.result") + "  " + this.resultInfo, bx2, ly + 8, -9739136);
         checker(g, bx1, by, box, box);
         checker(g, bx2, by, box, box);
         this.original.ifPresent(t -> Ui.texture(g, t, bx1 + 4, by + 4, box - 8, box - 8));
         this.result.ifPresent(t -> Ui.texture(g, t, bx2 + 4, by + 4, box - 8, box - 8));
         Ui.textCentered(g, "→", bx1 + box + 9, by + box / 2 - 4, th.accent);
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.import.explain"), mw - 16), mxL + 8, by + box + 10, -9739136);
      }

      Ui.rect(g, rx, ly, rightW, this.height - ly - 4, -15462116);
      Category c = this.category();
      Ui.text(g, Ui.tr("maneszn_styles.import.name"), rx + 8, 47, -9739136);
      int oy = 80;
      if (c != null) {
         Ui.text(g, Ui.tr("maneszn_styles.import.target"), rx + 8, oy, -9739136);
         Target t = this.target();
         String tLabel = t == null ? Ui.tr("maneszn_styles.import.target_all") : t.name();
         boolean hovT = Ui.hover(mx, my, rx + 8, oy + 10, rightW - 16, 16);
         Ui.button(g, rx + 8, oy + 10, rightW - 16, 16, "‹ " + tLabel + " ›", hovT, false, th);
         this.zones.add(rx + 8, oy + 10, rightW - 16, 16, () -> this.cycleTarget(1), () -> this.cycleTarget(-1));
         oy += 34;
      }

      Ui.text(g, Ui.tr("maneszn_styles.import.resolution"), rx + 8, oy, -9739136);
      int res = ModConfig.get().importResolution;
      String rLabel = res == 0 ? Ui.tr("maneszn_styles.import.auto") : res + "×" + res;
      boolean hovR = Ui.hover(mx, my, rx + 8, oy + 10, rightW - 16, 16);
      Ui.button(g, rx + 8, oy + 10, rightW - 16, 16, "‹ " + rLabel + " ›", hovR, false, th);
      this.zones.add(rx + 8, oy + 10, rightW - 16, 16, () -> this.cycleRes(1), () -> this.cycleRes(-1));
      oy += 40;
      boolean canImport = this.error == null && c != null && !this.nameBox.getValue().isBlank();
      boolean hovI = Ui.hover(mx, my, rx + 8, oy, rightW - 16, 18);
      Ui.button(g, rx + 8, oy, rightW - 16, 18, Ui.tr("maneszn_styles.import.do"), hovI && canImport, canImport, th);
      if (canImport) {
         this.zones.add(rx + 8, oy, rightW - 16, 18, this::doImport);
      }

      boolean hovC = Ui.hover(mx, my, rx + 8, oy + 22, rightW - 16, 16);
      Ui.button(g, rx + 8, oy + 22, rightW - 16, 16, Ui.tr("maneszn_styles.ui.cancel"), hovC, false, th);
      this.zones.add(rx + 8, oy + 22, rightW - 16, 16, this::onClose);
      super.render(g, mx, my, pt);
   }

   private static void checker(GuiGraphics g, int x, int y, int w, int h) {
      int s = 6;

      for (int yy = 0; yy < h; yy += s) {
         for (int xx = 0; xx < w; xx += s) {
            int c = (xx / s + yy / s & 1) == 0 ? -14804440 : -14015176;
            g.fill(x + xx, y + yy, x + Math.min(w, xx + s), y + Math.min(h, yy + s), c);
         }
      }

      Ui.border(g, x, y, w, h, -13752510);
   }

   private void cycleTarget(int dir) {
      Category c = this.category();
      if (c != null) {
         int n = c.targets().size() + 1;
         this.targetIndex = Math.floorMod(this.targetIndex + 1 + dir, n) - 1;
         this.recompute();
      }
   }

   private void cycleRes(int dir) {
      int cur = 0;

      for (int i = 0; i < RESOLUTIONS.length; i++) {
         if (RESOLUTIONS[i] == ModConfig.get().importResolution) {
            cur = i;
         }
      }

      ModConfig.get().importResolution = RESOLUTIONS[Math.floorMod(cur + dir, RESOLUTIONS.length)];
      ModConfig.save();
      this.recompute();
   }

   private void doImport() {
      Category c = this.category();

      try {
         String author = this.minecraft.getUser().getName();
         StyleImporter.Result r = StyleImporter.importImage(this.file, c, this.target(), this.nameBox.getValue().trim(), author);
         this.parent.onImported(c.id(), r.styleId());
         ModConfig.get().active.put(c.id(), r.styleId());
         ModConfig.save();
         StylePackBuilder.applyAndReload();
         this.parent.toast(Ui.tr("maneszn_styles.toast.imported", this.nameBox.getValue().trim()));
         this.minecraft.setScreen(this.parent);
      } catch (Exception var4) {
         ManesznStyles.LOGGER.error("Import fehlgeschlagen", var4);
         this.error = Ui.tr("maneszn_styles.import.failed") + ": " + var4.getMessage();
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.nameBox.isMouseOver(event.x(), event.y())) {
         return super.mouseClicked(event, doubleClick);
      } else {
         if (this.nameBox.isFocused()) {
            this.nameBox.setFocused(false);
            this.setFocused(null);
         }

         return this.zones.click(event.x(), event.y(), event.button()) ? true : super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (mouseY >= this.listY && mouseY < this.listY + this.listH && mouseX < this.width / 4.0) {
         this.listScroll = Math.max(0.0, Math.min((double)this.listMax, this.listScroll - scrollY * 15.0));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() == 257 && !this.nameBox.getValue().isBlank() && this.error == null) {
         this.doImport();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
