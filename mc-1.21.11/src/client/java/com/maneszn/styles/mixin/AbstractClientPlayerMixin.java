package com.maneszn.styles.mixin;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.cosmetic.CosmeticManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset.ResourceTexture;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class AbstractClientPlayerMixin {
   @Unique
   private static final ResourceTexture DEFAULT_ELYTRA = new ResourceTexture(Identifier.withDefaultNamespace("entity/equipment/wings/elytra"));
   @Unique
   private PlayerSkin maneszn$base;
   @Unique
   private Identifier maneszn$cape;
   @Unique
   private PlayerSkin maneszn$result;
   @Unique
   private boolean maneszn$physics;

   @Inject(
      method = {"getSkin"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void maneszn$cape(CallbackInfoReturnable<PlayerSkin> cir) {
      Hooks.cape = true;
      AbstractClientPlayer self = (AbstractClientPlayer)(Object)this;
      Identifier tex = CosmeticManager.capeTexture(self.getUUID());
      if (tex != null) {
         PlayerSkin skin = (PlayerSkin)cir.getReturnValue();
         if (skin != null) {
            boolean physics = ModConfig.get().capePhysics;
            if (skin != this.maneszn$base || !tex.equals(this.maneszn$cape) || physics != this.maneszn$physics) {
               ResourceTexture cape = new ResourceTexture(tex, tex);
               Texture elytra = (Texture)(skin.elytra() != null ? skin.elytra() : DEFAULT_ELYTRA);
               this.maneszn$result = physics
                  ? new PlayerSkin(skin.body(), null, elytra, skin.model(), skin.secure())
                  : new PlayerSkin(skin.body(), cape, elytra, skin.model(), skin.secure());
               this.maneszn$base = skin;
               this.maneszn$cape = tex;
               this.maneszn$physics = physics;
            }

            cir.setReturnValue(this.maneszn$result);
         }
      }
   }
}
