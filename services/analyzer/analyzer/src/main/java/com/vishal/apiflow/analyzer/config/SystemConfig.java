package com.vishal.apiflow.analyzer.config;

import org.springframework.stereotype.Component;

@Component
public class SystemConfig {
    public static boolean CACHE_ENABLED = false;
    public static boolean INDEX_APPLIED = false;
    public static boolean POOL_OPTIMIZED = false;

    public static void reset() {
        CACHE_ENABLED = false;
        INDEX_APPLIED = false;
        POOL_OPTIMIZED = false;
    }
}