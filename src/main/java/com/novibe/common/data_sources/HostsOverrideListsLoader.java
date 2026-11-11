package com.novibe.common.data_sources;

import org.springframework.stereotype.Service;

import java.util.stream.Stream;

/** Loads redirects: lines pointing domains to a routable IP address. */
@Service
public class HostsOverrideListsLoader extends ListLoader<HostsOverrideListsLoader.BypassRoute> {

    public record BypassRoute(String ip, String website) {
    }

    @Override
    protected String listType() {
        return "Override";
    }

    @Override
    protected Stream<BypassRoute> toObjects(HostsLine line) {
        if (!line.isOverride()) {
            return Stream.empty();
        }
        return line.domains().stream().map(domain -> new BypassRoute(line.ip(), domain));
    }

}
