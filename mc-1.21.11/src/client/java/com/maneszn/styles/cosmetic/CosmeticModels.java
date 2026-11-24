package com.maneszn.styles.cosmetic;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.render.CosmeticScenes;
import com.maneszn.styles.render.MiniPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class CosmeticModels {
   private static final Map<String, Optional<CosmeticModels.Baked>> CACHE = new HashMap<>();
   private static final Map<String, CosmeticModels.Mini> MINI = new HashMap<>();
   private static final Map<String, CosmeticModels.Mini> SKIN_COPIES = new HashMap<>();
   private static final Map<Identifier, RenderType> SKIN_TYPES = new HashMap<>();

   private CosmeticModels() {
   }

   public static CosmeticModels.Baked get(Cosmetic c) {
      return c != null && !c.parts().isEmpty() && c.texture() != null ? CACHE.computeIfAbsent(c.key(), k -> {
         try {
            return Optional.of(bake(c));
         } catch (Exception var3) {
            ManesznStyles.LOGGER.warn("Cosmetic {} konnte nicht gebaut werden: {}", k, var3.toString());
            return Optional.empty();
         }
      }).orElse(null) : null;
   }

   private static CosmeticModels.Baked bake(Cosmetic c) {
      int n = 5;
      MeshDefinition[] meshes = new MeshDefinition[5];

      for (int k = 0; k < 5; k++) {
         meshes[k] = new MeshDefinition();
      }

      boolean[] used = new boolean[5];
      List<List<String>> names = new ArrayList<>();
      List<List<Cosmetic.Part>> defs = new ArrayList<>();

      for (int k = 0; k < 5; k++) {
         names.add(new ArrayList<>());
         defs.add(new ArrayList<>());
      }

      int i = 0;

      for (Cosmetic.Part part : c.parts()) {
         String anim = part.attach();

         int r = switch (anim) {
            case "body" -> 1;
            case "buddy" -> 2;
            case "leg_r" -> 3;
            case "leg_l" -> 4;
            default -> 0;
         };
         CubeListBuilder b = CubeListBuilder.create();

         for (Cosmetic.Cube cube : part.cubes()) {
            b.texOffs(cube.uv()[0], cube.uv()[1])
               .addBox(cube.from()[0], cube.from()[1], cube.from()[2], cube.size()[0], cube.size()[1], cube.size()[2], new CubeDeformation(cube.inflate()));
         }

         String name = "p" + i++;
         PartDefinition root = meshes[r].getRoot();
         root.addOrReplaceChild(
            name, b, PartPose.offsetAndRotation(part.pivot()[0], part.pivot()[1], part.pivot()[2], part.rot()[0], part.rot()[1], part.rot()[2])
         );
         used[r] = true;
         names.get(r).add(name);
         defs.get(r).add(part);
      }

      int tw = c.texSize()[0];
      int th = c.texSize()[1];
      ModelPart[] baked = new ModelPart[5];
      List<CosmeticModels.Animated> anim = new ArrayList<>();

      for (int r = 0; r < 5; r++) {
         baked[r] = LayerDefinition.create(meshes[r], tw, th).bakeRoot();

         for (int k = 0; k < names.get(r).size(); k++) {
            Cosmetic.Part d = defs.get(r).get(k);
            if (!"none".equals(d.anim())) {
               anim.add(new CosmeticModels.Animated(baked[r].getChild(names.get(r).get(k)), d));
            }
         }
      }

      Identifier tex = CosmeticManager.texture(c.texture() + ".png");
      RenderType type = c.translucent() ? RenderTypes.entityTranslucent(tex) : RenderTypes.entityCutoutNoCull(tex);
      return new CosmeticModels.Baked(baked, used, type, c.glow(), List.copyOf(anim));
   }

   public static CosmeticModels.Mini mini(boolean slim) {
      return mini(slim, CosmeticScenes.BuddySpot.LEFT_SHOULDER, false);
   }

   public static CosmeticModels.Mini mini(boolean slim, CosmeticScenes.BuddySpot spot, boolean walking) {
      return MINI.computeIfAbsent(slim + ":" + spot + ":" + walking, key -> {
         MeshDefinition mesh = new MeshDefinition();
         Map<String, CubeListBuilder> builders = new LinkedHashMap<>();
         Map<String, float[]> pivots = new HashMap<>();

         for (MiniPlayer.Box b : MiniPlayer.boxes(slim)) {
            CubeListBuilder cb = builders.computeIfAbsent(b.part(), kx -> CubeListBuilder.create());
            pivots.put(b.part(), new float[]{b.px(), b.py(), b.pz()});
            cb.texOffs(b.u(), b.v()).addBox(b.x() - b.px(), b.y() - b.py(), b.z() - b.pz(), b.w(), b.h(), b.d(), new CubeDeformation(b.inflate()));
         }

         for (Entry<String, CubeListBuilder> e : builders.entrySet()) {
            float[] p = pivots.get(e.getKey());
            mesh.getRoot().addOrReplaceChild(e.getKey(), e.getValue(), PartPose.offsetAndRotation(p[0], p[1], p[2], 0.0F, 0.0F, 0.0F));
         }

         ModelPart root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
         Map<String, ModelPart> parts = new LinkedHashMap<>();

         for (String k : builders.keySet()) {
            parts.put(k, root.getChild(k));
         }

         return new CosmeticModels.Mini(root, parts);
      });
   }

   public static CosmeticModels.Mini skinCopy(int entityId, boolean slim) {
      if (SKIN_COPIES.size() > 64) {
         SKIN_COPIES.clear();
      }

      return SKIN_COPIES.computeIfAbsent(entityId + ":" + slim, key -> {
         MeshDefinition mesh = new MeshDefinition();
         Map<String, CubeListBuilder> builders = new LinkedHashMap<>();

         for (MiniPlayer.Box b : MiniPlayer.boxes(slim)) {
            CubeListBuilder cb = builders.computeIfAbsent(b.part(), kx -> CubeListBuilder.create());
            cb.texOffs(b.u(), b.v()).addBox(b.x() - b.px(), b.y() - b.py(), b.z() - b.pz(), b.w(), b.h(), b.d(), new CubeDeformation(b.inflate()));
         }

         for (Entry<String, CubeListBuilder> e : builders.entrySet()) {
            mesh.getRoot().addOrReplaceChild(e.getKey(), e.getValue(), PartPose.ZERO);
         }

         ModelPart root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
         Map<String, ModelPart> parts = new LinkedHashMap<>();

         for (String k : builders.keySet()) {
            parts.put(k, root.getChild(k));
         }

         return new CosmeticModels.Mini(root, parts);
      });
   }

   public static RenderType skinType(Identifier skin) {
      return SKIN_TYPES.computeIfAbsent(skin, RenderTypes::entityTranslucent);
   }

   public static void clear() {
      CACHE.clear();
   }

   private record Animated(ModelPart part, Cosmetic.Part def) {
   }

   public static final class Baked {
      public final ModelPart head;
      public final ModelPart body;
      public final ModelPart buddy;
      public final ModelPart legR;
      public final ModelPart legL;
      public final boolean hasHead;
      public final boolean hasBody;
      public final boolean hasBuddy;
      public final boolean hasLegR;
      public final boolean hasLegL;
      public final RenderType renderType;
      public final boolean glow;
      private final List<CosmeticModels.Animated> animated;

      Baked(ModelPart[] roots, boolean[] used, RenderType type, boolean glow, List<CosmeticModels.Animated> animated) {
         this.head = roots[0];
         this.body = roots[1];
         this.buddy = roots[2];
         this.legR = roots[3];
         this.legL = roots[4];
         this.hasHead = used[0];
         this.hasBody = used[1];
         this.hasBuddy = used[2];
         this.hasLegR = used[3];
         this.hasLegL = used[4];
         this.renderType = type;
         this.glow = glow;
         this.animated = animated;
      }

      public void animate(double ticks) {
         for (CosmeticModels.Animated a : this.animated) {
            float[] r = CosmeticScenes.animated(a.def(), ticks);
            float[] p = CosmeticScenes.animatedPivot(a.def(), ticks);
            a.part().xRot = r[0];
            a.part().yRot = r[1];
            a.part().zRot = r[2];
            a.part().x = p[0];
            a.part().y = p[1];
            a.part().z = p[2];
         }
      }
   }

   public record Mini(ModelPart root, Map<String, ModelPart> parts) {
      public void animate(double ticks) {
         this.animate(ticks, false, false);
      }

      public void animate(double ticks, boolean holdsHand, boolean walking) {
         this.animate(ticks, holdsHand ? MiniPlayer.Pose.HOLD : MiniPlayer.Pose.STAND, walking);
      }

      public void animate(double ticks, MiniPlayer.Pose pose, boolean walking) {
         for (Entry<String, ModelPart> e : this.parts.entrySet()) {
            float[] r = MiniPlayer.rotation(e.getKey(), ticks, pose, walking);
            e.getValue().xRot = r[0];
            e.getValue().yRot = r[1];
            e.getValue().zRot = r[2];
         }
      }
   }
}
