package com.doubledee.ultrastar;

import com.doubledee.ultrastar.covers.CoverService;
import com.doubledee.ultrastar.filters.FilterOptions;
import com.doubledee.ultrastar.i18n.Labels;
import com.doubledee.ultrastar.models.Language;
import com.doubledee.ultrastar.models.LanguageEnum;
import com.doubledee.ultrastar.models.Song;
import com.doubledee.ultrastar.models.UltrastarPlaylist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalog page (filters, sort, pages), redirects, audio and covers, on a small library built in memory.
 */
@WebMvcTest(UltrastarController.class)
@Import({Labels.class, FilterOptions.class, CoverService.class})
class CatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @TempDir
    Path songsDir;

    private Map<String, Song> savedSongs;
    private Map<String, UltrastarPlaylist> savedPlaylists;
    private List<Language> savedLanguages;
    private Map<String, String> savedConfigs;

    @BeforeEach
    void library() throws IOException {
        savedSongs = UltrastarApplication.SONGS;
        savedPlaylists = UltrastarApplication.PLAYLISTS;
        savedLanguages = UltrastarApplication.USED_LANGUAGES;
        savedConfigs = UltrastarApplication.CONFIGS;

        Map<String, Song> songs = new LinkedHashMap<>();
        add(songs, "queen", "Queen", "Bohemian Rhapsody", "English", "1975", "Party", 3);
        add(songs, "nena", "Nena", "99 Luftballons", "German", "1983", "", 1);
        add(songs, "mariah", "Mariah Carey", "All I Want for Christmas", "English", "1994", "Christmas", 2);
        add(songs, "herbert", "Herbert Leonard", "Pour le plaisir", "French", "1981", "Party, Love Song", 4);
        add(songs, "elton", "Elton John", "Don't Go Breaking My Heart", "English", "", "Duet", 0);
        // 70 more songs for the pages (60 cards per page)
        for (int i = 0; i < 70; i++) {
            add(songs, "filler" + i, "Zz Filler " + String.format("%02d", i), "Song " + i, "Italian", "2001", "", 0);
        }
        UltrastarApplication.SONGS = songs;
        UltrastarApplication.USED_LANGUAGES = new ArrayList<>(List.of(Language.UNDEFINED,
                LanguageEnum.getLanguage("English"), LanguageEnum.getLanguage("German"),
                LanguageEnum.getLanguage("French"), LanguageEnum.getLanguage("Italian")));

        Path playlistFile = songsDir.resolve("top.upl");
        Files.writeString(playlistFile, "#Name: Top\r\n#Songs:\r\nNena : 99 Luftballons\r\nQueen : Bohemian Rhapsody\r\n",
                          StandardCharsets.UTF_8);
        Map<String, UltrastarPlaylist> playlists = new HashMap<>();
        playlists.put("top.upl", new UltrastarPlaylist(playlistFile.toFile()));
        UltrastarApplication.PLAYLISTS = playlists;
        UltrastarApplication.CONFIGS = new HashMap<>();

        // audio file and cover for "queen"
        Path queenDir = Files.createDirectories(songsDir.resolve("queen"));
        Files.write(queenDir.resolve("song.mp3"), new byte[5000]);
        BufferedImage cover = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(cover, "jpg", queenDir.resolve("Queen - Bohemian Rhapsody [co].jpg").toFile());
    }

    @AfterEach
    void restore() {
        UltrastarApplication.SONGS = savedSongs;
        UltrastarApplication.PLAYLISTS = savedPlaylists;
        UltrastarApplication.USED_LANGUAGES = savedLanguages;
        UltrastarApplication.CONFIGS = savedConfigs;
    }

    private void add(Map<String, Song> songs, String uid, String artist, String title, String language, String year,
                     String tags, int daysAgo) {
        Song song = new Song(0, title, artist, "song.mp3", songsDir.resolve(uid).toString(), artist + " - " + title + ".txt");
        song.setUid(uid);
        song.setLanguage(LanguageEnum.getLanguages(language));
        song.setYear(year);
        song.setTags(tags);
        // folder arrived "daysAgo" days ago, but every txt was edited today: "Nouveautés" must follow the arrival
        song.setDateAdded(daysAgo == 0 ? null : new Date(System.currentTimeMillis() - daysAgo * 86_400_000L));
        song.setLastUpdate(daysAgo == 0 ? null : new Date());
        songs.put(uid, song);
    }

    /** Song ids in the order of the page (cards or list rows). */
    private List<String> ids(String url) throws Exception {
        String html = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("class=\"(?:flip-card-inner|songlist)\" data-songid=\"([^\"]+)\"").matcher(html);
        List<String> ids = new ArrayList<>();
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }

    @Test
    void severalValuesOfOneFilterAreCombinedWithOr() throws Exception {
        assertThat(ids("/?language=fr&language=de")).containsExactlyInAnyOrder("herbert", "nena");
        assertThat(ids("/?tags=Christmas,Party")).containsExactlyInAnyOrder("queen", "mariah", "herbert");
    }

    @Test
    void differentFiltersAreCombinedWithAnd() throws Exception {
        assertThat(ids("/?language=en&decade=70s")).containsExactly("queen");
        assertThat(ids("/?language=en&tags=Party")).containsExactly("queen");
    }

    @Test
    void playlistAndSearch() throws Exception {
        assertThat(ids("/?playlist=top.upl")).containsExactlyInAnyOrder("nena", "queen");
        assertThat(ids("/?searchterm=luftballons")).containsExactly("nena");
    }

    @Test
    void favoritesComeFromTheBrowser() throws Exception {
        assertThat(ids("/?fav=1&favs=nena,mariah")).containsExactlyInAnyOrder("nena", "mariah");
        assertThat(ids("/?fav=1&favs=")).isEmpty();
    }

    @Test
    void sortByYearKeepsSongsWithoutYearAtTheEnd() throws Exception {
        assertThat(ids("/?language=en&sort=year&dir=asc")).containsExactly("queen", "mariah", "elton");
        assertThat(ids("/?language=en&sort=year&dir=desc")).containsExactly("mariah", "queen", "elton");
    }

    @Test
    void sortByArtistTitleLanguageAndDate() throws Exception {
        String filter = "/?searchterm=&tags=Party,Christmas,Duet&language=en&language=fr&language=de";
        assertThat(ids(filter + "&sort=artist")).containsExactly("elton", "herbert", "mariah", "queen");
        assertThat(ids(filter + "&sort=artist&dir=desc")).containsExactly("queen", "mariah", "herbert", "elton");
        assertThat(ids(filter + "&sort=title")).containsExactly("mariah", "queen", "elton", "herbert");
        // English (elton, mariah, queen) then French
        assertThat(ids(filter + "&sort=language").get(3)).isEqualTo("herbert");
        // most recent first, songs without date at the end
        assertThat(ids(filter + "&sort=added&dir=desc")).containsExactly("mariah", "queen", "herbert", "elton");
    }

    @Test
    void songsComeByPages() throws Exception {
        MvcResult first = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        String html = first.getResponse().getContentAsString();
        assertThat(ids("/")).hasSize(UltrastarController.PAGE_SIZE_CARDS);
        assertThat(html).contains("75 songs found.").contains("id=\"loadMore\"").contains("data-next-page=\"1\"");

        mvc.perform(get("/?page=1&fragment=1"))
           .andExpect(status().isOk())
           .andExpect(header().string("X-Has-More", "false"));
        assertThat(ids("/?page=1&fragment=1")).hasSize(75 - UltrastarController.PAGE_SIZE_CARDS);
        assertThat(ids("/?view=list")).hasSize(75); // 200 rows per page in the list view
    }

    @Test
    void duetsHaveABadge() throws Exception {
        String html = mvc.perform(get("/?searchterm=elton")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("duo-badge");
        html = mvc.perform(get("/?searchterm=nena")).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("class=\"duo-badge\"");
    }

    @Test
    void oldAddressesRedirect() throws Exception {
        mvc.perform(get("/artists?language=fr")).andExpect(redirectedUrl("/?language=fr&sort=artist"));
        mvc.perform(get("/title")).andExpect(redirectedUrl("/?sort=title"));
    }

    @Test
    void audioSupportsRangeRequests() throws Exception {
        mvc.perform(get("/audio/queen.mp3").header(HttpHeaders.RANGE, "bytes=0-99"))
           .andExpect(status().isPartialContent())
           .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-99/5000"))
           .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "audio/mpeg"));
        mvc.perform(get("/audio/unknown.mp3")).andExpect(status().isNotFound());
    }

    @Test
    void coversAreResizedAndCached() throws Exception {
        MvcResult small = mvc.perform(get("/images/queen.jpg?w=240"))
                             .andExpect(status().isOk())
                             .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                             .andReturn();
        assertThat(small.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("max-age=604800");
        BufferedImage thumb = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(
                small.getResponse().getContentAsByteArray()));
        assertThat(thumb.getWidth()).isEqualTo(240);
    }

    @Test
    void missingCoverGivesTheDefaultPicture() throws Exception {
        mvc.perform(get("/images/nena.jpg?w=400"))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/svg+xml"));
        mvc.perform(get("/images/unknown.jpg")).andExpect(status().isOk());
    }
}
