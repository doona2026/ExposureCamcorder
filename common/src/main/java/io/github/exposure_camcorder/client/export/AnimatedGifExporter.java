package io.github.exposure_camcorder.client.export;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.client.util.LevelNameGetter;
import org.jetbrains.annotations.Nullable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class AnimatedGifExporter {
    private static final String GIF_METADATA_FORMAT = "javax_imageio_gif_image_1.0";

    protected final List<? extends Image> frames;
    protected final String fileName;
    protected String folder = "exposures";
    @Nullable
    protected String worldName = null;
    protected long creationUnixTimestamp = 0;
    protected int frameDelayCentiseconds = 10;
    protected boolean loop = true;
    protected Consumer<File> onExport = f -> { };

    public AnimatedGifExporter(List<? extends Image> frames, String fileName) {
        this.frames = List.copyOf(frames);
        this.fileName = fileName;
    }

    public AnimatedGifExporter withFolder(String folder) {
        this.folder = folder;
        return this;
    }

    public AnimatedGifExporter toExposuresFolder() {
        this.folder = "exposures";
        return this;
    }

    public AnimatedGifExporter organizeByWorld(@Nullable String worldName) {
        this.worldName = worldName;
        return this;
    }

    public AnimatedGifExporter organizeByWorld(boolean organize) {
        this.worldName = organize ? LevelNameGetter.getWorldName() : null;
        return this;
    }

    public AnimatedGifExporter setCreationDate(long unixTimestamp) {
        this.creationUnixTimestamp = unixTimestamp;
        return this;
    }

    public AnimatedGifExporter setFrameDelayCentiseconds(int frameDelayCentiseconds) {
        this.frameDelayCentiseconds = Math.max(1, frameDelayCentiseconds);
        return this;
    }

    public AnimatedGifExporter setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    public AnimatedGifExporter onExport(Consumer<File> onExport) {
        this.onExport = onExport;
        return this;
    }

    public boolean export() {
        if (frames.isEmpty()) {
            return false;
        }

        String filepath = getFolder() + "/" + (getWorldSubfolder() != null ? getWorldSubfolder() + "/" : "")
                + getFileName() + ".gif";
        File outputFile = new File(filepath);
        boolean ignored = outputFile.getParentFile().mkdirs();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
        if (!writers.hasNext()) {
            Exposure.LOGGER.error("Failed to export animated gif: no GIF writer is available.");
            return false;
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream output = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);

            for (Image frame : frames) {
                BufferedImage bufferedImage = convertToBufferedImage(frame);
                IIOMetadata metadata = writer.getDefaultImageMetadata(imageType, writeParam);
                configureMetadata(metadata);
                writer.writeToSequence(new IIOImage(bufferedImage, null, metadata), writeParam);
            }

            writer.endWriteSequence();

            if (creationUnixTimestamp > 0) {
                trySetFileCreationDate(outputFile.getAbsolutePath(), creationUnixTimestamp);
            }

            onExport.accept(outputFile);
            Exposure.LOGGER.info("Animated gif saved: {}", outputFile);
            return true;
        }
        catch (Exception e) {
            Exposure.LOGGER.error("Failed to save animated gif to file: {}", e.toString());
            return false;
        }
        finally {
            writer.dispose();
        }
    }

    public String getFileName() {
        return fileName;
    }

    public String getFolder() {
        return folder;
    }

    public @Nullable String getWorldSubfolder() {
        return worldName;
    }

    protected BufferedImage convertToBufferedImage(Image image) {
        BufferedImage bufferedImage = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < image.width(); x++) {
            for (int y = 0; y < image.height(); y++) {
                bufferedImage.setRGB(x, y, image.getPixelARGB(x, y));
            }
        }

        return bufferedImage;
    }

    protected void configureMetadata(IIOMetadata metadata) throws Exception {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(GIF_METADATA_FORMAT);

        IIOMetadataNode graphicsControlExtension = getOrCreateChild(root, "GraphicControlExtension");
        graphicsControlExtension.setAttribute("disposalMethod", "none");
        graphicsControlExtension.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtension.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtension.setAttribute("delayTime", Integer.toString(frameDelayCentiseconds));
        graphicsControlExtension.setAttribute("transparentColorIndex", "0");

        if (loop) {
            IIOMetadataNode applicationExtensions = getOrCreateChild(root, "ApplicationExtensions");
            IIOMetadataNode applicationExtension = new IIOMetadataNode("ApplicationExtension");
            applicationExtension.setAttribute("applicationID", "NETSCAPE");
            applicationExtension.setAttribute("authenticationCode", "2.0");
            applicationExtension.setUserObject(new byte[]{0x1, 0x0, 0x0});
            applicationExtensions.appendChild(applicationExtension);
        }

        metadata.setFromTree(GIF_METADATA_FORMAT, root);
    }

    protected IIOMetadataNode getOrCreateChild(IIOMetadataNode root, String name) {
        for (int index = 0; index < root.getLength(); index++) {
            if (root.item(index) instanceof IIOMetadataNode node && name.equals(node.getNodeName())) {
                return node;
            }
        }

        IIOMetadataNode child = new IIOMetadataNode(name);
        root.appendChild(child);
        return child;
    }

    protected void trySetFileCreationDate(String filePath, long creationTimeUnixSeconds) {
        try {
            Date creationDate = Date.from(Instant.ofEpochSecond(creationTimeUnixSeconds));

            BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(filePath), BasicFileAttributeView.class);
            FileTime creationTime = FileTime.fromMillis(creationDate.getTime());
            FileTime modifyTime = FileTime.fromMillis(System.currentTimeMillis());
            attributes.setTimes(modifyTime, modifyTime, creationTime);
        }
        catch (Exception ignored) { }
    }
}
