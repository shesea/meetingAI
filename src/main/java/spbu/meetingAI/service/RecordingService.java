package spbu.meetingAI.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spbu.meetingAI.entity.Meeting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

@Service
public class RecordingService {
    private static final Logger logger
            = LoggerFactory.getLogger(RecordingService.class);

    public static final String BUCKET_NAME = "mediafiles";
    private static final String EXTENSION = ".pcm";
    private static final String S3_ENDPOINT = "storage.yandexcloud.net";
    private static final String SIGNING_REGION = "ru-central1";

    private final AWSCredentials credentials;
    private final AmazonS3 s3;


    public RecordingService() {
        logger.info("Getting aws credentials");
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        logger.info("Building s3 client");
        s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AmazonS3ClientBuilder.EndpointConfiguration(
                                S3_ENDPOINT, SIGNING_REGION
                        )
                )
                .build();
    }

    public UUID uploadRecording(MultipartFile file) throws IOException {
        UUID id = UUID.randomUUID();
        String key = id + EXTENSION;
        logger.info("Generated meeting id: {}", id);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());

        logger.info("Starting file upload to s3, meeting id: {}", id);
        s3.putObject(new PutObjectRequest(BUCKET_NAME, key, new ByteArrayInputStream(file.getBytes()), metadata));
        logger.info("Successfully uploaded file to s3, id: {}", id);
        return id;
    }

    public Meeting getRecordingLink(Meeting meeting) {
        //TODO Links are hardcoded due to absence of sending video feature in desktop app
        URL url;
        if (Objects.equals(meeting.getId().toString(), "819546bf-9e44-4a27-8606-a8afb6392742") || Objects.equals(meeting.getId().toString(), "49849977-a9d9-4d44-9c82-f8f526562f23")) {
            url = s3.generatePresignedUrl(new GeneratePresignedUrlRequest(BUCKET_NAME, "meeting.mp4"));
        } else {
            url = s3.generatePresignedUrl(new GeneratePresignedUrlRequest(BUCKET_NAME, "video1661560214.mp4"));
        }
        meeting.setVideoLink(url);
        logger.info("Got video link '{}' for meeting with id: {}", url, meeting.getId());
        return meeting;
    }

    public void deleteRecording(UUID id) {
        String key = id + EXTENSION;
        logger.info("Starting file deletion from s3, meeting id: {}", id);
        s3.deleteObject(new DeleteObjectRequest(BUCKET_NAME, key));
        logger.info("Successfully deleted file from s3, meeting id: {}", id);
    }
}
