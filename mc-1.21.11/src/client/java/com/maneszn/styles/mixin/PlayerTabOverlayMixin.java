package com.maneszn.styles.mixin;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.name.NameDecorator;
import com.maneszn.styles.net.Profiles;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerTabOverlay.class})
public abstract class PlayerTabOverlayMixin {
   @Inject(
      method = {"getNameForDisplay"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void maneszn$decorateTab(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
      Hooks.tab = true;
      Object profile = info.getProfile();
      cir.setReturnValue(NameDecorator.decorate(Profiles.id(profile), (Component)cir.getReturnValue(), Profiles.name(profile)));
   }
}
