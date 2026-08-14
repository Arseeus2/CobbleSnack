package dev.cobblesnack.calc;

import java.util.Map;

public final class BucketOdds {
   private static final Map<Integer, double[]> TABLE = Map.ofEntries(
      Map.entry(0, new double[]{0.862, 0.1028, 0.0251, 0.0101}),
      Map.entry(1, new double[]{0.771, 0.1501, 0.0538, 0.0251}),
      Map.entry(2, new double[]{0.6784, 0.1873, 0.0884, 0.0459}),
      Map.entry(3, new double[]{0.5937, 0.2131, 0.1235, 0.0697}),
      Map.entry(10, new double[]{0.2848, 0.2408, 0.2732, 0.2013}),
      Map.entry(11, new double[]{0.2651, 0.2383, 0.2836, 0.213}),
      Map.entry(12, new double[]{0.243, 0.2356, 0.2926, 0.2235}),
      Map.entry(20, new double[]{0.1729, 0.2162, 0.3334, 0.2776}),
      Map.entry(21, new double[]{0.1675, 0.2142, 0.3363, 0.282}),
      Map.entry(30, new double[]{0.1358, 0.2009, 0.3533, 0.31})
   );

   private BucketOdds() {
   }

   public static double oddsFor(String bucket, int tier) {
      double[] row = TABLE.getOrDefault(tier, TABLE.get(0));

      return switch (normalizeBucket(bucket)) {
         case "common" -> row[0];
         case "uncommon" -> row[1];
         case "rare" -> row[2];
         case "ultrarare" -> row[3];
         default -> 0.0;
      };
   }

   public static String normalizeBucket(String bucket) {
      return bucket == null ? "common" : bucket.toLowerCase().replace("-", "").replace("_", "").replace(" ", "");
   }
}
