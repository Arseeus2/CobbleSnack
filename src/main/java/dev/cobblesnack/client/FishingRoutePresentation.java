package dev.cobblesnack.client;

import dev.cobblesnack.data.SpawnEntry;
import java.util.List;
import java.util.stream.Collectors;

public final class FishingRoutePresentation {
   private FishingRoutePresentation() {
   }

   public static List<String> rodIds(SpawnEntry var0) {
      if (var0 == null) {
         return List.of("cobblemon:poke_rod");
      }

      List<String> var1 = var0.conditions.stream().flatMap(var0x -> var0x.rodTypes.stream()).filter(var0x -> var0x != null && !var0x.isBlank()).distinct().toList();
      return var1.isEmpty() ? List.of("cobblemon:poke_rod") : var1;
   }

   public static String friendlyRodName(String var0) {
      if (var0 != null && !var0.isBlank() && !"cobblemon:poke_rod".equalsIgnoreCase(var0)) {
         String var1 = var0;
         int var2 = var1.indexOf(58);
         if (var2 >= 0 && var2 + 1 < var1.length()) {
            var1 = var1.substring(var2 + 1);
         }

         return titleWords(var1.replace('_', ' '));
      } else {
         return "Any Poké Rod";
      }
   }

   public static String useOutputLine(SpawnEntry var0) {
      List<String> var1 = rodIds(var0);
      String var2 = var1.stream().map(FishingRoutePresentation::friendlyRodName).collect(Collectors.joining(" or "));
      return "FISHUSE|" + var2 + "|" + String.join(",", var1);
   }

   private static String titleWords(String var0) {
      StringBuilder var1 = new StringBuilder();
      boolean var2 = true;

      for (char var6 : var0.toCharArray()) {
         if (var6 == '_' || var6 == '-') {
            var6 = ' ';
         }

         if (var2 && Character.isLetter(var6)) {
            var1.append(Character.toUpperCase(var6));
            var2 = false;
         } else {
            var1.append(var6);
         }

         if (var6 == ' ') {
            var2 = true;
         }
      }

      return var1.toString().trim();
   }
}
