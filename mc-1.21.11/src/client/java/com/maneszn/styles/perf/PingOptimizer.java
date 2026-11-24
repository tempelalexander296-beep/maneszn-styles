package com.maneszn.styles.perf;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;

public final class PingOptimizer {
   private static final String[][] REGIONS = new String[][]{
      {"eu", "Europa", "Europe"},
      {"de", "Deutschland", "Germany"},
      {"uk", "Großbritannien", "UK"},
      {"fr", "Frankreich", "France"},
      {"na", "Nordamerika", "North America"},
      {"us", "USA", "USA"},
      {"use", "USA Ost", "US East"},
      {"usw", "USA West", "US West"},
      {"as", "Asien", "Asia"},
      {"asia", "Asien", "Asia"},
      {"au", "Australien", "Australia"},
      {"oce", "Ozeanien", "Oceania"},
      {"sa", "Südamerika", "South America"},
      {"br", "Brasilien", "Brazil"},
      {"me", "Naher Osten", "Middle East"},
      {"af", "Afrika", "Africa"},
      {"ru", "Russland", "Russia"}
   };
   private static volatile List<PingOptimizer.Result> results = List.of();
   private static volatile boolean running;
   private static volatile String status = "";

   private PingOptimizer() {
   }

   public static List<PingOptimizer.Result> results() {
      return results;
   }

   public static boolean running() {
      return running;
   }

   public static String status() {
      return status;
   }

   public static PingOptimizer.Result best() {
      PingOptimizer.Result b = null;

      for (PingOptimizer.Result r : results) {
         if (r.ok() && (b == null || r.ping() < b.ping())) {
            b = r;
         }
      }

      return b;
   }

   public static String baseHost(String host) {
      String h = host.toLowerCase(Locale.ROOT).trim();
      int dot = h.indexOf(46);
      if (dot > 0 && h.indexOf(46, dot + 1) > 0) {
         String first = h.substring(0, dot);

         for (String[] r : REGIONS) {
            if (r[0].equals(first)) {
               return h.substring(dot + 1);
            }
         }
      }

      return h;
   }

   public static void start(String input, boolean german) {
      if (!running && input != null && !input.isBlank()) {
         running = true;
         results = List.of();
         status = german ? "Messe Ping …" : "Measuring ping …";
         Thread t = new Thread(() -> {
            try {
               run(input.trim(), german);
            } catch (Throwable var6) {
               status = (german ? "Fehler: " : "Error: ") + var6.getMessage();
            } finally {
               running = false;
            }
         }, "Maneszn-PingOptimizer");
         t.setDaemon(true);
         t.start();
      }
   }

   private static void run(String input, boolean german) throws Exception {
      ServerAddress given = ServerAddress.parseString(input);
      String base = baseHost(given.getHost());
      int port = given.getPort();
      LinkedHashMap<String, String> candidates = new LinkedHashMap<>();
      candidates.put(base, german ? "Standard" : "Default");
      if (!given.getHost().equalsIgnoreCase(base)) {
         candidates.put(given.getHost().toLowerCase(Locale.ROOT), regionName(given.getHost(), german));
      }

      if (!isIp(base)) {
         for (String[] r : REGIONS) {
            candidates.putIfAbsent(r[0] + "." + base, german ? r[1] : r[2]);
         }
      }

      ExecutorService pool = Executors.newFixedThreadPool(8, rx -> {
         Thread th = new Thread(rx, "Maneszn-Ping");
         th.setDaemon(true);
         return th;
      });
      Map<String, Future<PingOptimizer.Result>> futures = new LinkedHashMap<>();

      for (Entry<String, String> e : candidates.entrySet()) {
         String addr = e.getKey();
         String region = e.getValue();
         futures.put(addr, pool.submit(() -> measure(addr, port, region)));
      }

      List<PingOptimizer.Result> found = new ArrayList<>();
      Set<String> seenIps = new HashSet<>();

      for (Future<PingOptimizer.Result> f : futures.values()) {
         PingOptimizer.Result r;
         try {
            r = f.get(8L, TimeUnit.SECONDS);
         } catch (Exception var14) {
            continue;
         }

         if (r != null && r.ok() && seenIps.add(r.ip())) {
            found.add(r);
         }
      }

      pool.shutdownNow();
      found.sort(Comparator.comparingLong(PingOptimizer.Result::ping));
      results = List.copyOf(found);
      PingOptimizer.Result b = best();
      if (b == null) {
         status = german ? "Server nicht erreichbar." : "Server not reachable.";
      } else if (found.size() == 1) {
         status = german ? "Nur eine Region gefunden: " + b.ping() + " ms" : "Only one region found: " + b.ping() + " ms";
      } else {
         status = (german ? "Beste Region: " : "Best region: ") + b.region() + " (" + b.ping() + " ms)";
      }
   }

   private static PingOptimizer.Result measure(String host, int port, String region) {
      Optional<ResolvedServerAddress> res;
      try {
         res = ServerNameResolver.DEFAULT.resolveAddress(new ServerAddress(host, port));
      } catch (Throwable var16) {
         return null;
      }

      if (res.isEmpty()) {
         return null;
      } else {
         InetSocketAddress sock = res.get().asInetSocketAddress();
         if (sock.isUnresolved()) {
            return null;
         } else {
            long best = Long.MAX_VALUE;

            for (int i = 0; i < 3; i++) {
               long start = System.nanoTime();

               try (Socket s = new Socket()) {
                  s.setTcpNoDelay(true);
                  s.connect(sock, 2000);
                  best = Math.min(best, (System.nanoTime() - start) / 1000000L);
               } catch (Exception var15) {
               }
            }

            if (best == Long.MAX_VALUE) {
               return null;
            } else {
               String ip = sock.getAddress().getHostAddress() + ":" + sock.getPort();
               return new PingOptimizer.Result(port == 25565 ? host : host + ":" + port, region, ip, best);
            }
         }
      }
   }

   private static String regionName(String host, boolean german) {
      String first = host.toLowerCase(Locale.ROOT).split("\\.")[0];

      for (String[] r : REGIONS) {
         if (r[0].equals(first)) {
            return german ? r[1] : r[2];
         }
      }

      return host;
   }

   private static boolean isIp(String h) {
      return h.matches("[0-9.]+") || h.contains(":");
   }

   public record Result(String address, String region, String ip, long ping) {
      public boolean ok() {
         return this.ping >= 0L;
      }
   }
}
