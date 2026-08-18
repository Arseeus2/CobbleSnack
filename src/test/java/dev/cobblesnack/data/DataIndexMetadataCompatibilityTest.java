package dev.cobblesnack.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataIndexMetadataCompatibilityTest {
   @Test
   void acceptsBothWeightMultiplierSpellingsAndDirectSelectors() {
      JsonObject route = object(
         """
         {
           "weightMultiplier": {"multiplier": 2, "condition": {"biomes": ["minecraft:plains"]}},
           "weightMultipliers": [{"multiplier": 5, "biomes": ["minecraft:ocean"]}]
         }
         """
      );

      List<SpawnEntry.WeightMultiplier> multipliers = DataIndex.parseWeightMultipliers(route);

      assertEquals(2, multipliers.size());
      assertEquals(2.0, multipliers.get(0).multiplier());
      assertEquals("minecraft:plains", multipliers.get(0).conditions().getFirst().biomes.getFirst());
      assertEquals(5.0, multipliers.get(1).multiplier());
      assertEquals("minecraft:ocean", multipliers.get(1).conditions().getFirst().biomes.getFirst());
   }

   @Test
   void convertsDirectRouteRequirementsIntoACondition() {
      SpawnCondition condition = DataIndex.directConditions(
            object("{\"canSeeSky\": false, \"timeRange\": \"night\", \"neededNearbyBlocks\": [\"minecraft:water\"]}")
         )
         .getFirst();

      assertEquals(Boolean.FALSE, condition.canSeeSky);
      assertEquals("night", condition.timeRange);
      assertEquals(List.of("minecraft:water"), condition.neededNearbyBlocks);
   }

   @Test
   void acceptsBothEnabledSpellings() {
      assertFalse(DataIndex.isEnabled(object("{\"enable\": false}")));
      assertFalse(DataIndex.isEnabled(object("{\"enabled\": false, \"enable\": true}")));
      assertTrue(DataIndex.isEnabled(object("{\"enable\": true}")));
   }

   @Test
   void retainsFishingAndSpecialWorldRequirements() {
      SpawnCondition condition = SpawnCondition.from(
         object(
            """
            {
              "moonPhase": 0,
              "isSlimeChunk": false,
              "minX": 10,
              "maxX": 20,
              "minLureLevel": 2,
              "bait": "cobblemon:poke_bait",
              "rodType": "cobblemon:good_rod"
            }
            """
         )
      );

      assertEquals(2, condition.minLureLevel);
      assertEquals(List.of("cobblemon:poke_bait"), condition.bait);
      assertTrue(condition.hasSpecialWorldRequirement());
      assertTrue(condition.hasFishingRequirement());
      assertTrue(condition.conciseSummaryParts().contains("Moon: Full"));
      assertTrue(condition.conciseSummaryParts().contains("Not a slime chunk"));
   }

   private static JsonObject object(String json) {
      return JsonParser.parseString(json).getAsJsonObject();
   }
}
