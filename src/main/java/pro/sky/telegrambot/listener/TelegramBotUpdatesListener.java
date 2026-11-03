package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private static final String DATE_PATTERN = "dd.MM.yyyy HH:mm";
    private static final String REMINDER_REGEX = "(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)";

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            if (update.message() != null) {
                String text = update.message().text();
                Long chatId = update.message().chat().id();

                if ("/start".equalsIgnoreCase(text)) {
                    sendWelcomeMessage(chatId);
                } else {
                    parseAndSaveReminder(chatId, text);
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void sendWelcomeMessage(Long chatId) {
        String greeting = "*Привет! Я Telegram-бот.*\n\nЯ готов к работе.";
        SendResponse response = telegramBot.execute(new SendMessage(chatId, greeting));
        if (response.isOk()) {
            logger.info("Welcome message sent to chat {}", chatId);
        } else {
            logger.error("Failed to send welcome message: {}", response.description());
        }
    }

    private void parseAndSaveReminder(Long chatId, String text) {
        Pattern pattern = Pattern.compile(REMINDER_REGEX);
        Matcher matcher = pattern.matcher(text);

        if (matcher.matches()) {
            String dateTimeStr = matcher.group(1);
            String reminderText = matcher.group(3);

            try {
                LocalDateTime notificationDateTime = LocalDateTime.parse(
                        dateTimeStr,
                        DateTimeFormatter.ofPattern(DATE_PATTERN)
                );

                NotificationTask task = new NotificationTask();
                task.setChatId(chatId);
                task.setNotificationText(reminderText);
                task.setNotificationDateTime(notificationDateTime);
                task.setStatus("CREATED");

                notificationTaskRepository.save(task);

                String successMessage = "*Напоминание сохранено:*\n" +
                        "Дата: " + dateTimeStr + "\n" +
                        "Текст: *" + reminderText + "*";
                SendResponse response = telegramBot.execute(new SendMessage(chatId, successMessage));
                if (response.isOk()) {
                    logger.info("Reminder saved for chat {}", chatId);
                } else {
                    logger.error("Failed to send confirmation: {}", response.description());
                }
            } catch (Exception e) {
                logger.error("Error parsing date: {}", dateTimeStr, e);
                telegramBot.execute(new SendMessage(chatId,
                        "*Ошибка:* не удалось распознать дату. Используйте формат: ДД.ММ.ГГГГ ЧЧ:ММ"));
            }
        }
    }

}
