package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Translate {
    public Map<String, String> local = new HashMap<>();
    public Map<String, String> translated = new HashMap<>();

    public void putLocal(String key, String value, String translate) {
        local.put(key + "=" + value, translate);
    }

    public void putTranslate(String key, String value) {
        translated.put(key, value);
    }

    public String getByValue(String value) {
        return translated.get(value);
    }

    public String getByKey(String key, String value) {
        return local.get(key + "=" + value);
    }
}
