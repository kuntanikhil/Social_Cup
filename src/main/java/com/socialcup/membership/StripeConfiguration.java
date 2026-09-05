package com.socialcup.membership;

import com.stripe.net.RequestOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfiguration {

    @Bean
    RequestOptions stripeRequestOptions(StripeProperties properties) {
        return RequestOptions.builder()
                .setApiKey(properties.getSecretKey())
                .setMaxNetworkRetries(2)
                .build();
    }
}
