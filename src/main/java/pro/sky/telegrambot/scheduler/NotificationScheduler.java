package pro.sky.telegrambot.scheduler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private TelegramBot telegramBot;

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotifications() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        List<NotificationTask> tasks = notificationTaskRepository
                .findByNotificationDateTimeAndStatus(now, "CREATED");

        for (NotificationTask task : tasks) {
            sendNotification(task);
            task.setStatus("SENT");
            notificationTaskRepository.save(task);
        }
    }

    private void sendNotification(NotificationTask task) {
        SendMessage request = new SendMessage(task.getChatId(), task.getNotificationText());
        SendResponse response = telegramBot.execute(request);
        if (response.isOk()) {
            logger.info("Notification sent to chat {} at {}", task.getChatId(), task.getNotificationDateTime());
        } else {
            logger.error("Failed to send notification to chat {}: {}", task.getChatId(), response.description());
        }
    }
}
