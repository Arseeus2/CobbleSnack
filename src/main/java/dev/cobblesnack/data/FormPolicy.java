package dev.cobblesnack.data;

public final class FormPolicy {
   private FormPolicy() {
   }

   public static boolean collapseSpawnForms(DataIndex var0, String var1) {
      if (var0 == null) {
         return false;
      }

      SpeciesInfo var2 = var0.findSpecies(var1);
      return var2 != null && "unown".equals(var2.key());
   }
}
