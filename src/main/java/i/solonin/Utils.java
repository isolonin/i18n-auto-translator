package i.solonin;

import com.intellij.openapi.vfs.VirtualFile;
import i.solonin.model.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utils {
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
        return list.stream().map(Utils::pair).filter(Objects::nonNull)
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
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
        return Arrays.stream(file.getParent().getChildren()).filter(check(ANY_MESSAGE_REGX))
                .filter(f -> !f.getName().equals(file.getName())).collect(Collectors.toSet());
    }

    public static Predicate<VirtualFile> check(String regx) {
        return f -> f.getName().matches(regx);
    }
}
