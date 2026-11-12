package com.doubledee.ultrastar.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class UltrastarFileTest {

    @TempDir
    Path songsDir;

    @Test
    void addedIsTheArrivalOfTheSongFolderNotTheTxtDate() throws IOException {
        Path folder = Files.createDirectories(songsDir.resolve("Queen - Bohemian Rhapsody"));
        Path txt = folder.resolve("Queen - Bohemian Rhapsody.txt");
        Files.writeString(txt, "#TITLE:Bohemian Rhapsody\r\n#ARTIST:Queen\r\n#MP3:song.mp3\r\nE\r\n",
                          StandardCharsets.UTF_8);
        // the txt was edited much later than the folder arrived
        Files.setLastModifiedTime(txt, FileTime.from(Instant.now().plus(30, ChronoUnit.DAYS)));

        UltrastarFile file = new UltrastarFile(txt.toFile(), "UTF-8");

        long folderCreation = Files.readAttributes(folder, BasicFileAttributes.class).creationTime().toMillis();
        assertThat(file.getAdded().getTime()).isEqualTo(folderCreation);
        assertThat(file.getLastUpdate().getTime()).isEqualTo(Files.getLastModifiedTime(txt).toMillis());
        assertThat(new Song(file).getDateAdded()).isEqualTo(file.getAdded());
    }

    @Test
    void txtDateWhenTheFolderCannotBeRead() {
        File txt = new File("does-not-exist.txt");

        assertThat(UltrastarFile.folderCreationDate(txt).getTime()).isEqualTo(txt.lastModified());
    }
}
