package com.maneszn.styles.hud;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PvpStats {
   private static long tick;
   private static int pendingTarget = -1;
   private static long pendingTick;
   private static double pendingReach;
   private static int combo;
   private static long lastHitTick = -1000L;
   private static double lastReach = -1.0;
   private static long lastReachTick = -1000L;
   private static int lastHurtTime;
   private static boolean wasOnGround = true;
   private static long lastJumpTick = -1000L;
   private static long hurtTick = -1000L;
   private static boolean hurtOnGround;
   private static boolean waitingForJump;
   private static int lastResult = Integer.MAX_VALUE;
   private static long resultTick = -1000L;
   private static int tries;
   private static int good;

   private PvpStats() {
   }

   public static void register() {
      AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
         if (player instanceof LocalPlayer) {
            onAttack(entity);
         }

         return InteractionResult.PASS;
      });
   }

   private static void onAttack(Entity target) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && target instanceof LivingEntity) {
         pendingTarget = target.getId();
         pendingTick = tick;
         pendingReach = reach(mc.player.getEyePosition(), target.getBoundingBox());
      }
   }

   static double reach(Vec3 eye, AABB box) {
      double x = Math.max(box.minX, Math.min(eye.x, box.maxX));
      double y = Math.max(box.minY, Math.min(eye.y, box.maxY));
      double z = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
      return eye.distanceTo(new Vec3(x, y, z));
   }

   public static void tick(Minecraft mc) {
      tick++;
      if (mc.player != null && mc.level != null) {
         LocalPlayer p = mc.player;
         if (pendingTarget >= 0) {
            if (mc.level.getEntity(pendingTarget) instanceof LivingEntity le && le.hurtTime > 0) {
               combo = tick - lastHitTick > 40L ? 1 : combo + 1;
               lastHitTick = tick;
               lastReach = pendingReach;
               lastReachTick = tick;
               pendingTarget = -1;
            } else if (tick - pendingTick > 4L) {
               pendingTarget = -1;
            }
         }

         if (tick - lastHitTick > 40L) {
            combo = 0;
         }

         boolean hurtNow = p.hurtTime > lastHurtTime && p.hurtTime >= 8;
         lastHurtTime = p.hurtTime;
         boolean onGround = p.onGround();
         if (wasOnGround && !onGround && p.getDeltaMovement().y > 0.2) {
            lastJumpTick = tick;
         }

         if (hurtNow) {
            combo = 0;
            hurtTick = tick;
            hurtOnGround = onGround || tick - lastJumpTick <= 2L;
            waitingForJump = hurtOnGround;
            if (hurtOnGround && tick - lastJumpTick <= 2L) {
               finish((int)(lastJumpTick - tick));
            }
         }

         if (waitingForJump) {
            if (lastJumpTick >= hurtTick) {
               finish((int)(lastJumpTick - hurtTick));
            } else if (tick - hurtTick > 6L) {
               finish(Integer.MIN_VALUE);
            }
         }

         wasOnGround = onGround;
      }
   }

   private static void finish(int diff) {
      waitingForJump = false;
      lastResult = diff;
      resultTick = tick;
      tries++;
      if (diff != Integer.MIN_VALUE && Math.abs(diff) <= 1) {
         good++;
      }
   }

   public static int combo() {
      return combo;
   }

   public static double reach() {
      return tick - lastReachTick > 100L ? -1.0 : lastReach;
   }

   public static String jumpResetText(boolean de) {
      return jumpResetText(de, true, true);
   }

   public static String jumpResetText(boolean de, boolean showTicks, boolean showRate) {
      if (lastResult != Integer.MAX_VALUE && tick - resultTick <= 80L) {
         String rate = tries != 0 && showRate ? " (" + Math.round(good * 100.0 / tries) + "%)" : "";
         if (lastResult == Integer.MIN_VALUE) {
            return (de ? "Jump Reset: verpasst" : "Jump reset: missed") + rate;
         } else {
            int a = Math.abs(lastResult);
            String grade;
            if (a == 0) {
               grade = de ? "perfekt" : "perfect";
            } else if (a == 1) {
               grade = de ? "gut" : "good";
            } else {
               grade = de ? "knapp" : "close";
            }

            String when = lastResult != 0 && showTicks
               ? (lastResult < 0 ? (de ? ", " + a + " Tick zu früh" : ", " + a + " tick early") : (de ? ", " + a + " Tick zu spät" : ", " + a + " tick late"))
               : "";
            return "Jump Reset: " + grade + when + rate;
         }
      } else {
         return null;
      }
   }

   public static int jumpResetColor() {
      if (lastResult == Integer.MIN_VALUE) {
         return -43691;
      } else {
         int a = Math.abs(lastResult);
         return a == 0 ? -11141240 : (a == 1 ? -10163 : -24512);
      }
   }
}
