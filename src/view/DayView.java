package view;

import controller.CalendarController;
import controller.NoSuchCalendarException;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.util.Pair;
import model.CalendarEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class DayView implements CalendarViewMode {
    private static final int NUM_HOUR_SUBSECTIONS = 4;
    private static final int MINUTE_INCREMENT = 60 / NUM_HOUR_SUBSECTIONS;
    private final CalendarController controller;
    private final BorderPane root, top;
    private final Button forward;
    private final Button backward;
    private final Label header;
    private GridPane dayPane;
    private Set<String> visibleCalendars;
    private LocalDate date;

    public DayView(CalendarController controller) {
        this.controller = controller;
        date = LocalDate.now();
        visibleCalendars = controller.getCalendarNames();

        root = new BorderPane();
        top = new BorderPane();
        backward = new Button("<-");
        header = new Label();
        forward = new Button("->");
        forward.setOnAction(e -> setDate(date.plusDays(1)));
        backward.setOnAction(e -> setDate(date.minusDays(1)));

        top.setLeft(backward);
        top.setRight(forward);
        top.setCenter(header);
        root.setTop(top);
//        dayPane.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        drawDay();
    }

    /**
     * @return a list of columns containing events, used for displaying the events
     */
    private List<List<Pair<String, CalendarEvent>>> getEventColumns() {
        List<List<Pair<String, CalendarEvent>>> eventColumns = new ArrayList<>(new ArrayList<>());
        visibleCalendars.stream()
                // filter out names that would throw an exception
                .filter(controller.getCalendarNames()::contains)
                // get all events for this day from the controller, flattening them into a single stream
                .flatMap(name -> {
                    try {
                        return Arrays.stream(controller.getEventsInDay(name, date)).map(e -> new Pair<>(name, e));
                    } catch (NoSuchCalendarException e) {
                        // should never get here: filtered out
                        return null;
                    }
                }).forEach(pair -> {
            // find any one column which the event could be added to
            // without overlapping any other events in said column
            Optional<List<Pair<String, CalendarEvent>>> selectedColumn = eventColumns.parallelStream()
                    // filter out columns which contain events that overlap with the event to be added
                    .filter(col -> col.parallelStream().noneMatch(b -> eventsOverlap(pair.getValue(), b.getValue())))
                    .findAny();
            if (selectedColumn.isPresent()) {
                selectedColumn.get().add(pair);
            } else {
                // if all columns contained an event that would overlap,
                // then add a new column
                List<Pair<String, CalendarEvent>> newColumn = new ArrayList<>();
                newColumn.add(pair);
                eventColumns.add(newColumn);
            }
        });
        return eventColumns;
    }

    private boolean eventsOverlap(CalendarEvent a, CalendarEvent b) {
        if (!a.getDate().equals(b.getDate()))
            // different days
            return false;
        LocalTime
                aStart = a.getStartTime(),
                bStart = b.getStartTime(),
                aEnd = a.getEndTime(),
                bEnd = b.getEndTime();
        return (!aStart.isAfter(bEnd) && !bStart.isAfter(aEnd));
    }

    private GridPane constructDayPane() {
        GridPane dayPane = new GridPane();
//        dayPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
//        dayPane.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMaxWidth(100);
//        col0.setPrefWidth(50);
        dayPane.getColumnConstraints().add(col0);
        final int NUM_ROWS = 24 * NUM_HOUR_SUBSECTIONS;
        int hour = 0, minute = 0;
        for (int row = 0; row < NUM_ROWS; row++) {
            String timeStr = String.format("%02d:%02d", hour, minute);
            dayPane.add(new Label(timeStr), 0, row);

            minute += MINUTE_INCREMENT;
            if (minute >= 60) {
                minute = 0;
                hour++;
            }
        }
        return dayPane;
    }

    private void displayEvents(List<List<Pair<String, CalendarEvent>>> eventColumns) {
        for (int colNum = 1; colNum <= eventColumns.size(); colNum++) {
//            ColumnConstraints constraints = new ColumnConstraints();
//            dayPane.getColumnConstraints().add(constraints);
            for (Pair<String, CalendarEvent> pair : eventColumns.get(colNum - 1)) {
                String calName = pair.getKey();
                CalendarEvent event = pair.getValue();
                int rowNum = getRowNumber(event.getStartTime());
                int height = getRowNumber(event.getEndTime()) + 1 - rowNum;
                Button butt = new Button(event.getTitle());
                butt.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                butt.setAlignment(Pos.TOP_CENTER);
                butt.setOnAction(e ->
                        EventDialog.editEvent(event, calName, controller.getCalendarNames())
                                .showAndWait()
                                .ifPresent(p -> {
                                    try {
                                        // move between calendars if necessary
                                        if (!calName.equals(p.getKey())) {
                                            controller.removeEvent(calName, event);
                                            controller.addEvent(p.getKey(), event);
                                        } else {
                                            butt.setText(event.getTitle());
                                        }
                                    } catch (NoSuchCalendarException ex) {
                                        ex.printStackTrace();
                                    }
                                })
                );
                dayPane.add(butt, colNum, rowNum, 1, height);
//                GridPane.setFillHeight(butt, true);
            }
        }
    }

    private int getRowNumber(LocalTime t) {
        return (t.getHour() * NUM_HOUR_SUBSECTIONS)
                + (int) Math.floor(NUM_HOUR_SUBSECTIONS * (t.getMinute() / 60.0));
    }

    private void drawDay() {
        header.setText(date.toString());
        List<List<Pair<String, CalendarEvent>>> eventColumns = getEventColumns();
        dayPane = constructDayPane();
        displayEvents(eventColumns);

        ScrollPane scroll = new ScrollPane(dayPane);
        scroll.setPrefSize(500, 400);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        root.setCenter(scroll);
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        } else {
            this.date = date;
            drawDay();
        }
    }

    @Override
    public void setVisibleCalendars(Set<String> calNames) throws NoSuchCalendarException {
        Set<String> superset = controller.getCalendarNames();
        Set<String> curSet = new HashSet<>();
        for (String name : calNames) {
            curSet.add(name);
            if (!superset.contains(name)) throw new NoSuchCalendarException(name);
        }
        visibleCalendars = curSet;
    }
}
