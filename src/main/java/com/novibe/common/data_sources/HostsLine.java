package com.novibe.common.data_sources;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One meaningful line of a hosts file: an IP address followed by one or more domains.
 * <p>
 * Comments, surrounding whitespace and letter case are normalized; IP and domains may be separated
 * by any whitespace (spaces or tabs). Domains are cleaned from the {@code www.} prefix and only valid,
 * non-local domain names are kept.
 */
public record HostsLine(String ip, List<String> domains) {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");

    private static final Pattern IPV6 = Pattern.compile("^[0-9a-f:.]*:[0-9a-f:.]*(%[0-9a-z._-]+)?$");

    private static final Pattern DOMAIN = Pattern.compile("^[a-z0-9_-]+(\\.[a-z0-9_-]+)+$");

    /** Addresses that mean "block this domain" in hosts files. */
    private static final Set<String> BLOCK_ADDRESSES = Set.of("0.0.0.0", "127.0.0.1", "::1", "::");

    private static final Set<String> LOCAL_DOMAINS = Set.of("localhost.localdomain");

    /**
     * Removes an inline comment ({@code # ...}) and surrounding whitespace.
     *
     * @return cleaned line, empty if the line carries no data
     */
    public static String stripComment(String rawLine) {
        int hash = rawLine.indexOf('#');
        String line = hash >= 0 ? rawLine.substring(0, hash) : rawLine;
        return line.strip();
    }

    /**
     * Parses a non-empty line already cleaned with {@link #stripComment(String)}.
     *
     * @return empty if the line is not in the {@code <ip> <domain> [<domain>...]} format
     */
    public static Optional<HostsLine> parse(String cleanLine) {
        String[] tokens = WHITESPACE.split(cleanLine.toLowerCase(Locale.ROOT));
        if (tokens.length < 2 || !isIpLiteral(tokens[0])) {
            return Optional.empty();
        }
        List<String> domains = Arrays.stream(tokens, 1, tokens.length)
                .map(HostsLine::removeWWW)
                .filter(HostsLine::isPublicDomain)
                .distinct()
                .toList();
        return Optional.of(new HostsLine(tokens[0], domains));
    }

    /** {@code true} if the line blocks its domains: points them to 0.0.0.0, 127.0.0.1, ::1 or ::. */
    public boolean isBlock() {
        return BLOCK_ADDRESSES.contains(ip);
    }

    /** {@code true} if the line points its domains to an address usable as a DNS override target. */
    public boolean isOverride() {
        return !isBlock() && isRoutable(ip);
    }

    static boolean isIpLiteral(String value) {
        return IPV4.matcher(value).matches()
                || (IPV6.matcher(value).matches() && value.chars().filter(c -> c == ':').count() >= 2);
    }

    /**
     * Rejects addresses that are meaningless as a redirect target:
     * unspecified, loopback, broadcast, multicast, link-local and zoned IPv6 addresses.
     */
    static boolean isRoutable(String ip) {
        if (IPV4.matcher(ip).matches()) {
            int first = Integer.parseInt(ip.substring(0, ip.indexOf('.')));
            return first != 0
                    && first != 127
                    && !(first >= 224) // multicast 224-239, reserved 240-255 incl. broadcast
                    && !ip.startsWith("169.254.");
        }
        return !ip.contains("%")
                && !ip.equals("::")
                && !ip.equals("::1")
                && !ip.startsWith("ff")
                && !ip.startsWith("fe80:");
    }

    static boolean isPublicDomain(String domain) {
        return DOMAIN.matcher(domain).matches()
                && !LOCAL_DOMAINS.contains(domain)
                && !domain.endsWith(".localhost")
                && !domain.endsWith(".local");
    }

    static String removeWWW(String domain) {
        return domain.startsWith("www.") ? domain.substring("www.".length()) : domain;
    }
}
