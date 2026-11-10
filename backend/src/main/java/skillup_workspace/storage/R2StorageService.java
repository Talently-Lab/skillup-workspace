package skillup_workspace.storage;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import skillup_workspace.config.R2StorageProperties;
import skillup_workspace.dto.request.CompleteUploadRequest;
import skillup_workspace.dto.request.PresignUploadRequest;
import skillup_workspace.dto.response.CompletedUploadResponse;
import skillup_workspace.dto.response.PresignedUploadResponse;
import skillup_workspace.exception.InvalidUploadException;
import skillup_workspace.exception.R2StorageException;
import skillup_workspace.observability.CourseCreationPerformanceLog;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);

    private static final String IMAGE_PREFIX = "courses/images/";
    private static final String SYLLABUS_PREFIX = "courses/syllabi/";
    private static final String PNG = "image/png";
    private static final String JPEG = "image/jpeg";
    private static final String WEBP = "image/webp";
    private static final String PDF = "application/pdf";
    private static final String MP4 = "video/mp4";
    private static final String WEBM = "video/webm";
    private static final String QUICKTIME = "video/quicktime";
    private static final String VIDEO_PREFIX = "courses/videos/";

    private final R2StorageProperties properties;
    private final ObjectProvider<S3Presigner> presignerProvider;
    private final ObjectProvider<S3Client> s3ClientProvider;

    public PresignedUploadResponse createPresignedUpload(PresignUploadRequest request) {
        ensureStorageReady();
        String extension = validateFile(
                request.fileName(), request.contentType(), request.fileSize(), request.purpose()
        );
        String prefix = prefixFor(request.purpose());
        String objectKey = prefix + UUID.randomUUID() + "." + extension;
        String contentType = normalizeContentType(request.contentType());

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(properties.getPresignedUrlDurationSeconds()))
                    .putObjectRequest(putRequest)
                    .build();
            PresignedPutObjectRequest signedRequest = presignerProvider.getObject()
                    .presignPutObject(presignRequest);
            return new PresignedUploadResponse(
                    signedRequest.url().toString(),
                    objectKey,
                    contentType,
                    properties.getPresignedUrlDurationSeconds(),
                    Map.of("Content-Type", contentType)
            );
        } catch (SdkException exception) {
            throw new R2StorageException("No se pudo preparar la carga en el almacenamiento.", exception);
        }
    }

    public StoredAsset upload(MultipartFile file, UploadPurpose purpose) {
        return upload(prepareUpload(file, purpose));
    }

    public PreparedUpload prepareUpload(MultipartFile file, UploadPurpose purpose) {
        ensureStorageReady();
        if (purpose == null) {
            throw new InvalidUploadException("El propósito del archivo no es válido.");
        }
        if (file == null) {
            throw new InvalidUploadException("El archivo es obligatorio.");
        }

        String extension = validateFile(
                file.getOriginalFilename(), file.getContentType(), file.getSize(), purpose
        );
        String objectKey = prefixFor(purpose) + UUID.randomUUID() + "." + extension;
        try {
            if (!hasValidSignature(file, purpose, objectKey)) {
                throw new InvalidUploadException(
                        "El contenido del archivo no coincide con el tipo seleccionado."
                );
            }
            return new PreparedUpload(
                    objectKey,
                    normalizeContentType(file.getContentType()),
                    file,
                    file.getSize(),
                    purpose
            );
        } catch (IOException exception) {
            throw new R2StorageException("No se pudo leer el archivo seleccionado.", exception);
        }
    }

    public StoredAsset upload(PreparedUpload upload) {
        ensureStorageReady();
        if (upload == null) {
            throw new IllegalArgumentException("La carga preparada es obligatoria.");
        }
        ContentStreamProvider contentStreamProvider = () -> {
            try {
                return upload.file().getInputStream();
            } catch (IOException exception) {
                throw new R2StorageException("No se pudo leer el archivo seleccionado.", exception);
            }
        };
        return putObject(
                upload.objectKey(),
                upload.contentType(),
                RequestBody.fromContentProvider(contentStreamProvider, upload.fileSize(), upload.contentType()),
                upload.purpose()
        );
    }

    public void validateWebp(byte[] content) {
        ensureStorageReady();
        if (content == null || content.length == 0) {
            throw new InvalidUploadException("La imagen convertida no puede estar vacía.");
        }
        if (content.length > properties.getMaxImageSizeBytes()) {
            throw new InvalidUploadException("La imagen convertida supera el límite de 5 MB.");
        }
        if (!hasValidSignature(UploadPurpose.COURSE_IMAGE, IMAGE_PREFIX + "validation.webp", content)) {
            throw new InvalidUploadException("El archivo convertido no tiene un formato WebP válido.");
        }
    }

    public StoredAsset uploadWebp(byte[] content) {
        validateWebp(content);
        return putObject(
                IMAGE_PREFIX + UUID.randomUUID() + ".webp",
                WEBP,
                RequestBody.fromBytes(content),
                UploadPurpose.COURSE_IMAGE
        );
    }

    private StoredAsset putObject(
            String objectKey,
            String contentType,
            RequestBody requestBody,
            UploadPurpose purpose
    ) {
        try {
            s3ClientProvider.getObject().putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    requestBody
            );
            return new StoredAsset(objectKey, buildPublicUrl(objectKey));
        } catch (SdkException | R2StorageException exception) {
            try {
                deleteObject(objectKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                CourseCreationPerformanceLog.warning(log, "course_creation_storage")
                        .addKeyValue("operation", "partial_upload_cleanup")
                        .addKeyValue("result", "failure")
                        .addKeyValue("errorType", cleanupException.getClass().getSimpleName())
                        .log("Could not clean up a partially uploaded object");
            }
            if (exception instanceof R2StorageException storageException) {
                throw storageException;
            }
            throw new R2StorageException(
                    purpose == UploadPurpose.COURSE_IMAGE
                            ? "No se pudo subir la imagen WebP a R2."
                            : "No se pudo subir el archivo a R2.",
                    exception
            );
        }
    }

    public void deleteAsset(StoredAsset asset) {
        if (asset != null) {
            deleteObject(asset.objectKey());
        }
    }

    public void deleteManagedAsset(String publicUrl) {
        if (!StringUtils.hasText(publicUrl) || !StringUtils.hasText(properties.getPublicUrl())) {
            return;
        }

        String prefix = validatedPublicBaseUrl() + "/";
        if (!publicUrl.startsWith(prefix)) {
            return;
        }

        String objectKey = publicUrl.substring(prefix.length());
        if (isManagedObjectKey(objectKey)) {
            deleteObject(objectKey);
        }
    }

    public CompletedUploadResponse completeUpload(CompleteUploadRequest request) {
        ensureStorageReady();
        validateObjectKey(request);

        try {
            S3Client client = s3ClientProvider.getObject();
            HeadObjectResponse object = client.headObject(builder -> builder
                    .bucket(properties.getBucketName())
                    .key(request.objectKey()));
            validateStoredObject(request, object);

            ResponseBytes<GetObjectResponse> firstBytes = client.getObject(
                    GetObjectRequest.builder()
                            .bucket(properties.getBucketName())
                            .key(request.objectKey())
                            .range("bytes=0-1023")
                            .build(),
                    ResponseTransformer.toBytes()
            );
            if (!hasValidSignature(request.purpose(), request.objectKey(), firstBytes.asByteArray())) {
                client.deleteObject(builder -> builder
                        .bucket(properties.getBucketName())
                        .key(request.objectKey()));
                throw new InvalidUploadException(
                        "El contenido del archivo no coincide con el tipo seleccionado."
                );
            }

            return new CompletedUploadResponse(buildPublicUrl(request.objectKey()));
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new InvalidUploadException("No se encontró el archivo cargado en R2.");
            }
            throw new R2StorageException("No se pudo verificar el archivo en R2.", exception);
        } catch (SdkException exception) {
            throw new R2StorageException("No se pudo verificar el archivo en R2.", exception);
        }
    }

    private String validateFile(String fileName, String contentType, long fileSize, UploadPurpose purpose) {
        if (!StringUtils.hasText(fileName) || fileName.contains("/") || fileName.contains("\\")) {
            throw new InvalidUploadException("El nombre del archivo no es válido.");
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1 || dotIndex == fileName.length() - 1) {
            throw new InvalidUploadException("El archivo debe tener una extensión permitida.");
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        contentType = normalizeContentType(contentType);
        long maxSize = maxSizeFor(purpose);

        if (fileSize < 1 || fileSize > maxSize) {
            throw new InvalidUploadException(
                    fileSize < 1
                            ? "El archivo no puede estar vacío."
                            : sizeLimitMessage(purpose)
            );
        }

        if (purpose == UploadPurpose.COURSE_IMAGE) {
            boolean valid = switch (extension) {
                case "png" -> PNG.equals(contentType);
                case "jpg", "jpeg" -> JPEG.equals(contentType);
                case "webp" -> WEBP.equals(contentType);
                default -> false;
            };
            if (!valid) {
                throw new InvalidUploadException("Selecciona una imagen PNG, JPG, JPEG o WEBP válida.");
            }
            return extension.equals("jpeg") ? "jpg" : extension;
        }

        if (purpose == UploadPurpose.COURSE_MODULE_VIDEO) {
            boolean valid = switch (extension) {
                case "mp4" -> MP4.equals(contentType);
                case "webm" -> WEBM.equals(contentType);
                case "mov" -> QUICKTIME.equals(contentType);
                default -> false;
            };
            if (!valid) {
                throw new InvalidUploadException("Selecciona un video MP4, WEBM o MOV válido.");
            }
            return extension;
        }

        if (!"pdf".equals(extension) || !PDF.equals(contentType)) {
            throw new InvalidUploadException("Selecciona un documento PDF válido.");
        }
        return "pdf";
    }

    private String prefixFor(UploadPurpose purpose) {
        return switch (purpose) {
            case COURSE_IMAGE -> IMAGE_PREFIX;
            case COURSE_SYLLABUS -> SYLLABUS_PREFIX;
            case COURSE_MODULE_VIDEO -> VIDEO_PREFIX;
        };
    }

    private boolean isManagedObjectKey(String objectKey) {
        return objectKey.matches(
                "^courses/images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:png|jpg|webp)$"
        ) || objectKey.matches(
                "^courses/syllabi/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf$"
        ) || objectKey.matches(
                "^courses/videos/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:mp4|webm|mov)$"
        );
    }

    private void deleteObject(String objectKey) {
        ensureStorageReady();
        if (!isManagedObjectKey(objectKey)) {
            throw new InvalidUploadException("La referencia del archivo no es válida.");
        }

        long startedAt = System.nanoTime();
        try {
            s3ClientProvider.getObject().deleteObject(builder -> builder
                    .bucket(properties.getBucketName())
                    .key(objectKey));
            CourseCreationPerformanceLog.event(log, "course_creation_storage")
                    .addKeyValue("operation", "delete")
                    .addKeyValue("durationMs", (System.nanoTime() - startedAt) / 1_000_000)
                    .addKeyValue("result", "success")
                    .log("Course asset deletion completed");
        } catch (SdkException exception) {
            CourseCreationPerformanceLog.warning(log, "course_creation_storage")
                    .addKeyValue("operation", "delete")
                    .addKeyValue("durationMs", (System.nanoTime() - startedAt) / 1_000_000)
                    .addKeyValue("result", "failure")
                    .addKeyValue("errorType", exception.getClass().getSimpleName())
                    .log("Course asset deletion failed");
            throw new R2StorageException("No se pudo eliminar el archivo de R2.", exception);
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private void validateObjectKey(CompleteUploadRequest request) {
        String expectedPattern = switch (request.purpose()) {
            case COURSE_IMAGE ->
                    "^courses/images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:png|jpg|webp)$";
            case COURSE_SYLLABUS ->
                    "^courses/syllabi/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf$";
            case COURSE_MODULE_VIDEO ->
                    "^courses/videos/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:mp4|webm|mov)$";
        };
        if (!request.objectKey().matches(expectedPattern)) {
            throw new InvalidUploadException("La referencia del archivo no es válida.");
        }
    }

    private void validateStoredObject(CompleteUploadRequest request, HeadObjectResponse object) {
        long maxSize = maxSizeFor(request.purpose());
        if (object.contentLength() == null || object.contentLength() < 1 || object.contentLength() > maxSize) {
            throw new InvalidUploadException("El tamaño del archivo cargado no es válido.");
        }
        String expectedContentType = contentTypeFor(request.purpose(), extensionFor(request.objectKey()));
        if (!expectedContentType.equals(normalizeContentType(object.contentType()))) {
            throw new InvalidUploadException("El tipo de contenido del archivo cargado no es válido.");
        }
    }

    private boolean hasValidSignature(UploadPurpose purpose, String objectKey, byte[] bytes) {
        String extension = extensionFor(objectKey);
        if (purpose == UploadPurpose.COURSE_SYLLABUS) {
            return containsAscii(bytes, "%PDF-");
        }
        if (purpose == UploadPurpose.COURSE_MODULE_VIDEO) {
            return switch (extension) {
                case "mp4", "mov" -> bytes.length >= 8 && asciiAt(bytes, 4, "ftyp");
                case "webm" -> bytes.length >= 4
                        && bytes[0] == 0x1a && bytes[1] == 0x45
                        && bytes[2] == (byte) 0xdf && bytes[3] == (byte) 0xa3;
                default -> false;
            };
        }
        return switch (extension) {
            case "png" -> startsWith(bytes, new byte[]{
                    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
            });
            case "jpg" -> bytes.length >= 3
                    && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
            case "webp" -> bytes.length >= 12
                    && asciiAt(bytes, 0, "RIFF") && asciiAt(bytes, 8, "WEBP");
            default -> false;
        };
    }

    private boolean hasValidSignature(MultipartFile file, UploadPurpose purpose, String objectKey) throws IOException {
        try (InputStream input = file.getInputStream()) {
            if (purpose == UploadPurpose.COURSE_SYLLABUS) {
                return containsAscii(input, "%PDF-");
            }
            return hasValidSignature(purpose, objectKey, input.readNBytes(12));
        }
    }

    private boolean containsAscii(InputStream input, String signature) throws IOException {
        byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
        int matched = 0;
        int nextByte;
        while ((nextByte = input.read()) != -1) {
            if ((byte) nextByte == expected[matched]) {
                matched++;
                if (matched == expected.length) {
                    return true;
                }
            } else {
                matched = (byte) nextByte == expected[0] ? 1 : 0;
            }
        }
        return false;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) return false;
        }
        return true;
    }

    private boolean containsAscii(byte[] bytes, String signature) {
        byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
        for (int offset = 0; offset <= bytes.length - expected.length; offset++) {
            boolean match = true;
            for (int index = 0; index < expected.length; index++) {
                if (bytes[offset + index] != expected[index]) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    private boolean asciiAt(byte[] bytes, int offset, String expected) {
        byte[] signature = expected.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < offset + signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (bytes[offset + index] != signature[index]) return false;
        }
        return true;
    }

    private String extensionFor(String objectKey) {
        return objectKey.substring(objectKey.lastIndexOf('.') + 1);
    }

    private String contentTypeForImage(String extension) {
        return switch (extension) {
            case "png" -> PNG;
            case "jpg" -> JPEG;
            case "webp" -> WEBP;
            default -> "";
        };
    }

    private long maxSizeFor(UploadPurpose purpose) {
        return switch (purpose) {
            case COURSE_IMAGE -> properties.getMaxImageSizeBytes();
            case COURSE_SYLLABUS -> properties.getMaxPdfSizeBytes();
            case COURSE_MODULE_VIDEO -> properties.getMaxVideoSizeBytes();
        };
    }

    private String sizeLimitMessage(UploadPurpose purpose) {
        return switch (purpose) {
            case COURSE_IMAGE -> "La imagen supera el límite de 5 MB.";
            case COURSE_SYLLABUS -> "El PDF supera el límite de 20 MB.";
            case COURSE_MODULE_VIDEO -> "El video supera el límite de 20 MB.";
        };
    }

    private String contentTypeFor(UploadPurpose purpose, String extension) {
        if (purpose == UploadPurpose.COURSE_MODULE_VIDEO) {
            return switch (extension) {
                case "mp4" -> MP4;
                case "webm" -> WEBM;
                case "mov" -> QUICKTIME;
                default -> "";
            };
        }
        return purpose == UploadPurpose.COURSE_SYLLABUS ? PDF : contentTypeForImage(extension);
    }

    private String buildPublicUrl(String objectKey) {
        return validatedPublicBaseUrl() + "/" + objectKey;
    }

    private String validatedPublicBaseUrl() {
        String baseUrl = properties.getPublicUrl();
        try {
            URI uri = URI.create(baseUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !StringUtils.hasText(uri.getHost())
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new R2StorageException(
                    "R2_PUBLIC_URL debe ser el dominio HTTPS real configurado para servir los objetos.",
                    exception
            );
        }
        return baseUrl.replaceAll("/+$", "");
    }

    private void ensureStorageReady() {
        if (!properties.isEnabled()) {
            throw new R2StorageException(
                    "La carga de archivos no está habilitada. Configura R2_ENABLED y las variables de R2 en Render."
            );
        }
        if (!StringUtils.hasText(properties.getAccountId())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getSecretAccessKey())
                || !StringUtils.hasText(properties.getBucketName())) {
            throw new R2StorageException(
                    "R2_ENABLED=true requiere R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, "
                            + "R2_SECRET_ACCESS_KEY y R2_BUCKET_NAME configurados."
            );
        }
        if (!StringUtils.hasText(properties.getPublicUrl())) {
            throw new R2StorageException(
                    "La carga no está disponible hasta configurar R2_PUBLIC_URL con el dominio público real del bucket."
            );
        }
        validatedPublicBaseUrl();
        if (properties.getPresignedUrlDurationSeconds() < 1
                || properties.getPresignedUrlDurationSeconds() > 900) {
            throw new R2StorageException("La duración de la autorización de carga no está configurada correctamente.");
        }
    }

    public record StoredAsset(String objectKey, String publicUrl) {
    }

    public record PreparedUpload(
            String objectKey,
            String contentType,
            MultipartFile file,
            long fileSize,
            UploadPurpose purpose
    ) {
    }
}
