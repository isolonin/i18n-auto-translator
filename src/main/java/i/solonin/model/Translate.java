package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Translate {
    public Map<String, String> translated = new HashMap<>();

    public void put(String key, String value) {
        translated.put(key, value);
    }

    public String get(String value) {
        return translated.get(value);
    }
}
