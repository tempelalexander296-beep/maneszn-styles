package com.maneszn.styles.cosmetic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PetTracker {
   private static final Map<UUID, PetTracker.Pet> PETS = new HashMap<>();

   private PetTracker() {
   }

   public static PetTracker.Pet update(UUID uuid, double px, double py, double pz, float bodyYaw) {
      if (PETS.size() > 64) {
         PETS.clear();
      }

      PetTracker.Pet p = PETS.computeIfAbsent(uuid, u -> new PetTracker.Pet());
      double yr = Math.toRadians(bodyYaw);
      double fx = -Math.sin(yr);
      double fz = Math.cos(yr);
      double lx = Math.cos(yr);
      double lz = Math.sin(yr);
      double tx = px + lx * 0.95 - fx * 0.7;
      double tz = pz + lz * 0.95 - fz * 0.7;
      long now = System.nanoTime();
      double dt = p.lastNanos == 0L ? 0.016 : Math.min(0.1, (now - p.lastNanos) / 1.0E9);
      p.lastNanos = now;
      double dx = tx - p.x;
      double dz = tz - p.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (p.init && !(dist > 10.0) && !(Math.abs(py - p.y) > 6.0)) {
         double k = 1.0 - Math.exp(-dt * 3.5);
         double step = dist > 0.12 ? k : 0.0;
         p.x += dx * step;
         p.z += dz * step;
         p.y = p.y + (py - p.y) * (1.0 - Math.exp(-dt * 8.0));
         double moved = Math.sqrt(dx * dx + dz * dz) * step;
         double sp = Math.min(1.0, moved / Math.max(1.0E-4, dt) / 4.0);
         p.speed = p.speed + (sp - p.speed) * Math.min(1.0, dt * 8.0);
         p.phase = p.phase + dt * (6.0 + 10.0 * p.speed);
         float want = dist > 0.25 ? (float)Math.toDegrees(Math.atan2(-dx, dz)) : bodyYaw;
         float diff = wrap(want - p.yaw);
         p.yaw = p.yaw + diff * (float)Math.min(1.0, dt * 6.0);
         return p;
      } else {
         p.x = tx;
         p.y = py;
         p.z = tz;
         p.yaw = bodyYaw;
         p.init = true;
         return p;
      }
   }

   private static float wrap(float a) {
      a %= 360.0F;
      if (a >= 180.0F) {
         a -= 360.0F;
      }

      if (a < -180.0F) {
         a += 360.0F;
      }

      return a;
   }

   public static final class Pet {
      public double x;
      public double y;
      public double z;
      public float yaw;
      public double phase;
      public double speed;
      long lastNanos;
      boolean init;
   }
}
