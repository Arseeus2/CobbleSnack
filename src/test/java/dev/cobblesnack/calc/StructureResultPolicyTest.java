package dev.cobblesnack.calc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class StructureResultPolicyTest {
   @Test
   void presentsSuccessfulStructureEstimateForMixedRoutes() {
      assertTrue(StructureResultPolicy.shouldPresent(true, false, true));
   }

   @Test
   void preservesLegacyFallbackForStructureOnlyPokemon() {
      assertTrue(StructureResultPolicy.shouldPresent(false, true, true));
   }

   @Test
   void doesNotInventFallbackWithoutAUsableStructureRoute() {
      assertFalse(StructureResultPolicy.shouldPresent(true, false, false));
      assertFalse(StructureResultPolicy.shouldPresent(false, false, true));
   }
}
