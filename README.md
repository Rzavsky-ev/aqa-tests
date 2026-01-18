# API Automation Tests

## Структура проекта
- `src/test/java/aqa/api/base/` - базовые тестовые классы
- `src/test/java/aqa/api/tests/` - тестовые классы
- `src/test/java/aqa/api/utils/` - утилиты
- `src/test/java/aqa/api/exceptions/` - кастомные исключения

## Запуск тестов
```bash
# Все тесты
mvn clean test

# Конкретный тестовый класс
mvn clean test -Dtest=LoginTest

# Сразу с генерацией Allure отчета
mvn clean test allure:serve
