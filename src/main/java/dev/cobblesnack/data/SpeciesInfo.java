package dev.cobblesnack.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SpeciesInfo {
   private final String key;
   private String displayName;
   private int nationalPokedexNumber = Integer.MAX_VALUE;
   private String resourceId;
   private String primaryType;
   private String secondaryType;
   private final List<String> eggGroups = new ArrayList<>();
   private final Map<String, Integer> evYield = new LinkedHashMap<>();
   private final List<SpeciesInfo.FormInfo> forms = new ArrayList<>();
   private final List<String> baseAspects = new ArrayList<>();
   private final List<String> sources = new ArrayList<>();

   public SpeciesInfo(String var1, String var2) {
      this.key = normalize(var1);
      this.displayName = var2 != null && !var2.isBlank() ? var2 : var1;
   }

   public String key() {
      return this.key;
   }

   public String displayName() {
      return this.displayName;
   }

   public int nationalPokedexNumber() {
      return this.nationalPokedexNumber;
   }

   public String resourceId() {
      return this.resourceId != null && !this.resourceId.isBlank() ? this.resourceId : "cobblemon:" + this.key;
   }

   public String primaryType() {
      return this.primaryType;
   }

   public String secondaryType() {
      return this.secondaryType;
   }

   public List<String> eggGroups() {
      return Collections.unmodifiableList(this.eggGroups);
   }

   public Map<String, Integer> evYield() {
      return Collections.unmodifiableMap(this.evYield);
   }

   public List<SpeciesInfo.FormInfo> forms() {
      return Collections.unmodifiableList(this.forms);
   }

   public List<String> baseAspects() {
      return Collections.unmodifiableList(this.baseAspects);
   }

   public List<String> sources() {
      return Collections.unmodifiableList(this.sources);
   }

   public void setDisplayName(String var1) {
      if (var1 != null && !var1.isBlank()) {
         this.displayName = var1;
      }
   }

   public void setNationalPokedexNumber(int var1) {
      if (var1 > 0) {
         this.nationalPokedexNumber = var1;
      }
   }

   public void setResourceId(String var1) {
      if (var1 != null && !var1.isBlank()) {
         this.resourceId = var1.toLowerCase(Locale.ROOT);
      }
   }

   public void setPrimaryType(String var1) {
      this.primaryType = normalizeNullable(var1);
   }

   public void setSecondaryType(String var1) {
      this.secondaryType = normalizeNullable(var1);
   }

   public void setEggGroups(List<String> var1) {
      this.eggGroups.clear();
      if (var1 != null) {
         for (String var3 : var1) {
            String var4 = normalizeNullable(var3);
            if (var4 != null && !this.eggGroups.contains(var4)) {
               this.eggGroups.add(var4);
            }
         }
      }
   }

   public void setEvYield(Map<String, Integer> var1) {
      this.evYield.clear();
      if (var1 != null) {
         var1.forEach((var1x, var2) -> {
            String var3 = normalizeStat(var1x);
            if (var3 != null && var2 != null) {
               this.evYield.put(var3, var2);
            }
         });
      }
   }

   public void setBaseAspects(List<String> var1) {
      this.baseAspects.clear();
      if (var1 != null) {
         for (String var3 : var1) {
            if (var3 != null && !var3.isBlank()) {
               this.baseAspects.add(var3.toLowerCase(Locale.ROOT));
            }
         }
      }
   }

   public void addForm(SpeciesInfo.FormInfo var1) {
      if (var1 != null && this.forms.stream().noneMatch(var1x -> var1x.sameDefinition(var1))) {
         this.forms.add(var1);
      }
   }

   public void addSource(String var1) {
      if (var1 != null && !var1.isBlank() && !this.sources.contains(var1)) {
         this.sources.add(var1);
      }
   }

   public SpeciesInfo.ResolvedTraits resolveForSpawnString(String var1) {
      String var2 = var1 == null ? "" : var1.toLowerCase(Locale.ROOT);
      SpeciesInfo.FormInfo var3 = null;
      int var4 = 0;

      for (SpeciesInfo.FormInfo var6 : this.forms) {
         int var7 = var6.matchScore(var2);
         if (var7 > var4) {
            var3 = var6;
            var4 = var7;
         }
      }

      return var3 != null
         ? new SpeciesInfo.ResolvedTraits(
            var3.primaryType != null ? var3.primaryType : this.primaryType,
            var3.secondaryType != null ? var3.secondaryType : this.secondaryType,
            var3.eggGroups.isEmpty() ? this.eggGroups : var3.eggGroups,
            var3.evYield.isEmpty() ? this.evYield : var3.evYield
         )
         : new SpeciesInfo.ResolvedTraits(this.primaryType, this.secondaryType, this.eggGroups, this.evYield);
   }

   public static String normalize(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = var0.toLowerCase(Locale.ROOT).trim();
      int var2 = var1.indexOf(58);
      if (var2 >= 0 && var1.chars().noneMatch(Character::isWhitespace)) {
         var1 = var1.substring(var2 + 1);
      }

      return var1.replaceAll("[^a-z0-9]", "");
   }

   private static String normalizeFormText(String var0) {
      String var1 = normalize(var0);
      return var1.replace("hisuian", "hisui").replace("alolan", "alola").replace("galarian", "galar").replace("paldean", "paldea");
   }

   private static String normalizeNullable(String var0) {
      return var0 != null && !var0.isBlank() ? var0.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_') : null;
   }

   private static String normalizeStat(String var0) {
      String var1 = normalizeNullable(var0);
      if (var1 == null) {
         return null;
      }

      return switch (var1) {
         case "defense" -> "defence";
         case "special_defense", "sp_def", "spdef" -> "special_defence";
         case "special_attack", "sp_atk", "spatk" -> "special_attack";
         default -> var1;
      };
   }

   public static final class FormInfo {
      private final String name;
      private final List<String> aspects;
      private final String primaryType;
      private final String secondaryType;
      private final List<String> eggGroups;
      private final Map<String, Integer> evYield;

      public FormInfo(String var1, List<String> var2, String var3, String var4, List<String> var5, Map<String, Integer> var6) {
         this.name = var1 == null ? "" : var1.trim();
         this.aspects = var2 == null
            ? List.of()
            : var2.stream().filter(var0 -> var0 != null && !var0.isBlank()).map(var0 -> var0.toLowerCase(Locale.ROOT)).toList();
         this.primaryType = SpeciesInfo.normalizeNullable(var3);
         this.secondaryType = SpeciesInfo.normalizeNullable(var4);
         this.eggGroups = var5 == null ? List.of() : var5.stream().filter(var0 -> var0 != null && !var0.isBlank()).map(SpeciesInfo::normalizeNullable).toList();
         LinkedHashMap var7 = new LinkedHashMap();
         if (var6 != null) {
            var6.forEach((var1x, var2x) -> {
               String var3x = SpeciesInfo.normalizeStat(var1x);
               if (var3x != null && var2x != null) {
                  var7.put(var3x, var2x);
               }
            });
         }

         this.evYield = Collections.unmodifiableMap(var7);
      }

      public String name() {
         return this.name;
      }

      public List<String> aspects() {
         return this.aspects;
      }

      public String primaryType() {
         return this.primaryType;
      }

      public String secondaryType() {
         return this.secondaryType;
      }

      public List<String> eggGroups() {
         return this.eggGroups;
      }

      public Map<String, Integer> evYield() {
         return this.evYield;
      }

      private boolean sameDefinition(SpeciesInfo.FormInfo var1) {
         return var1 != null
            && this.name.equals(var1.name)
            && this.aspects.equals(var1.aspects)
            && Objects.equals(this.primaryType, var1.primaryType)
            && Objects.equals(this.secondaryType, var1.secondaryType)
            && this.eggGroups.equals(var1.eggGroups)
            && this.evYield.equals(var1.evYield);
      }

      public boolean matches(String var1) {
         return this.matchScore(var1) > 0;
      }

      public int matchScore(String var1) {
         if (var1 != null && !var1.isBlank()) {
            String var2 = SpeciesInfo.normalizeFormText(var1);
            byte var3 = 0;
            String var4 = SpeciesInfo.normalizeFormText(this.name);
            if (!var4.isBlank() && var2.contains(var4)) {
               var3 += 4;
            }

            for (String var6 : this.aspects) {
               String var8 = SpeciesInfo.normalizeFormText(var6);
               boolean var7 = !var8.isBlank() && var2.contains(var8);
               if (!var7) {
                  for (String var13 : var6.split("[-_ ]+")) {
                     String var14 = SpeciesInfo.normalizeFormText(var13);
                     if (var14.length() >= 3
                        && !var14.equals("form")
                        && !var14.equals("breed")
                        && !var14.equals("mode")
                        && !var14.equals("style")
                        && !var14.equals("color")
                        && !var14.equals("colour")
                        && !var14.equals("pattern")
                        && var2.contains(var14)) {
                        var7 = true;
                        break;
                     }
                  }
               }

               if (var7) {
                  var3 += 2;
               }
            }

            return var3;
         } else {
            return 0;
         }
      }
   }

   public record ResolvedTraits(String primaryType, String secondaryType, List<String> eggGroups, Map<String, Integer> evYield) {
      public boolean hasType(String var1) {
         return var1 == null ? false : var1.equals(this.primaryType) || var1.equals(this.secondaryType);
      }

      public boolean hasEggGroup(String var1) {
         return var1 != null && this.eggGroups != null && this.eggGroups.contains(var1);
      }

      public int evYieldFor(String var1) {
         if (var1 != null && this.evYield != null) {
            String var2 = SpeciesInfo.normalizeStat(var1);
            return var2 == null ? 0 : this.evYield.getOrDefault(var2, 0);
         } else {
            return 0;
         }
      }
   }
}
