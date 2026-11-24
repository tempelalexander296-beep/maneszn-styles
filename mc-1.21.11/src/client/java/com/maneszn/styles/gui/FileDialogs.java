package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class FileDialogs {
   private FileDialogs() {
   }

   public static CompletableFuture<Optional<Path>> openImage(String title) {
      CompletableFuture<Optional<Path>> result = new CompletableFuture<>();
      Thread t = new Thread(() -> {
         try {
            MemoryStack stack = MemoryStack.stackPush();

            try {
               PointerBuffer filters = stack.mallocPointer(5);
               filters.put(stack.UTF8("*.png"));
               filters.put(stack.UTF8("*.jpg"));
               filters.put(stack.UTF8("*.jpeg"));
               filters.put(stack.UTF8("*.gif"));
               filters.put(stack.UTF8("*.bmp"));
               filters.flip();
               String start = ModConfig.get().lastImportDir;
               if (start == null || start.isBlank() || !Files.isDirectory(Path.of(start))) {
                  start = System.getProperty("user.home");
               }

               String picked = TinyFileDialogs.tinyfd_openFileDialog(title, start + File.separator, filters, "Bilder (PNG, JPG)", false);
               result.complete(Optional.ofNullable(picked).map(x$0 -> Path.of(x$0)));
            } catch (Throwable var7) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stack != null) {
               stack.close();
            }
         } catch (Throwable var8) {
            ManesznStyles.LOGGER.warn("Dateidialog fehlgeschlagen: {}", var8.toString());
            result.complete(Optional.empty());
         }
      }, "Maneszn-FileDialog");
      t.setDaemon(true);
      t.start();
      return result;
   }
}
