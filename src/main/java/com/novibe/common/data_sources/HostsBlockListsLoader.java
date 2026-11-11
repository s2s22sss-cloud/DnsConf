package com.novibe.common.data_sources;

import org.springframework.stereotype.Service;

import java.util.stream.Stream;

/** Loads domains to block: lines pointing to 0.0.0.0, 127.0.0.1, ::1 or ::. */
@Service
public class HostsBlockListsLoader extends ListLoader<String> {

    @Override
    protected String listType() {
        return "Block";
    }

    @Override
    protected Stream<String> toObjects(HostsLine line) {
        return line.isBlock() ? line.domains().stream() : Stream.empty();
    }

}
