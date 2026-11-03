package com.urbanpay.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Monólito modular: cada subpacote direto (identity, ledger, shared) é um módulo
 * do Spring Modulith. Módulos só se enxergam pelos tipos do pacote raiz de cada um;
 * tudo em {@code internal} é privado. As fronteiras são verificadas em teste
 * (ModularityTests), o que permite extrair um módulo para um microsserviço no futuro.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
