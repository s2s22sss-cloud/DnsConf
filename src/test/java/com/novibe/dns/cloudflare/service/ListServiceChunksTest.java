package com.novibe.dns.cloudflare.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListServiceChunksTest {

    @ParameterizedTest
    @CsvSource({
            "0, ''",
            "1, '1'",
            "999, '999'",
            "1000, '1000'",
            "1001, '1000 1'",
            "1500, '1000 500'",
            "2000, '1000 1000'",
            "3000, '1000 1000 1000'",
            "3001, '1000 1000 1000 1'"
    })
    void cutsIntoChunksOfAtMost1000(int size, String expectedSizes) {
        List<Integer> list = IntStream.range(0, size).boxed().toList();

        List<List<Integer>> chunks = ListService.cutChunks(list);

        List<Integer> sizes = chunks.stream().map(List::size).toList();
        List<Integer> expected = expectedSizes.isBlank() ? List.of()
                : java.util.Arrays.stream(expectedSizes.split(" ")).map(Integer::valueOf).toList();
        assertEquals(expected, sizes);
        assertEquals(list, chunks.stream().flatMap(List::stream).toList());
    }
}
