package de.jjedele.sbf.quantitative;

import de.jjedele.sbf.BloomFilter;
import de.jjedele.sbf.BloomFilterBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jeff on 16/05/16.
 */
public class FalsePositiveRateTest {

    @Test
    public void whenContinuouslyAddingElements_falsePositivesIncrease() {
        BloomFilter<String> filter = BloomFilterBuilder.get()
                .withSize(10000)
                .buildFilter();
        final int batchSize = 1000;
        final int numberOfBatches = 10;

        for (int currentBatchNumber = 0; currentBatchNumber < numberOfBatches; currentBatchNumber++) {
            List<String> containedStrings = randomStrings("a", batchSize);
            List<String> nonContainedStrings = randomStrings("b", batchSize);

            containedStrings.forEach(filter::add);

            long truePositives = containedStrings.stream()
                    .filter(filter::contains)
                    .count();
            long trueNegatives = nonContainedStrings.stream()
                    .filter(string -> !filter.contains(string))
                    .count();
            double falsePositiveRate = 100.0 * (batchSize - trueNegatives) / batchSize;
            double falseNegativeRate = 100.0 * (batchSize - truePositives) / batchSize;
            double accuracy = 100.0 * (truePositives + trueNegatives) / (2 * batchSize);

            System.err.printf(
                    "N:%6d : FPR:%.2f%% FNR:%.2f%% ACC:%.2f%%\n",
                    (currentBatchNumber * batchSize),
                    falsePositiveRate,
                    falseNegativeRate,
                    accuracy);
        }
    }

    private static List<String> randomStrings(String prefix, int count) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = prefix + RandomStringUtils.randomAlphanumeric(7);
            strings.add(value);
        }
        return strings;
    }

}
