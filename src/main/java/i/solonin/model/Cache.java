package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class Cache {
    public Map<String, Translate> data = new HashMap<>();

    public void put(String fileName, String v1, String v2) {
        Translate translate = data.get(fileName);
        if (translate == null) {
            translate = new Translate();
            data.put(fileName, translate);
        }
        translate.put(v1, v2);
    }

    public String get(String fileName, String value) {
        return Optional.ofNullable(data.get(fileName)).map(t -> t.get(value)).orElse(null);
    }
}
