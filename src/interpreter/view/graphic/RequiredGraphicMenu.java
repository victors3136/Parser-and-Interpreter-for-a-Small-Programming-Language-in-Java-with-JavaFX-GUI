package interpreter.view.graphic;

import inputmanager.parser.ParseException;
import inputmanager.tokenizer.TokenizerException;
import interpreter.controller.Controller;
import interpreter.model.exceptions.*;
import interpreter.model.programstate.ProgramState;
import interpreter.model.values.Value;
import interpreter.repository.RepositoryException;
import interpreter.view.utils.SourceGenerator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static interpreter.view.utils.SourceGenerator.CODE;

public class RequiredGraphicMenu extends AbstractGraphicMenu {

    @SuppressWarnings("unused")
    public static class TableEntry {
        private final SimpleStringProperty first;
        private final SimpleStringProperty second;

        protected TableEntry(String first, String second) {
            this.first = new SimpleStringProperty(first);
            this.second = new SimpleStringProperty(second);
        }

        public String getFirst() {
            return first.get();
        }

        public void setFirst(String first) {
            this.first.set(first);
        }

        public String getSecond() {
            return second.get();
        }

        public void setSecond(String second) {
            this.second.set(second);
        }
    }


    private TextField programCounter;
    private ListView<String> programList;
    private ListView<String> outputList;
    private ListView<String> fileTable;
    private ListView<String> exeStack;
    private ListView<String> concurrentThreads;
    private TableView<TableEntry> symbolTable;
    private TableView<TableEntry> heapTable;
    private final List<Controller> controllers = new ArrayList<>();
    private Button stepButton;

    @Override
    public void show() {
        launch();
    }

    @Override
    public void start(Stage stage) {
        initNodes();
        addPrograms();
        setupPage(stage);
        addNodes();
        populateProgramList();
        registerHandlers();
        programList.requestFocus();
        stage.show();
    }

    @Override
    protected void initNodes() {
        super.initNodes();

        programCounter = new TextField();
        programCounter.setPrefWidth(pageWidth / 4);
        programCounter.setEditable(false);
        programCounter.setPrefHeight(pageHeight / 8);

        programList = new ListView<>();
        programList.setPrefWidth(pageWidth / 4);
        programList.setPrefHeight(pageHeight / 2);

        stepButton = new Button("Take one step in selected program");
        stepButton.setPrefWidth(pageWidth / 4);
        stepButton.setPrefHeight(pageHeight / 8);

        outputList = new ListView<>();
        outputList.setPrefWidth(pageWidth / 2);
        outputList.setPrefHeight(pageHeight / 4);

        exeStack = new ListView<>();
        exeStack.setPrefWidth(pageWidth / 2);
        exeStack.setPrefHeight(pageHeight / 2);

        fileTable = new ListView<>();
        fileTable.setPrefWidth(pageWidth / 2);
        fileTable.setPrefHeight(pageHeight / 4);

        concurrentThreads = new ListView<>();
        concurrentThreads.setPrefWidth(pageWidth / 4);
        concurrentThreads.setPrefHeight(pageHeight / 4);

        symbolTable = new TableView<>();
        symbolTable.setPrefWidth(pageWidth / 4);
        symbolTable.setPrefHeight(pageHeight / 2);
        TableColumn<TableEntry, String> symbolColumn = new TableColumn<>("Symbol");
        symbolColumn.setCellValueFactory(new PropertyValueFactory<>("first"));
        TableColumn<TableEntry, String> valueColumnForSymT = new TableColumn<>("Value");
        valueColumnForSymT.setCellValueFactory(new PropertyValueFactory<>("second"));
        symbolTable.getColumns().addAll(symbolColumn, valueColumnForSymT);

        heapTable = new TableView<>();
        heapTable.setPrefWidth(pageWidth / 4);
        heapTable.setPrefHeight(pageHeight / 2);
        TableColumn<TableEntry, String> addressColumn = new TableColumn<>("Address");
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("first"));
        TableColumn<TableEntry, String> valueColumnForHeap = new TableColumn<>("Value");
        valueColumnForHeap.setCellValueFactory(new PropertyValueFactory<>("second"));
        heapTable.getColumns().addAll(addressColumn, valueColumnForHeap);
    }

    @Override
    protected void addPrograms() {
        for (String[] source : SourceGenerator.makeList()) {
            Controller controller;
            try {
                controller = inputManager.program(source[CODE], logFileCounter.getAndIncrement());
            } catch (TypecheckException e) {
                showErrorMessageBox("Typechecking error:\n" + e.getMessage());
                continue;
            } catch (TokenizerException e) {
                showErrorMessageBox("Tokenizing error:\n" + e.getMessage());
                continue;
            } catch (ParseException e) {
                showErrorMessageBox("Parsing exception:\n" + e.getMessage());
                continue;
            }
            controllers.add(controller);
        }
    }

    private void addNodes() {
        HBox main = new HBox();
        main.setPadding(new Insets(2 * defaultSpacing));
        main.setSpacing(2 * defaultSpacing);
        VBox left = new VBox(), center = new VBox(), right = new VBox();
        left.setSpacing(defaultSpacing);
        left.setAlignment(Pos.TOP_RIGHT);
        center.setSpacing(defaultSpacing);
        center.setAlignment(Pos.TOP_CENTER);
        right.setSpacing(defaultSpacing);
        right.setAlignment(Pos.TOP_LEFT);
        left.getChildren().addAll(programCounter, programList, concurrentThreads, stepButton);
        center.getChildren().addAll(outputList, exeStack, fileTable);
        right.getChildren().addAll(symbolTable, heapTable);
        main.getChildren().addAll(left, center, right);
        pageLayout.setCenter(main);
    }

    @Override
    protected void populateProgramList() {
        int i = programList.getSelectionModel().getSelectedIndex();
        programList.getItems().clear();
        for (Controller controller : controllers) {
            programList.getItems().add(controller.getProgram().getID().toString());
        }
        programCounter.setText(String.valueOf(programList.getItems().size()));
        if (i != -1 && i < programList.getItems().size()) {
            programList.getSelectionModel().select(i);
        }
    }

    private void updateComponents(ProgramState state) {
        programCounter.setText(String.valueOf(programList.getItems().size()));
        outputList.getItems().clear();
        outputList.getItems().addAll(state
                .getOutputList()
                .stream()
                .map(Value::toString)
                .toList());
        fileTable.getItems().clear();
        fileTable.getItems().addAll(state
                .getFileTable()
                .stream()
                .toList());
        heapTable.getItems().clear();
        ObservableList<TableEntry> heapData = FXCollections.observableArrayList();
        state.getHeapTable().entriesStream().forEach(entry -> heapData.add(new TableEntry(entry.getKey().toString(), entry.getValue().toString())));
        heapTable.setItems(heapData);
        symbolTable.getItems().clear();
        ObservableList<TableEntry> symbolData = FXCollections.observableArrayList();
        state.getSymbolTable().stream().forEach(entry -> symbolData.add(new TableEntry(entry.getKey(), entry.getValue().toString())));
        symbolTable.setItems(symbolData);
        exeStack.getItems().clear();
        exeStack.getItems().addAll(state
                .getExecutionStack()
                .stream()
                .map(Object::toString)
                .toList());
    }

    private void populateThreadList() {
        int programID = programList.getSelectionModel().getSelectedIndex();
        if (programID == -1) {
            showErrorMessageBox("No program selected");
            return;
        }
        Controller current = controllers.get(programID);
        concurrentThreads.getItems().clear();
        concurrentThreads
                .getItems()
                .addAll(current
                        .programs()
                        .stream()
                        .map(s -> "Execution thread " + s.getID().toString())
                        .toList());
        if (!concurrentThreads.getItems().isEmpty()) {
            concurrentThreads.getSelectionModel().select(0);
        }
    }

    private int getCurrentController() {
        int programID = programList.getSelectionModel().getSelectedIndex();
        if (programID == -1) {
            showErrorMessageBox("No program selected");
        }
        return programID;
    }

    private int getCurrentThread() {
        int threadID = concurrentThreads.getSelectionModel().getSelectedIndex();
        if (threadID == -1) {
            showErrorMessageBox("No thread selected");
        }
        return threadID;
    }

    private void updateComponentsOnThreadListAction() {
        int currentController = getCurrentController();
        if (currentController == -1)
            return;
        int currentThread = getCurrentThread();
        if (currentThread == -1)
            return;
        updateComponents(controllers.get(currentController).programs().get(currentThread));
    }

    private void executeAStepAndUpdate() {
        int currentController = getCurrentController();
        if (currentController == -1)
            return;
        int currentThread = getCurrentThread();
        if (currentThread == -1)
            return;
        Controller current = controllers.get(currentController);
        ProgramState selection = current.programs().get(currentThread);
        ProgramState spawnedProcess = null;
        try {
            spawnedProcess = selection.takeOneStep();
            updateComponents(selection);
        } catch (SymbolTableException e) {
            showErrorMessageBox("Symbol table exception:\n" + e.getMessage());
        } catch (StatementException e) {
            showErrorMessageBox("Statement exception:\n" + e.getMessage());
        } catch (ValueException e) {
            showErrorMessageBox("Value exception:\n" + e.getMessage());
        } catch (ProgramStateException e) {
            showErrorMessageBox("Program state exception -- " + e.getMessage());
        } catch (HeapException e) {
            showErrorMessageBox("Heap exception:\n" + e.getMessage());
        } catch (ExpressionException e) {
            showErrorMessageBox("Expression exception:\n" + e.getMessage());
        }
        try {
            current.log(selection);
        } catch (RepositoryException e) {
            throw new RuntimeException("VERY STRANGE AND UNEXPECTED REPOSITORY EXCEPTION:\n" + e.getMessage() + "\n"
                                       + "Did you just change something in the project structure? ,':-\\");
        }
        current.removeCompletedPrograms();
        if (spawnedProcess != null && !spawnedProcess.getExecutionStack().empty()) {
            current.programs().add(spawnedProcess);
        }
        current.removeCompletedPrograms();
        current.collectGarbage();
        if (current.programs().isEmpty()) {
            controllers.remove(currentController);
        }
        populateProgramList();
        if (programList.getItems().size() > currentController) {
            programList.getSelectionModel().select(currentController);
        } else if (!programList.getItems().isEmpty()) {
            programList.getSelectionModel().select(programList.getItems().size() - 1);
        }
        populateThreadList();
        if (concurrentThreads.getItems().size() > currentThread) {
            concurrentThreads.getSelectionModel().select(currentThread);
        } else {
            concurrentThreads.getSelectionModel().select(concurrentThreads.getItems().size() - 1);
        }
    }

    @Override
    protected void registerHandlers() {
        programList.setOnMouseClicked(event -> populateThreadList());
        concurrentThreads.setOnMouseClicked(event -> updateComponentsOnThreadListAction());
        stepButton.setOnAction(event -> executeAStepAndUpdate());
    }


}
