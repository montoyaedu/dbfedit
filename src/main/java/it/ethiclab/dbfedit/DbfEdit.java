package it.ethiclab.dbfedit;

import java.util.*;
import java.io.*;
import java.text.*;
import java.math.BigDecimal;

import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.*;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.input.*;

public class DbfEdit extends Application {

    Stage stage;
    Label label;
    SmDBF dbf;
    String dStFormat = "yyyy/MM/dd";
    String tStFormat = "yyyy/MM/dd-HH:mm:ss";

    public static void main(String args[]) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        BorderPane layout = new BorderPane();
        stage.setScene(new Scene(layout, 700, 400));
        TableView<IntVal> tableView = new TableView<>();
        label = new Label();
        File file;
        List<String> p = getParameters().getRaw();
        if (!p.isEmpty()) {
            file = new File(p.get(0));
            if (!file.isFile())
                throw new RuntimeException(file.getName() + " file not found");
        } else
            file = new File("demo.dbf");
        try {
            if (file.exists())
                dbf = new SmDBF(file);
            else
                throw new IOException(file + " does not exist.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            tableView.setItems(new VirtObservableList());
            for (int i = 1; i <= dbf.getFieldCount(); i++) {
                final int f = i;
                TableColumn<IntVal, IntVal> column = new TableColumn<>(dbf.getFieldName(f));
                column.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<IntVal>(cd.getValue()));
                column.setCellFactory(c -> {
                    TableCell<IntVal, IntVal> cell = new TableCell<IntVal, IntVal>() {

                        @Override
                        protected void updateItem(IntVal item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                                setStyle("");
                            } else {
                                String s;
                                Object obj = null;
                                dbf.goToRec(item.get() + 1);
                                char type = dbf.getFieldType(f);
                                try {
                                    obj = dbf.get(f);
                                    if (obj == null) {
                                        s = "null";
                                    } else if (obj instanceof Boolean) {
                                        s = (Boolean) obj ? "T" : "F";
                                    } else if (obj instanceof Date) {
                                        s = new SimpleDateFormat(type == 'D' ? dStFormat : tStFormat)
                                                .format((Date) obj);
                                    } else if (obj instanceof String) {
                                        s = SmDBF.rtrim((String) obj);
                                    } else
                                        s = obj.toString();
                                } catch (Exception e) {
                                    s = "error";
                                }
                                if (type == 'N' || type == 'F')
                                    setAlignment(Pos.CENTER_RIGHT);
                                setText(s);
                            }
                        }
                    };
                    return cell;
                });
                if (dbf.getFieldLength(f) > 30)
                    column.setPrefWidth(200);
                tableView.getColumns().add(column);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tableView.setOnKeyPressed(ke -> {
            try {
                VirtObservableList items = (VirtObservableList) tableView.getItems();
                if (ke.getCode() == KeyCode.P && ke.isControlDown()) {
                    dbf.pack();
                } else if (ke.getCode() == KeyCode.ENTER && ke.isControlDown()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Append record?");
                    alert.initOwner(stage);
                    alert.setHeaderText(null);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK) {
                        dbf.append();
                        items.insert(items.size());
                        tableView.getSelectionModel().selectLast();
                        tableView.scrollTo(tableView.getSelectionModel().getSelectedIndices().get(0));
                    }
                } else if (ke.getCode() == KeyCode.DELETE) {
                    IntVal item = tableView.getSelectionModel().getSelectedItems().get(0);
                    if (item != null) {
                        dbf.goToRec(item.get() + 1);
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete record?");
                        alert.initOwner(stage);
                        alert.setHeaderText(null);
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.get() == ButtonType.OK) {
                            dbf.delete();
                            items.update(item.get());
                        }
                    }
                } else if (ke.getCode() == KeyCode.ENTER) {
                    IntVal item = tableView.getSelectionModel().getSelectedItems().get(0);
                    if (item != null) {
                        dbf.goToRec(item.get() + 1);
                        int c = tableView.getSelectionModel().getSelectedCells().get(0).getColumn() + 1;
                        char t = dbf.getFieldType(c);
                        Object obj = dbf.get(c);
                        String s = "", l = "";
                        int len;
                        if (t == 'C' || t == 'M') {
                            s = obj != null ? SmDBF.rtrim(obj.toString()) : "";
                            len = dbf.getFieldLength(c);
                            l = "Length: " + len;
                        } else if (t == 'L') {
                            s = obj == null ? "F" : (Boolean) obj ? "T" : "F";
                            len = 1;
                            l = "Value: T or F";
                        } else if (t == 'D' || t == 'T') {
                            String stFormat = t == 'D' ? dStFormat : tStFormat;
                            s = new SimpleDateFormat(stFormat).format(obj == null ? new Date() : (Date) obj);
                            len = stFormat.length();
                            l = "Format: " + stFormat.toUpperCase();
                        } else if (t == 'N' || t == 'F') {
                            s = obj == null ? "0" : obj.toString();
                            len = dbf.getFieldLength(c);
                            l = "Length: " + len
                                    + (dbf.getFieldLengthDecimal(c) > 0 ? "." + dbf.getFieldLengthDecimal(c) : "");
                        } else
                            len = 0;
                        TextInputDialog dialog = new TextInputDialog(s);
                        TextArea ta = new TextArea(s);
                        dialog.initOwner(stage);
                        dialog.setHeaderText(null);
                        dialog.setContentText(dbf.getFieldName(c));
                        dialog.getDialogPane()
                                .setExpandableContent(new Label("Type: " + SmDBF.typeToString(t) + ", " + l));
                        dialog.getDialogPane().setExpanded(true);
                        if (t == 'M') {
                            System.out.println("" + dialog.getDialogPane().getContent().getClass());
                            GridPane gp = (GridPane) dialog.getDialogPane().getContent();
                            gp.getChildren().stream().forEach(System.out::println);
                            gp.getChildren().remove(dialog.getEditor());
                            gp.getChildren().add(ta);
                        }
                        Optional<String> result = dialog.showAndWait();
                        if (result.isPresent()) {
                            obj = null;
                            try {
                                if (t == 'C') {
                                    obj = result.get();
                                } else if (t == 'M') {
                                    obj = ta.getText();
                                } else if (t == 'L') {
                                    if (result.get().equalsIgnoreCase("T"))
                                        obj = new Boolean(true);
                                    else if (result.get().equalsIgnoreCase("F"))
                                        obj = new Boolean(false);
                                } else if (t == 'D' || t == 'T') {
                                    obj = new SimpleDateFormat(t == 'D' ? dStFormat : tStFormat).parse(result.get());
                                } else if (t == 'N' || t == 'F') {
                                    obj = new BigDecimal(result.get());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                dbf.set(c, obj);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
                                alert.initOwner(stage);
                                alert.setHeaderText(null);
                                alert.showAndWait();
                            }
                            items.update(item.get());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.sortPolicyProperty().set(t -> false);

        @SuppressWarnings("rawtypes")
        ObservableList<TablePosition> selectedCells = tableView.getSelectionModel().getSelectedCells();
        selectedCells.addListener(
                (@SuppressWarnings("rawtypes") ListChangeListener.Change<? extends TablePosition> change) -> {
                    if (selectedCells.size() > 0) {
                        TablePosition<?, ?> pos = (TablePosition<?, ?>) selectedCells.get(0);
                        label.setText(String.format("Field: %5d / %d   Record: %7d / %d", pos.getColumn() + 1,
                                dbf.getFieldCount(), pos.getRow() + 1, dbf.getRecCount()));
                    } else {
                        label.setText("");
                    }
                });

        tableView.getSelectionModel().selectFirst();
        layout.setCenter(tableView);
        layout.setBottom(label);

        stage.setOnCloseRequest(event -> {
            try {
                dbf.close();
            } catch (Exception e) {
            }
        });

        stage.show();
    }

    public class IntVal {
        private int value;

        public IntVal(int value) {
            this.value = value;
        }

        public void set(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    } // End of class IntVal

    public class VirtObservableList extends ObservableListBase<IntVal> {
        IntVal value = new IntVal(-1);

        @Override
        public int size() {
            return dbf.getRecCount();
        }

        @Override
        public IntVal get(int index) {
            if (index < 0 || index >= size())
                throw new IndexOutOfBoundsException();
            if (index != value.get()) {
                value = new IntVal(index);
            }
            return value;
        }

        void update(int index) {
            beginChange();
            nextUpdate(index);
            endChange();
            value.set(-1);
        }

        void insert(int index) {
            beginChange();
            nextAdd(index, index + 1);
            endChange();
            value.set(-1);
        }

    } // End of class VirtObservableList

} // End of class DbfEdit
