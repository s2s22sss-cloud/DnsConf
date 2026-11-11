package com.novibe.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvParserTest {

    @Test
    void emptyValues() {
        assertEquals(List.of(), EnvParser.parse(null));
        assertEquals(List.of(), EnvParser.parse(""));
        assertEquals(List.of(), EnvParser.parse("   "));
        assertEquals(List.of(), EnvParser.parse(" , ,"));
    }

    @Test
    void splitsAndTrims() {
        assertEquals(List.of("https://a/hosts", "https://b/hosts"),
                EnvParser.parse(" https://a/hosts, https://b/hosts ,"));
    }

    @Test
    void handlesNewlines() {
        assertEquals(List.of("https://a/hosts", "https://b/hosts"),
                EnvParser.parse("https://a/hosts,\nhttps://b/hosts\n"));
    }
}
