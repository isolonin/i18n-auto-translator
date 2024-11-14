package i.solonin.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import i.solonin.model.Settings;
import i.solonin.processor.FilesComparator;
import i.solonin.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;

import static i.solonin.utils.Utils.*;

@Service(Service.Level.PROJECT)
public final class MainService implements BulkFileListener {
    private static final Logger log = Logger.getInstance(MainService.class);
    private final Map<String, Map<String, String>> localizationFilesContent = new HashMap<>();
    private final Project project;
    private final Settings settings;
    private final FilesComparator filesComparator;

    public MainService(Project project) {
        this.project = project;
        this.settings = ServiceManager.getService(Settings.class);
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(Executors.newFixedThreadPool(settings.httpPool))
                .connectTimeout(Duration.of(settings.httpTimeout, ChronoUnit.MILLIS))
                .build();
        log.info("Start i18n-auto-translator plugin");
        for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors())
            if (fileEditor.getFile() != null)
                fillLocal(fileEditor.getFile());
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                FileEditorManagerListener.super.fileOpened(source, file);
                fillLocal(file);
            }

            @Override
            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                FileEditorManagerListener.super.fileClosed(source, file);
                clearLocal(file);
            }
        });

        this.filesComparator = new FilesComparator(project, settings, client);
    }

    private void fillLocal(@NotNull VirtualFile file) {
        if (!check(ENG_MESSAGE_REGX).test(file) || settings.isDisabled()) return;
        log.info("Update " + file.getName() + " content");
        Utils.fillLocal(file, localizationFilesContent);
    }

    private void clearLocal(@NotNull VirtualFile file) {
        if (!check(ENG_MESSAGE_REGX).test(file) || settings.isDisabled()) return;
        log.info("Clear " + file.getName() + " content");
        localizationFilesContent.clear();
    }

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        if (settings.isDisabled()) return;

        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && check(ENG_MESSAGE_REGX).test(file)) {
                if (!isFIleOpenInEditor(file))
                    continue;
                if (localizationFilesContent.isEmpty())
                    continue;
                filesComparator.process(file, getLocalizationFiles(file), localizationFilesContent);
            }
        }
    }

    private boolean isFIleOpenInEditor(@NotNull VirtualFile file) {
        return Arrays.stream(FileEditorManager.getInstance(project).getAllEditors()).anyMatch(e -> Objects.equals(e.getFile(), file));
    }
}
