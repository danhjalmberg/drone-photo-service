package io.github.danhjalmberg.dronephotoservice.support;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Iterator;
import java.util.List;

/**
 * Writes {@link BufferedImage} sequences as animated GIF files using ImageIO.
 *
 * <p>This implementation uses the standard Java ImageIO API and does not rely
 * on external libraries. It supports configurable frame delays and looping.
 * GIF color-depth and transparency limitations are handled by converting
 * frames to opaque RGB format. Frame delays are stored in GIF centiseconds:
 * millisecond values are truncated to 10 ms units with a minimum of 10 ms.</p>
 *
 * <p>A null or empty frame list returns without creating output. Interruption is
 * checked before and between frame writes and before sequence completion, but an
 * individual ImageIO write may not respond immediately. This writer closes its
 * stream and disposes the ImageIO writer; it does not remove partial files after
 * cancellation or other failure.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * List<BufferedImage> frames = ...;
 * File outputFile = new File("output.gif");
 * int frameDelayMs = 100;
 * boolean loop = true;
 * AnimatedGifWriter.writeGif(frames, outputFile, frameDelayMs, loop);
 * }</pre>
 *
 * @author Dan Hjälmberg
 */
public final class AnimatedGifWriter {

    /**
     * Prevents instantiation of this utility class.
     */
    private AnimatedGifWriter() {
    }

    /**
     * Writes frames in list order to a GIF sequence.
     *
     * @param frames       frames to encode; null and empty lists have no effect
     * @param outputFile   destination GIF file
     * @param frameDelayMs requested frame delay in milliseconds
     * @param loop         whether to add indefinite-loop metadata
     * @throws NullPointerException     if a frame is null
     * @throws IllegalArgumentException if output is required and
     *                                  {@code outputFile} is null
     * @throws IOException              if no GIF writer exists, writing fails, metadata cannot
     *                                  be configured, or cancellation is detected
     */
    public static void writeGif(List<BufferedImage> frames,
                                File outputFile,
                                int frameDelayMs,
                                boolean loop) throws IOException {

        if (frames == null || frames.isEmpty()) {
            return;
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");

        if (!writers.hasNext()) {
            throw new IOException("No GIF ImageWriter found.");
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile)) {

            checkForCancellation();

            writer.setOutput(outputStream);
            writer.prepareWriteSequence(null);

            for (BufferedImage frame : frames) {

                checkForCancellation();

                BufferedImage gifFrame = toRgb(frame);

                ImageWriteParam params = writer.getDefaultWriteParam();

                ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromBufferedImageType(gifFrame.getType());

                IIOMetadata metadata = writer.getDefaultImageMetadata(imageType, params);

                configureMetadata(metadata, frameDelayMs, loop);

                writer.writeToSequence(
                        new IIOImage(gifFrame, null, metadata),
                        params);
            }

            checkForCancellation();

            writer.endWriteSequence();

        } finally {
            writer.dispose();
        }
    }

    /**
     * Returns an existing RGB frame or renders it into a new opaque RGB image.
     *
     * @param source source frame
     * @return source itself when already {@code TYPE_INT_RGB}, otherwise a copy
     */
    private static BufferedImage toRgb(BufferedImage source) {

        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        BufferedImage rgbImage = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        return rgbImage;
    }

    /**
     * Configures non-transparent frame timing and optional Netscape indefinite
     * looping metadata.
     *
     * @param metadata The IIOMetadata object to configure.
     * @param delayMs  The delay time in milliseconds.
     * @param loop     Whether the GIF should loop indefinitely.
     * @throws IOException If an I/O error occurs during metadata configuration.
     */
    private static void configureMetadata(IIOMetadata metadata,
                                          int delayMs,
                                          boolean loop) throws IOException {

        String nativeFormat = metadata.getNativeMetadataFormatName();

        IIOMetadataNode root =
                (IIOMetadataNode) metadata.getAsTree(nativeFormat);

        IIOMetadataNode graphicsControlExtension =
                getOrCreateNode(root, "GraphicControlExtension");

        graphicsControlExtension.setAttribute("disposalMethod", "none");
        graphicsControlExtension.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtension.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtension.setAttribute(
                "delayTime",
                Integer.toString(Math.max(1, delayMs / 10)));
        graphicsControlExtension.setAttribute("transparentColorIndex", "0");

        if (loop) {
            IIOMetadataNode appExtensions =
                    getOrCreateNode(root, "ApplicationExtensions");

            IIOMetadataNode appExtension =
                    new IIOMetadataNode("ApplicationExtension");

            appExtension.setAttribute("applicationID", "NETSCAPE");
            appExtension.setAttribute("authenticationCode", "2.0");

            appExtension.setUserObject(new byte[]{
                    0x1,
                    0x0,
                    0x0
            });

            appExtensions.appendChild(appExtension);
        }

        metadata.setFromTree(nativeFormat, root);
    }

    /**
     * Retrieves an existing child node with the specified name from the given root node,
     * or creates a new one if it does not exist.
     *
     * @param root     The root node to search for the child node.
     * @param nodeName The name of the child node to retrieve or create.
     * @return The existing or newly created child node.
     */
    private static IIOMetadataNode getOrCreateNode(IIOMetadataNode root,
                                                   String nodeName) {

        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) root.item(i);
            }
        }

        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }

    /**
     * Stops GIF generation when the export worker has been interrupted.
     *
     * @throws InterruptedIOException if cancellation has been requested
     */
    private static void checkForCancellation()
            throws InterruptedIOException {

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException(
                    "Animated GIF export was cancelled.");
        }
    }
}
