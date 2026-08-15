package dev.cobblesnack.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class DataIndexSpawnVariantTest {
   @Test
   void minimumPerfectIvsRemainSpawnMetadataInsteadOfBecomingAForm() throws Exception {
      Object variant = spawnVariant("slitherwing min_perfect_ivs=3");

      assertEquals("__base__", component(variant, "key"));
      assertEquals("Base form", component(variant, "label"));
   }

   @Test
   void minimumPerfectIvsDoNotReplaceARealFormProperty() throws Exception {
      Object variant = spawnVariant("magearna paint_color=original min_perfect_ivs=3");

      assertEquals("Original", component(variant, "label"));
   }

   private static Object spawnVariant(String expression) throws Exception {
      Method method = DataIndex.class.getDeclaredMethod("spawnVariant", SpeciesInfo.class, String.class);
      method.setAccessible(true);
      return method.invoke(null, null, expression);
   }

   private static Object component(Object record, String name) throws Exception {
      Method method = record.getClass().getDeclaredMethod(name);
      method.setAccessible(true);
      return method.invoke(record);
   }
}
