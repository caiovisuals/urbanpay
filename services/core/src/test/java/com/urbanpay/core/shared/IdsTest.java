package com.urbanpay.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void generatesTimeOrderedVersion7Uuids() throws Exception {
        var first = Ids.newId();
        Thread.sleep(2);
        var second = Ids.newId();

        assertThat(first.version()).isEqualTo(7);
        assertThat(first.variant()).isEqualTo(2);
        assertThat(first.toString().compareTo(second.toString())).isNegative();
    }
}
