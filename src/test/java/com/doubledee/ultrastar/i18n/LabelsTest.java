package com.doubledee.ultrastar.i18n;

import com.doubledee.ultrastar.models.Decade;
import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.LanguageEnum;
import com.doubledee.ultrastar.models.Song;
import com.doubledee.ultrastar.models.TagsEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LabelsTest {

    private Labels labels;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false); // same settings as application.properties
        labels = new Labels(messages);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void tagsAreTranslatedInFrench() {
        LocaleContextHolder.setLocale(Locale.FRENCH);

        assertThat(labels.tag(TagsEnum.CHRISTMAS)).isEqualTo("Noël");
        assertThat(labels.tag(TagsEnum.LOVE_SONG)).isEqualTo("Chanson d'amour");
    }

    @Test
    void tagsStayInEnglishWithoutTranslation() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(labels.tag(TagsEnum.CHRISTMAS)).isEqualTo("Christmas");

        LocaleContextHolder.setLocale(Locale.JAPANESE); // no messages_ja.properties: English
        assertThat(labels.tag(TagsEnum.CHRISTMAS)).isEqualTo("Christmas");
    }

    @Test
    void parseTagsAcceptsEnglishAndTranslatedNames() {
        LocaleContextHolder.setLocale(Locale.FRENCH);

        assertThat(labels.parseTags("Noël, Party")).containsExactly(TagsEnum.CHRISTMAS, TagsEnum.PARTY);
        assertThat(labels.parseTags("")).isEmpty();
    }

    @Test
    void languagesAreTranslatedByJava() {
        LocaleContextHolder.setLocale(Locale.FRENCH);
        Language english = LanguageEnum.getLanguage("English");

        assertThat(labels.language(english)).isEqualTo("Anglais");
        assertThat(labels.language(Language.UNDEFINED)).isEqualTo("Toutes");
        assertThat(labels.language(Language.OTHER)).isEqualTo("Autre");
    }

    @Test
    void songLanguagesAreJoined() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        Song song = new Song(1, "Title", "Artist", "a.mp3", "path", "a.txt");
        song.setLanguage(LanguageEnum.getLanguages("German, French"));

        assertThat(labels.languages(song).split(", ")).containsExactlyInAnyOrder("German", "French");
    }

    @Test
    void decadeAllIsTranslated() {
        LocaleContextHolder.setLocale(Locale.FRENCH);

        assertThat(labels.decade(Decade.ALL)).isEqualTo("Toutes");
        assertThat(labels.decade(Decade.EIGHTIES)).isEqualTo("80s");
    }
}
