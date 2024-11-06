package i.solonin.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class NotificationUtils {
    public static void show(Project project, String message, NotificationType type) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("i18n-auto-translator-notifications")
                .createNotification(message, type);
        notification.notify(project);
    }
}
