package dev.cobblesnack.client;

import dev.cobblesnack.data.SpeciesInfo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FormSpriteKeyResolver {
   private FormSpriteKeyResolver() {
   }

   public static List<String> spriteKeys(SpeciesInfo var0, String var1, String var2) {
      LinkedHashSet var3 = new LinkedHashSet();
      if (var1 != null && !var1.isBlank() && !"__base__".equalsIgnoreCase(var1)) {
         String var4 = var0 == null ? "" : SpeciesInfo.normalize(var0.key());
         String var5 = SpeciesInfo.normalize(var1);
         String var6 = SpeciesInfo.normalize(var2);
         String var7 = var5 + var6;
         switch (var4) {
            case "pikachu":
               addIfContains(var3, var7, "alolabias", "alolabias");
               addIfContains(var3, var7, "rockstar", "rockstar");
               addIfContains(var3, var7, "popstar", "popstar");
               addIfContains(var3, var7, "phd", "phd");
               addIfContains(var3, var7, "libre", "libre");
               addIfContains(var3, var7, "belle", "belle");
               addIfContains(var3, var7, "cosplay", "cosplay");
               break;
            case "raichu":
               addIfContains(var3, var7, "alola", "alolan");
               break;
            case "electrode":
               addIfContains(var3, var7, "hisui", "hisuian");
               break;
            case "cyndaquil":
            case "quilava":
               addIfContains(var3, var7, "hisuibias", "hisuibias");
               break;
            case "pichu":
               addIfContains(var3, var7, "alolabias", "alolabias");
               break;
            case "miltank":
               addIfContains(var3, var7, "brown", "brownmushroom");
               addIfContains(var3, var7, "red", "redmushroom");
               break;
            case "keldeo":
               addIfContains(var3, var7, "ordinary", "__base__");
               addIfContains(var3, var7, "resolute", "resolute");
               break;
            case "castform":
               addIfContains(var3, var7, "normal", "__base__");
               break;
            case "basculin":
               addIfContains(var3, var7, "red", "redstripe");
               addIfContains(var3, var7, "blue", "bluestripe");
               addIfContains(var3, var7, "white", "whitestripe");
               break;
            case "cherrim":
               addIfContains(var3, var7, "overcast", "__base__");
               addIfContains(var3, var7, "sunshine", "sunshine");
               break;
            case "tauros":
               addIfContains(var3, var7, "kanto", "__base__");
               addIfContains(var3, var7, "combat", "combat");
               addIfContains(var3, var7, "blaze", "blaze");
               addIfContains(var3, var7, "aqua", "aqua");
               break;
            case "sinistea":
            case "polteageist":
               addIfContains(var3, var7, "phony", "__base__");
               addIfContains(var3, var7, "antique", "antique");
               break;
            case "indeedee":
               addIfContains(var3, var7, "female", "female");
               addIfContains(var3, var7, "male", "male");
               break;
            case "poltchageist":
               addIfContains(var3, var7, "counterfeit", "__base__");
               addIfContains(var3, var7, "artisan", "artisan");
               break;
            case "sinistcha":
               addIfContains(var3, var7, "unremarkable", "__base__");
               addIfContains(var3, var7, "masterpiece", "masterpiece");
               break;
            case "maushold":
               addIfContains(var3, var7, "three", "__base__");
               addIfContains(var3, var7, "four", "four");
               break;
            case "minior":
               if (var7.contains("meteor") && !var7.contains("core")) {
                  var3.add("__base__");
               }

               for (String var11 : List.of("red", "orange", "yellow", "green", "blue", "indigo", "violet")) {
                  if (var7.contains(var11)) {
                     var3.add(var11);
                  }
               }
               break;
            case "magikarp":
               addMagikarpKey(var3, var6);
               break;
            case "whiscash":
               addIfContains(var3, var7, "nero", "nero");
               break;
            case "wooper":
               addIfContains(var3, var7, "heart", "heart");
         }

         if (!var6.isBlank()) {
            var3.add(var6);
            addGenericLabelAliases(var3, var6);
         }

         addRegionalAliases(var3, var6);
         return new ArrayList<>(var3);
      } else {
         var3.add("__base__");
         return List.copyOf(var3);
      }
   }

   private static void addGenericLabelAliases(Set<String> var0, String var1) {
      for (String var3 : List.of("form", "pattern", "style", "cloak", "breed", "flower", "sea", "core", "plumage", "size", "mode", "trim", "color", "colour")) {
         if (var1.endsWith(var3) && var1.length() > var3.length()) {
            var0.add(var1.substring(0, var1.length() - var3.length()));
         }
      }
   }

   private static void addMagikarpKey(Set<String> var0, String var1) {
      switch (var1) {
         case "calicoorangewhite":
            var0.add("calico1");
            break;
         case "calicoorangewhiteblack":
            var0.add("calico2");
            break;
         case "calicowhiteorange":
            var0.add("calico3");
            break;
         case "calicoorangegold":
            var0.add("calico4");
            break;
         case "orangetwotone":
            var0.add("twotone");
            break;
         case "orangeorca":
            var0.add("orca");
            break;
         case "orangedapples":
            var0.add("dapples");
      }
   }

   private static void addRegionalAliases(Set<String> var0, String var1) {
      switch (var1) {
         case "alola":
         case "alolan":
            var0.add("alolan");
            var0.add("alola");
            break;
         case "hisui":
         case "hisuian":
            var0.add("hisuian");
            var0.add("hisui");
            break;
         case "galar":
         case "galarian":
            var0.add("galarian");
            var0.add("galar");
            break;
         case "paldea":
         case "paldean":
            var0.add("paldean");
            var0.add("paldea");
      }
   }

   private static void addIfContains(Set<String> var0, String var1, String var2, String var3) {
      if (var1.contains(var2)) {
         var0.add(var3);
      }
   }
}
