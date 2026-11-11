package com.novibe.common.data_sources;

import com.novibe.common.util.Log;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ListLoader<T> {

    private static final Pattern LINE_BREAK = Pattern.compile("\\r?\\n");

    /**
     * Separate client for downloading sources: follows redirects (short links, GitHub "raw" redirects).
     * DNS provider API calls use the shared client, which never follows redirects with credentials.
     */
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Converts a parsed hosts line into result objects; returns an empty stream for unrelated lines. */
    protected abstract Stream<T> toObjects(HostsLine line);

    protected abstract String listType();

    @SneakyThrows
    @SuppressWarnings("preview")
    public List<T> fetchWebsites(List<String> urls) {
        @Cleanup var scope = StructuredTaskScope.open();
        List<StructuredTaskScope.Subtask<String>> requests = new ArrayList<>();
        urls.stream()
                .map(url -> scope.fork(() -> fetchList(url)))
                .forEach(requests::add);
        scope.join();
        List<String> contents = requests.stream()
                .map(StructuredTaskScope.Subtask::get)
                .toList();
        return parse(contents);
    }

    /**
     * Parses hosts files content. Sources order is preserved, so the first occurrence of a domain wins.
     */
    List<T> parse(List<String> contents) {
        AtomicInteger unrecognized = new AtomicInteger();
        List<T> result = contents.stream()
                .flatMap(LINE_BREAK::splitAsStream)
                .map(HostsLine::stripComment)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    Optional<HostsLine> parsed = HostsLine.parse(line);
                    if (parsed.isEmpty()) unrecognized.incrementAndGet();
                    return parsed;
                })
                .flatMap(Optional::stream)
                .flatMap(this::toObjects)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (unrecognized.get() > 0) {
            Log.common("%s list: skipped %s lines not in \"<ip> <domain>\" format"
                    .formatted(listType(), unrecognized.get()));
        }
        return result;
    }

    private String fetchList(String url) throws InterruptedException {
        Log.io("Loading %s list from url: %s".formatted(listType(), url));
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException e) {
            Log.fail("Failed to load %s list from url: %s\n%s".formatted(listType(), url, e));
            System.exit(1);
            throw new IllegalStateException(e);
        }
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            Log.fail("Failed to load %s list from url: %s\nHTTP code: %s. Check that the link points directly to a hosts file."
                    .formatted(listType(), url, response.statusCode()));
            System.exit(1);
        }
        return response.body();
    }

}
