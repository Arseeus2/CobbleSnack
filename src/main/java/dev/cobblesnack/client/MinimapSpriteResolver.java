package dev.cobblesnack.client;

import dev.cobblesnack.cache.SessionDiagnostics;
import dev.cobblesnack.data.DataIndex;
import dev.cobblesnack.data.SpeciesInfo;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class MinimapSpriteResolver {
   private static final String XAERO_NAMESPACE = "xaerominimap";
   private static final String ICON_ROOT = "entity/icon";
   private static final long FORM_CYCLE_MILLIS = 2500L;
   private static final int MAX_PENDING_FORM_JOBS = 24;
   private static ResourceManager indexedManager;
   private static boolean indexReady;
   private static List<MinimapSpriteResolver.Candidate> candidates = List.of();
   private static Map<Integer, List<MinimapSpriteResolver.Candidate>> candidatesByDex = Map.of();
   private static final Map<String, List<MinimapSpriteResolver.SpriteRef>> RESOLVED = new ConcurrentHashMap<>();
   private static final Map<String, List<MinimapSpriteResolver.FormSpriteOption>> FORM_OPTIONS = new ConcurrentHashMap<>();
   private static final Map<String, Map<String, MinimapSpriteResolver.SpriteRef>> FORM_SPRITES = new ConcurrentHashMap<>();
   private static final Map<Identifier, MinimapSpriteResolver.Dimensions> DIMENSIONS = new ConcurrentHashMap<>();
   private static final Set<Identifier> NEAREST_FILTERED = ConcurrentHashMap.newKeySet();
   static final Set<String> PENDING_FORMS = ConcurrentHashMap.newKeySet();
   private static volatile long generation;
   private static volatile long lastAuditGeneration = -1L;
   private static final ExecutorService FORM_WORKER = Executors.newSingleThreadExecutor(var0 -> {
      Thread var1 = new Thread(var0, "CobbleSnack Sprite Forms");
      var1.setDaemon(true);
      var1.setPriority(1);
      return var1;
   });

   private MinimapSpriteResolver() {
   }

   public static MinimapSpriteResolver.SpriteRef spriteFor(SpeciesInfo var0) {
      return spriteFor(var0, false);
   }

   public static MinimapSpriteResolver.SpriteRef spriteFor(SpeciesInfo var0, boolean var1) {
      if (var0 == null) {
         return null;
      }

      ensureIndex();
      String var2 = var0.resourceId();
      List<MinimapSpriteResolver.SpriteRef> var3 = RESOLVED.get(var2);
      if (var3 == null) {
         MinimapSpriteResolver.SpriteRef var4 = resolveBase(var0);
         var3 = var4 == null ? List.of() : List.of(var4);
         RESOLVED.put(var2, var3);
      }

      if (var1 && !FORM_OPTIONS.containsKey(var2)) {
         scheduleForms(var0);
      }

      if (var3.isEmpty()) {
         return null;
      } else if (var1 && var3.size() != 1) {
         int var5 = (int)(System.currentTimeMillis() / 2500L % var3.size());
         return (MinimapSpriteResolver.SpriteRef)var3.get(var5);
      } else {
         return (MinimapSpriteResolver.SpriteRef)var3.get(0);
      }
   }

   public static MinimapSpriteResolver.SpriteRef spriteFor(SpeciesInfo var0, String var1) {
      return spriteForSpawnForm(var0, var1, var1);
   }

   public static MinimapSpriteResolver.SpriteRef spriteForSpawnForm(SpeciesInfo var0, String var1, String var2) {
      if (var0 == null) {
         return null;
      }

      ensureIndex();
      String var3 = var0.resourceId();
      if (!FORM_OPTIONS.containsKey(var3)) {
         scheduleForms(var0);
      }

      Map var4 = FORM_SPRITES.getOrDefault(var3, Map.of());

      for (String var6 : FormSpriteKeyResolver.spriteKeys(var0, var1, var2)) {
         MinimapSpriteResolver.SpriteRef var7 = (MinimapSpriteResolver.SpriteRef)var4.get("__base__".equals(var6) ? var6 : SpeciesInfo.normalize(var6));
         if (var7 != null) {
            return var7;
         }
      }

      MinimapSpriteResolver.SpriteRef var8 = (MinimapSpriteResolver.SpriteRef)var4.get("__base__");
      return var8 != null ? var8 : resolveBase(var0);
   }

   public static List<MinimapSpriteResolver.FormSpriteOption> formOptions(SpeciesInfo var0) {
      if (var0 != null && !isCollapsedVisualSpecies(var0)) {
         ensureIndex();
         if (!FORM_OPTIONS.containsKey(var0.resourceId())) {
            scheduleForms(var0);
         }

         return FORM_OPTIONS.getOrDefault(var0.resourceId(), List.of());
      } else {
         return List.of();
      }
   }

   public static void useNearest(MinimapSpriteResolver.SpriteRef var0) {
      if (var0 != null && var0.texture() != null && NEAREST_FILTERED.add(var0.texture())) {
         try {
            MinecraftClient.getInstance().getTextureManager().getTexture(var0.texture()).setFilter(false, false);
         } catch (Throwable var2) {
            NEAREST_FILTERED.remove(var0.texture());
         }
      }
   }

   public static void auditInstalledMappings() {
      ensureIndex();
      long var0 = generation;
      if (indexReady && !candidates.isEmpty() && lastAuditGeneration != var0) {
         lastAuditGeneration = var0;
         FORM_WORKER.execute(
            () -> {
               try {
                  DataIndex var2 = DataIndex.get();
                  List<String> var3 = new ArrayList<>();
                  int var4 = 0;
                  int var5 = 0;
                  int var6 = 0;

                  for (SpeciesInfo var8 : var2.browserSpecies()) {
                     if (var0 != generation) {
                        return;
                     }

                     List<MinimapSpriteResolver.Candidate> var9 = matchingCandidates(var8);
                     String var10 = var9.isEmpty() ? "missing" : var9.get(0).id.toString();
                     if (var9.isEmpty()) {
                        var4++;
                     }

                     Map<String, MinimapSpriteResolver.Candidate> var11 = new LinkedHashMap<>();

                     for (MinimapSpriteResolver.Candidate var13 : matchingFormCandidates(var8)) {
                        String var14 = rawSuffixAfterSpecies(var13.rawBase, var8);
                        if (var14 != null) {
                           String var15 = var14.isBlank() ? "__base__" : SpeciesInfo.normalize(var14);
                           if (!var15.isBlank()) {
                              var11.putIfAbsent(var15, var13);
                           }
                        }
                     }

                     List<String> var19 = new ArrayList<>();

                     for (DataIndex.FormSpawnOption var22 : var2.spawnFormOptions(var8.key())) {
                        MinimapSpriteResolver.Candidate var24 = null;

                        for (String var17 : FormSpriteKeyResolver.spriteKeys(var8, var22.key(), var22.label())) {
                           var24 = (MinimapSpriteResolver.Candidate)var11.get("__base__".equals(var17) ? var17 : SpeciesInfo.normalize(var17));
                           if (var24 != null) {
                              break;
                           }
                        }

                        if (var24 == null) {
                           var5++;
                           var19.add(var22.key() + "=missing");
                        } else {
                           var6++;
                           var19.add(var22.key() + "=" + var24.id);
                        }
                     }

                     if (!var19.isEmpty() || var9.isEmpty()) {
                         List<String> var21 = var9.stream().limit(12L).map(var0xx -> var0xx.id.toString()).toList();
                        String var23 = var9.size() > var21.size() ? " +" + (var9.size() - var21.size()) + " more" : "";
                        var3.add(
                           "SPRITE species="
                              + var8.key()
                              + " base="
                              + var10
                              + " selectableForms="
                              + var19
                              + " cycleCount="
                              + var9.size()
                              + " cyclePreview="
                              + var21
                              + var23
                        );
                     }
                  }

                  var3.add(
                     0,
                     "SPRITE-SUMMARY candidates="
                        + candidates.size()
                        + " species="
                        + var2.browserSpecies().size()
                        + " missingBase="
                        + var4
                        + " mappedSelectableForms="
                        + var6
                        + " missingSelectableForms="
                        + var5
                  );
                  SessionDiagnostics.auditLines(var3);
               } catch (Throwable var18) {
                  SessionDiagnostics.error("sprite-audit", var18);
               }
            }
         );
      }
   }

   public static void invalidate() {
      generation++;
      indexedManager = null;
      indexReady = false;
      candidates = List.of();
      candidatesByDex = Map.of();
      RESOLVED.clear();
      FORM_OPTIONS.clear();
      FORM_SPRITES.clear();
      DIMENSIONS.clear();
      NEAREST_FILTERED.clear();
      PENDING_FORMS.clear();
      lastAuditGeneration = -1L;
   }

   private static synchronized void ensureIndex() {
      MinecraftClient var1 = MinecraftClient.getInstance();
      if (var1 != null) {
         ResourceManager var2 = var1.getResourceManager();
         if (var2 != indexedManager || !indexReady) {
            generation++;
            indexedManager = var2;
            indexReady = false;
            RESOLVED.clear();
            FORM_OPTIONS.clear();
            FORM_SPRITES.clear();
            DIMENSIONS.clear();
            PENDING_FORMS.clear();

            Map<Identifier, Resource> var0;
            try {
               var0 = var2.findResources(
                  "entity/icon",
                  var0x -> "xaerominimap".equals(var0x.getNamespace()) && var0x.getPath().endsWith(".png") && var0x.getPath().contains("/sprite/")
               );
            } catch (RuntimeException var15) {
               candidates = List.of();
               candidatesByDex = Map.of();
               indexReady = true;
               return;
            }

            List<MinimapSpriteResolver.Candidate> var3 = new ArrayList<>(var0.size());
            Map<Integer, List<MinimapSpriteResolver.Candidate>> var4 = new HashMap<>();

            for (Entry<Identifier, Resource> var7 : var0.entrySet()) {
               Identifier var10 = var7.getKey();
               String var11 = var10.getPath().toLowerCase(Locale.ROOT);
               Resource var8;
               String var9;
               if (!isVanillaMobIcon(var11, var9 = (var8 = var7.getValue()).getPackId() == null ? "" : var8.getPackId().toLowerCase(Locale.ROOT))
                  && !isUnwantedSpriteTree(var11)) {
                  String var12 = var11.substring(var11.lastIndexOf(47) + 1, var11.length() - 4);
                  MinimapSpriteResolver.Candidate var13 = new MinimapSpriteResolver.Candidate(
                     var10, var8, normalize(var12), var12.toLowerCase(Locale.ROOT), var11, var9
                  );
                  var3.add(var13);
                  int var14 = leadingDex(var12);
                  if (var14 > 0) {
                     var4.computeIfAbsent(var14, var0x -> new ArrayList<>()).add(var13);
                  }
               }
            }

            candidates = List.copyOf(var3);
            Map<Integer, List<MinimapSpriteResolver.Candidate>> var16 = new HashMap<>();
            var4.forEach((var1x, var2x) -> var16.put(var1x, List.copyOf(var2x)));
            candidatesByDex = Map.copyOf(var16);
            indexReady = true;
         }
      }
   }

   private static int leadingDex(String var0) {
      if (var0 != null && !var0.isBlank()) {
         int var2 = 0;

         while (var2 < var0.length() && Character.isDigit(var0.charAt(var2))) {
            var2++;
         }

         if (var2 != 0 && var2 <= 4) {
            char var1;
            if (var2 < var0.length() && (var1 = var0.charAt(var2)) != '_' && var1 != '-') {
               return -1;
            }

            try {
               return Integer.parseInt(var0.substring(0, var2));
            } catch (NumberFormatException var4) {
               return -1;
            }
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static List<MinimapSpriteResolver.Candidate> candidatePool(SpeciesInfo var0) {
      int var2 = var0 == null ? Integer.MAX_VALUE : var0.nationalPokedexNumber();
      List var1;
      return var2 != Integer.MAX_VALUE && (var1 = candidatesByDex.get(var2)) != null && !var1.isEmpty() ? var1 : candidates;
   }

   private static MinimapSpriteResolver.SpriteRef resolveBase(SpeciesInfo var0) {
      List var1 = matchingCandidates(var0);
      if (var1.isEmpty()) {
         return null;
      }

      MinimapSpriteResolver.Candidate var2 = (MinimapSpriteResolver.Candidate)var1.get(0);
      MinimapSpriteResolver.Dimensions var3 = DIMENSIONS.computeIfAbsent(var2.id, var1x -> readDimensions(var2.resource));
      return new MinimapSpriteResolver.SpriteRef(var2.id, var3.width, var3.height);
   }

   private static void scheduleForms(SpeciesInfo var0) {
      if (var0 != null && !isCollapsedVisualSpecies(var0) && indexReady) {
         String var1 = var0.resourceId();
         if (!FORM_OPTIONS.containsKey(var1) && !PENDING_FORMS.contains(var1)) {
            List var2 = candidatePool(var0);
            if (var2.size() <= 1) {
               FORM_OPTIONS.putIfAbsent(var1, List.of());
            } else if (PENDING_FORMS.size() < 24 && PENDING_FORMS.add(var1)) {
               long var3 = generation;
               FORM_WORKER.execute(
                  () -> {
                     long var4 = System.nanoTime();

                     try {
                        MinimapSpriteResolver.Resolution var6 = resolveAll(var0);
                        if (var3 == generation) {
                           if (!var6.sprites().isEmpty()) {
                              RESOLVED.put(var1, var6.sprites());
                           }

                           FORM_OPTIONS.put(var1, var6.options());
                           FORM_SPRITES.put(var1, var6.byForm());
                           long var7 = (System.nanoTime() - var4) / 1000000L;
                           if (var7 >= 20L || var6.options().size() >= 8) {
                              SessionDiagnostics.event(
                                 "sprite-forms-ready",
                                 "pokemon=" + var0.key() + " forms=" + var6.options().size() + " sprites=" + var6.sprites().size() + " elapsedMs=" + var7
                              );
                           }

                           return;
                        }
                     } catch (Throwable var12) {
                        SessionDiagnostics.error("sprite-form-resolve", var12);
                        return;
                     } finally {
                        PENDING_FORMS.remove(var1);
                     }
                  }
               );
            }
         }
      }
   }

   private static MinimapSpriteResolver.Resolution resolveAll(SpeciesInfo var0) {
      List<MinimapSpriteResolver.Candidate> var1 = matchingFormCandidates(var0);
      if (var1.isEmpty()) {
         return new MinimapSpriteResolver.Resolution(List.of(), List.of(), Map.of());
      }

      List<MinimapSpriteResolver.Candidate> var2 = matchingCandidates(var0);
      List<MinimapSpriteResolver.SpriteRef> var3 = new ArrayList<>(var2.size());

      for (MinimapSpriteResolver.Candidate var5 : var2) {
         MinimapSpriteResolver.Dimensions var6 = DIMENSIONS.computeIfAbsent(var5.id, var1x -> readDimensions(var5.resource));
         var3.add(new MinimapSpriteResolver.SpriteRef(var5.id, var6.width, var6.height));
      }

      Map<String, MinimapSpriteResolver.FormSpriteOption> var13 = new LinkedHashMap<>();
      Map<String, MinimapSpriteResolver.SpriteRef> var14 = new LinkedHashMap<>();

      for (MinimapSpriteResolver.Candidate var7 : var1) {
         MinimapSpriteResolver.Dimensions var9 = DIMENSIONS.computeIfAbsent(var7.id, var1x -> readDimensions(var7.resource));
         MinimapSpriteResolver.SpriteRef var10 = new MinimapSpriteResolver.SpriteRef(var7.id, var9.width, var9.height);
         String var11 = rawSuffixAfterSpecies(var7.rawBase, var0);
         if (var11 != null && var11.isBlank()) {
            var14.putIfAbsent("__base__", var10);
         } else {
            String var8;
            if (var11 != null && !var11.isBlank() && !(var8 = SpeciesInfo.normalize(var11)).isBlank()) {
               String var12 = friendlySpriteFormLabel(var0, var11);
               var13.putIfAbsent(var8, new MinimapSpriteResolver.FormSpriteOption(var8, var12));
               var14.putIfAbsent(var8, var10);
            }
         }
      }

      return new MinimapSpriteResolver.Resolution(List.copyOf(var3), List.copyOf(var13.values()), Map.copyOf(var14));
   }

   private static List<MinimapSpriteResolver.Candidate> matchingCandidates(SpeciesInfo var0) {
      if (var0 == null) {
         return List.of();
      }

      String var1 = SpeciesInfo.normalize(var0.key());
      if (var1.isBlank()) {
         return List.of();
      }

      int var2 = var0.nationalPokedexNumber();
      String var3 = var2 == Integer.MAX_VALUE ? "" : Integer.toString(var2);
      String var4 = var2 == Integer.MAX_VALUE ? "" : String.format(Locale.ROOT, "%03d", var2);
      String var5 = var2 == Integer.MAX_VALUE ? "" : String.format(Locale.ROOT, "%04d", var2);
      return candidatePool(var0)
         .stream()
         .map(var4x -> new MinimapSpriteResolver.ScoredCandidate(var4x, score(var4x, var1, var3, var4, var5)))
         .filter(var0x -> var0x.score < Integer.MAX_VALUE)
         .filter(var0x -> !isShiny(var0x.candidate))
         .filter(var5x -> isAllowedCycleSprite(var5x.candidate, var0, var1, var3, var4, var5))
         .sorted(Comparator.comparingInt(MinimapSpriteResolver.ScoredCandidate::score).thenComparing(var0x -> var0x.candidate.id.toString()))
         .map(MinimapSpriteResolver.ScoredCandidate::candidate)
         .distinct()
         .toList();
   }

   private static List<MinimapSpriteResolver.Candidate> matchingFormCandidates(SpeciesInfo var0) {
      if (var0 == null) {
         return List.of();
      }

      String var1 = SpeciesInfo.normalize(var0.key());
      if (var1.isBlank()) {
         return List.of();
      }

      int var2 = var0.nationalPokedexNumber();
      String var3 = var2 == Integer.MAX_VALUE ? "" : Integer.toString(var2);
      String var4 = var2 == Integer.MAX_VALUE ? "" : String.format(Locale.ROOT, "%03d", var2);
      String var5 = var2 == Integer.MAX_VALUE ? "" : String.format(Locale.ROOT, "%04d", var2);
      return candidatePool(var0)
         .stream()
         .map(var4x -> new MinimapSpriteResolver.ScoredCandidate(var4x, score(var4x, var1, var3, var4, var5)))
         .filter(var0x -> var0x.score < Integer.MAX_VALUE)
         .filter(var0x -> !isShiny(var0x.candidate))
         .filter(var5x -> isAllowedFormSprite(var5x.candidate, var0, var1, var3, var4, var5))
         .sorted(Comparator.comparingInt(MinimapSpriteResolver.ScoredCandidate::score).thenComparing(var0x -> var0x.candidate.id.toString()))
         .map(MinimapSpriteResolver.ScoredCandidate::candidate)
         .distinct()
         .toList();
   }

   private static boolean isCollapsedVisualSpecies(SpeciesInfo var0) {
      return false;
   }

   private static String rawSuffixAfterSpecies(String var0, SpeciesInfo var1) {
      if (var0 != null && var1 != null) {
         String var4 = var0.toLowerCase(Locale.ROOT);
         int var5 = var1.nationalPokedexNumber();
         if (var5 != Integer.MAX_VALUE) {
            for (String var7 : List.of(String.format(Locale.ROOT, "%04d_", var5), String.format(Locale.ROOT, "%03d_", var5), var5 + "_")) {
               String var3 = var7;
               if (var4.startsWith(var3)) {
                  var4 = var4.substring(var3.length());
                  break;
               }
            }
         }

         String var9 = var1.key().toLowerCase(Locale.ROOT).replace('-', '_');
         if (var4.equals(var9)) {
            return "";
         }

         if (var4.startsWith(var9 + "_")) {
            return var4.substring(var9.length() + 1);
         }

         String var8 = SpeciesInfo.normalize(var4);
         String var2;
         return var8.startsWith(var2 = SpeciesInfo.normalize(var1.key())) && var8.length() > var2.length() ? var8.substring(var2.length()) : null;
      } else {
         return null;
      }
   }

   private static String friendlySpriteFormLabel(SpeciesInfo var0, String var1) {
      String var2 = var1 == null ? "" : var1.toLowerCase(Locale.ROOT).replace('-', '_');
      if ("floette".equals(SpeciesInfo.normalize(var0.key()))) {
         if (var2.equals("eternal")) {
            return "Eternal Flower";
         }

         if (var2.matches("red|yellow|orange|blue|white|pink|purple|black|green|cyan|brown")) {
            return prettyWords(var2) + " Flower";
         }
      }

      return prettyWords(var2);
   }

   private static String prettyWords(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.replace('_', ' ').replace('-', ' ');
         StringBuilder var2 = new StringBuilder(var1.length());
         boolean var3 = true;

         for (char var7 : var1.toCharArray()) {
            if (var3 && Character.isLetter(var7)) {
               var2.append(Character.toUpperCase(var7));
               var3 = false;
            } else {
               var2.append(var7);
            }

            if (var7 == ' ') {
               var3 = true;
            }
         }

         return var2.toString();
      } else {
         return "Form";
      }
   }

   private static boolean isUnwantedSpriteTree(String var0) {
      if (var0 == null) {
         return false;
      }

      String var1 = var0.toLowerCase(Locale.ROOT);
      return var1.contains("/sprite/shiny/")
         || var1.contains("/sprite/shiny_raw/")
         || var1.contains("/sprite/radiant/")
         || var1.contains("/sprite/regular_raw/")
         || var1.contains("/sprite/raw/");
   }

   private static boolean isAllowedCycleSprite(MinimapSpriteResolver.Candidate var0, SpeciesInfo var1, String var2, String var3, String var4, String var5) {
      String var6 = var0.normalizedBase;
      String var7 = suffixAfterSpecies(var6, var2, var3, var4, var5);
      if (var7 == null) {
         return false;
      }

      if (var7.isBlank()) {
         return true;
      }

      String var8 = var0.path == null ? "" : var0.path.toLowerCase(Locale.ROOT);
      return !var8.contains("/sprite/regular/")
         ? false
         : !var7.contains("shiny")
            && !var7.contains("radiant")
            && !var7.contains("bias")
            && !var7.endsWith("alt")
            && !var7.contains("terastallizes")
            && !var7.contains("overdrive");
   }

   private static boolean isAllowedFormSprite(MinimapSpriteResolver.Candidate var0, SpeciesInfo var1, String var2, String var3, String var4, String var5) {
      String var7 = suffixAfterSpecies(var0.normalizedBase, var2, var3, var4, var5);
      if (var7 == null) {
         return false;
      }

      if (var7.isBlank()) {
         return true;
      }

      String var6 = var0.path == null ? "" : var0.path.toLowerCase(Locale.ROOT);
      return !var6.contains("/sprite/regular/")
         ? false
         : !var7.contains("shiny") && !var7.contains("radiant") && !var7.endsWith("alt") && !var7.contains("terastallizes") && !var7.contains("overdrive");
   }

   private static String suffixAfterSpecies(String var0, String var1, String var2, String var3, String var4) {
      if (var0 != null && var1 != null && !var1.isBlank()) {
         for (String var6 : List.of(var4, var3, var2)) {
            String var7;
            if (var6 != null && !var6.isBlank() && var0.startsWith(var7 = var6 + var1)) {
               return var0.substring(var7.length());
            }
         }

         return var0.startsWith(var1) ? var0.substring(var1.length()) : null;
      } else {
         return null;
      }
   }

   private static boolean isShiny(MinimapSpriteResolver.Candidate var0) {
      String var1 = var0.path == null ? "" : var0.path;
      String var2 = var0.normalizedBase == null ? "" : var0.normalizedBase;
      return var1.contains("shiny") || var2.contains("shiny");
   }

   private static int score(MinimapSpriteResolver.Candidate var0, String var1, String var2, String var3, String var4) {
      String var7 = var0.normalizedBase;
      if (var7.isBlank()) {
         return Integer.MAX_VALUE;
      }

      boolean var8 = !var4.isBlank() && var7.startsWith(var4 + var1)
         || !var3.isBlank() && var7.startsWith(var3 + var1)
         || !var2.isBlank() && var7.startsWith(var2 + var1);
      boolean var9 = var0.packId.contains("cobble")
         || var0.packId.contains("pokemon")
         || var0.packId.contains("poke")
         || var0.packId.contains("e19")
         || var0.path.contains("/pokemon/")
         || var0.path.contains("/cobblemon/");
      if (!var8 && !var9) {
         return Integer.MAX_VALUE;
      }

      int var6;
      if (!var4.isBlank() && var7.equals(var4 + var1)) {
         var6 = 0;
      } else if (!var3.isBlank() && var7.equals(var3 + var1)) {
         var6 = 1;
      } else if (!var2.isBlank() && var7.equals(var2 + var1)) {
         var6 = 2;
      } else if (var7.equals(var1)) {
         var6 = 8;
      } else if ((var4.isBlank() || !var7.startsWith(var4 + var1))
         && (var3.isBlank() || !var7.startsWith(var3 + var1))
         && (var2.isBlank() || !var7.startsWith(var2 + var1))) {
         if (var7.startsWith(var1)) {
            var6 = 24 + Math.max(0, var7.length() - var1.length());
         } else {
            if (!var7.endsWith(var1)) {
               return Integer.MAX_VALUE;
            }

            String var5 = var7.substring(0, var7.length() - var1.length());
            var6 = !var5.isBlank() && var5.chars().allMatch(Character::isDigit) ? 5 + var5.length() : 30 + var5.length();
         }
      } else {
         var6 = 18 + Math.max(0, var7.length() - var1.length());
      }

      String var11 = var0.path;
      if (var11.contains("/pokemon/")) {
         var6 -= 2;
      }

      if (var0.packId.contains("cobble") || var0.packId.contains("pokemon") || var0.packId.contains("poke") || var0.packId.contains("e19")) {
         var6 -= 4;
      }

      if (var11.contains("mega") || var11.contains("gmax") || var11.contains("gigantamax")) {
         var6 += 500;
      }

      return Math.max(0, var6);
   }

   private static boolean isVanillaMobIcon(String var0, String var1) {
      String var2 = var0 == null ? "" : var0.toLowerCase(Locale.ROOT);
      String var3 = var1 == null ? "" : var1.toLowerCase(Locale.ROOT);
      return var2.contains("/minecraft/") || var2.contains("/vanilla/") || var3.equals("vanilla") || var3.contains("minecraft default");
   }

   private static MinimapSpriteResolver.Dimensions readDimensions(Resource var0) {
      try (InputStream var1 = var0.getInputStream()) {
         DataInputStream var5 = new DataInputStream(var1);

         MinimapSpriteResolver.Dimensions var17;
         label115: {
            int var3;
            int var4;
            try {
               byte[] var7 = new byte[16];
               var5.readFully(var7);
               var4 = var5.readInt();
               var3 = var5.readInt();
               if (var4 <= 0) {
                  var17 = new MinimapSpriteResolver.Dimensions(64, 64);
                  break label115;
               }
            } catch (Throwable var11) {
               try {
                  var5.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            if (var4 <= 4096) {
               if (var3 > 0) {
                  if (var3 <= 4096) {
                     var17 = new MinimapSpriteResolver.Dimensions(var4, var3);
                     var5.close();
                     return var17;
                  }

                  var17 = new MinimapSpriteResolver.Dimensions(64, 64);
                  var5.close();
                  return var17;
               }

               var17 = new MinimapSpriteResolver.Dimensions(64, 64);
               var5.close();
               return var17;
            }

            var17 = new MinimapSpriteResolver.Dimensions(64, 64);
            var5.close();
            return var17;
         }

         var5.close();
         return var17;
      } catch (IOException | RuntimeException var13) {
         return new MinimapSpriteResolver.Dimensions(64, 64);
      }
   }

   private static String normalize(String var0) {
      return var0 == null ? "" : var0.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
   }

   private record Candidate(Identifier id, Resource resource, String normalizedBase, String rawBase, String path, String packId) {
   }

   private record Dimensions(int width, int height) {
   }

   public record FormSpriteOption(String key, String label) {
   }

   private record Resolution(
      List<MinimapSpriteResolver.SpriteRef> sprites, List<MinimapSpriteResolver.FormSpriteOption> options, Map<String, MinimapSpriteResolver.SpriteRef> byForm
   ) {
   }

   private record ScoredCandidate(MinimapSpriteResolver.Candidate candidate, int score) {
   }

   public record SpriteRef(Identifier texture, int textureWidth, int textureHeight) {
   }
}
