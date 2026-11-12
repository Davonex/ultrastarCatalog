package com.doubledee.ultrastar.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagsEnumTest {

    @Test
    void valueOfReturnsTheEnumConstant() {
        assertThat(TagsEnum.valueOf("DISNEY")).isEqualTo(TagsEnum.DISNEY);
        assertThatThrownBy(() -> TagsEnum.valueOf("Hello")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTagFromStringIgnoresCaseAndSpaces() {
        assertThat(TagsEnum.getTagFromString(" christmas ")).isEqualTo(TagsEnum.CHRISTMAS);
        assertThat(TagsEnum.getTagFromString("Love Song")).isEqualTo(TagsEnum.LOVE_SONG);
        assertThat(TagsEnum.getTagFromString("Rap")).isEqualTo(TagsEnum.RAP);
    }

    @Test
    void unknownTagIsUntagged() {
        assertThat(TagsEnum.getTagFromString("Klingon opera")).isEqualTo(TagsEnum.UNTAGGED);
    }

    @Test
    void getTagsByStringSplitsOnCommas() {
        assertThat(TagsEnum.getTagsByString("Party, Duet")).containsExactlyInAnyOrder(TagsEnum.PARTY, TagsEnum.DUET);
        assertThat(TagsEnum.getTagsByString("")).isEmpty();
        assertThat(TagsEnum.getTagsByString("", true)).containsExactly(TagsEnum.UNTAGGED);
    }

    @Test
    void filenameIsTheLowerCaseName() {
        assertThat(TagsEnum.EUROVISION_SONG_CONTEST.getFilename()).isEqualTo("eurovision_song_contest");
    }
}
