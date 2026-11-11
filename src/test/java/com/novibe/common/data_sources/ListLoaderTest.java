package com.novibe.common.data_sources;

import com.novibe.common.data_sources.HostsOverrideListsLoader.BypassRoute;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListLoaderTest {

    private static final String HOSTS = """
            # Sample hosts file
            127.0.0.1 localhost
            ::1 ip6-localhost
            255.255.255.255 broadcasthost
            ff02::1 ip6-allnodes

            0.0.0.0 ads.example
            127.0.0.1\twww.tracker.example
            0.0.0.0 tracker.example
            1.2.3.4 www.blocked.example   # inline comment
            5.6.7.8\tvideo.example cdn.video.example
            404: Not Found
            """.replace("\n", "\r\n");

    private final HostsBlockListsLoader blockLoader = new HostsBlockListsLoader();
    private final HostsOverrideListsLoader overrideLoader = new HostsOverrideListsLoader();

    @Test
    void blockLoaderKeepsOnlyBlockedDomains() {
        assertEquals(List.of("ads.example", "tracker.example"), blockLoader.parse(List.of(HOSTS)));
    }

    @Test
    void overrideLoaderKeepsOnlyRoutableRedirects() {
        assertEquals(List.of(
                        new BypassRoute("1.2.3.4", "blocked.example"),
                        new BypassRoute("5.6.7.8", "video.example"),
                        new BypassRoute("5.6.7.8", "cdn.video.example")),
                overrideLoader.parse(List.of(HOSTS)));
    }

    @Test
    void sourcesOrderIsPreserved() {
        List<BypassRoute> routes = overrideLoader.parse(List.of(
                "1.1.1.1 site.example",
                "2.2.2.2 site.example\n3.3.3.3 other.example"));
        assertEquals(List.of(
                new BypassRoute("1.1.1.1", "site.example"),
                new BypassRoute("2.2.2.2", "site.example"),
                new BypassRoute("3.3.3.3", "other.example")), routes);
    }

    @Test
    void duplicatesAcrossSourcesAreRemoved() {
        assertEquals(List.of("ads.example"),
                blockLoader.parse(List.of("0.0.0.0 ads.example", "0.0.0.0 www.ads.example\n127.0.0.1 ADS.example")));
    }
}
