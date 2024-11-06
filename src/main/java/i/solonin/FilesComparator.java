package i.solonin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import i.solonin.model.Error;
import i.solonin.model.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static i.solonin.Utils.*;

public class FilesComparator {
    private static final Logger log = Logger.getInstance(FilesComparator.class);
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
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
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
                        log.info("Start process file " + file.getName());
                        result.put(f, process(origin, f));
                    } catch (Exception e) {
                        NotificationUtils.show(project, e.getMessage(), NotificationType.ERROR);
                        log.error(e.getMessage());
                    } finally {
                        log.info("End process file " + file.getName());
                    }
                }
            } catch (Exception e) {
                NotificationUtils.show(project, e.getMessage(), NotificationType.ERROR);
                log.error(e.getMessage());
            }
            return result;
        });

        ApplicationManager.getApplication().runWriteAction(() -> newContent.forEach((f, strings) -> {
            try {
                if (f.isWritable()) {
                    f.setBinaryContent(String.join("\n", strings).getBytes(StandardCharsets.UTF_8));
                } else {
                    log.error("Can't write file " + f.getName());
                }
            } catch (IOException e) {
                log.error("Can't write file " + f.getName() + ": " + e.getMessage());
            }
        }));
    }

    private List<String> process(List<String> origin, VirtualFile f) {
        String language = getLanguage(f.getName());
        List<String> result = new ArrayList<>();
        List<StringWithPosition> toTranslate = new ArrayList<>();
        for (int i = 0, originSize = origin.size(); i < originSize; i++) {
            String s = origin.get(i);
            Pair pair = pair(s);
            if (pair != null) {
                String translate = settings.getByLocal(f.getName(), pair.getKey(), pair.getValue());
                if (translate == null)
                    translate = settings.getByTranslated(language, pair.getValue());
                if (translate == null)
                    toTranslate.add(new StringWithPosition(i, pair.getValue()));
                result.add(pair.getKey() + "=" + (translate == null ? "" : translate));
            } else {
                result.add(s);
            }
        }
        List<String> strings = translate(toTranslate.stream().map(StringWithPosition::getValue).collect(Collectors.toList()), f.getName());
        if (strings.size() != toTranslate.size()) {
            log.warn("Translated strings is not equals with required");
            return result;
        }
        for (int i = 0, stringsSize = strings.size(); i < stringsSize; i++) {
            try {
                String translate = strings.get(i);
                StringWithPosition stringWithPosition = toTranslate.get(i);
                String key = result.get(stringWithPosition.getPosition());
                result.set(stringWithPosition.getPosition(), key + translate);
                if (translate != null && !translate.isEmpty())
                    settings.putAsTranslated(language, toTranslate.get(i).getValue(), translate);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }

    private @NotNull List<String> translate(List<String> list, String fileName) {
        List<String> result = new ArrayList<>();
        try {
            if (!list.isEmpty())
                log.info("Try to translate " + list.size() + " words");
            for (List<String> texts : splitByCharacterLimit(list, TRANSLATE_LIMIT)) {
                var request = HttpRequest.newBuilder()
                        .uri(new URI("https://translate.api.cloud.yandex.net/translate/v2/translate"))
                        .header("Authorization", "Api-Key " + settings.yandexSecretKey)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(new TranslateRequest(fileName, texts)), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    Error error = mapper.readValue(response.body(), Error.class);
                    NotificationUtils.show(project, "Yandex can't translate: " + error.getMessage(), NotificationType.ERROR);
                    return result;
                }
                TranslateResponse translateResponse = mapper.readValue(response.body(), TranslateResponse.class);
                result.addAll(translateResponse.getTranslations().stream().map(Text::getText).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            log.info("Translate is done");
        }
        return result;
    }
}
