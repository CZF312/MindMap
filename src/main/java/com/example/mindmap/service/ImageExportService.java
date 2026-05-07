package com.example.mindmap.service;

import com.example.mindmap.view.MindMapCanvas;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class ImageExportService {
    private static final double EXPORT_SCALE = 2.0;
    private static final float JPEG_QUALITY = 0.95f;

    public void exportPng(MindMapCanvas canvas, Path path) throws IOException {
        write(canvas.snapshotFull(EXPORT_SCALE), path, "png");
    }

    public void exportJpg(MindMapCanvas canvas, Path path) throws IOException {
        write(canvas.snapshotFull(EXPORT_SCALE), path, "jpg");
    }

    private void write(WritableImage image, Path path, String format) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        BufferedImage buffered = toBufferedImage(image, "jpg".equals(format));
        if ("jpg".equals(format)) {
            writeJpg(buffered, path);
        } else {
            ImageIO.write(buffered, format, path.toFile());
        }
    }

    private void writeJpg(BufferedImage image, Path path) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", path.toFile());
            return;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
        }
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage toBufferedImage(WritableImage image, boolean opaque) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(width, height,
                opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        if (opaque) {
            Graphics2D graphics = buffered.createGraphics();
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.dispose();
        }
        var reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                int alpha = opaque ? 255 : (int) Math.round(color.getOpacity() * 255);
                int red = (int) Math.round(color.getRed() * 255);
                int green = (int) Math.round(color.getGreen() * 255);
                int blue = (int) Math.round(color.getBlue() * 255);
                buffered.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return buffered;
    }
}
