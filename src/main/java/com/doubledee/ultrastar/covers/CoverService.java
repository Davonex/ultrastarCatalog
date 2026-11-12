package com.doubledee.ultrastar.covers;

import com.doubledee.ultrastar.models.Song;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;

/**
 * Song covers for the browser: small copies ("thumbnails") instead of the full-size images, so that
 * phones load the catalog much faster. Thumbnails are cached on disk and rebuilt when the cover changes.
 */
@Component
public class CoverService {

    /** Allowed widths (?w=...). Any other value gives the original image. */
    public static final List<Integer> WIDTHS = List.of(120, 240, 400);

    private static final float JPEG_QUALITY = 0.85f;

    private final Path cacheDir;

    public CoverService() {
        this(Paths.get(System.getProperty("java.io.tmpdir"), "ultrastarCatalog-thumbs"));
    }

    CoverService(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /** The cover file of a song (#COVER, or "Artist - Title [co].jpg"), or null when there is none. */
    public File coverFile(Song song) {
        if (song == null || StringUtils.isEmpty(song.getPath())) {
            return null;
        }
        String name = StringUtils.isNotBlank(song.getCover())
                ? song.getCover()
                : song.getArtist() + " - " + song.getTitle() + " [co].jpg";
        File file = new File(song.getPath() + File.separator + name);
        return file.isFile() ? file : null;
    }

    /**
     * The image to send: a thumbnail of the requested width when possible, otherwise the original cover.
     * Returns null when the song has no cover (the caller then sends the default picture).
     */
    public File image(Song song, Integer width) {
        File cover = coverFile(song);
        if (cover == null || width == null || !WIDTHS.contains(width)) {
            return cover;
        }
        Path thumb = cacheDir.resolve(song.getUid() + "-" + width + ".jpg");
        try {
            if (Files.isRegularFile(thumb) && Files.getLastModifiedTime(thumb).toMillis() >= cover.lastModified()) {
                return thumb.toFile();
            }
            return createThumbnail(cover, thumb, width) ? thumb.toFile() : cover;
        } catch (IOException | RuntimeException e) {
            // unreadable or unusual image (e.g. CMYK JPEG): the original is sent as it is
            return cover;
        }
    }

    /** Writes a JPEG of the given width. Returns false when the cover is already small or cannot be read. */
    private boolean createThumbnail(File cover, Path thumb, int width) throws IOException {
        BufferedImage source = ImageIO.read(cover);
        if (source == null || source.getWidth() <= width) {
            return false;
        }
        int height = Math.max(1, Math.round((float) source.getHeight() * width / source.getWidth()));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.BLACK); // background for transparent PNG covers
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        Files.createDirectories(cacheDir);
        // written to a temporary file first, so that a request never reads a half-written thumbnail
        Path tmp = Files.createTempFile(cacheDir, "thumb", ".tmp");
        try {
            writeJpeg(target, tmp.toFile());
            Files.move(tmp, thumb, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return true;
    }

    private static void writeJpeg(BufferedImage image, File file) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
