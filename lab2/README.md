# Java In-Memory Database

## Описание

Java In-Memory Database — это реляционная база данных, работающая в оперативной памяти и файлах. Она поддерживает основные SQL-команды и предоставляет графический интерфейс (JavaFX) для работы с данными.

## Функциональность

- **Команды SQL:**
    - `CREATE TABLE <table_name> (<column_definitions>)` — создание таблицы
    - `INSERT INTO <table_name> VALUES (<values>)` — добавление данных
    - `DROP TABLE <table_name>` — удаление таблицы
    - `DELETE FROM <table_name> WHERE VALUES (<values>)`- удаление данных
    - `SELECT <columns> FROM <table_name> WHERE VALUES (<values>) SORT <column>;` - поиск и сортировка данных
    - Добавление/удаление колонок в таблицах
- **Примеры:**
  ```sql
  CREATE TABLE users (
      id int unique;
      name string not-null;
      email string unique;
      age int;
      is_active boolean;
  );
  ```
  ```sql
  INSERT INTO users (1, "John", "john@example.com", 25, true);
  INSERT INTO users (
    (1, "John", "john@example.com", 25, true),
    (2, "Alice", "alice@example.com", 30, false)
  );
  ```
  ```sql
  DELETE FROM users WHERE id=1 AND name="John";
  ```
  ```sql
  SELECT * FROM users;
  SELECT * FROM users WHERE id=1 AND name="John";
  SELECT name, id FROM users SORT id;
  ```
  ```sql
  DROP TABLE users;
  ```
 
- **Хранение данных:**
    - В памяти с использованием `TreeMap`/`HashMap`/`LinkedMap`
    - В файлах (`my-database/users.db`, `my-database/friends.db` и т. д.)


## Структура проекта

```
my-database/        # Каталог для хранения таблиц в файлах
src/                # Исходный код проекта
 ├── db/            # Логика базы данных
 ├── gui/           # Графический интерфейс (JavaFX)
 └── parser/        # Разбор SQL-запросов (регулярные выражения)
```

