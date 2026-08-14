package dev.cobblesnack.client;

import dev.cobblesnack.data.SpeciesInfo;

public final class FormDisplayNames {
   private FormDisplayNames() {
   }

   public static String baseFormLabel(SpeciesInfo var0) {
      String var1 = var0 == null ? "" : SpeciesInfo.normalize(var0.key());
      switch (var1) {
         case "castform":
            return "Normal";
         case "magikarp":
         case "magearna":
         case "ursaluna":
         case "whiscash":
            return "Regular";
         default:
            int var4 = var0 == null ? Integer.MAX_VALUE : var0.nationalPokedexNumber();
            if (var4 <= 151) {
               return "Kantonian";
            } else if (var4 <= 251) {
               return "Johtonian";
            } else if (var4 <= 386) {
               return "Hoennian";
            } else if (var4 <= 493) {
               return "Sinnohan";
            } else if (var4 <= 649) {
               return "Unovan";
            } else if (var4 <= 721) {
               return "Kalosian";
            } else if (var4 <= 809) {
               return "Alolan";
            } else if (var4 >= 899 && var4 <= 905) {
               return "Hisuian";
            } else if (var4 <= 905) {
               return "Galarian";
            } else {
               return var4 <= 1025 ? "Paldean" : "Original";
            }
      }
   }

   public static String formLabel(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = SpeciesInfo.normalize(var0).replace("form", "");

         return switch (var1) {
            case "kanto", "kantonian" -> "Kantonian";
            case "johto", "johtonian" -> "Johtonian";
            case "hoenn", "hoennian" -> "Hoennian";
            case "sinnoh", "sinnohan" -> "Sinnohan";
            case "unova", "unovan" -> "Unovan";
            case "kalos", "kalosian" -> "Kalosian";
            case "alola", "alolan" -> "Alolan";
            case "galar", "galarian" -> "Galarian";
            case "hisui", "hisuian" -> "Hisuian";
            case "paldea", "paldean" -> "Paldean";
            default -> var0;
         };
      } else {
         return var0;
      }
   }

   public static String dropdownLabel(String var0) {
      return var0 == null ? "" : var0.replaceFirst("(?i)\\s+form\\s*$", "").trim();
   }

   public static String formLabel(SpeciesInfo var0, String var1, String var2) {
      String var3 = var0 == null ? "" : SpeciesInfo.normalize(var0.key());
      String var4 = SpeciesInfo.normalize(var1);
      String var5 = SpeciesInfo.normalize(var2);
      String var6 = var4 + var5;
      if ("pikachu".equals(var3) && var6.contains("cosplay")) {
         if (var6.contains("rockstar")) {
            return "Rock Star";
         } else if (var6.contains("popstar")) {
            return "Pop Star";
         } else if (var6.contains("phd")) {
            return "PhD";
         } else if (var6.contains("libre")) {
            return "Libre";
         } else {
            return var6.contains("belle") ? "Belle" : "Cosplay";
         }
      } else {
         if ("indeedee".equals(var3)) {
            if (var6.contains("female")) {
               return "Female";
            }

            if (var6.contains("male")) {
               return "Male";
            }
         }

         if ("maushold".equals(var3)) {
            if (var6.contains("three")) {
               return "Family of Three";
            }

            if (var6.contains("four")) {
               return "Family of Four";
            }
         }

         if ("tympole".equals(var3) && var6.contains("paldea")) {
            return "Paldean";
         }

         if ("whiscash".equals(var3) && var6.contains("nero")) {
            return "Nero";
         }

         if ("wooper".equals(var3) && var6.contains("heart")) {
            return "Heart";
         }

         if ("basculin".equals(var3)) {
            if (var6.contains("red")) {
               return "Red-Striped Form";
            }

            if (var6.contains("blue")) {
               return "Blue-Striped Form";
            }

            if (var6.contains("white")) {
               return "White-Striped Form";
            }
         }

         if ("burmy".equals(var3) || "wormadam".equals(var3)) {
            if (var6.contains("plant")) {
               return "Plant Cloak";
            }

            if (var6.contains("sandy")) {
               return "Sandy Cloak";
            }

            if (var6.contains("trash")) {
               return "Trash Cloak";
            }
         }

         if ("cherrim".equals(var3)) {
            if (var6.contains("overcast")) {
               return "Overcast Form";
            }

            if (var6.contains("sunshine")) {
               return "Sunshine Form";
            }
         }

         if ("castform".equals(var3)) {
            if (var6.contains("rainy")) {
               return "Rainy Form";
            }

            if (var6.contains("snowy")) {
               return "Snowy Form";
            }

            if (var6.contains("sunny")) {
               return "Sunny Form";
            }
         }

         if ("dudunsparce".equals(var3)) {
            if (var6.contains("three")) {
               return "Three-Segment Form";
            }

            if (var6.contains("two")) {
               return "Two-Segment Form";
            }
         }

         if ("flabebe".equals(var3) || "floette".equals(var3) || "florges".equals(var3)) {
            if (var6.contains("eternal")) {
               return "Eternal Flower";
            }

            for (String var10 : new String[]{"red", "orange", "yellow", "blue", "white"}) {
               if (var6.contains(var10)) {
                  return Character.toUpperCase(var10.charAt(0)) + var10.substring(1) + " Flower";
               }
            }
         }

         if ("shellos".equals(var3) || "gastrodon".equals(var3)) {
            if (var6.contains("east")) {
               return "East Sea";
            }

            if (var6.contains("west")) {
               return "West Sea";
            }
         }

         if ("gimmighoul".equals(var3)) {
            if (var6.contains("chest")) {
               return "Chest Form";
            }

            if (var6.contains("roaming")) {
               return "Roaming Form";
            }
         }

         if ("keldeo".equals(var3)) {
            if (var6.contains("ordinary")) {
               return "Ordinary Form";
            }

            if (var6.contains("resolute")) {
               return "Resolute Form";
            }
         }

         if ("lycanroc".equals(var3)) {
            if (var6.contains("midday")) {
               return "Midday Form";
            }

            if (var6.contains("midnight")) {
               return "Midnight Form";
            }

            if (var6.contains("dusk")) {
               return "Dusk Form";
            }
         }

         if ("magearna".equals(var3) && var6.contains("original")) {
            return "Original Color";
         }

         if ("minior".equals(var3)) {
            if (var6.contains("meteor") && !var6.contains("core")) {
               return "Meteor Form";
            }

            for (String var14 : new String[]{"red", "orange", "yellow", "green", "blue", "indigo", "violet"}) {
               if (var6.contains(var14)) {
                  return Character.toUpperCase(var14.charAt(0)) + var14.substring(1) + " Core";
               }
            }
         }

         if ("oricorio".equals(var3)) {
            if (var6.contains("baile")) {
               return "Baile Style";
            }

            if (var6.contains("pompom")) {
               return "Pom-Pom Style";
            }

            if (var6.contains("pau")) {
               return "Pa'u Style";
            }

            if (var6.contains("sensu")) {
               return "Sensu Style";
            }
         }

         if ("sinistea".equals(var3) || "polteageist".equals(var3)) {
            if (var6.contains("phony")) {
               return "Phony Form";
            }

            if (var6.contains("antique")) {
               return "Antique Form";
            }
         }

         if ("poltchageist".equals(var3)) {
            if (var6.contains("counterfeit")) {
               return "Counterfeit Form";
            }

            if (var6.contains("artisan")) {
               return "Artisan Form";
            }
         }

         if ("sinistcha".equals(var3)) {
            if (var6.contains("unremarkable")) {
               return "Unremarkable Form";
            }

            if (var6.contains("masterpiece")) {
               return "Masterpiece Form";
            }
         }

         if ("tauros".equals(var3) && var6.contains("paldea")) {
            if (var6.contains("combat")) {
               return "Paldean Combat Breed";
            }

            if (var6.contains("blaze")) {
               return "Paldean Blaze Breed";
            }

            if (var6.contains("aqua")) {
               return "Paldean Aqua Breed";
            }
         }

         if ("ursaluna".equals(var3) && var6.contains("bloodmoon")) {
            return "Bloodmoon Form";
         } else if ("vivillon".equals(var3) && !var5.isBlank()) {
            return var2.endsWith(" Pattern") ? var2 : var2 + " Pattern";
         } else {
            return formLabel(var2);
         }
      }
   }
}
