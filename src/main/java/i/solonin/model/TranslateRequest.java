package i.solonin.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TranslateRequest {
    private String sourceLanguageCode = "en";
    private String targetLanguageCode;
    private String format = "PLAIN_TEXT";
    private String[] texts;
    private boolean speller;

    public TranslateRequest(String fileName, List<String> texts) {
        this.targetLanguageCode = fileName.replaceFirst(".*_messages_([^.]*)\\.properties", "$1");
        this.texts = texts.toArray(new String[0]);
    }
}
