package i.solonin;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import i.solonin.model.Settings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static i.solonin.Utils.*;

public class PostStartupActivity implements StartupActivity, DumbAware {
    private static final Logger log = Logger.getInstance(PostStartupActivity.class);

    private final Settings settings;

    public PostStartupActivity() {
        this.settings = ServiceManager.getService(Settings.class);
    }

    @Override
    public void runActivity(@NotNull Project project) {
        for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (fileEditor.getFile() != null)
                fillCache(fileEditor.getFile());
        }

        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                FileEditorManagerListener.super.fileOpened(source, file);
                fillCache(file);
            }
        });
    }

    private void fillCache(@NotNull VirtualFile file) {
        if (!check(ENG_MESSAGE_REGX).test(file)) return;

        try {
            Set<VirtualFile> localizationFiles = getLocalizationFiles(file);
            List<String> origin = content(file);
            for (VirtualFile f : localizationFiles) {
                try {
                    List<String> localization = content(f);
                    //fill translate cache base of existed localization values
                    fillCache(origin, localization, f.getName());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void fillCache(List<String> origin, List<String> localization, String fileName) {
        Map<String, String> m1 = toMap(origin);
        Map<String, String> m2 = toMap(localization);
        m1.forEach((k, v1) -> {
            String v2 = m2.get(k);
            if (v2 != null)
                settings.translateCache.putAsKey(fileName, k, v1.trim(), v2.trim());
        });
    }
}
