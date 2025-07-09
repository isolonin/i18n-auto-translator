package i.solonin.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import i.solonin.model.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utils {
    private static final Logger log = Logger.getInstance(Utils.class);

    public final static String ANY_MESSAGE_REGX = "(.*messages\\.properties|.*messages_[^.]*\\.properties)";
    public final static String ENG_MESSAGE_REGX = ".*messages\\.properties";

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

    public static Pair pair(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();
            return new Pair(key, value);
        }
        return null;
    }

    public static Map<String, String> toMap(List<String> list) {
        return list.stream()
                .map(Utils::pair)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Pair::getKey,
                        Pair::getValue,
                        (existing, replacement) -> existing
                ));
    }

    public static List<String> content(VirtualFile file) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String value;
            while ((value = reader.readLine()) != null)
                result.add(value);
        }
        return result;
    }

    public static Set<VirtualFile> getLocalizationFiles(VirtualFile file) {
        return Arrays.stream(Optional.ofNullable(file.getParent()).map(VirtualFile::getChildren).orElse(new VirtualFile[0]))
                .filter(check(ANY_MESSAGE_REGX))
                .filter(f -> !f.getName().equals(file.getName())).collect(Collectors.toSet());
    }

    public static Predicate<VirtualFile> check(String regx) {
        return f -> f.getName().matches(regx);
    }

    public static String getLanguage(String fileName) {
        return fileName.replaceFirst(".*_?messages_([^.]*)\\.properties", "$1");
    }

    public static void fillLocal(@NotNull VirtualFile file, Map<String, Map<String, String>> localizationFilesContent) {
        try {
            Set<VirtualFile> localizationFiles = getLocalizationFiles(file);
            List<String> origin = content(file);
            localizationFilesContent.clear();
            for (VirtualFile f : localizationFiles) {
                try {
                    List<String> localization = content(f);
                    //fill translate cache base of existed localization values
                    fillLocal(origin, localization, f.getName(), localizationFilesContent);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void fillLocal(List<String> origin, List<String> localization, String fileName,
                                 Map<String, Map<String, String>> localizationFilesContent) {
        Map<String, String> m1 = toMap(origin);
        Map<String, String> m2 = toMap(localization);
        m1.forEach((key, v1) -> {
            String v2 = m2.get(key);
            if (v2 != null) {
                Map<String, String> values = localizationFilesContent.computeIfAbsent(fileName, k -> new HashMap<>());
                values.put(key + "=" + v1, v2);
            }
        });
    }
}
