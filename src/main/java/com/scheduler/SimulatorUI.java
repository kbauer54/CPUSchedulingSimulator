package com.scheduler;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SimulatorUI {

    private Stage stage;
    private Scheduler scheduler;
    private List<PCB> loadedProcesses;
    private Timeline timeline;

    // Top controls
    private Button loadButton;
    private ComboBox<String> algorithmBox;
    private TextField quantumField;
    private Button startPauseButton;
    private Button nextButton;
    private ComboBox<String> speedBox;
    private Button saveButton;

    // Metrics bar
    private Label systemTimeLabel;
    private Label throughputLabel;
    private Label avgTurnLabel;
    private Label avgWaitLabel;
    private Label cpuUtilLabel;

    // Visualization panels
    private Label cpuLabel;
    private Label ioLabel;
    private FlowPane readyQueuePane;
    private FlowPane ioQueuePane;

    // Process info area
    private TextArea processInfoArea;

    // Process table
    private TableView<PCBTableRow> processTable;
    private ObservableList<PCBTableRow> tableData;

    // Event log
    private TextArea eventLogArea;

    // State
    private boolean running = false;
    private boolean loaded = false;
    private int lastLogSize = 0;

    public SimulatorUI(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        stage.setTitle("CPU Scheduling Simulator");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        root.setTop(buildTopBar());
        root.setCenter(buildCenter());
        root.setBottom(buildBottomBar());

        Scene scene = new Scene(root, 1200, 750);
        stage.setScene(scene);
        stage.show();
    }

    // ── TOP BAR ──────────────────────────────────────────────────────────────

    private VBox buildTopBar() {
        // Controls row
        loadButton = new Button("Load...");
        styleButton(loadButton, "#4a9eff");

        algorithmBox = new ComboBox<>();
        algorithmBox.getItems().addAll("FCFS", "SJF", "PS", "RR");
        algorithmBox.setValue("FCFS");
        algorithmBox.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-mark-color: white;");

        algorithmBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setTextFill(Color.WHITE);
            }
        });

        Label quantumLabel = new Label("Quantum:");
        quantumLabel.setTextFill(Color.LIGHTGRAY);
        quantumField = new TextField("2");
        quantumField.setPrefWidth(50);
        quantumField.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");

        startPauseButton = new Button("▶ Start");
        styleButton(startPauseButton, "#4CAF50");
        startPauseButton.setDisable(true);

        nextButton = new Button("⏭ Next");
        styleButton(nextButton, "#ff9800");
        nextButton.setDisable(true);

        speedBox = new ComboBox<>();
        speedBox.getItems().addAll("0.5 fps", "1 fps", "2 fps", "5 fps", "10 fps");
        speedBox.setValue("1 fps");
        speedBox.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-mark-color: white;");

        speedBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                setTextFill(Color.WHITE);
            }
        });

        saveButton = new Button("💾 Save Log");
        styleButton(saveButton, "#9c27b0");
        saveButton.setDisable(true);

        HBox controlsRow = new HBox(10,
                loadButton,
                new Label("  Algorithm:") {{ setTextFill(Color.LIGHTGRAY); }},
                algorithmBox,
                quantumLabel, quantumField,
                startPauseButton,
                new Label("  Speed:") {{ setTextFill(Color.LIGHTGRAY); }},
                speedBox,
                nextButton,
                saveButton
        );
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        controlsRow.setPadding(new Insets(10, 15, 5, 15));

        // Metrics row
        systemTimeLabel = metricLabel("System Time: 0");
        throughputLabel = metricLabel("Throughput: 0.00");
        avgTurnLabel    = metricLabel("AVG Turn: 0.00");
        avgWaitLabel    = metricLabel("AVG Wait: 0.00");
        cpuUtilLabel    = metricLabel("CPU Util: 0.00%");

        HBox metricsRow = new HBox(30,
                systemTimeLabel, throughputLabel,
                avgTurnLabel, avgWaitLabel, cpuUtilLabel
        );
        metricsRow.setPadding(new Insets(5, 15, 10, 15));
        metricsRow.setAlignment(Pos.CENTER_LEFT);

        // Wire up buttons
        loadButton.setOnAction(e -> loadFile());
        startPauseButton.setOnAction(e -> toggleStartPause());
        nextButton.setOnAction(e -> stepOnce());
        saveButton.setOnAction(e -> saveLog());
        algorithmBox.setOnAction(e -> resetSimulation());
        speedBox.setOnAction(e -> updateSpeed());

        VBox topBar = new VBox(controlsRow, metricsRow);
        topBar.setStyle("-fx-background-color: #252525; -fx-border-color: #444; " +
                "-fx-border-width: 0 0 1 0;");
        return topBar;
    }

    // ── CENTER ───────────────────────────────────────────────────────────────

    private HBox buildCenter() {
        // Left: visualization
        VBox vizPanel = buildVizPanel();
        vizPanel.setPrefWidth(700);

        // Right: event log
        VBox logPanel = buildLogPanel();
        logPanel.setPrefWidth(300);

        HBox center = new HBox(10, vizPanel, logPanel);
        center.setPadding(new Insets(10));
        HBox.setHgrow(vizPanel, Priority.ALWAYS);
        return center;
    }

    private VBox buildVizPanel() {
        // CPU box
        cpuLabel = new Label("CPU: idle");
        cpuLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        cpuLabel.setTextFill(Color.WHITE);
        VBox cpuBox = new VBox(cpuLabel);
        cpuBox.setPadding(new Insets(10));
        cpuBox.setStyle("-fx-background-color: #1a6b3c; -fx-border-color: #2ecc71; " +
                "-fx-border-width: 2; -fx-border-radius: 5;");
        cpuBox.setPrefWidth(200);

        // IO box
        ioLabel = new Label("I/O: idle");
        ioLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        ioLabel.setTextFill(Color.WHITE);
        VBox ioBox = new VBox(ioLabel);
        ioBox.setPadding(new Insets(10));
        ioBox.setStyle("-fx-background-color: #6b3a1a; -fx-border-color: #e67e22; " +
                "-fx-border-width: 2; -fx-border-radius: 5;");
        ioBox.setPrefWidth(200);

        HBox devicesRow = new HBox(20, cpuBox, ioBox);
        devicesRow.setAlignment(Pos.CENTER_LEFT);

        // Ready queue
        Label rqTitle = sectionLabel("Ready Queue");
        readyQueuePane = new FlowPane();
        readyQueuePane.setHgap(5);
        readyQueuePane.setVgap(5);
        readyQueuePane.setPadding(new Insets(8));
        readyQueuePane.setMinHeight(50);
        readyQueuePane.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #555; " +
                "-fx-border-width: 1;");

        // IO queue
        Label ioqTitle = sectionLabel("I/O Queue");
        ioQueuePane = new FlowPane();
        ioQueuePane.setHgap(5);
        ioQueuePane.setVgap(5);
        ioQueuePane.setPadding(new Insets(8));
        ioQueuePane.setMinHeight(50);
        ioQueuePane.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #555; " +
                "-fx-border-width: 1;");

        // Process info
        Label piTitle = sectionLabel("Process Details");
        processInfoArea = new TextArea();
        processInfoArea.setEditable(false);
        processInfoArea.setPrefHeight(120);
        processInfoArea.setStyle("-fx-control-inner-background: #1a1a1a; " +
                "-fx-text-fill: #c8c8c8; -fx-font-family: Monospaced;");

        // Process table
        Label tableTitle = sectionLabel("Process Table");
        processTable = buildProcessTable();

        VBox viz = new VBox(10,
                devicesRow,
                rqTitle, readyQueuePane,
                ioqTitle, ioQueuePane,
                piTitle, processInfoArea,
                tableTitle, processTable
        );
        viz.setPadding(new Insets(5));
        VBox.setVgrow(processTable, Priority.ALWAYS);
        return viz;
    }

    private VBox buildLogPanel() {
        Label title = sectionLabel("Event Log");
        eventLogArea = new TextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setWrapText(true);
        eventLogArea.setStyle("-fx-control-inner-background: #1a1a1a; " +
                "-fx-text-fill: #a8d8a8; -fx-font-family: Monospaced; " +
                "-fx-font-size: 11;");
        VBox.setVgrow(eventLogArea, Priority.ALWAYS);
        VBox panel = new VBox(5, title, eventLogArea);
        panel.setPadding(new Insets(5));
        return panel;
    }

    // ── BOTTOM BAR ───────────────────────────────────────────────────────────

    private HBox buildBottomBar() {
        Label hint = new Label("Load a scenario file to begin.");
        hint.setTextFill(Color.GRAY);
        hint.setFont(Font.font(11));
        HBox bar = new HBox(hint);
        bar.setPadding(new Insets(5, 15, 5, 15));
        bar.setStyle("-fx-background-color: #252525; -fx-border-color: #444; " +
                "-fx-border-width: 1 0 0 0;");
        return bar;
    }

    // ── PROCESS TABLE ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<PCBTableRow> buildProcessTable() {
        TableView<PCBTableRow> table = new TableView<>();
        table.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white;");
        table.setPrefHeight(180);

        TableColumn<PCBTableRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(40);

        TableColumn<PCBTableRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(80);

        TableColumn<PCBTableRow, Integer> arrCol = new TableColumn<>("Arrival");
        arrCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        arrCol.setPrefWidth(60);

        TableColumn<PCBTableRow, Integer> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        prioCol.setPrefWidth(60);

        TableColumn<PCBTableRow, Integer> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startCol.setPrefWidth(50);

        TableColumn<PCBTableRow, Integer> finishCol = new TableColumn<>("Finish");
        finishCol.setCellValueFactory(new PropertyValueFactory<>("finishTime"));
        finishCol.setPrefWidth(55);

        TableColumn<PCBTableRow, Integer> waitCol = new TableColumn<>("Wait");
        waitCol.setCellValueFactory(new PropertyValueFactory<>("waitTime"));
        waitCol.setPrefWidth(50);

        TableColumn<PCBTableRow, Integer> ioWaitCol = new TableColumn<>("IO Wait");
        ioWaitCol.setCellValueFactory(new PropertyValueFactory<>("ioWaitTime"));
        ioWaitCol.setPrefWidth(60);

        TableColumn<PCBTableRow, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
        stateCol.setPrefWidth(90);

        table.getColumns().addAll(idCol, nameCol, arrCol, prioCol,
                startCol, finishCol, waitCol, ioWaitCol, stateCol);

        tableData = FXCollections.observableArrayList();
        table.setItems(tableData);
        return table;
    }

    // ── SIMULATION CONTROL ───────────────────────────────────────────────────

    private void loadFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Scenario File");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.csv", "*.*")
        );
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        try {
            loadedProcesses = ScenarioParser.parse(file);
            resetSimulation();
            startPauseButton.setDisable(false);
            nextButton.setDisable(false);
            loaded = true;
            eventLogArea.appendText("Loaded: " + file.getName() +
                    " (" + loadedProcesses.size() + " processes)\n");
        } catch (IOException ex) {
            showError("Failed to load file: " + ex.getMessage());
        }
    }

    private void resetSimulation() {
        if (loadedProcesses == null) return;
        stopTimeline();
        running = false;
        startPauseButton.setText("▶ Start");

        String alg = algorithmBox.getValue();
        int q = parseQuantum();
        scheduler = new Scheduler(
                Scheduler.Algorithm.valueOf(alg),
                q, loadedProcesses
        );
        lastLogSize = 0;
        updateUI();
    }

    private void toggleStartPause() {
        if (!loaded) return;
        if (scheduler.isFinished()) {
            resetSimulation();
            return;
        }
        running = !running;
        if (running) {
            startPauseButton.setText("⏸ Pause");
            nextButton.setDisable(true);
            startTimeline();
        } else {
            startPauseButton.setText("▶ Resume");
            nextButton.setDisable(false);
            stopTimeline();
        }
    }

    private void stepOnce() {
        if (!loaded || scheduler.isFinished()) return;
        scheduler.tick();
        updateUI();
        if (scheduler.isFinished()) onFinished();
    }

    private void startTimeline() {
        double fps = parseFps();
        timeline = new Timeline(new KeyFrame(Duration.millis(1000.0 / fps), e -> {
            scheduler.tick();
            updateUI();
            if (scheduler.isFinished()) {
                stopTimeline();
                onFinished();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopTimeline() {
        if (timeline != null) timeline.stop();
    }

    private void onFinished() {
        running = false;
        startPauseButton.setText("🔄 Restart");
        nextButton.setDisable(true);
        saveButton.setDisable(false);
        eventLogArea.appendText("\n--- Simulation Complete ---\n");
        eventLogArea.appendText(String.format(
                "CPU Utilization: %.2f%%\nAvg Turnaround: %.2f\nAvg Wait: %.2f\nThroughput: %.4f\n",
                scheduler.getCpuUtilization(),
                scheduler.getAverageTurnaroundTime(),
                scheduler.getAverageWaitTime(),
                scheduler.getThroughput()
        ));
    }

    // ── UI UPDATE ────────────────────────────────────────────────────────────

    private void updateUI() {
        if (scheduler == null) return;

        // Metrics
        systemTimeLabel.setText("System Time: " + scheduler.getSystemTime());
        throughputLabel.setText(String.format("Throughput: %.4f",
                scheduler.getThroughput()));
        avgTurnLabel.setText(String.format("AVG Turn: %.2f",
                scheduler.getAverageTurnaroundTime()));
        avgWaitLabel.setText(String.format("AVG Wait: %.2f",
                scheduler.getAverageWaitTime()));
        cpuUtilLabel.setText(String.format("CPU Util: %.2f%%",
                scheduler.getCpuUtilization()));

        // CPU box
        if (scheduler.getCpu().isIdle()) {
            cpuLabel.setText("CPU: idle");
        } else {
            PCB p = scheduler.getCpu().getCurrentProcess();
            cpuLabel.setText("CPU: " + p.getName() +
                    " [burst: " + p.getRemainingBurstTime() + "]");
        }

        // IO box
        if (scheduler.getIoDevice().isIdle()) {
            ioLabel.setText("I/O: idle");
        } else {
            PCB p = scheduler.getIoDevice().getCurrentProcess();
            ioLabel.setText("I/O: " + p.getName() +
                    " [io: " + p.getRemainingIOTime() + "]");
        }

        // Ready queue chips
        readyQueuePane.getChildren().clear();
        for (PCB p : scheduler.getReadyQueue()) {
            readyQueuePane.getChildren().add(processChip(p, "#4a9eff"));
        }

        // IO queue chips
        ioQueuePane.getChildren().clear();
        for (PCB p : scheduler.getIoQueue()) {
            ioQueuePane.getChildren().add(processChip(p, "#e67e22"));
        }

        // Process details text
        StringBuilder sb = new StringBuilder();
        for (PCB p : scheduler.getAllProcesses()) {
            sb.append(String.format("%-10s state=%-12s cpuBurst=%d/%d\n",
                    p.getName(),
                    p.getState(),
                    p.getRemainingBurstTime(),
                    p.getCpuBursts().get(p.getCurrentBurstIndex())
            ));
        }
        processInfoArea.setText(sb.toString());

        // Process table
        tableData.clear();
        for (PCB p : scheduler.getAllProcesses()) {
            tableData.add(new PCBTableRow(p));
        }

        // Event log - only append new entries
        List<String> log = scheduler.getEventLog();
        for (int i = lastLogSize; i < log.size(); i++) {
            eventLogArea.appendText(log.get(i) + "\n");
        }
        lastLogSize = log.size();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Label processChip(PCB p, String color) {
        Label chip = new Label(p.getName());
        chip.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 4 8 4 8; -fx-background-radius: 12; " +
                "-fx-font-weight: bold; -fx-font-size: 11;");
        return chip;
    }

    private void saveLog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Event Log");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        fc.setInitialFileName("simulation_log.txt");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(eventLogArea.getText());
        } catch (IOException ex) {
            showError("Failed to save log: " + ex.getMessage());
        }
    }

    private void updateSpeed() {
        if (timeline != null && running) {
            stopTimeline();
            startTimeline();
        }
    }

    private double parseFps() {
        String val = speedBox.getValue().replace(" fps", "");
        try { return Double.parseDouble(val); } catch (Exception e) { return 1.0; }
    }

    private int parseQuantum() {
        try { return Integer.parseInt(quantumField.getText().trim()); }
        catch (Exception e) { return 2; }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
    }

    private Label metricLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.LIGHTGREEN);
        l.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.LIGHTGRAY);
        l.setFont(Font.font(null, FontWeight.BOLD, 12));
        return l;
    }
}