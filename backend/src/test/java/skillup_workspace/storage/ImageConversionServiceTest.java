package skillup_workspace.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import skillup_workspace.config.ImageWebpProperties;
import skillup_workspace.config.R2StorageProperties;
import skillup_workspace.exception.InvalidUploadException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageConversionServiceTest {

    private ImageWebpProperties imageProperties;
    private R2StorageProperties r2Properties;
    private ImageConversionService imageConversionService;

    @BeforeEach
    void setUp() {
        imageProperties = new ImageWebpProperties();
        r2Properties = new R2StorageProperties();
        imageConversionService = new ImageConversionService(imageProperties, r2Properties);
        imageConversionService.validateConfiguration();
    }

    @Test
    void convertsJpegToWebpAndPreservesDimensions() throws Exception {
        BufferedImage jpeg = new BufferedImage(32, 18, BufferedImage.TYPE_INT_RGB);
        jpeg.setRGB(4, 5, Color.BLUE.getRGB());

        ImageConversionService.ConvertedImage result = imageConversionService.convertToWebp(
                multipart("source.jpg", "image/jpeg", encode(jpeg, "jpeg"))
        );

        assertWebp(result.content());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.content()));
        assertEquals(32, decoded.getWidth());
        assertEquals(18, decoded.getHeight());
    }

    @Test
    void convertsPngToWebpAndPreservesTransparentPixels() throws Exception {
        BufferedImage png = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        png.setRGB(0, 0, 0x00000000);
        png.setRGB(5, 5, 0x80ff0000);

        ImageConversionService.ConvertedImage result = imageConversionService.convertToWebp(
                multipart("source.png", "image/png", encode(png, "png"))
        );

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.content()));
        assertTrue(decoded.getColorModel().hasAlpha());
        assertEquals(0, decoded.getRGB(0, 0) >>> 24);
        assertTrue((decoded.getRGB(5, 5) >>> 24) > 0);
    }

    @Test
    void convertsGifBmpAndWebpSourcesToWebp() throws Exception {
        BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        source.setRGB(1, 1, Color.GREEN.getRGB());

        for (String format : new String[]{"gif", "bmp"}) {
            ImageConversionService.ConvertedImage result = imageConversionService.convertToWebp(
                    multipart("source." + format, "application/octet-stream", encode(source, format))
            );
            assertWebp(result.content());
        }

        ImageConversionService.ConvertedImage firstWebp = imageConversionService.convertToWebp(
                multipart("source.jpg", "image/jpeg", encode(source, "jpeg"))
        );
        ImageConversionService.ConvertedImage webpSource = imageConversionService.convertToWebp(
                multipart("source.webp", "image/webp", firstWebp.content())
        );
        assertWebp(webpSource.content());
    }

    @Test
    void rejectsInvalidImageDataEvenWhenTheNameAndMimeTypeLookValid() {
        MockMultipartFile invalid = multipart("image.png", "image/png", "not an image".getBytes());

        assertThrows(InvalidUploadException.class, () -> imageConversionService.convertToWebp(invalid));
    }

    @Test
    void rejectsUnsupportedImageFormat() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes();

        InvalidUploadException error = assertThrows(
                InvalidUploadException.class,
                () -> imageConversionService.convertToWebp(multipart("image.svg", "image/svg+xml", svg))
        );

        assertTrue(error.getMessage().contains("no es compatible"));
    }

    @Test
    void rejectsImagesLargerThanTheConfiguredOriginalSizeLimit() {
        byte[] oversized = new byte[(int) r2Properties.getMaxImageSizeBytes() + 1];
        MockMultipartFile image = multipart("large.png", "image/png", oversized);

        InvalidUploadException error = assertThrows(
                InvalidUploadException.class,
                () -> imageConversionService.convertToWebp(image)
        );

        assertTrue(error.getMessage().contains("original supera"));
    }

    @Test
    void rejectsImagesWithExcessivePixelDimensionsBeforeDecoding() throws Exception {
        imageProperties.setMaxPixels(10);
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);

        InvalidUploadException error = assertThrows(
                InvalidUploadException.class,
                () -> imageConversionService.convertToWebp(
                        multipart("large.png", "image/png", encode(image, "png"))
                )
        );

        assertTrue(error.getMessage().contains("dimensiones"));
    }

    @Test
    void rejectsInvalidQualityConfiguration() {
        imageProperties.setQuality(1.1f);

        assertThrows(IllegalStateException.class, imageConversionService::validateConfiguration);
    }

    private MockMultipartFile multipart(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("image", name, contentType, bytes);
    }

    private byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        return output.toByteArray();
    }

    private void assertWebp(byte[] bytes) {
        assertEquals('R', bytes[0]);
        assertEquals('I', bytes[1]);
        assertEquals('F', bytes[2]);
        assertEquals('F', bytes[3]);
        assertEquals('W', bytes[8]);
        assertEquals('E', bytes[9]);
        assertEquals('B', bytes[10]);
        assertEquals('P', bytes[11]);
    }
}
