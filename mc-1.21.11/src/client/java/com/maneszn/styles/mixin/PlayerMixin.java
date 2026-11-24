package com.maneszn.styles.mixin;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.name.NameDecorator;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerMixin {
   @Inject(
      method = {"getDisplayName"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void maneszn$decorateName(CallbackInfoReturnable<Component> cir) {
      if ((Object)this instanceof AbstractClientPlayer player) {
         Hooks.nametag = true;
         cir.setReturnValue(NameDecorator.decorate(player.getUUID(), (Component)cir.getReturnValue(), player.getName().getString(), true));
      }
   }
}
