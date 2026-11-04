package skillup_workspace.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import skillup_workspace.config.R2StorageProperties;
import skillup_workspace.exception.R2StorageException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class R2StorageServiceTest {

    private final ObjectProvider<S3Presigner> presignerProvider = mock(ObjectProvider.class);
    private final ObjectProvider<S3Client> s3ClientProvider = mock(ObjectProvider.class);
    private final S3Client s3Client = mock(S3Client.class);
    private R2StorageProperties properties;
    private R2StorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new R2StorageProperties();
        properties.setEnabled(true);
        properties.setAccountId("account");
        properties.setAccessKeyId("access-key");
        properties.setSecretAccessKey("secret-key");
        properties.setBucketName("skillupcampus");
        properties.setPublicUrl("https://assets.example.test");
        when(s3ClientProvider.getObject()).thenReturn(s3Client);
        storageService = new R2StorageService(properties, presignerProvider, s3ClientProvider);
    }

    @Test
    void uploadCreatesUniqueManagedKeyAndReturnsPublicUrl() {
        when(s3Client.putObject(
                any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(RequestBody.class)
        ))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "cover.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );

        R2StorageService.StoredAsset uploaded = storageService.upload(image, UploadPurpose.COURSE_IMAGE);

        assertTrue(uploaded.objectKey().matches(
                "^courses/images/[0-9a-f-]{36}\\.png$"
        ));
        assertEquals("https://assets.example.test/" + uploaded.objectKey(), uploaded.publicUrl());
        verify(s3Client).putObject(
                any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(RequestBody.class)
        );
    }

    @Test
    void uploadWebpUsesWebpExtensionAndContentType() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        byte[] webp = new byte[]{
                'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P'
        };

        R2StorageService.StoredAsset uploaded = storageService.uploadWebp(webp);

        assertTrue(uploaded.objectKey().matches("^courses/images/[0-9a-f-]{36}\\.webp$"));
        assertEquals("https://assets.example.test/" + uploaded.objectKey(), uploaded.publicUrl());
        org.mockito.ArgumentCaptor<PutObjectRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("image/webp", requestCaptor.getValue().contentType());
        assertTrue(requestCaptor.getValue().key().endsWith(".webp"));
    }

    @Test
    void uploadSyllabusStillUsesPdfExtensionAndContentType() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        MockMultipartFile syllabus = new MockMultipartFile(
                "syllabus",
                "course.pdf",
                "application/pdf",
                "%PDF-1.7\ncourse syllabus".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );

        R2StorageService.StoredAsset uploaded = storageService.upload(syllabus, UploadPurpose.COURSE_SYLLABUS);

        assertTrue(uploaded.objectKey().matches("^courses/syllabi/[0-9a-f-]{36}\\.pdf$"));
        org.mockito.ArgumentCaptor<PutObjectRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertEquals("application/pdf", requestCaptor.getValue().contentType());
        assertTrue(requestCaptor.getValue().key().endsWith(".pdf"));
    }

    @Test
    void uploadRejectsFileWhoseSignatureDoesNotMatchItsMimeAndExtension() {
        MockMultipartFile disguisedPdf = new MockMultipartFile(
                "image",
                "cover.png",
                "image/png",
                "%PDF-1.7".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> storageService.upload(disguisedPdf, UploadPurpose.COURSE_IMAGE)
        );

        assertEquals("El contenido del archivo no coincide con el tipo seleccionado.", error.getMessage());
        verify(s3Client, never()).putObject(
                any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(RequestBody.class)
        );
    }

    @Test
    void uploadWhenStorageIsDisabledReturnsClearError() {
        properties.setEnabled(false);
        MockMultipartFile image = new MockMultipartFile(
                "image", "cover.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );

        R2StorageException error = assertThrows(
                R2StorageException.class,
                () -> storageService.upload(image, UploadPurpose.COURSE_IMAGE)
        );

        assertTrue(error.getMessage().contains("no está habilitada"));
        verify(s3ClientProvider, never()).getObject();
    }

    @Test
    void enabledStorageWithoutCredentialsReturnsConfigurationError() {
        properties.setAccessKeyId("");
        MockMultipartFile image = new MockMultipartFile(
                "image", "cover.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
        );

        R2StorageException error = assertThrows(
                R2StorageException.class,
                () -> storageService.upload(image, UploadPurpose.COURSE_IMAGE)
        );

        assertTrue(error.getMessage().contains("R2_ACCESS_KEY_ID"));
        verify(s3ClientProvider, never()).getObject();
    }
}
