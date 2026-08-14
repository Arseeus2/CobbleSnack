package dev.cobblesnack.cache;

import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;

public final class DataAuditSourceStore {
   private static final String FILE = "audit-sources.tsv";

   private DataAuditSourceStore() {
   }

   public static void save(DataIndex var0) {
      if (var0 != null) {
         try {
            ArrayList var1 = new ArrayList();
            var1.add("V\t1");
            var1.add("C\t" + var0.inactiveSpawnRuleFileCount() + "\t" + var0.excludedSpawnRouteCount());

            for (String var3 : var0.excludedSpawnRoutes()) {
               var1.add("X\t" + encode(var3));
            }

            for (SpeciesInfo var10 : var0.uniqueSpecies()) {
               for (String var5 : var10.sources()) {
                  var1.add("S\t" + encode(var10.key()) + "\t" + encode(var5));
               }
            }

            for (SpawnEntry var11 : var0.spawns()) {
               var1.add("R\t" + encode(signature(var11)) + "\t" + encode(var11.source));
            }

            Path var9 = DiskCacheStore.cacheDirectory().resolve("audit-sources.tsv");
            Files.createDirectories(var9.getParent());
            Path var12 = var9.resolveSibling("audit-sources.tsv.tmp");
            Files.write(var12, var1, StandardCharsets.UTF_8);
            Files.move(var12, var9, StandardCopyOption.REPLACE_EXISTING);
         } catch (Throwable var6) {
            SessionDiagnostics.error("audit-source-save", var6);
         }
      }
   }

   public static void restore(DataIndex var0) {
      if (var0 != null) {
         Path var1 = DiskCacheStore.cacheDirectory().resolve("audit-sources.tsv");
         if (Files.isRegularFile(var1)) {
            try {
               LinkedHashMap var2 = new LinkedHashMap();
               ArrayList var3 = new ArrayList();
               int var4 = 0;
               int var5 = 0;

               for (String var7 : Files.readAllLines(var1, StandardCharsets.UTF_8)) {
                  String[] var8 = var7.split("\\t", -1);
                  if (var8.length != 0) {
                     switch (var8[0]) {
                        case "C":
                           if (var8.length >= 3) {
                              var4 = integer(var8[1]);
                              var5 = integer(var8[2]);
                           }
                           break;
                        case "X":
                           if (var8.length >= 2) {
                              var3.add(decode(var8[1]));
                           }
                           break;
                        case "S":
                           if (var8.length >= 3) {
                              SpeciesInfo var11 = var0.findSpecies(decode(var8[1]));
                              if (var11 != null) {
                                 var11.addSource(decode(var8[2]));
                              }
                           }
                           break;
                        case "R":
                           if (var8.length >= 3) {
                              var2.put(decode(var8[1]), decode(var8[2]));
                           }
                     }
                  }
               }

               for (SpawnEntry var14 : var0.spawns()) {
                  var14.setSource((String)var2.get(signature(var14)));
               }

               var0.restoreAuditCounts(var4, var5, var3);
            } catch (Throwable var12) {
               SessionDiagnostics.error("audit-source-load", var12);
            }
         }
      }
   }

   private static String signature(SpawnEntry var0) {
      return var0.speciesKey + "|" + var0.id + "|" + var0.pokemonExpression + "|" + var0.context + "|" + var0.bucket + "|" + var0.weight;
   }

   private static String encode(String var0) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString((var0 == null ? "" : var0).getBytes(StandardCharsets.UTF_8));
   }

   private static String decode(String var0) {
      return new String(Base64.getUrlDecoder().decode(var0), StandardCharsets.UTF_8);
   }

   private static int integer(String var0) {
      try {
         return Integer.parseInt(var0);
      } catch (NumberFormatException var2) {
         return 0;
      }
   }
}
