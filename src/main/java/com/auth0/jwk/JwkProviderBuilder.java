package com.auth0.jwk;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * JwkProvider builder
 */
@SuppressWarnings("WeakerAccess")
public class JwkProviderBuilder {

    private final URL url;
    private TimeUnit expiresUnit;
    private long expiresIn;
    private long cacheSize;
    private boolean cached;
    private BucketImpl bucket;
    private boolean rateLimited;


    /**
     * Creates a new Builder with a URL from where to load the jwks.
     *
     * @param url to load the jwks
     * @throws IllegalStateException if url is null
     */
    public JwkProviderBuilder(URL url) {
        if (url == null) {
            throw new IllegalStateException("Cannot build provider without url to jwks");
        }
        this.url = url;
        this.cached = true;
        this.expiresIn = 10;
        this.expiresUnit = TimeUnit.HOURS;
        this.cacheSize = 5;
        this.rateLimited = true;
        this.bucket = new BucketImpl(10, 1, TimeUnit.MINUTES);
    }

    /**
     * Creates a new Builder with a URL from where to load the jwks.
     * It can be a url link 'https://samples.auth0.com' or just the Auth0 domain 'samples.auth0.com'.
     *
     * @param domain from where to load the jwks
     * @throws IllegalStateException if domain is null
     */
    public JwkProviderBuilder(String domain) {
        this(buildJwkUrl(domain));
    }

    /**
     * Toggle the cache of Jwk. By default the provider will use cache.
     *
     * @param cached if the provider should cache jwks
     * @return the builder
     */
    public JwkProviderBuilder cached(boolean cached) {
        this.cached = cached;
        return this;
    }

    /**
     * Enable the cache specifying size and expire time.
     *
     * @param cacheSize number of jwk to cache
     * @param expiresIn amount of time the jwk will be cached
     * @param unit      unit of time for the expire of jwk
     * @return the builder
     */
    public JwkProviderBuilder cached(long cacheSize, long expiresIn, TimeUnit unit) {
        this.cached = true;
        this.cacheSize = cacheSize;
        this.expiresIn = expiresIn;
        this.expiresUnit = unit;
        return this;
    }

    /**
     * Toggle the rate limit of Jwk. By default the Provider will use rate limit.
     *
     * @param rateLimited if the provider should rate limit jwks
     * @return the builder
     */
    public JwkProviderBuilder rateLimited(boolean rateLimited) {
        this.rateLimited = rateLimited;
        return this;
    }

    /**
     * Enable the cache specifying size and expire time.
     *
     * @param bucketSize max number of jwks to deliver in the given rate.
     * @param refillRate amount of time to wait before a jwk can the jwk will be cached
     * @param unit       unit of time for the expire of jwk
     * @return the builder
     */
    public JwkProviderBuilder rateLimited(long bucketSize, long refillRate, TimeUnit unit) {
        bucket = new BucketImpl(bucketSize, refillRate, unit);
        return this;
    }

    /**
     * Creates a {@link JwkProvider}
     *
     * @return a newly created {@link JwkProvider}
     */
    public JwkProvider build() {
        JwkProvider urlProvider = new UrlJwkProvider(url);
        if (this.rateLimited) {
            urlProvider = new RateLimitedJwkProvider(urlProvider, bucket);
        }
        if (this.cached) {
            urlProvider = new GuavaCachedJwkProvider(urlProvider, cacheSize, expiresIn, expiresUnit);
        }
        return urlProvider;
    }

    private static URL buildJwkUrl(String domain) {
        if (domain == null) {
            throw new IllegalStateException("Cannot build provider without domain");
        }
        return JwkUrlFactory.forNormalizedDomain(normalizeDomain(domain));
    }

    private static String normalizeDomain(String domain) {
        if (!domain.startsWith("http")) {
            return "https://" + domain;
        }
        return domain;
    }
}
