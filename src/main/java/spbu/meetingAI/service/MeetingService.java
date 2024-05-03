package spbu.meetingAI.service;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spbu.meetingAI.dto.MeetingDto;
import spbu.meetingAI.entity.Meeting;
import spbu.meetingAI.repository.MeetingRepository;

@Service
public class MeetingService {
    final private String BUCKET_NAME = "mediafiles";
    final private String EXTENSION = ".pcm";
    final private String S3_ENDPOINT = "storage.yandexcloud.net";
    final private String TRANSCRIPTION_ENDPOINT = "https://transcribe.api.cloud.yandex.net/speech/stt/v2/longRunningRecognize";
    final private String OPERATION_RESULT_ENDPOINT = "https://operation.api.cloud.yandex.net/operations/";
    final private String COMPLETION_ENDPOINT = "https://llm.api.cloud.yandex.net/foundationModels/v1/completionAsync";
    final private String LANGUAGE_MODEL_URI = "gpt://b1gsskjdo0qgt9m8g39i/yandexgpt/latest";
    final private String SIGNING_REGION = "ru-central1";

    final static HttpClient client = HttpClient.newHttpClient();
    final static ObjectMapper mapper = new ObjectMapper();


    MeetingRepository meetingRepository;

    @Autowired
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public CompletableFuture<Meeting> getMeeting(UUID id) {
        return CompletableFuture.completedFuture(meetingRepository.findById(id).orElseThrow());
                //TODO add handle
    }

    public CompletableFuture<Meeting> updateMeeting(MeetingDto dto) {
        Meeting meeting = meetingRepository.findById(UUID.fromString(dto.id)).orElse(null);
        if (meeting == null) {
            throw new RuntimeException("Meeting not found with id: " + UUID.fromString(dto.id));
        }

        meeting.setCustomSummary(dto.customSummary);
        return CompletableFuture.completedFuture(meetingRepository.save(meeting));
    }


    public Meeting createMeeting(MultipartFile file) {
        Meeting meeting = new Meeting();
        meeting.setTitle(file.getName());
        meeting.setCreatedAt(LocalDateTime.now());
        return meeting;
    }

    public void uploadRecording(MultipartFile file) throws InterruptedException, URISyntaxException, IOException {
        Meeting meeting = createMeeting(file);
        System.out.println(meeting.getId());
        uploadToS3(file, meeting);
        Thread.sleep(10000);
        String operationId = sendToRecognition(meeting);
        String transcript = getTranscript(operationId);
        String summary = getGeneratedValue("Напиши краткое содержание текста от первого лица", transcript);
        String keyWords = getGeneratedValue("Выдели до 10 ключевых слов и словосочетаний из текста, напиши их обязательно в одну строку", transcript).toLowerCase();
        String description = getGeneratedValue("Опиши полученный текст в одном предложении, ни за что не используй markdown", transcript);
        String title = getGeneratedValue("Придумай короткое название для текста без кавычек", transcript);
        String quotes = getGeneratedValue("Выдели из текста от двух до четырех цитат, не используй никаких кавычек в цитатах. Между всеми цитатами ставь ровно один перевод строки, нумеруй каждую цитату", transcript);
        meeting.setTranscript(transcript);
        meeting.setSummary(summary);
        meeting.setKeyWords(List.of(keyWords.substring(0, keyWords.length() - 1).split(", ")));
        meeting.setDescription(description);
        meeting.setTitle(title);
        meeting.setQuotes(Arrays.stream(quotes.split("\n\n")).map(s -> s.substring(3)).toList());
        meetingRepository.save(meeting);
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

    public String getTranscript(String operationId) throws URISyntaxException, InterruptedException, IOException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(OPERATION_RESULT_ENDPOINT + operationId))
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

    public String getGeneratedValue(String command, String transcript) throws URISyntaxException, IOException, InterruptedException {
        var response = sendGptRequest(command, transcript);
        System.out.println(response.statusCode());
        var body = response.body();
        //TODO add status code handling
        var operationId = getOperationId(body);

        if (operationId.isEmpty()) {
            return "";
        }

        var responseMessage = getCompleteOperation(operationId);
        JsonNode root = mapper.readTree(responseMessage);
        String summary = root.get("response").get("alternatives").get(0).get("message").get("text").textValue();

        System.out.println(summary);
        return summary;
    }

    private HttpResponse<String> sendGptRequest(String command, String transcript) throws URISyntaxException, IOException, InterruptedException {
        String json = new JSONObject()
                .put("modelUri", LANGUAGE_MODEL_URI)
                .put("completionOptions", new JSONObject()
                        .put("stream", true)
                        .put("temperature", 0)
                        .put("maxTokens", 2000))
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "system")
                                .put("text", command))
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("text", transcript)))
                .toString();

        var request = HttpRequest.newBuilder()
                .uri(new URI(COMPLETION_ENDPOINT))
                .headers("Authorization", "Api-Key " + System.getenv("API_KEY"), "Content-Type", "application/json")
                .version(HttpClient.Version.HTTP_1_1)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();


        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public String getOperationId(String body) {
        var operationId = "";
        var idString = Arrays.stream(body.split(",")).filter(s -> s.contains("id")).findFirst();
        if (idString.isPresent()) {
            var temp = idString.get();
            operationId = temp.substring(7, temp.length() - 1);
            System.out.println(operationId);
        }
        return operationId;
    }

    public String getCompleteOperation(String operationId) throws URISyntaxException, InterruptedException, IOException {
        var request = HttpRequest.newBuilder()
                .uri(new URI(OPERATION_RESULT_ENDPOINT + operationId))
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
        return str;
    }
}
