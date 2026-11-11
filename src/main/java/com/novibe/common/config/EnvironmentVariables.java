package com.novibe.common.config;

import com.novibe.common.util.Log;

import static java.util.Objects.isNull;

public class EnvironmentVariables {

    public static final String DNS = extractMandatoryVariable("DNS");

    public static final String CLIENT_ID = extractMandatoryVariable("CLIENT_ID");

    public static final String AUTH_SECRET = extractMandatoryVariable("AUTH_SECRET");

    public static final String BLOCK = System.getenv("BLOCK");

    public static final String REDIRECT = System.getenv("REDIRECT");

    /** NextDNS only: remove the whole denylist and all rewrites of the profile before synchronization. */
    public static final boolean NEXTDNS_CLEAR_ALL = "true".equalsIgnoreCase(System.getenv("NEXTDNS_CLEAR_ALL"));

    private static String extractMandatoryVariable(String key) {
        String env = System.getenv(key);
        if (isNull(env) || env.isBlank()) {
            Log.fail("Не обнаружена обязательная переменная среды: " + key);
            System.exit(1);
        }
        return env;
    }

}
