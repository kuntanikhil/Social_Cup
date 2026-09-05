package com.socialcup.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private final InitialAdminBootstrapService bootstrapService;
    private final String configuredEmail;

    public InitialAdminBootstrap(
            InitialAdminBootstrapService bootstrapService,
            @Value("${admin.bootstrap-email:}") String configuredEmail
    ) {
        this.bootstrapService = bootstrapService;
        this.configuredEmail = configuredEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.configureInitialAdmin(configuredEmail);
    }
}
