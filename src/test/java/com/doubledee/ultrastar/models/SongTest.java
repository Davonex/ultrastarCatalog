package com.doubledee.ultrastar.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SongTest {

    private static Song song(String title) {
        return new Song(1, title, "Some Artist", "song.mp3", "path", "song.txt");
    }

    @Test
    void featuredArtistComesFromTheTitle() {
        assertThat(song("test (feat. me)").getFeaturedArtist()).isEqualTo("me");
        assertThat(song("test").getFeaturedArtist()).isEmpty();
    }

    @ParameterizedTest(name = "#PREVIEWSTART:{0} -> {1}")
    @CsvSource(value = {
            "45.5 | 45.5",
            "45,5 | 45.5",
            "' 12 ' | 12.0",
            "'' | 0.0",
            "abc | 0.0",
            "-3 | 0.0"
    }, delimiter = '|')
    void previewStartAcceptsDotAndComma(String previewstart, double expected) {
        Song song = song("title");
        song.setPreviewstart(previewstart);

        assertThat(song.getPreviewStartSeconds()).isEqualTo(expected);
    }

    @Test
    void noPreviewStartMeansTheBeginning() {
        assertThat(song("title").getPreviewStartSeconds()).isZero();
    }

    @Test
    void duetWhenTwoSingersAreNamed() {
        Song song = song("title");
        song.setP1("Elton");
        song.setP2("Kiki");

        assertThat(song.isDuet()).isTrue();
        assertThat(song.getDuetSingers()).isEqualTo("Elton / Kiki");
    }

    @Test
    void duetWhenTheDuetTagIsSet() {
        Song song = song("title");
        song.setTags("Party, Duet");

        assertThat(song.isDuet()).isTrue();
        assertThat(song.getDuetSingers()).isEmpty();
    }

    @Test
    void notADuet() {
        Song song = song("title");
        song.setTags("Party");

        assertThat(song.isDuet()).isFalse();
    }
}
