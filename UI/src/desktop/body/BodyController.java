package desktop.body;

import com.sheetcell.engine.Engine;
import com.sheetcell.engine.EngineImpl;
import com.sheetcell.engine.cell.Cell;
import com.sheetcell.engine.coordinate.Coordinate;
import com.sheetcell.engine.coordinate.CoordinateFactory;
import com.sheetcell.engine.sheet.api.SheetReadActions;
import com.sheetcell.engine.utils.RangeValidator;
import com.sheetcell.engine.utils.SheetUpdateResult;
import desktop.dialogs.FilterDialog;
import desktop.dialogs.SortDialog;
import desktop.utils.UIHelper;
import desktop.utils.ValidationUtils;
import desktop.utils.cell.CellRange;
import desktop.utils.color.ColorUtils;
import desktop.utils.parameters.FilterParameters;
import desktop.utils.parameters.MultiColFilterParameters;
import desktop.utils.parameters.SortParameters;
import desktop.utils.parsing.ParsingUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import desktop.sheet.SheetController;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

public class BodyController {

    private Engine engine;

    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private BorderPane mainPane;
    // Top section components
    @FXML
    private Button loadFileButton;
    @FXML
    private TextField filePathField;
    @FXML
    private TextField selectedCellId;
    @FXML
    private ColorPicker backgroundColorPicker;
    @FXML
    private ColorPicker textColorPicker;
    @FXML
    private TextField originalCellValue;
    @FXML
    private Button updateValueButton;
    @FXML
    private TextField lastUpdateCellVersion;
    @FXML
    private ChoiceBox<String> versionSelector;

    // Left section components
    @FXML
    private Button sortButton;
    @FXML
    private Button filterButton;
    @FXML
    private Button addRangeButton;
    @FXML
    private Button deleteRangeButton;
    @FXML
    private Button ViewRanges;
    @FXML
    private TextField rowHeightField;
    @FXML
    private TextField columnWidthField;
    @FXML
    private ChoiceBox<String> alignmentChoiceBox;

    // Reference to SheetController
    @FXML
    private SheetController spreadsheetGridController;
    @FXML
    private ChoiceBox<String> columnChoiceBox;
    @FXML
    private Slider columnHeightSlider;
    @FXML
    private Label columnHeightLabel;
    @FXML
    private HBox columnHeightControls;

    boolean isSheetLoaded = false;



    @FXML
    private void initialize() {
        engine = new EngineImpl();
        if(spreadsheetGridController != null)
        {
            spreadsheetGridController.setEngine(engine);
            spreadsheetGridController.setBodyController(this);
        }

        columnHeightSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            columnHeightLabel.setText(String.valueOf(newValue.intValue()));
            adjustColumnHeight();
        });

        versionSelector.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isEmpty()) {
                // Parse the selected version number
                int selectedVersion = ParsingUtils.extractNewVersionSheet(newValue);
                int currentVersion = engine.getReadOnlySheet()!=null ? engine.getReadOnlySheet().getVersion() : 0;

                // Only display the popup if a past version is selected
                if (selectedVersion < currentVersion && selectedVersion > 0) {
                    handleVersionSelection(selectedVersion);
                }
            }
        });


    }

    @FXML
    private void handleColumnSelection(ActionEvent event) {
        String selectedColumn = columnChoiceBox.getSelectionModel().getSelectedItem();
        if (selectedColumn != null && !selectedColumn.isEmpty()) {
            columnHeightControls.setVisible(true);
        } else {
            columnHeightControls.setVisible(false);
        }
    }
    private void adjustColumnHeight() {

    }
    public void setSpreadsheetGridController(SheetController spreadsheetGridController) {
        this.spreadsheetGridController = spreadsheetGridController;
        spreadsheetGridController.setEngine(engine);
        spreadsheetGridController.setBodyController(this);
    }

    @FXML
    private void handleLoadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Spreadsheet File");
        File file = fileChooser.showOpenDialog(mainPane.getScene().getWindow());

        if (file != null) {
            // Create a new Task for loading the file
            Task<Void> loadFileTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        // Simulate a loading delay (1-2 seconds) to show progress
                        updateProgress(0, 100);
                        Thread.sleep(500);  // Simulating initial delay
                        updateProgress(50, 100);

                        // Load the XML file with the engine
                        engine.loadSheet(file.getAbsolutePath());
                        updateProgress(100, 100);

                        // Pass the sheet data to the SheetController
                        SheetReadActions sheetData = engine.getReadOnlySheet();
                        spreadsheetGridController.displaySheet(sheetData);
                        isSheetLoaded = true;
                        Platform.runLater(() -> {
                            versionSelector.getItems().clear();
                            versionSelector.getItems().add("Version 1"+ " Cells Changed: "+engine.getReadOnlySheet().getCellChangeCount());
                            versionSelector.getSelectionModel().select(0); // Select the first version
                        });

                        // Update the file path field on the UI thread
                        Platform.runLater(() -> filePathField.setText(file.getAbsolutePath()));
                        populateColumnChoiceBox();

                    } catch (Exception e) {
                        // If there's an error, show an alert on the UI thread
                        Platform.runLater(() -> UIHelper.showAlert(e.getMessage()));
                        throw e;  // Re-throw to handle in the task's failed handler
                    }
                    return null;
                }
            };

            // Show a progress indicator while the task is running
            ProgressBar progressBar = new ProgressBar();
            progressBar.progressProperty().bind(loadFileTask.progressProperty());

            // Create a dialog or progress bar indicator in your UI to show progress
            Stage progressStage = new Stage();
            VBox progressRoot = new VBox(progressBar);
            Scene progressScene = new Scene(progressRoot);
            progressStage.setScene(progressScene);
            progressStage.setTitle("Loading File...");
            progressStage.setAlwaysOnTop(true);
            progressStage.show();

            // Handle task completion
            loadFileTask.setOnSucceeded(workerStateEvent -> {
                progressStage.close();  // Close progress dialog when loading is done
            });

            loadFileTask.setOnFailed(workerStateEvent -> {
                progressStage.close();  // Close progress dialog if loading fails
                //showAlert("Failed to load the file. Please check the file format and try again.");
            });

            // Start the task in a new thread
            new Thread(loadFileTask).start();
        }
    }

    private void populateColumnChoiceBox() {
        // Clear existing choices
        columnChoiceBox.getItems().clear();

        int maxColumns = engine.getReadOnlySheet().getMaxColumns();
        for (int i = 0; i < maxColumns; i++) {
            columnChoiceBox.getItems().add("Column " + CoordinateFactory.convertIndexToColumnLabel(i));
        }

        // Reset slider visibility
        columnHeightControls.setVisible(false);
    }

    @FXML
    private void handleSort(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to sort data.");
            return;
        }
        // Display a dialog to collect sorting parameters
        SortDialog sortDialogMaker = new SortDialog();
        Dialog<SortParameters> dialog = sortDialogMaker.createSortDialog();
        Optional<SortParameters> result = dialog.showAndWait();

        if (result.isPresent()) {
            SortParameters params = result.get();
            String rangeInput=params.getRangeInput().trim().toUpperCase();
            String columnsInput=params.getColumnsInput().trim().toUpperCase();

            try {
                ValidationUtils.validateSortInput(rangeInput, columnsInput, engine.getReadOnlySheet().getMaxRows(), engine.getReadOnlySheet().getMaxColumns());
                // Parse the range and columns
                CellRange range = ParsingUtils.parseRange(rangeInput);
                List<Integer> sortColumns = ParsingUtils.parseColumns(columnsInput);

                // Trigger sorting in SheetController
                spreadsheetGridController.showSortedData(range, sortColumns);
            } catch (Exception e) {
                UIHelper.showError("Invalid input format.", e.getMessage());

            }
        }
    }

    @FXML
    void handleUpdateValue(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to update cell values.");
            return;
        }
        String cellAddress = selectedCellId.getText();
        String newValue = originalCellValue.getText();
        if (cellAddress == null || cellAddress.isEmpty()) {
            UIHelper.showAlert("No cell selected", "Please select a cell to update.");
            return;
        }
        if (newValue == null) {
            newValue = "";
        }
        int row = CoordinateFactory.getRowIndex(cellAddress);
        int column = CoordinateFactory.getColumnIndex(cellAddress);

        try{
            engine.doesCellIdVaild(cellAddress);
            SheetUpdateResult result = engine.setCellValue(cellAddress, newValue);
            if (result.isNoActionNeeded()) {
                UIHelper.showAlert(result.getErrorMessage());
                return;
            } else if (result.hasError()) {
                UIHelper.showError("Update failed: ",result.getErrorMessage());
                return;
            } else {
                System.out.println("Cell " + cellAddress + " updated successfully.");
            }
            spreadsheetGridController.refreshSpreadsheet();
            Cell changedCell = engine.getReadOnlySheet().getCell(row, column);
            if (changedCell != null) {
                int newVersion = changedCell.getVersion();
                lastUpdateCellVersion.setText(String.valueOf(newVersion));
            }
            int newVersionSheet = engine.getReadOnlySheet().getVersion();
            Platform.runLater(() -> {
                versionSelector.getItems().add("Version " + newVersionSheet + " Cells Changed: "+engine.getReadOnlySheet().getCellChangeCount());
                versionSelector.getSelectionModel().selectLast();
            });
            spreadsheetGridController.reselectCell(row, column);
            UIHelper.showAlert("Cell Updated", "Cell value updated successfully.");
        } catch (Exception e) {
            UIHelper.showAlert("Error updating cell", e.getMessage());
        }
    }

    @FXML
    private void handleApplyStyle(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to apply cell styles.");
            return;
        }
        String cellAddress = selectedCellId.getText();
        if (cellAddress != null && !cellAddress.isEmpty()) {
            int row = CoordinateFactory.getRowIndex(cellAddress);
            int column = CoordinateFactory.getColumnIndex(cellAddress);
            String backgroundColor = ColorUtils.colorToHex(backgroundColorPicker.getValue());
            String textColor = ColorUtils.colorToHex(textColorPicker.getValue());
            String style = "-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + "; -fx-fill: " + textColor + ";";

            spreadsheetGridController.applyCellStyle(row, column, style);
        }
    }

    @FXML
    private void handleVersionSelection(int selectedVersion) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to view past versions.");
            return;
        }
        try {
            // Fetch the sheet for the selected version
            SheetReadActions versionSheet = engine.getSheetVersion(selectedVersion);

            // Pass the versioned sheet to the SheetController to display in a popup
            spreadsheetGridController.displayVersionInPopup(versionSheet, selectedVersion);
            
        } catch (Exception e) {
            UIHelper.showError("Error displaying version", e.getMessage());
        }
    }

    @FXML
    private void handleResetStyle(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to reset cell styles.");
            return;
        }
        String cellAddress = selectedCellId.getText();
        if (cellAddress != null && !cellAddress.isEmpty()) {
            int row = CoordinateFactory.getRowIndex(cellAddress);
            int column = CoordinateFactory.getColumnIndex(cellAddress);
            spreadsheetGridController.resetCellStyle(row, column);
        }
    }

    public void updateSelectedCell(String cellAddress, String originalValue, String versionString) {
        selectedCellId.setText(cellAddress);
        originalCellValue.setText(originalValue);
        lastUpdateCellVersion.setText(versionString);
    }

    public void clearSelectedCell() {
        selectedCellId.clear();
        originalCellValue.clear();
        lastUpdateCellVersion.clear();
    }

    @FXML
    private void handleAddRange(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to define a range.");
            return;
        }
        // Create a dialog to input range name and definition
        TextInputDialog rangeDialog = new TextInputDialog();
        rangeDialog.setTitle("Add New Range");
        rangeDialog.setHeaderText("Define a New Range");
        rangeDialog.setContentText("Enter range name (unique):");

        // Create input fields for range definition
        TextField rangeNameField = new TextField();
        rangeNameField.setPromptText("Unique Range Name");

        TextField rangeDefinitionField = new TextField();
        rangeDefinitionField.setPromptText("Range Definition (e.g., A1..A4)");

        VBox dialogContent = new VBox();
        dialogContent.getChildren().addAll(
                new Label("Range Name:"), rangeNameField,
                new Label("Range Definition:"), rangeDefinitionField
        );
        rangeDialog.getDialogPane().setContent(dialogContent);

        Optional<String> result = rangeDialog.showAndWait();

        if (result.isPresent()) {
            String rangeName = rangeNameField.getText().trim();
            String rangeDefinition = rangeDefinitionField.getText().trim();

            try {
                engine.addRange(rangeName, rangeDefinition); // Add range using engine method
                UIHelper.showAlert("Success", "Range added successfully.");
            } catch (Exception e) {
                UIHelper.showError("Error Adding Range", e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteRange(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to delete a range.");
            return;
        }
        Set<String> ranges = engine.getAllRanges();
        List<String> rangeList = new ArrayList<>(ranges);
        ChoiceDialog<String> deleteDialog = new ChoiceDialog<>(rangeList.isEmpty() ? null : rangeList.get(0), rangeList);
        deleteDialog.setTitle("Delete Range");
        deleteDialog.setHeaderText("Select a Range to Delete");
        deleteDialog.setContentText("Choose a range:");

        Optional<String> result = deleteDialog.showAndWait();

        if (result.isPresent()) {
            String rangeName = result.get();
            try {
                engine.deleteRange(rangeName);
                UIHelper.showAlert("Success", "Range deleted successfully.");
            } catch (Exception e) {
                UIHelper.showError("Error Deleting Range", e.getMessage());
            }
        }
    }

    @FXML
    private void handleViewRange(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to view ranges.");
            return;
        }
        // Fetch all ranges from the engine
        Set<String> ranges = engine.getAllRanges();

        if (ranges.isEmpty()) {
            UIHelper.showAlert("No ranges defined", "There are no ranges currently defined to view.");
            return;
        }

        // Create a Choice Dialog to select a range
        ChoiceDialog<String> dialog = new ChoiceDialog<>(ranges.iterator().next(), ranges);
        dialog.setTitle("View Range");
        dialog.setHeaderText("Select a range to view:");
        dialog.setContentText("Available Ranges:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(rangeName -> {
            try {
                // Get the set of coordinates for the selected range
                Set<Coordinate> rangeCoordinates = engine.getRangeCoordinates(rangeName);

                // Highlight the cells within this range
                spreadsheetGridController.highlightRange(rangeCoordinates);

            } catch (Exception e) {
                UIHelper.showError("Error", e.getMessage());
            }
        });
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to apply filters.");
            return;
        }
        FilterDialog filterDialogMaker = new FilterDialog(this.spreadsheetGridController,
                engine.getReadOnlySheet().getMaxRows(), engine.getReadOnlySheet().getMaxColumns());
        // Open the column selection dialog
        Dialog<String> columnDialog = filterDialogMaker.createColumnSelectionDialog();
        Optional<String> result = columnDialog.showAndWait();

        if (result.isPresent()) {
            String selectedColumn = result.get();
            int filterColumnIndex = CoordinateFactory.convertColumnLabelToIndex(selectedColumn.trim().toUpperCase());

            // Proceed to open the filter dialog with the selected column
            Dialog<FilterParameters> filterDialog =filterDialogMaker.createFilterDialog(filterColumnIndex);
            Optional<FilterParameters> filterResult = filterDialog.showAndWait();

            if (filterResult.isPresent()) {
                FilterParameters params = filterResult.get();
                String rangeInput = params.getRangeInput().trim().toUpperCase();
                List<String> selectedValues = params.getFilterValues();

                try {
                    ValidationUtils.validateFilterInput(rangeInput, selectedColumn, selectedValues, engine.getReadOnlySheet().getMaxRows(), engine.getReadOnlySheet().getMaxColumns());
                    // Parse the range and column
                    CellRange range = ParsingUtils.parseRange(rangeInput);

                    // Trigger filtering in SheetController
                    spreadsheetGridController.showFilteredData(range, filterColumnIndex, selectedValues);
                } catch (Exception e) {
                    UIHelper.showError("Invalid input format.", e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleMultiColumnFilter(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to apply multi-column filters.");
            return;
        }
        // Open the dialog to select columns and filter values
        FilterDialog filterDialogMaker = new FilterDialog(this.spreadsheetGridController,
                engine.getReadOnlySheet().getMaxRows(), engine.getReadOnlySheet().getMaxColumns());
        Dialog<MultiColFilterParameters> dialog = filterDialogMaker.createMultiColumnFilterDialog();
        Optional<MultiColFilterParameters> result = dialog.showAndWait();

        if (result.isPresent()) {
            MultiColFilterParameters filterParams = result.get();

            // Print the selected range and filter criteria
            System.out.println("Selected range: " + filterParams.getRangeInput());
            System.out.println("Filter criteria: " + filterParams.getFilterCriteria());

            // Pass the filter parameters and range to the SheetController
            spreadsheetGridController.applyMultiColumnFilter(filterParams.getRangeInput(), filterParams.getFilterCriteria());
        }
    }

    @FXML
    private void handleApplyAlignment(ActionEvent event) {
        if (!isSheetLoaded) {
            UIHelper.showAlert("No sheet loaded", "Please load a spreadsheet file to apply column alignment.");
            return;
        }

    }

}
