package com.doubledee.ultrastar.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {

    @Test
    void hashesAText() {
        assertThat(HashUtil.md5Hash("Hello World")).isEqualTo("b10a8db164e0754105b7a99be72e3fe5");
        assertThat(HashUtil.sha1("Hello World")).isEqualTo("0a4d55a8d778e5022fab701");
    }

    @Test
    void sameTextSameHash() {
        assertThat(HashUtil.sha1("Queen : Bohemian Rhapsody")).isEqualTo(HashUtil.sha1("Queen : Bohemian Rhapsody"));
        assertThat(HashUtil.sha1("a")).isNotEqualTo(HashUtil.sha1("b"));
    }
}
