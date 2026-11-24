package com.maneszn.styles.tools;

import com.maneszn.styles.gui.Theme;
import com.maneszn.styles.gui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class TrimScreen extends ToolScreen {
   private static final String[] ARMOR = new String[]{"netherite", "diamond", "iron", "golden", "chainmail", "leather", "copper"};
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final String[] PIECES = new String[]{"helmet", "chestplate", "leggings", "boots"};
   private static final int[] armor = new int[]{0, 0, 0, 0};
   private static final int[] pattern = new int[]{-1, -1, -1, -1};
   private static final int[] material = new int[]{0, 0, 0, 0};
   private static int selected = 1;
   private List<Reference<TrimPattern>> patterns = new ArrayList<>();
   private List<Reference<TrimMaterial>> materials = new ArrayList<>();
   private ArmorStand stand;

   public TrimScreen(Screen parent) {
      super("maneszn_styles.trim.title", parent);
   }

   @Override
   protected int maxWidth() {
      return 380;
   }

   @Override
   protected void init() {
      super.init();
      if (this.minecraft.level != null) {
         RegistryAccess ra = this.minecraft.level.registryAccess();
         this.patterns = ra.lookupOrThrow(Registries.TRIM_PATTERN).listElements().toList();
         this.materials = ra.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().toList();
         this.stand = new ArmorStand(EntityType.ARMOR_STAND, this.minecraft.level);
         this.stand.setShowArms(true);
         this.stand.setNoBasePlate(true);
         this.rebuild();
      }
   }

   private void rebuild() {
      if (this.stand != null) {
         for (int i = 0; i < 4; i++) {
            Item it = item(ARMOR[armor[i]], PIECES[i]);
            ItemStack st = it == null ? ItemStack.EMPTY : new ItemStack(it);
            if (!st.isEmpty() && pattern[i] >= 0 && pattern[i] < this.patterns.size() && !this.materials.isEmpty()) {
               st.set(DataComponents.TRIM, new ArmorTrim(this.materials.get(material[i] % this.materials.size()), this.patterns.get(pattern[i])));
            }

            this.stand.setItemSlot(SLOTS[i], st);
         }
      }
   }

   private static Item item(String mat, String piece) {
      String id = mat + "_" + piece;
      Optional<Item> opt = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(id));
      return opt.orElse(null);
   }

   @Override
   protected void draw(GuiGraphics g, int mx, int my, Theme th) {
      if (this.stand == null) {
         Ui.text(g, Ui.tr("maneszn_styles.trim.ingame"), this.px + 10, this.py + 30, -6646866);
      } else {
         int sx = this.px + 10;
         int sy = this.py + 24;
         int sw = 140;
         int sh = this.ph - 34;
         Ui.rect(g, sx, sy, sw, sh, -14607314);
         InventoryScreen.renderEntityInInventoryFollowsMouse(g, sx, sy, sx + sw, sy + sh, Math.min(70, sh / 3), 0.0625F, mx, my, this.stand);
         int x = sx + sw + 10;
         int w = this.px + this.pw - 10 - x;
         int y = sy;
         int bw = (w - 9) / 4;

         for (int i = 0; i < 4; i++) {
            int bx = x + i * (bw + 3);
            boolean hov = Ui.hover(mx, my, bx, y, bw, 16);
            Ui.button(g, bx, y, bw, 16, Ui.tr("maneszn_styles.trim.piece." + PIECES[i]), hov, i == selected, th);
            int idx = i;
            this.zones.add(bx, y, bw, 16, () -> selected = idx);
         }

         y += 24;
         y = this.cycleRow(
            g,
            mx,
            my,
            th,
            x,
            y,
            w,
            Ui.tr("maneszn_styles.trim.armor"),
            Ui.tr("maneszn_styles.trim.mat." + ARMOR[armor[selected]]),
            d -> armor[selected] = Math.floorMod(armor[selected] + d, ARMOR.length)
         );
         String pat = pattern[selected] >= 0 && !this.patterns.isEmpty()
            ? this.patterns.get(pattern[selected]).value().description().getString()
            : Ui.tr("maneszn_styles.trim.none");
         y = this.cycleRow(
            g,
            mx,
            my,
            th,
            x,
            y,
            w,
            Ui.tr("maneszn_styles.trim.pattern"),
            pat,
            d -> pattern[selected] = Math.floorMod(pattern[selected] + 1 + d, this.patterns.size() + 1) - 1
         );
         String mat = this.materials.isEmpty() ? "–" : this.materials.get(material[selected] % this.materials.size()).value().description().getString();
         y = this.cycleRow(
            g,
            mx,
            my,
            th,
            x,
            y,
            w,
            Ui.tr("maneszn_styles.trim.material"),
            mat,
            d -> material[selected] = Math.floorMod(material[selected] + d, Math.max(1, this.materials.size()))
         );
         y += 6;
         boolean ha = Ui.hover(mx, my, x, y, w, 16);
         Ui.button(g, x, y, w, 16, Ui.tr("maneszn_styles.trim.apply_all"), ha, false, th);
         this.zones.add(x, y, w, 16, () -> {
            for (int i = 0; i < 4; i++) {
               armor[i] = armor[selected];
               pattern[i] = pattern[selected];
               material[i] = material[selected];
            }

            this.rebuild();
         });
         y += 26;
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.trim.hint"), w), x, y, -9739136);
      }
   }

   private int cycleRow(GuiGraphics g, int mx, int my, Theme th, int x, int y, int w, String label, String value, IntConsumer change) {
      Ui.text(g, label, x, y, -6646866);
      y += 11;
      boolean hl = Ui.hover(mx, my, x, y, 16, 16);
      boolean hr = Ui.hover(mx, my, x + w - 16, y, 16, 16);
      Ui.button(g, x, y, 16, 16, "‹", hl, false, th);
      Ui.button(g, x + w - 16, y, 16, 16, "›", hr, false, th);
      Ui.rect(g, x + 18, y, w - 36, 16, -14607314);
      Ui.textCentered(g, Ui.trim(value, w - 40), x + w / 2, y + 4, -1185282);
      this.zones.add(x, y, 16, 16, () -> {
         change.accept(-1);
         this.rebuild();
      });
      this.zones.add(x + w - 16, y, 16, 16, () -> {
         change.accept(1);
         this.rebuild();
      });
      return y + 22;
   }
}
