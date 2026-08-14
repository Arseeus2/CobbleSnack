package dev.cobblesnack.calc;

import dev.cobblesnack.data.SpeciesInfo;
import java.util.List;
import java.util.Locale;

public enum Seasoning {
   NONE("None", Seasoning.Kind.NONE, List.of(), null, 0, 0.0),
   POMEG("Pomeg Berry (HP)", Seasoning.Kind.EV_FILTER, List.of(), "hp", 0, 0.0),
   KELPSY("Kelpsy Berry (Attack)", Seasoning.Kind.EV_FILTER, List.of(), "attack", 0, 0.0),
   QUALOT("Qualot Berry (Defense)", Seasoning.Kind.EV_FILTER, List.of(), "defence", 0, 0.0),
   HONDEW("Hondew Berry (Sp. Atk)", Seasoning.Kind.EV_FILTER, List.of(), "special_attack", 0, 0.0),
   GREPA("Grepa Berry (Sp. Def)", Seasoning.Kind.EV_FILTER, List.of(), "special_defence", 0, 0.0),
   TAMATO("Tamato Berry (Speed)", Seasoning.Kind.EV_FILTER, List.of(), "speed", 0, 0.0),
   TANGA("Tanga Berry (Bug)", Seasoning.Kind.TYPE, List.of("bug"), null, 0, 0.0),
   COLBUR("Colbur Berry (Dark)", Seasoning.Kind.TYPE, List.of("dark"), null, 0, 0.0),
   HABAN("Haban Berry (Dragon)", Seasoning.Kind.TYPE, List.of("dragon"), null, 0, 0.0),
   WACAN("Wacan Berry (Electric)", Seasoning.Kind.TYPE, List.of("electric"), null, 0, 0.0),
   ROSELI("Roseli Berry (Fairy)", Seasoning.Kind.TYPE, List.of("fairy"), null, 0, 0.0),
   CHOPLE("Chople Berry (Fighting)", Seasoning.Kind.TYPE, List.of("fighting"), null, 0, 0.0),
   OCCA("Occa Berry (Fire)", Seasoning.Kind.TYPE, List.of("fire"), null, 0, 0.0),
   COBA("Coba Berry (Flying)", Seasoning.Kind.TYPE, List.of("flying"), null, 0, 0.0),
   KASIB("Kasib Berry (Ghost)", Seasoning.Kind.TYPE, List.of("ghost"), null, 0, 0.0),
   RINDO("Rindo Berry (Grass)", Seasoning.Kind.TYPE, List.of("grass"), null, 0, 0.0),
   SHUCA("Shuca Berry (Ground)", Seasoning.Kind.TYPE, List.of("ground"), null, 0, 0.0),
   YACHE("Yache Berry (Ice)", Seasoning.Kind.TYPE, List.of("ice"), null, 0, 0.0),
   CHILAN("Chilan Berry (Normal)", Seasoning.Kind.TYPE, List.of("normal"), null, 0, 0.0),
   KEBIA("Kebia Berry (Poison)", Seasoning.Kind.TYPE, List.of("poison"), null, 0, 0.0),
   PAYAPA("Payapa Berry (Psychic)", Seasoning.Kind.TYPE, List.of("psychic"), null, 0, 0.0),
   CHARTI("Charti Berry (Rock)", Seasoning.Kind.TYPE, List.of("rock"), null, 0, 0.0),
   BABIRI("Babiri Berry (Steel)", Seasoning.Kind.TYPE, List.of("steel"), null, 0, 0.0),
   PASSHO("Passho Berry (Water)", Seasoning.Kind.TYPE, List.of("water"), null, 0, 0.0),
   LUM("Lum Berry (Dragon / Monster)", Seasoning.Kind.EGG_GROUP, List.of("dragon", "monster"), null, 0, 0.0),
   PECHA("Pecha Berry (Water 3 / Bug)", Seasoning.Kind.EGG_GROUP, List.of("water_3", "bug"), null, 0, 0.0),
   CHERI("Cheri Berry (Fairy / Grass)", Seasoning.Kind.EGG_GROUP, List.of("fairy", "grass"), null, 0, 0.0),
   CHESTO("Chesto Berry (Human-Like / Flying)", Seasoning.Kind.EGG_GROUP, List.of("human_like", "flying"), null, 0, 0.0),
   RAWST("Rawst Berry (Field)", Seasoning.Kind.EGG_GROUP, List.of("field"), null, 0, 0.0),
   ASPEAR("Aspear Berry (Water 1 / Water 2)", Seasoning.Kind.EGG_GROUP, List.of("water_1", "water_2"), null, 0, 0.0),
   PERSIM("Persim Berry (Mineral / Amorphous)", Seasoning.Kind.EGG_GROUP, List.of("mineral", "amorphous"), null, 0, 0.0),
   RAZZ("Razz Berry (Attack 25%)", Seasoning.Kind.NATURE, List.of("attack", "25"), null, 0, 0.0),
   FIGY("Figy Berry (Attack 50%)", Seasoning.Kind.NATURE, List.of("attack", "50"), null, 0, 0.0),
   TOUGA("Touga Berry (Attack 75%)", Seasoning.Kind.NATURE, List.of("attack", "75"), null, 0, 0.0),
   SPELON("Spelon Berry (Attack 100%)", Seasoning.Kind.NATURE, List.of("attack", "100"), null, 0, 0.0),
   PINAP("Pinap Berry (Defense 25%)", Seasoning.Kind.NATURE, List.of("defense", "25"), null, 0, 0.0),
   IAPAPA("Iapapa Berry (Defense 50%)", Seasoning.Kind.NATURE, List.of("defense", "50"), null, 0, 0.0),
   NOMEL("Nomel Berry (Defense 75%)", Seasoning.Kind.NATURE, List.of("defense", "75"), null, 0, 0.0),
   BELUE("Belue Berry (Defense 100%)", Seasoning.Kind.NATURE, List.of("defense", "100"), null, 0, 0.0),
   BLUK("Bluk Berry (Sp. Atk 25%)", Seasoning.Kind.NATURE, List.of("special attack", "25"), null, 0, 0.0),
   WIKI("Wiki Berry (Sp. Atk 50%)", Seasoning.Kind.NATURE, List.of("special attack", "50"), null, 0, 0.0),
   CORNN("Cornn Berry (Sp. Atk 75%)", Seasoning.Kind.NATURE, List.of("special attack", "75"), null, 0, 0.0),
   PAMTRE("Pamtre Berry (Sp. Atk 100%)", Seasoning.Kind.NATURE, List.of("special attack", "100"), null, 0, 0.0),
   WEPEAR("Wepear Berry (Sp. Def 25%)", Seasoning.Kind.NATURE, List.of("special defense", "25"), null, 0, 0.0),
   AGUAV("Aguav Berry (Sp. Def 50%)", Seasoning.Kind.NATURE, List.of("special defense", "50"), null, 0, 0.0),
   RABUTA("Rabuta Berry (Sp. Def 75%)", Seasoning.Kind.NATURE, List.of("special defense", "75"), null, 0, 0.0),
   DURIN("Durin Berry (Sp. Def 100%)", Seasoning.Kind.NATURE, List.of("special defense", "100"), null, 0, 0.0),
   NANAB("Nanab Berry (Speed 25%)", Seasoning.Kind.NATURE, List.of("speed", "25"), null, 0, 0.0),
   MAGO("Mago Berry (Speed 50%)", Seasoning.Kind.NATURE, List.of("speed", "50"), null, 0, 0.0),
   MAGOST("Magost Berry (Speed 75%)", Seasoning.Kind.NATURE, List.of("speed", "75"), null, 0, 0.0),
   WATMEL("Watmel Berry (Speed 100%)", Seasoning.Kind.NATURE, List.of("speed", "100"), null, 0, 0.0),
   ENCHANTED_GOLDEN_APPLE("Enchanted Golden Apple (Rarity +10 / Bite -10% / Shiny +9 rolls)", Seasoning.Kind.RARITY, List.of(), null, 10, 0.1),
   GOLDEN_APPLE("Golden Apple (Rarity +1 / Bite -25% / Shiny +1 roll)", Seasoning.Kind.RARITY, List.of(), null, 1, 0.25),
   GOLDEN_CARROT("Golden Carrot (Rarity +1)", Seasoning.Kind.RARITY, List.of(), null, 1, 0.0),
   GLISTERING_MELON("Glistering Melon Slice (Rarity +1)", Seasoning.Kind.RARITY, List.of(), null, 1, 0.0),
   APPLE("Apple (Bite -50%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.5),
   SITRUS("Sitrus Berry (Bite -50%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.5),
   ORAN("Oran Berry (Bite -33%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.33),
   GLOW_BERRIES("Glow Berries (Bite -25%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.25),
   CUSTAP("Custap Berry (70% Pokémon when fishing / Bite -25%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.25),
   MICLE("Micle Berry (100% Pokémon when fishing / Bite -25%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.25),
   SWEET_BERRIES("Sweet Berries (Bite -12.5%)", Seasoning.Kind.BITE_TIME, List.of(), null, 0, 0.125),
   LEPPA("Leppa Berry (Level +5)", Seasoning.Kind.LEVEL, List.of(), null, 0, 0.0),
   HOPO("Hopo Berry (Level +10)", Seasoning.Kind.LEVEL, List.of(), null, 0, 0.0),
   STARF("Starf Berry (Shiny +4 rolls)", Seasoning.Kind.SHINY, List.of(), null, 0, 0.0),
   ENIGMA("Enigma Berry (Hidden Ability 5%)", Seasoning.Kind.HIDDEN_ABILITY, List.of(), null, 0, 0.0),
   JABOCA("Jaboca Berry (Friendship +100)", Seasoning.Kind.FRIENDSHIP, List.of(), null, 0, 0.0),
   KEE("Kee Berry (Female 25%)", Seasoning.Kind.GENDER, List.of("female"), null, 0, 0.0),
   MARANGA("Maranga Berry (Male 25%)", Seasoning.Kind.GENDER, List.of("male"), null, 0, 0.0),
   ROWAP("Rowap Berry (Drops reroll x1)", Seasoning.Kind.DROP_REROLL, List.of(), null, 0, 0.0),
   LANSAT("Lansat Berry (HP IV +5)", Seasoning.Kind.IV_BOOST, List.of("hp"), null, 0, 0.0),
   LIECHI("Liechi Berry (Attack IV +5)", Seasoning.Kind.IV_BOOST, List.of("attack"), null, 0, 0.0),
   GANLON("Ganlon Berry (Defense IV +5)", Seasoning.Kind.IV_BOOST, List.of("defense"), null, 0, 0.0),
   PETAYA("Petaya Berry (Sp. Atk IV +5)", Seasoning.Kind.IV_BOOST, List.of("special attack"), null, 0, 0.0),
   APICOT("Apicot Berry (Sp. Def IV +5)", Seasoning.Kind.IV_BOOST, List.of("special defense"), null, 0, 0.0),
   SALAC("Salac Berry (Speed IV +5)", Seasoning.Kind.IV_BOOST, List.of("speed"), null, 0, 0.0),
   EGGANT("Eggant Berry (Color only)", Seasoning.Kind.OTHER, List.of(), null, 0, 0.0);

   public final String displayName;
   public final Seasoning.Kind kind;
   public final List<String> targets;
   public final String evStat;
   public final int rarityTierBoost;
   public final double biteTimeReduction;

   Seasoning(String nullxx, Seasoning.Kind nullxxx, List<String> nullxxxx, String nullxxxxx, int nullxxxxxx, double nullxxxxxxx) {
      this.displayName = nullxx;
      this.kind = nullxxx;
      this.targets = nullxxxx;
      this.evStat = nullxxxxx;
      this.rarityTierBoost = nullxxxxxx;
      this.biteTimeReduction = nullxxxxxxx;
   }

   public boolean matches(SpeciesInfo.ResolvedTraits var1) {
      if (var1 == null) {
         return false;
      }

      if (this.kind == Seasoning.Kind.TYPE) {
         for (String var3 : this.targets) {
            if (var1.hasType(var3)) {
               return true;
            }
         }
      } else if (this.kind == Seasoning.Kind.EGG_GROUP) {
         for (String var5 : this.targets) {
            if (var1.hasEggGroup(var5)) {
               return true;
            }
         }
      } else if (this.kind == Seasoning.Kind.EV_FILTER) {
         return this.evStat != null && var1.evYieldFor(this.evStat) > 0;
      }

      return false;
   }

   public Seasoning.PickerGroup pickerGroup() {
      return switch (this.kind) {
         case NONE -> Seasoning.PickerGroup.ALL;
         case EV_FILTER -> Seasoning.PickerGroup.EV;
         case TYPE -> Seasoning.PickerGroup.TYPE;
         case EGG_GROUP -> Seasoning.PickerGroup.EGG_GROUP;
         case NATURE -> Seasoning.PickerGroup.NATURE;
         case RARITY, BITE_TIME, LEVEL -> Seasoning.PickerGroup.SPAWN;
         default -> Seasoning.PickerGroup.OTHER;
      };
   }

   public String searchText() {
      StringBuilder var1 = new StringBuilder(this.displayName).append(' ').append(this.kind.name());
      if (this.evStat != null) {
         var1.append(' ').append(this.evStat);
      }

      for (String var3 : this.targets) {
         var1.append(' ').append(var3);
      }

      return var1.toString().toLowerCase(Locale.ROOT).replace('_', ' ');
   }

   public String itemId() {
      return switch (this) {
         case NONE -> null;
         case ENCHANTED_GOLDEN_APPLE -> "minecraft:enchanted_golden_apple";
         case GOLDEN_APPLE -> "minecraft:golden_apple";
         case GOLDEN_CARROT -> "minecraft:golden_carrot";
         case GLISTERING_MELON -> "minecraft:glistering_melon_slice";
         case APPLE -> "minecraft:apple";
         case GLOW_BERRIES -> "minecraft:glow_berries";
         case SWEET_BERRIES -> "minecraft:sweet_berries";
         default -> "cobblemon:" + this.name().toLowerCase(Locale.ROOT) + "_berry";
      };
   }

   public boolean reducesBiteTime() {
      return this.biteTimeReduction > 0.0;
   }

   public int shinyBoost() {
      return switch (this) {
         case ENCHANTED_GOLDEN_APPLE -> 10;
         case GOLDEN_APPLE -> 2;
         case STARF -> 5;
         default -> 0;
      };
   }

   public int shinyRerolls() {
      return Math.max(0, this.shinyBoost() - 1);
   }

   public static int totalShinyMultiplier(List<Seasoning> var0) {
      if (var0 != null && !var0.isEmpty()) {
         int var1 = 1;

         for (Seasoning var3 : var0) {
            if (var3 != null) {
               var1 += var3.shinyRerolls();
            }
         }

         return var1;
      } else {
         return 1;
      }
   }

   public static double expectedBiteTimeReduction(List<Seasoning> var0) {
      if (var0 != null && !var0.isEmpty()) {
         double var1 = 0.0;
         int var3 = 0;

         for (Seasoning var5 : var0) {
            if (var5 != null && var5.reducesBiteTime()) {
               var1 += var5.biteTimeReduction;
               var3++;
            }
         }

         return var3 == 0 ? 0.0 : var1 / var3;
      } else {
         return 0.0;
      }
   }

   public enum Kind {
      NONE,
      EV_FILTER,
      TYPE,
      EGG_GROUP,
      NATURE,
      RARITY,
      BITE_TIME,
      LEVEL,
      SHINY,
      HIDDEN_ABILITY,
      FRIENDSHIP,
      GENDER,
      DROP_REROLL,
      IV_BOOST,
      OTHER;
   }

   public enum PickerGroup {
      ALL,
      EV,
      TYPE,
      EGG_GROUP,
      NATURE,
      SPAWN,
      OTHER;
   }
}
