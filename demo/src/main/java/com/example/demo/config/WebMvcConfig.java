package com.example.demo.config;

import com.example.demo.interceptor.SubdomainInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SubdomainInterceptor subdomainInterceptor;

    public WebMvcConfig(SubdomainInterceptor subdomainInterceptor) {
        this.subdomainInterceptor = subdomainInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subdomainInterceptor);
    }
}