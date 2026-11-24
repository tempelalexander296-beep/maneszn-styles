package com.maneszn.styles.mixin;

import com.maneszn.styles.cosmetic.CosmeticManager;
import com.maneszn.styles.emote.EmoteManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HumanoidModel.class})
public abstract class HumanoidModelMixin {
   @Inject(
      method = {"setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"},
      at = {@At("RETURN")},
      require = 0
   )
   private void maneszn$emote(HumanoidRenderState state, CallbackInfo ci) {
      if (state instanceof AvatarRenderState avatar) {
         HumanoidModel self = (HumanoidModel)(Object)this;
         if (CosmeticManager.holdsHand(CosmeticManager.uuidOf(avatar.id))) {
            self.rightArm.xRot *= 0.35F;
            self.rightArm.zRot += 0.3F;
         }

         EmoteManager.apply(avatar.id, new ModelPart[]{self.head, self.body, self.rightArm, self.leftArm, self.rightLeg, self.leftLeg});
      }
   }
}
