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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

public class ImageExportService {
    private static final double EXPORT_SCALE = 2.0;
    private static final float JPEG_QUALITY = 0.95f;

    public void exportPng(MindMapCanvas canvas, Path path) throws IOException {
        exportPng(createExportSnapshot(canvas), path);
    }

    public void exportJpg(MindMapCanvas canvas, Path path) throws IOException {
        exportJpg(createExportSnapshot(canvas), path);
    }

    public void exportPdf(MindMapCanvas canvas, Path path) throws IOException {
        exportPdf(createExportSnapshot(canvas), path);
    }

    public WritableImage createExportSnapshot(MindMapCanvas canvas) {
        // 按更高倍率截图，保证导出的图片细节足够清晰。
        return canvas.snapshotFull(EXPORT_SCALE);
    }

    public void exportPng(WritableImage image, Path path) throws IOException {
        write(image, path, "png");
    }

    public void exportJpg(WritableImage image, Path path) throws IOException {
        write(image, path, "jpg");
    }

    public void exportPdf(WritableImage image, Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        BufferedImage buffered = toBufferedImage(image, true);
        writePdf(buffered, path);
    }

    private void write(WritableImage image, Path path, String format) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        // JPG 不支持透明通道，因此需要先转换为不透明背景。
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

    private void writePdf(BufferedImage image, Path path) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] imageData = deflateRgb(image);
        double pageWidth = width / EXPORT_SCALE;
        double pageHeight = height / EXPORT_SCALE;
        String content = "q\n" + format(pageWidth) + " 0 0 " + format(pageHeight) + " 0 0 cm\n/Im0 Do\nQ\n";
        byte[] contentData = content.getBytes(StandardCharsets.US_ASCII);

        List<byte[]> objects = new ArrayList<>();
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R >>\n"));
        objects.add(ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n"));
        objects.add(ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + format(pageWidth) + " "
                + format(pageHeight) + "] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\n"));
        objects.add(join(
                ascii("<< /Type /XObject /Subtype /Image /Width " + width + " /Height " + height
                        + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
                        + imageData.length + " >>\nstream\n"),
                imageData,
                ascii("\nendstream\n")));
        objects.add(join(
                ascii("<< /Length " + contentData.length + " >>\nstream\n"),
                contentData,
                ascii("endstream\n")));

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(ascii("%PDF-1.4\n%"));
        pdf.write(new byte[]{(byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3});
        pdf.write(ascii("\n"));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.size());
            pdf.write(ascii((i + 1) + " 0 obj\n"));
            pdf.write(objects.get(i));
            pdf.write(ascii("endobj\n"));
        }
        int xrefOffset = pdf.size();
        pdf.write(ascii("xref\n0 " + offsets.size() + "\n"));
        pdf.write(ascii("0000000000 65535 f \n"));
        for (int i = 1; i < offsets.size(); i++) {
            pdf.write(ascii(String.format("%010d 00000 n \n", offsets.get(i))));
        }
        pdf.write(ascii("trailer\n<< /Size " + offsets.size() + " /Root 1 0 R >>\nstartxref\n"
                + xrefOffset + "\n%%EOF\n"));
        Files.write(path, pdf.toByteArray());
    }

    private byte[] deflateRgb(BufferedImage image) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(image.getWidth() * image.getHeight() * 3);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                raw.write((rgb >> 16) & 0xFF);
                raw.write((rgb >> 8) & 0xFF);
                raw.write(rgb & 0xFF);
            }
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
            raw.writeTo(deflater);
        }
        return compressed.toByteArray();
    }

    private String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] join(byte[]... parts) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            output.write(part);
        }
        return output.toByteArray();
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
