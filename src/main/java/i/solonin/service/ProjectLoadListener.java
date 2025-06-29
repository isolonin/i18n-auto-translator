package i.solonin.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class ProjectLoadListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        MainService mainService = project.getService(MainService.class);
        mainService.initFileListeners();
    }
}
