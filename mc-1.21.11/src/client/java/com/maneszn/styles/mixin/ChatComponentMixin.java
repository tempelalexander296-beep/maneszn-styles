package com.maneszn.styles.mixin;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.name.NameDecorator;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ChatComponent.class})
public abstract class ChatComponentMixin {
   @ModifyVariable(
      method = {"addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"},
      at = @At("HEAD"),
      argsOnly = true,
      require = 0
   )
   private Component maneszn$decorateChat(Component message) {
      Hooks.chat = true;
      return NameDecorator.decorateChat(message);
   }
}
