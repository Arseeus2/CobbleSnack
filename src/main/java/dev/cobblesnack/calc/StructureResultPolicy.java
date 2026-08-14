package dev.cobblesnack.calc;

public final class StructureResultPolicy {
   private StructureResultPolicy() {
   }

   public static boolean shouldPresent(boolean estimateAvailable, boolean allRoutesRequireStructure, boolean hasUsableStructureRoutes) {
      return hasUsableStructureRoutes && (estimateAvailable || allRoutesRequireStructure);
   }
}
