package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TranslateResponse {
    private List<Text> translations = new ArrayList<>();
}
