package com.urbanpay.core;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Falha o build se um módulo acessar classes internas de outro (ex.: ledger usando identity.internal). */
class ModularityTests {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules.of(CoreApplication.class).verify();
    }
}