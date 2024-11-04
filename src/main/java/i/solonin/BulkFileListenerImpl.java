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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class BulkFileListenerImpl implements BulkFileListener {
    private final static String ENG_MESSAGE_REGX = ".*messages\\.properties";
    private final static String ANY_MESSAGE_REGX = "(.*messages\\.properties|.*messages_[^.]*\\.properties)";
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
            if (file != null && check(ENG_MESSAGE_REGX).test(file)) {
                Set<VirtualFile> files = Arrays.stream(file.getParent().getChildren()).filter(check(ANY_MESSAGE_REGX))
                        .filter(f -> !f.getName().equals(file.getName())).collect(Collectors.toSet());
                filesComparator.process(file, files);
            }
        }
    }

    private Predicate<VirtualFile> check(String regx) {
        return f -> f.getName().matches(regx);
    }
}
