package com.nba.sdui.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link SduiHandlerInterceptor} for the SDUI URL space only
 * ({@code /v1/sdui/**}). Non-SDUI endpoints are unaffected.
 */
@Configuration
public class SduiWebMvcConfig implements WebMvcConfigurer {

    private final SduiHandlerInterceptor interceptor;

    public SduiWebMvcConfig(SduiHandlerInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/v1/sdui/**");
    }
}
