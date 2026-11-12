package com.doubledee.ultrastar.models;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageEnumTest {

    @ParameterizedTest(name = "{0} -> {1} ({2})")
    @CsvSource({
            "Englisch,  English,  en",
            "Anglais,   English,  en",
            "French,    French,   fr",
            "Hebräisch, Hebrew,   he",
            "Croate,    Croatian, hr",
            "Klingon,   Other,    other"
    })
    void getLanguageFindsTheLanguageByItsName(String name, String expectedName, String expectedCode) {
        Language language = LanguageEnum.getLanguage(name);

        assertThat(language.getDisplayLanguage()).isEqualTo(expectedName);
        assertThat(language.getLanguageCode()).isEqualTo(expectedCode);
    }

    @Test
    void noLanguageIsUndefined() {
        Language language = LanguageEnum.getLanguage(null);

        assertThat(language.getDisplayLanguage()).isEqualTo("ALL");
        assertThat(language.getLanguageCode()).isEmpty();
    }

    @Test
    void severalLanguagesAreSeparatedByCommas() {
        assertThat(LanguageEnum.getLanguages("English, German"))
                .extracting(Language::getLanguageCode)
                .containsExactlyInAnyOrder("en", "de");
    }
}
