package com.maneszn.styles.render;

import com.maneszn.styles.config.ModConfig;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class CustomSky {
   public static final Identifier TEXTURE = Identifier.withDefaultNamespace("optifine/sky/world0/maneszn_sky.png");
   private static final float R = 100.0F;
   private static GpuBuffer buffer;
   private static String checkedStyle;
   private static boolean available;
   private static int recheck;

   private CustomSky() {
   }

   public static boolean active() {
      String id = ModConfig.get().active.get("sky");
      if (id != null && !id.isEmpty()) {
         if (!id.equals(checkedStyle) || recheck-- <= 0) {
            checkedStyle = id;
            recheck = 200;

            try {
               available = Minecraft.getInstance().getResourceManager().getResource(TEXTURE).isPresent();
            } catch (Throwable var2) {
               available = false;
            }
         }

         return available;
      } else {
         return false;
      }
   }

   public static void render(int skyColor) {
      if (buffer == null) {
         buffer = build();
      }

      AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(TEXTURE);
      float r = (skyColor >> 16 & 0xFF) / 255.0F;
      float g = (skyColor >> 8 & 0xFF) / 255.0F;
      float b = (skyColor & 0xFF) / 255.0F;
      float lum = 0.299F * r + 0.587F * g + 0.114F * b;
      float k = Math.max(0.12F, Math.min(1.0F, lum / 0.6F));
      AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(Mode.QUADS);
      GpuBuffer indices = quads.getBuffer(72);
      RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
      GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
         .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(k, k, k, 1.0F), new Vector3f(), new Matrix4f());

      try (RenderPass pass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Maneszn sky", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
         pass.setPipeline(RenderPipelines.END_SKY);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("DynamicTransforms", transform);
         pass.bindTexture("Sampler0", tex.getTextureView(), tex.getSampler());
         pass.setVertexBuffer(0, buffer);
         pass.setIndexBuffer(indices, quads.type());
         pass.drawIndexed(0, 0, 72, 1);
      }
   }

   private static GpuBuffer build() {
      GpuBuffer var19;
      try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(48 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
         BufferBuilder bb = new BufferBuilder(bytes, Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         String[] faces = new String[]{"down", "up", "south", "west", "north", "east"};
         int[][] cells = new int[][]{{0, 0}, {1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}};
         float[][] uv = new float[][]{{0.0F, 0.0F}, {0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}};
         float e = 9.765625E-4F;

         for (int f = 0; f < 6; f++) {
            float[][] pos = new float[4][];
            float[][] tc = new float[4][];

            for (int c = 0; c < 4; c++) {
               float u = uv[c][0];
               float v = uv[c][1];
               pos[c] = dir(faces[f], u, v);
               float uu = e + u * (1.0F - 2.0F * e);
               float vv = e + v * (1.0F - 2.0F * e);
               tc[c] = new float[]{(cells[f][0] + uu) / 3.0F, (cells[f][1] + vv) / 2.0F};
            }

            for (int c = 0; c < 4; c++) {
               vert(bb, pos[c], tc[c]);
            }

            for (int c = 3; c >= 0; c--) {
               vert(bb, pos[c], tc[c]);
            }
         }

         try (MeshData mesh = bb.buildOrThrow()) {
            var19 = RenderSystem.getDevice().createBuffer(() -> "Maneszn sky", 40, mesh.vertexBuffer());
         }
      }

      return var19;
   }

   private static void vert(BufferBuilder bb, float[] p, float[] t) {
      bb.addVertex(p[0], p[1], p[2]).setUv(t[0], t[1]).setColor(-1);
   }

   private static float[] dir(String face, float u, float v) {
      float a = 2.0F * u - 1.0F;
      float b = 1.0F - 2.0F * v;
      float x;
      float y;
      float z;
      switch (face) {
         case "north":
            x = a;
            y = b;
            z = -1.0F;
            break;
         case "east":
            x = 1.0F;
            y = b;
            z = a;
            break;
         case "south":
            x = -a;
            y = b;
            z = 1.0F;
            break;
         case "west":
            x = -1.0F;
            y = b;
            z = -a;
            break;
         case "up":
            x = a;
            y = 1.0F;
            z = b;
            break;
         default:
            x = a;
            y = -1.0F;
            z = -b;
      }

      return new float[]{x * 100.0F, y * 100.0F, z * 100.0F};
   }
}
