package spbu.meetingAI.service;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spbu.meetingAI.entity.Meeting;
import spbu.meetingAI.repository.MeetingRepository;

@Service
public class MeetingService {
    final private String BUCKET_NAME = "mediafiles";
    final private String EXTENSION = ".pcm";
    final private String S3_ENDPOINT = "storage.yandexcloud.net";
    final private String TRANSCRIPTION_ENDPOINT = "https://transcribe.api.cloud.yandex.net/speech/stt/v2/longRunningRecognize";
    final private String RECOGNITION_RESULT_ENDPOINT = "https://operation.api.cloud.yandex.net/operations/";
    final private String SIGNING_REGION = "ru-central1";

    final HttpClient client = HttpClient.newHttpClient();

    MeetingRepository meetingRepository;

    @Autowired
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public CompletableFuture<Meeting> getMeeting(UUID id) {
        return CompletableFuture.completedFuture(meetingRepository.findById(id).orElseThrow());
                //TODO add handle
    }

    public Meeting createMeeting(MultipartFile file) {
        Meeting meeting = new Meeting();
        meeting.setTitle(file.getName());
        return meeting;
    }

    public void uploadRecording(MultipartFile file) throws InterruptedException, URISyntaxException, IOException {
        Meeting meeting = createMeeting(file);
        System.out.println(meeting.getId());
        uploadToS3(file, meeting);
        Thread.sleep(10000);
        String operationId = sendToRecognition(meeting);
        String transcription = getTranscription(operationId);
    }

    private void uploadToS3(MultipartFile file, Meeting meeting) throws IOException {
        AWSCredentials credentials;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AmazonS3ClientBuilder.EndpointConfiguration(
                                S3_ENDPOINT, SIGNING_REGION
                        )
                )
                .build();

        String key = meeting.getId() + EXTENSION;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());

        s3.putObject(new PutObjectRequest(BUCKET_NAME, key, new ByteArrayInputStream(file.getBytes()), metadata));
    }

    private String sendToRecognition(Meeting meeting) throws URISyntaxException, IOException, InterruptedException {
        String json = new JSONObject()
                .put("config", new JSONObject()
                        .put("specification", new JSONObject()
                                .put("languageCode", "ru-RU")
                                .put("model", "general:rc")
                                .put("audioEncoding", "LINEAR16_PCM")
                                .put("sampleRateHertz", 16000)
                                .put("audioChannelCount", 2)
                                .put("literature_text", true)))
                .put("audio", new JSONObject()
                        .put("uri", "https://" + S3_ENDPOINT + "/" + BUCKET_NAME + "/" + meeting.getId() + EXTENSION))
                .toString();

        var request = HttpRequest.newBuilder()
                .uri(new URI(TRANSCRIPTION_ENDPOINT))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"), "Content-Type", "application/json")
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        var operationId = "";

        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        System.out.println(response.statusCode());
        var body = response.body();
        //TODO add status code handling
        var idString = body.filter(s -> s.contains("id")).findFirst();
        if (idString.isPresent()) {
            var temp = idString.get();
            operationId = temp.substring(8, temp.length() - 2);
            System.out.println(operationId);
        }
        return operationId;
    }

    public String getTranscription(String operationId) throws URISyntaxException, InterruptedException, IOException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(RECOGNITION_RESULT_ENDPOINT + operationId))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        boolean found = false;
        var str = "";
        while (!found) {
            Thread.sleep(10000);
            str = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            found = str.contains("\"done\": true,");
        }

        StringBuilder transcript = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(str);
        var chunks = root.get("response").get("chunks");
        if (chunks == null) {
            return "";
        }
        for (JsonNode chunk: chunks) {
            var channel = chunk.get("channelTag").asInt();
            if (channel != 1) {
                continue;
            }
            var alternatives = chunk.get("alternatives");
            if (alternatives.size() != 1) {
                System.out.println(alternatives.textValue());
            } else {
                var text = alternatives.get(0).get("text").textValue();
                transcript.append(" ").append(text);
            }
        }
        System.out.println(transcript);
        return transcript.toString();
    }
}
