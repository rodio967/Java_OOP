package org.app;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.database.DatabaseManager;
import org.model.Table;
import org.parser.SQLParser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatabaseApp extends Application {
    private final DatabaseManager dbManager = new DatabaseManager();
    private final SQLParser parser = new SQLParser(dbManager);
    private final ListView<String> tableList = new ListView<>();
    private final TableView<ObservableList<Object>> tableView = new TableView<>();
    private final TextArea sqlInput = new TextArea();


    private final ComboBox<String> columnTypeInput = new ComboBox<>();
    private final Button addColumnButton = new Button("Добавить столбец");


    @Override
    public void start(Stage primaryStage) {
        InputStream iconStream = getClass().getResourceAsStream("/icon.png");
        assert iconStream != null;
        Image image = new Image(iconStream);
        primaryStage.getIcons().add(image);


        dbManager.ensure_DB_DirectoryExists();
        dbManager.loadAllTables();
        BorderPane root = new BorderPane();

        Button executeButton = new Button("Execute");
        executeButton.setOnAction(e -> executeCommand());


        addColumnButton.setOnAction(e -> {
            String selectedTable = tableList.getSelectionModel().getSelectedItem();
            if (selectedTable != null) {
                showAddColumnDialog(selectedTable);
            } else {
                showError("Выберите таблицу перед добавлением столбца.");
            }
        });


        columnTypeInput.getItems().addAll("INT", "STRING", "BOOLEAN");
        columnTypeInput.setValue("INT");


        Button dropColumnButton = new Button("Удалить столбец");
        dropColumnButton.setOnAction(e -> {
            String selectedTable = tableList.getSelectionModel().getSelectedItem();
            if (selectedTable != null) {
                showDropColumnDialog(selectedTable);
            } else {
                showError("Выберите таблицу для удаления столбца.");
            }
        });

        VBox leftPanel = new VBox(new Label("Tables"), tableList, addColumnButton, dropColumnButton);


        tableList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selectedTable = tableList.getSelectionModel().getSelectedItem();
                if (selectedTable != null) {
                    showTable(selectedTable);
                }
            }
        });

        HBox bottomPanel = new HBox(sqlInput, executeButton);
        VBox centerPanel = new VBox(new Label("Table Data"), tableView);

        root.setLeft(leftPanel);
        root.setBottom(bottomPanel);
        root.setCenter(centerPanel);

        Scene scene = new Scene(root, 800, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("In-Memory DB");
        primaryStage.show();

        updateTableList();
    }

    private void executeCommand() {
        try {
            parser.execute(sqlInput.getText());
            updateTableList();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void updateTableList() {
        tableList.getItems().setAll(dbManager.listTables());
    }

    private void showTable(String tableName) {
        Table table = dbManager.getTable(tableName);
        if (table == null) {
            showError("Table not found: " + tableName);
            return;
        }

        tableView.getColumns().clear();
        tableView.getItems().clear();


        for (String column : table.getColumns().keySet()) {
            TableColumn<ObservableList<Object>, Object> col = new TableColumn<>(column);
            int colIndex = table.getColumns().keySet().stream().toList().indexOf(column);
            col.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().get(colIndex)));
            tableView.getColumns().add(col);
        }


        for (Map<String, Object> row : table.getRows()) {
            ObservableList<Object> rowData = FXCollections.observableArrayList();
            for (String colName : table.getColumns().keySet()) {
                rowData.add(row.getOrDefault(colName, null));
            }
            tableView.getItems().add(rowData);
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.show();
    }


    private void showDropColumnDialog(String tableName) {
        Table table = dbManager.getTable(tableName);
        if (table == null) {
            showError("Таблица не найдена.");
            return;
        }

        List<String> columnNames = table.getColumnNames();
        if (columnNames.isEmpty()) {
            showError("В таблице нет столбцов для удаления.");
            return;
        }


        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Удаление столбца");
        dialog.setHeaderText("Выберите столбец для удаления");


        ComboBox<String> columnDropdown = new ComboBox<>();
        columnDropdown.getItems().addAll(columnNames);
        columnDropdown.setValue(columnNames.get(0));


        ButtonType deleteButtonType = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);


        VBox content = new VBox(new Label("Столбец:"), columnDropdown);
        content.setSpacing(10);
        dialog.getDialogPane().setContent(content);


        dialog.setResultConverter(button -> {
            if (button == deleteButtonType) {
                return columnDropdown.getValue();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(columnName -> {
            table.dropColumn(columnName);

            dbManager.saveTable(tableName, table);
            showTable(tableName);
        });
    }


    private void showAddColumnDialog(String tableName) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить столбец");
        dialog.setHeaderText("Введите параметры нового столбца");


        TextField columnNameInput = new TextField();
        columnNameInput.setPromptText("Имя столбца");


        ComboBox<String> columnTypeInput = new ComboBox<>();
        columnTypeInput.getItems().addAll("INT", "STRING", "BOOLEAN", "DATE", "[]STRINGS", "[]INTS", "[]BOOLEANS");
        columnTypeInput.setValue("STRING");


        CheckBox uniqueCheck = new CheckBox("UNIQUE");
        CheckBox notNullCheck = new CheckBox("NOT NULL");


        VBox content = new VBox(10,
                new Label("Имя столбца:"), columnNameInput,
                new Label("Тип данных:"), columnTypeInput,
                uniqueCheck, notNullCheck);

        dialog.getDialogPane().setContent(content);


        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String columnName = columnNameInput.getText().trim();
                String columnType = columnTypeInput.getValue();
                boolean isUnique = uniqueCheck.isSelected();
                boolean isNotNull = notNullCheck.isSelected();

                if (!columnName.isEmpty()) {
                    Table table = dbManager.getTable(tableName);
                    table.addNewColumn(columnName, columnType, isUnique, isNotNull);
                    dbManager.saveTable(tableName, table);
                    showTable(tableName);
                } else {
                    showError("Имя столбца не может быть пустым!");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }





    public static void main(String[] args) {
        launch(args);
    }
}
