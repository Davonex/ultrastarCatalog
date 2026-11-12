package com.doubledee.ultrastar.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UltrastarPlaylistTest {

    @TempDir
    Path dir;

    private UltrastarPlaylist read(String fileName, String content) throws IOException {
        Path file = dir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return new UltrastarPlaylist(file.toFile());
    }

    @Test
    void readsNameAndSongs() throws IOException {
        UltrastarPlaylist playlist = read("top.upl", "#Name: Top FR 80-90\r\n#Songs:\r\nNena : 99 Luftballons\r\nQueen : Bohemian Rhapsody\r\n");

        assertThat(playlist.getName()).isEqualTo("Top FR 80-90");
        assertThat(playlist.getSongs()).containsExactly("Nena : 99 Luftballons", "Queen : Bohemian Rhapsody");
        assertThat(playlist.contains("Queen : Bohemian Rhapsody")).isTrue();
    }

    @Test
    void blankLinesAreNotSongs() throws IOException {
        UltrastarPlaylist playlist = read("empty.upl", "#Name: Empty\r\n#Songs:\r\n\r\n   \r\n");

        assertThat(playlist.getSongs()).isEmpty();
    }

    @Test
    void fileNameIsUsedWithoutNameLine() throws IOException {
        UltrastarPlaylist playlist = read("Party Night.upl", "#Songs:\r\nNena : 99 Luftballons\r\n");

        assertThat(playlist.getName()).isEqualTo("Party Night");
    }

    @Test
    void utf8BomIsIgnored() throws IOException {
        UltrastarPlaylist playlist = read("bom.upl", "﻿#Name: Soirée\r\n#Songs:\r\n");

        assertThat(playlist.getName()).isEqualTo("Soirée");
    }

    @Test
    void writtenContentCanBeReadAgain() throws IOException {
        UltrastarPlaylist playlist = read("rw.upl", "#Name: Rock\r\n#Songs:\r\nQueen : Bohemian Rhapsody\r\n");
        playlist.getSongs().add("AC/DC : Thunderstruck");
        Files.write(dir.resolve("rw.upl"), playlist.getContentsAsByteArray());

        UltrastarPlaylist again = new UltrastarPlaylist(dir.resolve("rw.upl").toFile());

        assertThat(again.getName()).isEqualTo("Rock");
        assertThat(again.getSongs()).containsExactly("Queen : Bohemian Rhapsody", "AC/DC : Thunderstruck");
    }
}
