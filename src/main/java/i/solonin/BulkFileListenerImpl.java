package i.solonin;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import i.solonin.model.Settings;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;

import static i.solonin.Utils.*;


public class BulkFileListenerImpl implements BulkFileListener {
    private final Settings settings;
    private final FilesComparator filesComparator;

    public BulkFileListenerImpl(Project project) {
        settings = ServiceManager.getService(Settings.class);
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .executor(Executors.newFixedThreadPool(settings.httpPool))
                .connectTimeout(Duration.of(settings.httpTimeout, ChronoUnit.MILLIS))
                .build();
        filesComparator = new FilesComparator(project, settings, client);
    }

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.after(events);
        if (!settings.isEnabled()) return;

        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && check(ENG_MESSAGE_REGX).test(file))
                filesComparator.process(file, getLocalizationFiles(file));
        }
    }
}
