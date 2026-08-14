package dev.cobblesnack.cache;

import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import dev.cobblesnack.data.SpeciesInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DataAuditDiagnostics {
   private DataAuditDiagnostics() {
   }

   public static void emit(DataIndex var0) {
      List<String> var1 = new ArrayList<>();
      List<SpeciesInfo> var2 = var0.uniqueSpecies()
         .stream()
         .sorted(Comparator.comparingInt(SpeciesInfo::nationalPokedexNumber).thenComparing(SpeciesInfo::displayName, String.CASE_INSENSITIVE_ORDER))
         .toList();
      Set<String> var3 = var0.browserSpecies().stream().map(SpeciesInfo::key).collect(Collectors.toCollection(LinkedHashSet::new));
      Map<String, List<SpawnEntry>> var4 = new LinkedHashMap<>();
      int var5 = 0;
      int var6 = 0;

      for (SpawnEntry var8 : var0.spawns()) {
         SpeciesInfo var9 = var0.findSpecies(var8.speciesKey);
         String var10 = var9 == null ? var8.speciesKey : var9.key();
         var4.computeIfAbsent(var10, var0x -> new ArrayList<>()).add(var8);
         if (var8.hasUnknownConditions) {
            var5++;
         }

         if (var9 == null) {
            var6++;
         }
      }

      var1.add("BEGIN schema=3 meaning=installed-Pokemon-forms-and-world-spawn-routes");
      var1.add(
         "SUMMARY species="
            + var2.size()
            + " browserSpecies="
            + var3.size()
            + " activeRoutes="
            + var0.spawns().size()
            + " excludedRoutes="
            + var0.excludedSpawnRouteCount()
            + " activeSpawnRules="
            + var0.spawnRuleFileCount()
            + " inactiveSpawnRules="
            + var0.inactiveSpawnRuleFileCount()
            + " unknownConditionRoutes="
            + var5
            + " routesMissingSpecies="
            + var6
      );
      var1.add(
         "CHANCE-MODEL route weights below are source data; the percentage shown in results is conditional on the chosen biome, conditions, rarity bucket and seasonings. It is not a guarantee that any Pokemon spawns."
      );
      var1.add(
         "BUCKET-ODDS tier0={common=86.20%,uncommon=10.28%,rare=2.51%,ultra-rare=1.01%} tier1={77.10%,15.01%,5.38%,2.51%} tier2={67.84%,18.73%,8.84%,4.59%} tier3={59.37%,21.31%,12.35%,6.97%} tier10={28.48%,24.08%,27.32%,20.13%} tier11={26.51%,23.83%,28.36%,21.30%} tier12={24.30%,23.56%,29.26%,22.35%} tier20={17.29%,21.62%,33.34%,27.76%} tier21={16.75%,21.42%,33.63%,28.20%} tier30={13.58%,20.09%,35.33%,31.00%}"
      );

      for (String var19 : var0.excludedSpawnRoutes()) {
         var1.add("EXCLUDED " + clean(var19));
      }

      for (SpeciesInfo var20 : var2) {
         List<SpawnEntry> var22 = var4.getOrDefault(var20.key(), List.of());
         List<DataIndex.FormSpawnOption> var23 = var0.spawnFormOptions(var20.key());
         String var11 = var22.stream().map(var0x -> var0x.bucket).distinct().sorted().collect(Collectors.joining(","));
         String var12 = var22.stream().map(var0x -> var0x.context).distinct().sorted().collect(Collectors.joining(","));
         String var13 = var20.sources().isEmpty() ? "unknown" : String.join(",", var20.sources());
         var1.add(
            "SPECIES key="
               + var20.key()
               + " dex="
               + dex(var20.nationalPokedexNumber())
               + " name="
               + clean(var20.displayName())
               + " resource="
               + clean(var20.resourceId())
               + " browser="
               + var3.contains(var20.key())
               + " routes="
               + var22.size()
               + " declaredForms="
               + var20.forms().size()
               + " selectableForms="
               + formOptions(var23)
               + " types="
               + clean(var20.primaryType())
               + "/"
               + clean(var20.secondaryType())
               + " eggGroups="
               + clean(var20.eggGroups())
               + " evYield="
               + clean(var20.evYield())
               + " contexts="
               + clean(var12)
               + " buckets="
               + clean(var11)
               + " sources="
               + clean(var13)
         );

         for (SpeciesInfo.FormInfo var15 : var20.forms()) {
            var1.add(
               "FORM species="
                  + var20.key()
                  + " name="
                  + clean(var15.name())
                  + " aspects="
                  + clean(var15.aspects())
                  + " types="
                  + clean(var15.primaryType())
                  + "/"
                  + clean(var15.secondaryType())
                  + " eggGroups="
                  + clean(var15.eggGroups())
                  + " evYield="
                  + clean(var15.evYield())
            );
         }
      }

      for (SpawnEntry var21 : var0.spawns()) {
         var1.add(
            "ROUTE species="
               + clean(var21.speciesKey)
               + " id="
               + clean(var21.id)
               + " pokemon="
               + clean(var21.pokemonExpression)
               + " context="
               + clean(var21.context)
               + " bucket="
               + clean(var21.bucket)
               + " weight="
               + format(var21.weight)
               + " conditions="
               + conditionList(var21.conditions)
               + " antiConditions="
               + conditionList(var21.antiConditions)
               + " multipliers="
               + multiplierList(var21.weightMultipliers)
               + " unknown="
               + var21.hasUnknownConditions
               + " source="
               + clean(var21.source)
         );
      }

      var1.add("END species=" + var2.size() + " activeRoutes=" + var0.spawns().size());
      SessionDiagnostics.auditLines(var1);
   }

   private static String formOptions(List<DataIndex.FormSpawnOption> var0) {
      return var0.stream().map(var0x -> clean(var0x.key()) + ":" + clean(var0x.label())).collect(Collectors.joining(",", "[", "]"));
   }

   private static String conditionList(List<SpawnCondition> var0) {
      return var0.stream().map(DataAuditDiagnostics::condition).collect(Collectors.joining(",", "[", "]"));
   }

   private static String multiplierList(List<SpawnEntry.WeightMultiplier> var0) {
      return var0.stream()
         .map(
            var0x -> "{" + format(var0x.multiplier()) + ":when=" + conditionList(var0x.conditions()) + ":unless=" + conditionList(var0x.antiConditions()) + "}"
         )
         .collect(Collectors.joining(",", "[", "]"));
   }

   private static String condition(SpawnCondition var0) {
      if (var0 == null) {
         return "{}";
      }

      List<String> var1 = new ArrayList<>();

      for (Field var5 : SpawnCondition.class.getFields()) {
         if (!Modifier.isStatic(var5.getModifiers())) {
            try {
               Object var6 = var5.get(var0);
               if (var6 != null && !(var6 instanceof Collection var7 && var7.isEmpty())) {
                  var1.add(var5.getName() + "=" + clean(var6));
               }
            } catch (IllegalAccessException var8) {
               var1.add(var5.getName() + "=<unreadable>");
            }
         }
      }

      var1.sort(String.CASE_INSENSITIVE_ORDER);
      return var1.stream().collect(Collectors.joining(",", "{", "}"));
   }

   private static String dex(int var0) {
      return var0 == Integer.MAX_VALUE ? "none" : Integer.toString(var0);
   }

   private static String format(double var0) {
      return String.format(Locale.ROOT, "%.6f", var0).replaceAll("0+$", "").replaceAll("\\.$", "");
   }

   private static String clean(Object var0) {
      return var0 == null ? "none" : String.valueOf(var0).replace('\n', ' ').replace('\r', ' ').replace('|', '/');
   }
}
