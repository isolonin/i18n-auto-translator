package i.solonin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import i.solonin.model.Settings;
import i.solonin.model.Text;
import i.solonin.model.TranslateRequest;
import i.solonin.model.TranslateResponse;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FilesComparator {
    private final static int TRANSLATE_LIMIT = 10000;
    private final Project project;
    private final Settings settings;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public FilesComparator(Project project, Settings settings, HttpClient client) {
        this.project = project;
        this.settings = settings;
        this.client = client;
        mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    public void process(VirtualFile file, Set<VirtualFile> files) {
        Map<VirtualFile, List<String>> newContent = ApplicationManager.getApplication().runReadAction((Computable<Map<VirtualFile, List<String>>>) () -> {
            Map<VirtualFile, List<String>> result = new HashMap<>();
            try {
                List<String> origin = content(file);
                for (VirtualFile f : files) {
                    try {
                        List<String> localization = content(f);
                        //fill translate cache base of existed localization values
                        fillCache(origin, localization, f.getName());

                        result.put(f, process(origin, f));
                    } catch (Exception e) {
                        NotificationUtils.showError(project, e.getMessage(), NotificationType.ERROR);
                    }
                }
            } catch (Exception e) {
                NotificationUtils.showError(project, e.getMessage(), NotificationType.ERROR);
            }
            return result;
        });

        ApplicationManager.getApplication().runWriteAction(() -> newContent.forEach((f, strings) -> {
            try {
                if (f.isWritable()) {
                    f.setBinaryContent(String.join("\n", strings).getBytes(StandardCharsets.UTF_8));
                } else {
                    System.out.println("Can't write file " + file.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private List<String> process(List<String> origin, VirtualFile f) {
        List<String> result = new ArrayList<>();
        List<StringWithPosition> toTranslate = new ArrayList<>();
        for (int i = 0, originSize = origin.size(); i < originSize; i++) {
            String s = origin.get(i);
            Pair pair = pair(s);
            if (pair != null) {
                String translate = settings.translateCache.get(f.getName(), pair.value);
                if (translate == null)
                    toTranslate.add(new StringWithPosition(i, pair.value));
                if (translate != null)
                    settings.translateCache.put(f.getName(), pair.value, translate);
                result.add(pair.key + "=" + (translate == null ? "" : translate));
            } else {
                result.add(s);
            }
        }
        List<String> strings = translate(toTranslate.stream().map(s -> s.value).collect(Collectors.toList()), f.getName());
        for (int i = 0, stringsSize = strings.size(); i < stringsSize; i++) {
            try {
                String translate = strings.get(i);
                StringWithPosition stringWithPosition = toTranslate.get(i);
                String keyValue = result.get(stringWithPosition.position);
                Pair pair = pair(keyValue);
                result.set(stringWithPosition.position, keyValue.replaceFirst("(.*)=.*", "$1=" + translate));
                settings.translateCache.put(f.getName(), pair.value, translate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private void fillCache(List<String> origin, List<String> localization, String fileName) {
        Map<String, String> m1 = toMap(origin);
        Map<String, String> m2 = toMap(localization);
        m1.forEach((k, v1) -> {
            String v2 = m2.get(k);
            if (v2 != null)
                settings.translateCache.put(fileName, v1.trim(), v2.trim());
        });
    }

    private @NotNull List<String> translate(List<String> list, String fileName) {
        List<String> result = new ArrayList<>();
        try {
            for (List<String> texts : splitByCharacterLimit(list, TRANSLATE_LIMIT)) {
                var request = HttpRequest.newBuilder()
                        .uri(new URI("https://translate.api.cloud.yandex.net/translate/v2/translate"))
                        .header("Authorization", "Api-Key " + settings.yandexSecretKey)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(new TranslateRequest(fileName, texts)), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200)
                    continue;
                TranslateResponse translateResponse = mapper.readValue(response.body(), TranslateResponse.class);
                result.addAll(translateResponse.getTranslations().stream().map(Text::getText).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<List<String>> splitByCharacterLimit(List<String> strings, int limit) {
        List<List<String>> result = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        int currentLength = 0;

        for (String str : strings) {
            if (currentLength + str.length() > limit) {
                result.add(new ArrayList<>(temp));
                temp.clear();
                currentLength = 0;
            }
            temp.add(str);
            currentLength += str.length();
        }
        if (!temp.isEmpty())
            result.add(temp);
        return result;
    }

    private Pair pair(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();
            return new Pair(key, value);
        }
        return null;
    }

    private Map<String, String> toMap(List<String> list) {
        return list.stream().map(this::pair).filter(Objects::nonNull)
                .collect(Collectors.toMap(p -> p.key, p -> p.value));
    }

    private List<String> content(VirtualFile file) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String value;
            while ((value = reader.readLine()) != null)
                result.add(value);
        }
        return result;
    }

    @AllArgsConstructor
    private static class Pair {
        private String key;
        private String value;
    }

    @AllArgsConstructor
    private static class StringWithPosition {
        private int position;
        private String value;
    }
}
