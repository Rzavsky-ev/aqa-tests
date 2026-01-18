package aqa.api.utils;

import aqa.api.exceptions.UtilityClassException;
import io.qameta.allure.Allure;

import static aqa.api.utils.Constants.*;

/**
 * Утилитарный класс для формирования Allure-отчетов и работы с метриками тестирования.
 * Предоставляет методы для добавления тестовых данных в Allure и получения текстовых
 * описаний HTTP-статусов.
 */
public class AllureReporter {

    /**
     * Добавляет текстовые данные в Allure-отчет в виде вложения.
     * Данные отображаются на вкладке "Attachments" в сгенерированном отчете.
     */
    public static void addTestData(String title, String content) {
        Allure.addAttachment(title, "text/plain", content);
    }

    /**
     * Возвращает текстовое описание HTTP-статуса по его числовому коду.
     * Поддерживает основные коды ответов, используемые в API тестировании.
     */
    public static String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> RESULT_OK;
            case 400 -> BAD_REQUEST_MESSAGE;
            case 401 -> UNAUTHORIZED_MESSAGE;
            case 403 -> FORBIDDEN_MESSAGE;
            case 404 -> NOT_FOUND_MESSAGE;
            case 409 -> CONFLICT_MESSAGE;
            case 500 -> INTERNAL_MESSAGE;
            default -> UNKNOWN_MESSAGE;
        };
    }

    private AllureReporter() {
        throw new UtilityClassException(getClass());
    }
}