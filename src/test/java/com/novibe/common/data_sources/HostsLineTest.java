package com.novibe.common.data_sources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HostsLineTest {

    private static HostsLine parse(String rawLine) {
        return HostsLine.parse(HostsLine.stripComment(rawLine)).orElseThrow();
    }

    @Test
    void parsesSpaceSeparatedLine() {
        HostsLine line = parse("1.2.3.4 site.example");
        assertEquals("1.2.3.4", line.ip());
        assertEquals(List.of("site.example"), line.domains());
    }

    @Test
    void parsesTabsAndMultipleSpaces() {
        assertEquals(List.of("site.example"), parse("1.2.3.4\tsite.example").domains());
        assertEquals(List.of("site.example"), parse("1.2.3.4 \t   site.example  ").domains());
        assertTrue(parse("0.0.0.0\tads.example").isBlock());
    }

    @Test
    void stripsInlineComment() {
        assertEquals(List.of("site.example"), parse("1.2.3.4 site.example # comment").domains());
        assertEquals(List.of("site.example"), parse("1.2.3.4 site.example#comment").domains());
    }

    @Test
    void commentOnlyLineIsEmpty() {
        assertEquals("", HostsLine.stripComment("   # just a comment"));
    }

    @Test
    void parsesMultipleDomains() {
        assertEquals(List.of("a.example", "b.example"), parse("1.2.3.4 a.example b.example").domains());
    }

    @Test
    void normalizesCaseAndWww() {
        assertEquals(List.of("site.example"), parse("1.2.3.4 WWW.Site.Example").domains());
    }

    @Test
    void deduplicatesDomainsWithinLine() {
        assertEquals(List.of("site.example"), parse("1.2.3.4 www.site.example site.example").domains());
    }

    @ParameterizedTest
    @ValueSource(strings = {"localhost", "broadcasthost", "ip6-allnodes", "localhost.localdomain",
            "foo.localhost", "printer.local", "<html>", "*.wildcard.example"})
    void dropsLocalAndInvalidDomains(String domain) {
        assertEquals(List.of(), parse("127.0.0.1 " + domain).domains());
    }

    @ParameterizedTest
    @ValueSource(strings = {"site.example", "404: not found", "<html>", "<!doctype html>", "1.2.3 site.example",
            "999.1.1.1 site.example"})
    void rejectsLinesWithoutIp(String line) {
        assertEquals(Optional.empty(), HostsLine.parse(line));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "127.0.0.1", "::1", "::"})
    void blockAddresses(String ip) {
        HostsLine line = parse(ip + " ads.example");
        assertTrue(line.isBlock());
        assertFalse(line.isOverride());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.2.3.4", "185.10.20.30", "2001:db8::1", "2a00:1450:4001:82a::200e"})
    void overrideAddresses(String ip) {
        HostsLine line = parse(ip + " site.example");
        assertTrue(line.isOverride());
        assertFalse(line.isBlock());
    }

    @ParameterizedTest
    @ValueSource(strings = {"255.255.255.255", "224.0.0.1", "127.0.0.2", "169.254.1.1", "0.1.2.3",
            "ff02::1", "fe80::1", "fe80::1%lo0"})
    void specialAddressesAreNeitherBlockNorOverride(String ip) {
        HostsLine line = parse(ip + " site.example");
        assertFalse(line.isBlock());
        assertFalse(line.isOverride());
    }
}
