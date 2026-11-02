package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.NotificationTask;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByChatId(Long chatId);

    List<NotificationTask> findByStatus(String status);

    List<NotificationTask> findByNotificationDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<NotificationTask> findByStatusAndNotificationDateTimeBefore(String status, LocalDateTime dateTime);

    List<NotificationTask> findByNotificationDateTimeAndStatus(LocalDateTime dateTime, String status);
}
