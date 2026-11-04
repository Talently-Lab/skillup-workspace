package skillup_workspace.storage;

import com.luciad.imageio.webp.WebPWriteParam;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import skillup_workspace.config.ImageWebpProperties;
import skillup_workspace.config.R2StorageProperties;
import skillup_workspace.exception.InvalidUploadException;
import skillup_workspace.exception.R2StorageException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class ImageConversionService {

    private static final String WEBP_MIME_TYPE = "image/webp";
    private static final String LOSSY_COMPRESSION = "Lossy";

    private final ImageWebpProperties properties;
    private final R2StorageProperties r2Properties;

    @PostConstruct
    void validateConfiguration() {
        if (properties.getQuality() < 0f || properties.getQuality() > 1f
                || !Float.isFinite(properties.getQuality())) {
            throw new IllegalStateException("image.webp.quality debe estar entre 0.0 y 1.0.");
        }
        if (properties.getMaxPixels() < 1) {
            throw new IllegalStateException("image.webp.max-pixels debe ser mayor que cero.");
        }
        if (properties.isEnabled() && !ImageIO.getImageWritersByMIMEType(WEBP_MIME_TYPE).hasNext()) {
            throw new IllegalStateException(
                    "No hay un codificador WebP disponible. Verifica la dependencia webp-imageio y sus bibliotecas nativas."
            );
        }
    }

    public ConvertedImage convertToWebp(MultipartFile file) {
        if (!properties.isEnabled()) {
            throw new R2StorageException("La conversión de imágenes a WebP no está habilitada.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("La imagen no puede estar vacía.");
        }
        if (file.getSize() > r2Properties.getMaxImageSizeBytes()) {
            throw new InvalidUploadException("La imagen original supera el límite de 5 MB.");
        }

        try {
            BufferedImage decodedImage = decodeImage(file.getBytes());
            byte[] webpBytes = encodeWebp(decodedImage);
            if (webpBytes.length > r2Properties.getMaxImageSizeBytes()) {
                throw new InvalidUploadException("La imagen convertida supera el límite de 5 MB.");
            }
            return new ConvertedImage(webpBytes);
        } catch (IOException exception) {
            throw new InvalidUploadException("No se pudo leer o convertir la imagen seleccionada.", exception);
        }
    }

    private BufferedImage decodeImage(byte[] content) throws IOException {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidUploadException("El formato de imagen no es compatible.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > properties.getMaxPixels()) {
                    throw new InvalidUploadException(
                            "Las dimensiones de la imagen superan el límite de procesamiento permitido."
                    );
                }

                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new InvalidUploadException("No se pudo decodificar la imagen seleccionada.");
                }
                return normalizeColorModel(decoded);
            } catch (IndexOutOfBoundsException | IllegalArgumentException exception) {
                if (exception instanceof InvalidUploadException invalidUploadException) {
                    throw invalidUploadException;
                }
                throw new InvalidUploadException("El archivo seleccionado no contiene una imagen válida.", exception);
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage normalizeColorModel(BufferedImage source) {
        int imageType = source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage normalized = new BufferedImage(source.getWidth(), source.getHeight(), imageType);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return normalized;
    }

    private byte[] encodeWebp(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(WEBP_MIME_TYPE);
        if (!writers.hasNext()) {
            throw new R2StorageException(
                    "No hay un codificador WebP disponible en el entorno de ejecución."
            );
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (!(writeParam instanceof WebPWriteParam webpWriteParam)) {
                throw new R2StorageException("El codificador disponible no permite configurar la calidad WebP.");
            }
            webpWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            webpWriteParam.setCompressionType(LOSSY_COMPRESSION);
            webpWriteParam.setCompressionQuality(properties.getQuality());
            writer.write(null, new IIOImage(image, null, null), webpWriteParam);
            output.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    public record ConvertedImage(byte[] content) {

        public ConvertedImage {
            if (content == null || content.length == 0) {
                throw new IllegalArgumentException("La imagen WebP convertida no puede estar vacía.");
            }
        }

        public String fileName() {
            return "course-image.webp";
        }

        public String contentType() {
            return WEBP_MIME_TYPE;
        }

        public long size() {
            return content.length;
        }
    }
}
