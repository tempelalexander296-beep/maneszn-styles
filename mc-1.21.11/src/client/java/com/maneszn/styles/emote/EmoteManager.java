package com.maneszn.styles.emote;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.cosmetic.CosmeticManager;
import com.maneszn.styles.net.ApiClient;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;

public final class EmoteManager {
   private static final Map<UUID, EmoteManager.Active> REMOTE = new ConcurrentHashMap<>();
   private static volatile EmoteManager.Active local;
   private static CameraType cameraBefore;
   private static boolean switchedCamera;

   private EmoteManager() {
   }

   public static EmoteManager.Active local() {
      return local;
   }

   public static void play(String id) {
      Emote e = Emotes.get(id);
      if (e != null) {
         if (e.unlocked()) {
            Minecraft mc = Minecraft.getInstance();
            if (local != null && local.id().equals(id)) {
               stop();
            } else {
               local = new EmoteManager.Active(id, System.currentTimeMillis());
               if (!switchedCamera) {
                  cameraBefore = mc.options.getCameraType();
                  mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                  switchedCamera = true;
               }

               ApiClient.get().sendEmote(id);
            }
         }
      }
   }

   public static void stop() {
      if (local != null) {
         local = null;
         Minecraft mc = Minecraft.getInstance();
         if (switchedCamera && cameraBefore != null) {
            mc.options.setCameraType(cameraBefore);
         }

         switchedCamera = false;
         ApiClient.get().sendEmote("");
      }
   }

   public static void tick(Minecraft mc) {
      EmoteMusic.tick(mc);
      EmoteManager.Active a = local;
      if (a != null) {
         Emote e = Emotes.get(a.id());
         boolean over = e == null || !e.loops() && System.currentTimeMillis() - a.startedAt() > e.duration() * 50L;
         boolean moved = mc.player == null
            || mc.options.keyUp.isDown()
            || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown()
            || mc.options.keyRight.isDown()
            || mc.options.keyJump.isDown()
            || mc.options.keyShift.isDown();
         if (over || moved) {
            stop();
         }
      }

      long now = System.currentTimeMillis();
      REMOTE.entrySet().removeIf(en -> {
         Emote e = Emotes.get(en.getValue().id());
         return e == null || !e.loops() && now - en.getValue().startedAt() > e.duration() * 50L || now - en.getValue().startedAt() > 600000L;
      });
   }

   public static void setRemote(Map<UUID, EmoteManager.Active> active) {
      REMOTE.keySet().removeIf(u -> !active.containsKey(u));

      for (Entry<UUID, EmoteManager.Active> e : active.entrySet()) {
         EmoteManager.Active old = REMOTE.get(e.getKey());
         if (old == null || !old.id().equals(e.getValue().id()) || Math.abs(old.startedAt() - e.getValue().startedAt()) > 1500L) {
            REMOTE.put(e.getKey(), e.getValue());
         }
      }
   }

   public static EmoteManager.Active of(UUID uuid) {
      if (uuid == null) {
         return null;
      } else {
         return ApiClient.isSelf(uuid) ? local : REMOTE.get(uuid);
      }
   }

   public static void apply(int entityId, ModelPart[] parts) {
      UUID uuid = CosmeticManager.uuidOf(entityId);
      EmoteManager.Active a = of(uuid);
      if (a != null) {
         Emote e = Emotes.get(a.id());
         if (e != null) {
            Hooks.emote = true;
            double t = (System.currentTimeMillis() - a.startedAt()) / 50.0;
            EmotePose p = e.pose(t);

            for (int i = 0; i < 6; i++) {
               ModelPart part = parts[i];
               if (part != null) {
                  if (p.set[i]) {
                     part.xRot = p.rot[i][0];
                     part.yRot = p.rot[i][1];
                     part.zRot = p.rot[i][2];
                  }

                  part.x = part.x + p.off[i][0];
                  part.y = part.y + p.off[i][1];
                  part.z = part.z + p.off[i][2];
               }
            }
         }
      }
   }

   public record Active(String id, long startedAt) {
   }
}
