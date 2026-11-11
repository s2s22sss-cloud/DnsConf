package com.novibe.dns.next_dns;

import com.novibe.common.DnsTaskRunner;
import com.novibe.common.data_sources.HostsBlockListsLoader;
import com.novibe.common.data_sources.HostsOverrideListsLoader;
import com.novibe.common.util.EnvParser;
import com.novibe.common.util.Log;
import com.novibe.dns.next_dns.http.dto.request.CreateRewriteDto;
import com.novibe.dns.next_dns.service.NextDnsDenyService;
import com.novibe.dns.next_dns.service.NextDnsRewriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.novibe.common.config.EnvironmentVariables.BLOCK;
import static com.novibe.common.config.EnvironmentVariables.NEXTDNS_CLEAR_ALL;
import static com.novibe.common.config.EnvironmentVariables.REDIRECT;

@Service
@RequiredArgsConstructor
public class NextDnsTaskRunner implements DnsTaskRunner {

    private final HostsBlockListsLoader blockListsLoader;
    private final HostsOverrideListsLoader overrideListsLoader;
    private final NextDnsRewriteService nextDnsRewriteService;
    private final NextDnsDenyService nextDnsDenyService;

    @Override
    public void run() {

        Log.global("NextDNS");
        Log.common("""
        Script behaviour: block/redirect settings are updated via provided block/redirect sources.
        New domains are added, rewrites with a changed IP are updated, other entries remain untouched.
        If only one type of sources is provided, another type remains untouched.
        Set NEXTDNS_CLEAR_ALL=true to remove the whole denylist and all rewrites before synchronization.
        NextDNS API rate limiter reset config: 60 seconds after the last request""");

        List<String> blockSources = EnvParser.parse(BLOCK);
        List<String> rewriteSources = EnvParser.parse(REDIRECT);

        if (NEXTDNS_CLEAR_ALL) {
            Log.step("Remove all denylist and rewrites settings (NEXTDNS_CLEAR_ALL=true)");
            nextDnsDenyService.removeAll();
            nextDnsRewriteService.removeAll();
        } else if (blockSources.isEmpty() && rewriteSources.isEmpty()) {
            Log.fail("""
                    No block or rewrite sources provided: nothing to do.
                    To remove all NextDNS denylist and rewrites settings, set NEXTDNS_CLEAR_ALL=true""");
        }

        if (!blockSources.isEmpty()) {
            Log.step("Obtain block lists from %s sources".formatted(blockSources.size()));
            List<String> blocks = blockListsLoader.fetchWebsites(blockSources);
            Log.step("Prepare denylist");
            List<String> filteredBlocklist = nextDnsDenyService.dropExistingDenys(blocks);
            Log.common("Prepared %s domains to block".formatted(filteredBlocklist.size()));
            Log.step("Save denylist");
            nextDnsDenyService.saveDenyList(filteredBlocklist);
        } else {
            Log.fail("No block sources provided");
        }

        if (!rewriteSources.isEmpty()) {
            Log.step("Obtain rewrite lists from %s sources".formatted(rewriteSources.size()));
            List<HostsOverrideListsLoader.BypassRoute> overrides = overrideListsLoader.fetchWebsites(rewriteSources);

            Log.step("Prepare rewrites");
            Map<String, CreateRewriteDto> requests = nextDnsRewriteService.buildNewRewrites(overrides);
            List<CreateRewriteDto> createRewriteDtos = nextDnsRewriteService.cleanupOutdated(requests);

            Log.step("Save rewrites");
            nextDnsRewriteService.saveRewrites(createRewriteDtos);
        } else {
            Log.fail("No rewrite sources provided");
        }

        Log.global("FINISHED");
    }

}
