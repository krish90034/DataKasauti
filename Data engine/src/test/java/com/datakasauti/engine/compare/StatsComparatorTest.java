package com.datakasauti.engine.compare;

import com.datakasauti.engine.profile.ProfileResult;
import com.datakasauti.engine.profile.Profiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsComparatorTest {

    @TempDir
    Path tempDir;

    private ProfileResult profile(String filename, String content)
            throws Exception {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return new Profiler().profile(file);
    }

    @Test
    void comparesNumericColumnsAndCalculatesMeanDelta()
            throws Exception {

        ProfileResult baseline = profile(
                "baseline.csv",
                "sales,region\n10,North\n20,South\n30,East\n"
        );

        ProfileResult current = profile(
                "current.csv",
                "sales,region\n20,North\n30,South\n40,West\n"
        );

        List<CompareResult.CommonColumnStats> result =
                new StatsComparator().compare(baseline, current);

        assertEquals(1, result.size());

        CompareResult.CommonColumnStats sales = result.get(0);

        assertEquals("sales", sales.name());

        assertEquals(3, sales.baseline().count());
        assertEquals(0, sales.baseline().missing());
        assertEquals(10.0, sales.baseline().min(), 0.001);
        assertEquals(30.0, sales.baseline().max(), 0.001);
        assertEquals(20.0, sales.baseline().mean(), 0.001);

        assertEquals(3, sales.current().count());
        assertEquals(20.0, sales.current().min(), 0.001);
        assertEquals(40.0, sales.current().max(), 0.001);
        assertEquals(30.0, sales.current().mean(), 0.001);

        assertEquals(10.0, sales.meanDelta(), 0.001);
    }

    @Test
    void includesMissingCountsInNumericComparison()
            throws Exception {

        ProfileResult baseline = profile(
                "baseline-missing.csv",
                "id,sales\nA,10\nB,20\nC,\nD,30\n"
        );

        ProfileResult current = profile(
                "current-missing.csv",
                "id,sales\nA,20\nB,30\nC,\nD,50\n"
        );

        List<CompareResult.CommonColumnStats> result =
                new StatsComparator().compare(baseline, current);

        CompareResult.CommonColumnStats sales = result.stream()
                .filter(column -> column.name().equals("sales"))
                .findFirst()
                .orElseThrow();

        assertEquals(3, sales.baseline().count());
        assertEquals(1, sales.baseline().missing());
        assertEquals(3, sales.current().count());
        assertEquals(1, sales.current().missing());
        assertEquals(13.33, sales.meanDelta(), 0.001);
    }

    @Test
    void ignoresNonNumericColumnsAndChangedToStringColumns()
            throws Exception {

        ProfileResult baseline = profile(
                "baseline-text.csv",
                "metric,label\n10,apple\n20,banana\n"
        );

        ProfileResult current = profile(
                "current-text.csv",
                "metric,label\n10,apple\nhello,banana\n"
        );

        List<CompareResult.CommonColumnStats> result =
                new StatsComparator().compare(baseline, current);

        assertTrue(result.isEmpty());
    }
}