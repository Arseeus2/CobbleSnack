package dev.cobblesnack.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import dev.cobblesnack.data.SpawnCondition;
import dev.cobblesnack.data.SpawnEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

class HabitatPolicyTest {
   @Test
   void rejectsVisibleSkyInCaveBiomes() {
      assertTrue(HabitatPolicy.hasCaveSkyConflict(true, entryWith("canSeeSky", true)));
      assertFalse(HabitatPolicy.hasCaveSkyConflict(false, entryWith("canSeeSky", true)));
      assertFalse(HabitatPolicy.hasCaveSkyConflict(true, entryWith("canSeeSky", false)));
   }

   @Test
   void rejectsBrightSkyLightButNotOrdinaryBlockLight() {
      assertTrue(HabitatPolicy.hasCaveSkyConflict(true, entryWith("minSkyLight", 8)));
      assertFalse(HabitatPolicy.hasCaveSkyConflict(true, entryWith("minSkyLight", 7)));
      assertFalse(HabitatPolicy.hasCaveSkyConflict(true, entryWith("minLight", 8)));
   }

   @Test
   void rejectsOnlyGroundedRoutesFromOceanBiomes() {
      assertTrue(HabitatPolicy.hasGroundedOceanConflict(true, "grounded"));
      assertFalse(HabitatPolicy.hasGroundedOceanConflict(true, "seafloor"));
      assertFalse(HabitatPolicy.hasGroundedOceanConflict(true, "submerged"));
      assertFalse(HabitatPolicy.hasGroundedOceanConflict(false, "grounded"));

      assertEquals(0, HabitatPolicy.rankForSignals(true, true, false, true, true, "grounded", false, false));
      assertEquals(0, HabitatPolicy.rankForSignals(true, true, false, true, true, "seafloor", true, false));
      assertEquals(0, HabitatPolicy.rankForSignals(true, true, false, true, true, "submerged", true, false));
   }

   private static SpawnEntry entryWith(String var0, boolean var1) {
      JsonObject var2 = new JsonObject();
      var2.addProperty(var0, var1);
      return entryWith(var2);
   }

   private static SpawnEntry entryWith(String var0, int var1) {
      JsonObject var2 = new JsonObject();
      var2.addProperty(var0, var1);
      return entryWith(var2);
   }

   private static SpawnEntry entryWith(JsonObject var0) {
      return new SpawnEntry(
         "test-route",
         "testmon",
         "testmon",
         "grounded",
         "common",
         1.0,
         List.of(SpawnCondition.from(var0)),
         List.of(),
         List.of()
      );
   }
}
