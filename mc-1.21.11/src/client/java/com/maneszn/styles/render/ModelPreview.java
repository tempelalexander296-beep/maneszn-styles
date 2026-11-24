package com.maneszn.styles.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maneszn.styles.image.ImageProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

public final class ModelPreview {
   private static final double[] BLOCK_GUI_ROT = new double[]{30.0, 225.0, 0.0};
   private static final double BLOCK_GUI_SCALE = 0.625;
   private static final double[] DEFAULT_3D_ROT = new double[]{20.0, -35.0, 0.0};

   private ModelPreview() {
   }

   public static Optional<ModelPreview.Scene> item(ModelPreview.Lookup l, String itemId) {
      String model = null;
      JsonObject def = l.json("minecraft", "items/" + itemId + ".json");
      if (def != null && def.has("model")) {
         model = pickModel(def.get("model"), 0);
      }

      if (model == null) {
         model = "minecraft:item/" + itemId;
      }

      return model(l, model);
   }

   static String pickModel(JsonElement e, int depth) {
      if (depth > 12 || e == null) {
         return null;
      } else if (e.isJsonPrimitive()) {
         return e.getAsString();
      } else if (!e.isJsonObject()) {
         return null;
      } else {
         JsonObject o = e.getAsJsonObject();
         String type = o.has("type") ? o.get("type").getAsString().replace("minecraft:", "") : "model";
         switch (type) {
            case "model":
               return o.has("model") ? o.get("model").getAsString() : null;
            case "special":
               return o.has("base") ? o.get("base").getAsString() : null;
            case "condition":
               return pickModel(o.has("on_false") ? o.get("on_false") : o.get("on_true"), depth + 1);
            case "composite":
               if (o.has("models") && o.getAsJsonArray("models").size() > 0) {
                  return pickModel(o.getAsJsonArray("models").get(0), depth + 1);
               }

               return null;
            default:
               if (o.has("fallback")) {
                  return pickModel(o.get("fallback"), depth + 1);
               } else {
                  for (String k : new String[]{"cases", "entries"}) {
                     if (o.has(k) && o.getAsJsonArray(k).size() > 0) {
                        JsonObject first = o.getAsJsonArray(k).get(0).getAsJsonObject();
                        return pickModel(first.get("model"), depth + 1);
                     }
                  }

                  return null;
               }
         }
      }
   }

   public static Optional<ModelPreview.Scene> model(ModelPreview.Lookup l, String modelRef) {
      Map<String, String> textures = new HashMap<>();
      JsonArray elements = null;
      JsonObject gui = null;
      boolean blockLike = false;
      String ref = modelRef;

      for (int depth = 0; depth < 24 && ref != null; depth++) {
         String[] id = split(ref);
         String path = id[1];
         if (path.equals("item/generated") || path.equals("builtin/generated") || path.equals("builtin/entity")) {
            break;
         }

         if (path.equals("block/block") || path.startsWith("block/cube")) {
            blockLike = true;
         }

         JsonObject m = l.json(id[0], "models/" + path + ".json");
         if (m == null) {
            break;
         }

         if (m.has("textures") && m.get("textures").isJsonObject()) {
            for (Entry<String, JsonElement> t : m.getAsJsonObject("textures").entrySet()) {
               if (t.getValue().isJsonPrimitive()) {
                  textures.putIfAbsent(t.getKey(), t.getValue().getAsString());
               }
            }
         }

         if (elements == null && m.has("elements") && m.get("elements").isJsonArray()) {
            elements = m.getAsJsonArray("elements");
         }

         if (gui == null && m.has("display") && m.getAsJsonObject("display").has("gui")) {
            gui = m.getAsJsonObject("display").getAsJsonObject("gui");
         }

         ref = m.has("parent") ? m.get("parent").getAsString() : null;
      }

      if (elements != null && !elements.isEmpty()) {
         ModelPreview.Scene s = new ModelPreview.Scene();
         Map<String, ImageProcessor.Img> texCache = new HashMap<>();

         for (JsonElement ee : elements) {
            if (ee.isJsonObject()) {
               JsonObject el = ee.getAsJsonObject();
               double[] from = vec(el.getAsJsonArray("from"));
               double[] to = vec(el.getAsJsonArray("to"));
               boolean shade = !el.has("shade") || el.get("shade").getAsBoolean();
               if (el.has("faces")) {
                  for (Entry<String, JsonElement> fe : el.getAsJsonObject("faces").entrySet()) {
                     JsonObject f = fe.getValue().getAsJsonObject();
                     String texRef = f.has("texture") ? f.get("texture").getAsString() : null;
                     ImageProcessor.Img tex = texCache.computeIfAbsent(String.valueOf(texRef), k -> resolveTexture(l, textures, k));
                     double[] uv = f.has("uv") ? vec(f.getAsJsonArray("uv")) : null;
                     int rot = f.has("rotation") ? f.get("rotation").getAsInt() : 0;
                     ModelPreview.Face face = boxFace(fe.getKey(), from, to, uv, rot, tex, shade);
                     if (face != null) {
                        applyElementRotation(face, el);
                        s.faces.add(face);
                     }
                  }
               }
            }
         }

         double[] rot = gui != null && gui.has("rotation") ? vec(gui.getAsJsonArray("rotation")) : (blockLike ? BLOCK_GUI_ROT : DEFAULT_3D_ROT);
         double[] tr = gui != null && gui.has("translation") ? vec(gui.getAsJsonArray("translation")) : new double[3];
         double[] sc = gui != null && gui.has("scale")
            ? vec(gui.getAsJsonArray("scale"))
            : (blockLike ? new double[]{0.625, 0.625, 0.625} : new double[]{1.0, 1.0, 1.0});
         applyDisplay(s, rot, tr, sc);
         return Optional.of(s);
      } else {
         return Optional.empty();
      }
   }

   private static ImageProcessor.Img resolveTexture(ModelPreview.Lookup l, Map<String, String> textures, String ref) {
      String r = ref;

      for (int i = 0; i < 12 && r != null && r.startsWith("#"); i++) {
         r = textures.get(r.substring(1));
      }

      if (r != null && !r.startsWith("#") && !r.equals("null")) {
         String[] id = split(r);
         return firstFrame(l.texture(id[0], "textures/" + id[1] + ".png"));
      } else {
         return null;
      }
   }

   public static ModelPreview.Scene cube(
      ImageProcessor.Img up, ImageProcessor.Img down, ImageProcessor.Img north, ImageProcessor.Img south, ImageProcessor.Img east, ImageProcessor.Img west
   ) {
      ModelPreview.Scene s = new ModelPreview.Scene();
      double[] f = new double[]{0.0, 0.0, 0.0};
      double[] t = new double[]{16.0, 16.0, 16.0};
      String[] names = new String[]{"up", "down", "north", "south", "east", "west"};
      ImageProcessor.Img[] tex = new ImageProcessor.Img[]{up, down, north, south, east, west};

      for (int i = 0; i < 6; i++) {
         ModelPreview.Face face = boxFace(names[i], f, t, null, 0, firstFrame(tex[i]), true);
         if (face != null) {
            s.faces.add(face);
         }
      }

      applyDisplay(s, BLOCK_GUI_ROT, new double[3], new double[]{0.625, 0.625, 0.625});
      return s;
   }

   public static ModelPreview.Scene endCrystal(ImageProcessor.Img tex) {
      ModelPreview.Scene s = new ModelPreview.Scene();
      s.noCull = true;
      if (tex == null) {
         return s;
      } else {
         double ku = tex.w() / 64.0;
         double kv = tex.h() / 32.0;
         addEntityBox(s, tex, ku, kv, 0, 16, -6.0, -13.0, -6.0, 12, 4, 12, 1.0, 0.0, 0.0);
         addEntityBox(s, tex, ku, kv, 0, 0, -4.0, -4.0, -4.0, 8, 8, 8, 1.0, 0.0, 60.0);
         addEntityBox(s, tex, ku, kv, 0, 0, -4.0, -4.0, -4.0, 8, 8, 8, 0.875, 40.0, 60.0);
         addEntityBox(s, tex, ku, kv, 32, 0, -4.0, -4.0, -4.0, 8, 8, 8, 0.766, 80.0, 60.0);
         applyDisplay(s, new double[]{20.0, -30.0, 0.0}, new double[]{0.0, 0.0, 0.0}, new double[]{0.9, 0.9, 0.9});
         return s;
      }
   }

   public static ModelPreview.Scene armor(ImageProcessor.Img outer, ImageProcessor.Img inner) {
      ModelPreview.Scene s = new ModelPreview.Scene();
      s.noCull = true;
      ImageProcessor.Img body = new ImageProcessor.Img(1, 1, new int[]{-14015424});
      double[][] parts = new double[][]{
         {-4.0, 24.0, -4.0, 8.0, 8.0, 8.0},
         {-4.0, 12.0, -2.0, 8.0, 12.0, 4.0},
         {-8.0, 12.0, -2.0, 4.0, 12.0, 4.0},
         {4.0, 12.0, -2.0, 4.0, 12.0, 4.0},
         {-4.0, 0.0, -2.0, 4.0, 12.0, 4.0},
         {0.0, 0.0, -2.0, 4.0, 12.0, 4.0}
      };

      for (double[] p : parts) {
         addColorBox(s, body, p, 0.0);
      }

      if (inner != null) {
         double ku = inner.w() / 64.0;
         double kv = inner.h() / 32.0;
         addArmorBox(s, inner, ku, kv, 16, 16, parts[1], 0.5);
         addArmorBox(s, inner, ku, kv, 0, 16, parts[4], 0.5);
         addArmorBox(s, inner, ku, kv, 0, 16, parts[5], 0.5);
      }

      if (outer != null) {
         double ku = outer.w() / 64.0;
         double kv = outer.h() / 32.0;
         addArmorBox(s, outer, ku, kv, 0, 0, parts[0], 1.0);
         addArmorBox(s, outer, ku, kv, 16, 16, parts[1], 1.0);
         addArmorBox(s, outer, ku, kv, 40, 16, parts[2], 1.0);
         addArmorBox(s, outer, ku, kv, 40, 16, parts[3], 1.0);
         addArmorBox(s, outer, ku, kv, 0, 16, parts[4], 1.0);
         addArmorBox(s, outer, ku, kv, 0, 16, parts[5], 1.0);
      }

      for (ModelPreview.Face f : s.faces) {
         for (double[] p : f.pos()) {
            p[0] += 8.0;
            p[1] += -8.0;
            p[2] += 8.0;
         }
      }

      applyDisplay(s, new double[]{8.0, 200.0, 0.0}, new double[3], new double[]{1.0, 1.0, 1.0});
      return s;
   }

   private static void addColorBox(ModelPreview.Scene s, ImageProcessor.Img color, double[] p, double grow) {
      double[] from = new double[]{p[0] - grow, p[1] - grow, p[2] - grow};
      double[] to = new double[]{p[0] + p[3] + grow, p[1] + p[4] + grow, p[2] + p[5] + grow};

      for (String dir : new String[]{"up", "down", "north", "south", "east", "west"}) {
         ModelPreview.Face f = boxFaceRaw(dir, from, to);
         s.faces.add(new ModelPreview.Face(f.pos(), new double[4][2], color, true));
      }
   }

   private static void addArmorBox(ModelPreview.Scene s, ImageProcessor.Img tex, double ku, double kv, int u, int v, double[] p, double grow) {
      int w = (int)p[3];
      int h = (int)p[4];
      int d = (int)p[5];
      double[] from = new double[]{p[0] - grow, p[1] - grow, p[2] - grow};
      double[] to = new double[]{p[0] + w + grow, p[1] + h + grow, p[2] + d + grow};
      double[][] uvs = new double[][]{
         {u + d, v, u + d + w, v + d},
         {u + d + w, v + d, u + d + 2 * w, v},
         {u + d, v + d, u + d + w, v + d + h},
         {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h},
         {u, v + d, u + d, v + d + h},
         {u + d + w, v + d, u + 2 * d + w, v + d + h}
      };
      String[] names = new String[]{"up", "down", "north", "south", "east", "west"};

      for (int i = 0; i < 6; i++) {
         ModelPreview.Face f = boxFaceRaw(names[i], from, to);
         double[] uv = uvs[i];
         setUv(f, new double[]{uv[0] * ku, uv[1] * kv, uv[2] * ku, uv[3] * kv}, 0);
         s.faces.add(new ModelPreview.Face(f.pos(), f.uv(), tex, true));
      }
   }

   private static void addEntityBox(
      ModelPreview.Scene s,
      ImageProcessor.Img tex,
      double ku,
      double kv,
      int u,
      int v,
      double x,
      double y,
      double z,
      int w,
      int h,
      int d,
      double scale,
      double yaw,
      double pitch
   ) {
      double[] from = new double[]{x, y, z};
      double[] to = new double[]{x + w, y + h, z + d};
      double[][] uvs = new double[][]{
         {u + d, v, u + d + w, v + d},
         {u + d + w, v + d, u + d + 2 * w, v},
         {u + d, v + d, u + d + w, v + d + h},
         {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h},
         {u + d + w, v + d, u + 2 * d + w, v + d + h},
         {u, v + d, u + d, v + d + h}
      };
      String[] names = new String[]{"up", "down", "north", "south", "east", "west"};

      for (int i = 0; i < 6; i++) {
         ModelPreview.Face f = boxFaceRaw(names[i], from, to);
         double[] uv = uvs[i];
         setUv(f, new double[]{uv[0] * ku, uv[1] * kv, uv[2] * ku, uv[3] * kv}, 0);
         ModelPreview.Face face = new ModelPreview.Face(f.pos(), f.uv(), tex, true);

         for (double[] p : face.pos()) {
            for (int a = 0; a < 3; a++) {
               p[a] *= scale;
            }

            if (pitch != 0.0) {
               rotateAround(p, new double[]{0.7071, 0.0, 0.7071}, Math.toRadians(pitch));
            }

            rotateAxis(p, 1, Math.toRadians(yaw));
         }

         s.faces.add(face);
      }

      for (int i = s.faces.size() - 6; i < s.faces.size(); i++) {
         for (double[] p : s.faces.get(i).pos()) {
            p[0] += 8.0;
            p[1] += 8.0;
            p[2] += 8.0;
         }
      }
   }

   private static ModelPreview.Face boxFaceRaw(String dir, double[] f, double[] t) {
      double[][] p = switch (dir) {
         case "north" -> new double[][]{{t[0], t[1], f[2]}, {f[0], t[1], f[2]}, {f[0], f[1], f[2]}, {t[0], f[1], f[2]}};
         case "south" -> new double[][]{{f[0], t[1], t[2]}, {t[0], t[1], t[2]}, {t[0], f[1], t[2]}, {f[0], f[1], t[2]}};
         case "west" -> new double[][]{{f[0], t[1], f[2]}, {f[0], t[1], t[2]}, {f[0], f[1], t[2]}, {f[0], f[1], f[2]}};
         case "east" -> new double[][]{{t[0], t[1], t[2]}, {t[0], t[1], f[2]}, {t[0], f[1], f[2]}, {t[0], f[1], t[2]}};
         case "up" -> new double[][]{{f[0], t[1], f[2]}, {t[0], t[1], f[2]}, {t[0], t[1], t[2]}, {f[0], t[1], t[2]}};
         case "down" -> new double[][]{{f[0], f[1], t[2]}, {t[0], f[1], t[2]}, {t[0], f[1], f[2]}, {f[0], f[1], f[2]}};
         default -> null;
      };
      return p == null ? null : new ModelPreview.Face(p, new double[4][2], null, true);
   }

   private static ModelPreview.Face boxFace(String dir, double[] f, double[] t, double[] uv, int rotation, ImageProcessor.Img tex, boolean shade) {
      ModelPreview.Face raw = boxFaceRaw(dir, f, t);
      if (raw == null) {
         return null;
      } else {
         if (uv == null) {
            uv = switch (dir) {
               case "north" -> new double[]{16.0 - t[0], 16.0 - t[1], 16.0 - f[0], 16.0 - f[1]};
               case "south" -> new double[]{f[0], 16.0 - t[1], t[0], 16.0 - f[1]};
               case "west" -> new double[]{f[2], 16.0 - t[1], t[2], 16.0 - f[1]};
               case "east" -> new double[]{16.0 - t[2], 16.0 - t[1], 16.0 - f[2], 16.0 - f[1]};
               case "up" -> new double[]{f[0], f[2], t[0], t[2]};
               default -> new double[]{f[0], 16.0 - t[2], t[0], 16.0 - f[2]};
            };
         }

         double sx = tex == null ? 1.0 : tex.w() / 16.0;
         double sy = tex == null ? 1.0 : tex.h() / 16.0;
         ModelPreview.Face face = new ModelPreview.Face(raw.pos(), raw.uv(), tex, shade);
         setUv(face, new double[]{uv[0] * sx, uv[1] * sy, uv[2] * sx, uv[3] * sy}, rotation);
         return face;
      }
   }

   private static void setUv(ModelPreview.Face f, double[] uv, int rotation) {
      double[][] c = new double[][]{{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}};
      int k = Math.floorMod(rotation / 90, 4);

      for (int i = 0; i < 4; i++) {
         double[] src = c[Math.floorMod(i - k, 4)];
         f.uv()[i][0] = src[0];
         f.uv()[i][1] = src[1];
      }
   }

   private static void applyElementRotation(ModelPreview.Face face, JsonObject el) {
      if (el.has("rotation") && el.get("rotation").isJsonObject()) {
         JsonObject r = el.getAsJsonObject("rotation");
         double[] origin = r.has("origin") ? vec(r.getAsJsonArray("origin")) : new double[]{8.0, 8.0, 8.0};
         List<double[]> rots = new ArrayList<>();
         if (r.has("axis") && r.has("angle")) {
            String ax = r.get("axis").getAsString();
            rots.add(new double[]{ax.equals("x") ? 0.0 : (ax.equals("y") ? 1.0 : 2.0), r.get("angle").getAsDouble()});
         } else {
            for (int a = 0; a < 3; a++) {
               String key = new String[]{"x", "y", "z"}[a];
               if (r.has(key) && r.get(key).isJsonPrimitive()) {
                  rots.add(new double[]{a, r.get(key).getAsDouble()});
               }
            }
         }

         boolean rescale = r.has("rescale") && r.get("rescale").getAsBoolean();

         for (double[] p : face.pos()) {
            for (int ax = 0; ax < 3; ax++) {
               p[ax] -= origin[ax];
            }

            for (double[] ra : rots) {
               int axis = (int)ra[0];
               double ang = Math.toRadians(ra[1]);
               if (rescale && Math.abs(Math.cos(ang)) > 1.0E-6) {
                  double sc = 1.0 / Math.cos(ang);

                  for (int ax = 0; ax < 3; ax++) {
                     if (ax != axis) {
                        p[ax] *= sc;
                     }
                  }
               }

               rotateAxis(p, axis, ang);
            }

            for (int axx = 0; axx < 3; axx++) {
               p[axx] += origin[axx];
            }
         }
      }
   }

   private static void applyDisplay(ModelPreview.Scene s, double[] rot, double[] tr, double[] sc) {
      for (ModelPreview.Face f : s.faces) {
         for (double[] p : f.pos()) {
            p[0] = (p[0] - 8.0) * sc[0];
            p[1] = (p[1] - 8.0) * sc[1];
            p[2] = (p[2] - 8.0) * sc[2];
            rotateAxis(p, 2, Math.toRadians(rot[2]));
            rotateAxis(p, 1, Math.toRadians(rot[1]));
            rotateAxis(p, 0, Math.toRadians(rot[0]));
            p[0] += tr[0];
            p[1] += tr[1];
            p[2] += tr[2];
         }
      }
   }

   static void rotateAround(double[] p, double[] k, double a) {
      double c = Math.cos(a);
      double sn = Math.sin(a);
      double dot = k[0] * p[0] + k[1] * p[1] + k[2] * p[2];
      double[] cr = new double[]{k[1] * p[2] - k[2] * p[1], k[2] * p[0] - k[0] * p[2], k[0] * p[1] - k[1] * p[0]};

      for (int i = 0; i < 3; i++) {
         p[i] = p[i] * c + cr[i] * sn + k[i] * dot * (1.0 - c);
      }
   }

   static void rotateAxis(double[] p, int axis, double a) {
      if (a != 0.0) {
         double c = Math.cos(a);
         double s = Math.sin(a);
         double x = p[0];
         double y = p[1];
         double z = p[2];
         switch (axis) {
            case 0:
               p[1] = y * c - z * s;
               p[2] = y * s + z * c;
               break;
            case 1:
               p[0] = x * c + z * s;
               p[2] = -x * s + z * c;
               break;
            default:
               p[0] = x * c - y * s;
               p[1] = x * s + y * c;
         }
      }
   }

   public static ImageProcessor.Img render(ModelPreview.Scene s, int size, double yawDeg, boolean fitSphere) {
      List<double[][]> pos = new ArrayList<>();
      double yaw = Math.toRadians(yawDeg);
      double minX = 1.0E9;
      double maxX = -1.0E9;
      double minY = 1.0E9;
      double maxY = -1.0E9;
      double radius = 0.0;

      for (ModelPreview.Face f : s.faces) {
         double[][] q = new double[4][];

         for (int i = 0; i < 4; i++) {
            double[] p = (double[])f.pos()[i].clone();
            rotateAxis(p, 1, yaw);
            q[i] = p;
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            radius = Math.max(radius, Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]));
         }

         pos.add(q);
      }

      int[] out = new int[size * size];
      if (s.faces.isEmpty()) {
         return new ImageProcessor.Img(size, size, out);
      } else {
         double cy;
         double scale;
         double cx;
         if (fitSphere) {
            scale = size * 0.46 / Math.max(0.001, radius);
            cx = 0.0;
            cy = 0.0;
         } else {
            scale = size * 0.92 / Math.max(0.001, Math.max(maxX - minX, maxY - minY));
            cx = (minX + maxX) / 2.0;
            cy = (minY + maxY) / 2.0;
         }

         double[] zbuf = new double[size * size];
         Arrays.fill(zbuf, -1.0E18);
         Integer[] order = new Integer[s.faces.size()];
         double[] depth = new double[order.length];

         for (int i = 0; i < order.length; i++) {
            order[i] = i;
            double[][] q = pos.get(i);
            depth[i] = (q[0][2] + q[1][2] + q[2][2] + q[3][2]) / 4.0;
         }

         Arrays.sort(order, (a, b) -> Double.compare(depth[a], depth[b]));
         Integer[] var46 = order;
         int var47 = order.length;

         for (int var30 = 0; var30 < var47; var30++) {
            int idx = var46[var30];
            ModelPreview.Face f = s.faces.get(idx);
            double[][] q = pos.get(idx);
            double[] e1 = sub(q[3], q[0]);
            double[] e2 = sub(q[1], q[0]);
            double[] n = cross(e1, e2);
            double len = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (!(len < 1.0E-9)) {
               for (int a = 0; a < 3; a++) {
                  n[a] /= len;
               }

               if (n[2] <= 1.0E-6) {
                  if (!s.noCull) {
                     continue;
                  }

                  for (int a = 0; a < 3; a++) {
                     n[a] = -n[a];
                  }
               }

               double light = f.shade() ? Math.min(1.0, Math.max(0.42, 0.52 + 0.48 * Math.max(0.0, n[1]) + 0.22 * n[2] + 0.1 * n[0])) : 1.0;
               double[][] sp = new double[4][3];

               for (int i = 0; i < 4; i++) {
                  sp[i][0] = size / 2.0 + (q[i][0] - cx) * scale;
                  sp[i][1] = size / 2.0 - (q[i][1] - cy) * scale;
                  sp[i][2] = q[i][2];
               }

               tri(out, zbuf, size, sp[0], sp[1], sp[2], f.uv()[0], f.uv()[1], f.uv()[2], f.tex(), light);
               tri(out, zbuf, size, sp[0], sp[2], sp[3], f.uv()[0], f.uv()[2], f.uv()[3], f.tex(), light);
            }
         }

         return new ImageProcessor.Img(size, size, out);
      }
   }

   private static void tri(
      int[] out, double[] zbuf, int size, double[] a, double[] b, double[] c, double[] ua, double[] ub, double[] uc, ImageProcessor.Img tex, double light
   ) {
      int x0 = (int)Math.max(0.0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
      int x1 = (int)Math.min((double)(size - 1), Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
      int y0 = (int)Math.max(0.0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
      int y1 = (int)Math.min((double)(size - 1), Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
      double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
      if (!(Math.abs(area) < 1.0E-9)) {
         for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
               double px = x + 0.5;
               double py = y + 0.5;
               double w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
               double w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
               double w2 = 1.0 - w0 - w1;
               if (!(w0 < -1.0E-6) && !(w1 < -1.0E-6) && !(w2 < -1.0E-6)) {
                  double z = w0 * a[2] + w1 * b[2] + w2 * c[2];
                  int i = y * size + x;
                  if (!(z < zbuf[i] - 1.0E-6)) {
                     int col;
                     if (tex == null) {
                        col = -7700576;
                     } else {
                        double u = w0 * ua[0] + w1 * ub[0] + w2 * uc[0];
                        double v = w0 * ua[1] + w1 * ub[1] + w2 * uc[1];
                        int tx = Math.floorMod((int)Math.floor(u), tex.w());
                        int ty = Math.floorMod((int)Math.floor(v), tex.h());
                        col = tex.px()[ty * tex.w() + tx];
                     }

                     int alpha = col >>> 24;
                     if (alpha != 0) {
                        int r = (int)((col >> 16 & 0xFF) * light);
                        int g = (int)((col >> 8 & 0xFF) * light);
                        int bl = (int)((col & 0xFF) * light);
                        if (alpha >= 250) {
                           out[i] = 0xFF000000 | r << 16 | g << 8 | bl;
                           zbuf[i] = z;
                        } else {
                           int dst = out[i];
                           double fa = alpha / 255.0;
                           double da = (dst >>> 24) / 255.0;
                           double oa = fa + da * (1.0 - fa);
                           int or = (int)((r * fa + (dst >> 16 & 0xFF) * da * (1.0 - fa)) / oa);
                           int og = (int)((g * fa + (dst >> 8 & 0xFF) * da * (1.0 - fa)) / oa);
                           int ob = (int)((bl * fa + (dst & 0xFF) * da * (1.0 - fa)) / oa);
                           out[i] = (int)(oa * 255.0) << 24 | or << 16 | og << 8 | ob;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static ImageProcessor.Img firstFrame(ImageProcessor.Img t) {
      if (t != null && t.h() > t.w() && t.h() % t.w() == 0) {
         int[] px = new int[t.w() * t.w()];
         System.arraycopy(t.px(), 0, px, 0, px.length);
         return new ImageProcessor.Img(t.w(), t.w(), px);
      } else {
         return t;
      }
   }

   static String[] split(String ref) {
      int i = ref.indexOf(58);
      return i < 0 ? new String[]{"minecraft", ref} : new String[]{ref.substring(0, i), ref.substring(i + 1)};
   }

   private static double[] vec(JsonArray a) {
      double[] v = new double[a == null ? 0 : a.size()];

      for (int i = 0; i < v.length; i++) {
         v[i] = a.get(i).getAsDouble();
      }

      if (v.length < 3 && a != null && a.size() != 4) {
         double[] w = new double[3];
         System.arraycopy(v, 0, w, 0, v.length);
         return w;
      } else {
         return v;
      }
   }

   private static double[] sub(double[] a, double[] b) {
      return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
   }

   private static double[] cross(double[] a, double[] b) {
      return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
   }

   record Face(double[][] pos, double[][] uv, ImageProcessor.Img tex, boolean shade) {
   }

   public interface Lookup {
      JsonObject json(String var1, String var2);

      ImageProcessor.Img texture(String var1, String var2);
   }

   public static final class Scene {
      final List<ModelPreview.Face> faces = new ArrayList<>();
      boolean noCull;

      public boolean isEmpty() {
         return this.faces.isEmpty();
      }
   }
}
