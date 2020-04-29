package view;

import controller.NoSuchCalendarException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import model.CalendarEvent;
import model.CalendarModel;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MonthView implements CalendarViewMode {
    private BorderPane b;
    private LocalDate currentView;
    private GridPane grid;
    private CalendarModel model;
    private ArrayList<TilePane> panes;
    private Label title;

    /**
     * The start method overridden from Application
     * This method is called when the Calendar class
     * calls it's launch method. This is the main
     * method of the program and holds all of the
     * initialization of the GUI and it's event handelers.
     */
    public MonthView() {
        // Initialize current day and lists to help with construction
        currentView = LocalDate.now();
        model = new CalendarModel();

        // Label on Calendar with all the weekdays as well as month/year label
        GridPane dayNames = new GridPane();
        title = new Label();
        title.setFont(new Font(50));
        String[] weekDays = new String[]{"Sun", "Mon", "Tue", "Wed", "Thu",
                "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(weekDays[i]);
            dayNames.add(l, i, 0);
            GridPane.setMargin(l, new Insets(1, 28, 1, 28));
        }

        // initialize buttons
        Button forward = new Button("->");
        Button backward = new Button("<-");
        HBox hbox = new HBox();
        hbox.getChildren().add(backward);
        hbox.getChildren().add(forward);
        hbox.setSpacing(492);

        // vBox to help layout
        VBox vbox = new VBox();
        vbox.getChildren().add(hbox);
        vbox.getChildren().add(title);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(dayNames);

        // Create grid
        grid = new GridPane();
        grid.setGridLinesVisible(true);

        // List of panes created
        panes = new ArrayList<>();

        // Create borderpane
        b = new BorderPane();

        // Initialize board with panes
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                TilePane t = new TilePane();
                t.setPrefHeight(100);
                Label l = new Label();
                t.getChildren().add(l);
                panes.add(t);
                grid.add(t, j, i);
            }
        }

        // draws the current month
        drawMonth();

        // Click on grid and get the row/col coordinates
        grid.setOnMouseClicked(event -> {
            // get the row and col that is clicked on
            for (Node node : grid.getChildren()) {
                if (node instanceof TilePane) {
                    if (node.getBoundsInParent().contains(event.getX(), event.getY())) {
                        int clickedY = GridPane.getRowIndex(node);
                        int clickedX = GridPane.getColumnIndex(node);
                        int day = getDayOnClick(clickedY, clickedX);
                        if (day > 0) {
                            // TODO
//                            EventDialog.newEventAt(
//                                    currentView.withDayOfMonth(day),
//                                    controller.getCalendarNames()
//                            ).showAndWait()
//                                    // add the event if it was created
//                                    .ifPresent(pair -> controller.addEvent(
//                                            pair.getValue(),
//                                            pair.getKey()
//                                    ));
                        }
                    }
                }
            }
        });

        // Next Month
        forward.setOnAction(e -> {
            currentView = currentView.plusMonths(1);

            drawMonth();
        });

        // Previous Month
        backward.setOnAction(e -> {
            currentView = currentView.minusMonths(1);
            drawMonth();
        });

        b.setTop(vbox);
        b.setCenter(grid);
    }

    /**
     * This method draws the month view.
     */
    public void drawMonth() {
        // *Remove Events will be implemented here*

        String month = currentView.getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        String year = "" + currentView.getYear();
        title.setText(month + " " + year);

        LocalDate beg = currentView.withDayOfMonth(1);

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                int index = i * 7 + j;
                TilePane t = panes.get(index);
                t.setStyle("");
                t.setPrefHeight(100);
                ((Label) t.getChildren().get(0)).setText("");
                t.setStyle("-fx-background-color:white");

                if (beg.getMonthValue() > currentView.getMonthValue()) continue;
                if (index < beg.getDayOfWeek().getValue()) continue;
                ((Label) t.getChildren().get(0)).setText(beg.getDayOfMonth() + "");
                if (LocalDate.now().equals(beg))
                    t.setStyle("-fx-background-color:aqua");

                beg = beg.plusDays(1);

                t.getChildren().removeIf(Button.class::isInstance);
                CalendarEvent[] events = model.getEventsInDay(beg);
                for (CalendarEvent event : events) {
                    Button button = new Button(event.getTitle());
                    t.getChildren().add(button);
                    // TODO
//                    button.setOnMouseClicked(butt ->
//                            EventDialog.editEvent(
//                                    event,
//                                    currentCalendarName,
//                                    controller.getCalendarNames()
//                            ).showAndWait().ifPresent(pair ->
//                                    controller.moveEvent(
//                                            // event to move
//                                            pair.getValue(),
//                                            // calendar name to move it to
//                                            pair.getKey()
//                                    )
//                            )
//                    );
                }
            }
        }

    }

    /**
     * This method removes events from each pane
     * This method removes all of the events from
     * the panes in the grid so new events can be
     * updated for the month.
     */
    public void removeEvents() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {

            }
        }
    }

    /**
     * This method returns the day the user clicks on
     * <p>
     * This method, when given a row and column that
     * the user has clicked on, will return the int of
     * the day that was clicked. If the user clicks on
     * a box that isn't a day, it returns -1.
     *
     * @param row the row clicked on
     * @param col the column clicked on
     * @return the day clicked on or -1.
     */
    public int getDayOnClick(int row, int col) {
        TilePane t = panes.get(row * 7 + col);
        String dayClicked = ((Label) t.getChildren().get(0)).getText();
        if (dayClicked.equals("")) {
            return -1;
        } else {
            return Integer.parseInt(dayClicked);
        }
    }

    @Override
    public Node getNode() {
        return b;
    }

    @Override
    public LocalDate getDate() {
        return currentView.withDayOfMonth(1);
    }

    @Override
    public void setVisibleCalendars(Set<String> calNames) throws NoSuchCalendarException {
        // TODO
    }

    @Override
    public void setDate(LocalDate d) {
        currentView = d;
        drawMonth();
    }
}
