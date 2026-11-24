package com.maneszn.styles.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.render.ModelPreview;
import com.maneszn.styles.render.PreviewScenes;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

public final class VanillaLookup implements ModelPreview.Lookup {
   public static final VanillaLookup INSTANCE = new VanillaLookup();
   private final Map<String, Optional<JsonObject>> jsons = new HashMap<>();
   private final Map<String, Optional<ImageProcessor.Img>> textures = new HashMap<>();

   private VanillaLookup() {
   }

   @Override
   public JsonObject json(String ns, String path) {
      return this.jsons.computeIfAbsent(ns + ":" + path, k -> {
         try {
            Optional<JsonObject> var5;
            try (InputStream in = open(ns, path)) {
               if (in == null) {
                  return Optional.empty();
               }

               JsonElement e = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
               var5 = e.isJsonObject() ? Optional.of(e.getAsJsonObject()) : Optional.empty();
            }

            return var5;
         } catch (Exception var8) {
            return Optional.empty();
         }
      }).orElse(null);
   }

   @Override
   public ImageProcessor.Img texture(String ns, String path) {
      return this.textures.computeIfAbsent(ns + ":" + path, k -> {
         try {
            Optional<ImageProcessor.Img> var4;
            try (InputStream in = open(ns, path)) {
               var4 = in == null ? Optional.empty() : Optional.ofNullable(PreviewScenes.decode(in));
            }

            return var4;
         } catch (Exception var8) {
            return Optional.empty();
         }
      }).orElse(null);
   }

   private static InputStream open(String ns, String path) throws IOException {
      IoSupplier<InputStream> sup = Minecraft.getInstance()
         .getVanillaPackResources()
         .getResource(PackType.CLIENT_RESOURCES, Identifier.fromNamespaceAndPath(ns, path));
      return sup == null ? null : sup.get();
   }

   public void clear() {
      this.jsons.clear();
      this.textures.clear();
   }
}
