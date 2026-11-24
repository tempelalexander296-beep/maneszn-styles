package com.maneszn.styles.mixin;

import com.maneszn.styles.pack.StylePackBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ReloadableResourceManager.class})
public abstract class ReloadableResourceManagerMixin {
   @Shadow
   @Final
   private PackType type;

   @ModifyVariable(
      method = {"createReload"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private List<PackResources> maneszn$appendVirtualPack(List<PackResources> packs) {
      if (this.type != PackType.CLIENT_RESOURCES) {
         return packs;
      } else {
         PackResources virtualPack = StylePackBuilder.createPackResources();
         if (virtualPack == null) {
            return packs;
         } else {
            List<PackResources> withOurs = new ArrayList<>(packs);
            withOurs.add(virtualPack);
            return withOurs;
         }
      }
   }
}
