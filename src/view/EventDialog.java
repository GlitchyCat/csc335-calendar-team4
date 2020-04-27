package view;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.CalendarEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * a modal dialog which can produce new or edit existing CalendarEvent objects
 * instantiation of/changes to CalendarEvents only happens when the dialog is
 * invoked via the blocking {@link Dialog#showAndWait()} call. If the user closes
 * the window by any means other than the OK button, no object is created and all
 * changes are discarded.
 *
 * @author Kitty Elliott
 */
public class EventDialog extends Dialog<CalendarEvent> {

    private static final int
            MAX_YEAR_LEN = 4,
            MAX_NOTE_AREA_WID = 375, MAX_NOTE_AREA_HEI = 100;

    private final CalendarEvent event;
    private final TextField titleEntryField, locationEntryField, yearField;
    private final TextArea notesEntryArea;
    private final ChoiceBox<String>
            daySelector, monthSelector,
            startHourSelector, startMinuteSelector,
            endHourSelector, endMinuteSelector;
    private final Calendar date, start, end;

    /**
     * Constructor.
     * If given a CalendarEvent object, then that event will be loaded, and
     * will be updated with the information the user inputs into this EventDialog
     * if the user clicks OK. This can be determined if the return value of
     * {@link super#showAndWait()} {@link java.util.Optional#isPresent() is present}.
     * <p>
     * If given a Date object, the initial date and start/end times
     * will be derived from that Date, and {@link super#showAndWait()}
     * will {@link java.util.Optional optionally} return a new CalendarEvent.
     * If given null, then the current date and time will be used to set the
     * initial values.
     *
     * @param seed either a {@link CalendarEvent}, {@link Date}, or null
     */
    private EventDialog(Object seed) {
        super();

        date = Calendar.getInstance();
        start = Calendar.getInstance();
        end = Calendar.getInstance();
        if (seed instanceof CalendarEvent) {
            event = (CalendarEvent) seed;
            date.setTime(event.getDate());
            start.setTime(event.getStartTime());
            end.setTime(event.getEndTime());
        } else if (seed instanceof Date) {
            event = null;
            Date d = (Date) seed;
            date.setTime(d);
            start.setTime(d);
            end.setTime(d);
        } else {
            // initial values will be current date+time
            event = null;
        }

        titleEntryField = new TextField();
        locationEntryField = new TextField();
        notesEntryArea = new TextArea();

        yearField = new TextField();
        yearField.setTextFormatter(new TextFormatter<>(EventDialog::yearFormatter));
        yearField.setOnKeyPressed(this::yearKeyHandler);

        daySelector = new ChoiceBox<>();
        monthSelector = new ChoiceBox<>();
        startHourSelector = new ChoiceBox<>();
        startMinuteSelector = new ChoiceBox<>();
        endHourSelector = new ChoiceBox<>();
        endMinuteSelector = new ChoiceBox<>();

        setupTimeElements();
        fillNonTimeElements();
        this.setResultConverter(this::getResult);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        this.getDialogPane().lookupButton(ButtonType.OK)
                // show error popup and don't close the dialog
                // if OK was clicked while year or title is blank
                .addEventFilter(ActionEvent.ACTION, e -> {
                    if (titleEntryField.getText().trim().isEmpty()
                            || yearField.getText().isEmpty()) {
                        e.consume();
                        new Alert(Alert.AlertType.ERROR, "Neither Title nor Year may be blank")
                                .showAndWait();
                    }
                    if (end.before(start)) {
                        e.consume();
                        new Alert(Alert.AlertType.ERROR, "End Time must not be before Start Time")
                                .showAndWait();
                    }
                });
        constructGUI();
    }

    /**
     * Create a new instance of this class to edit the given event.
     *
     * @param event the event to be edited. must not be null.
     * @return the EventDialog object which will edit the event when
     * called via the blocking {@link super#showAndWait()} method
     * @throws IllegalArgumentException if event is null.
     */
    public static EventDialog editEvent(CalendarEvent event) {
        if (event == null)
            throw new IllegalArgumentException("CalendarEvent to be edited must not be null");
        return new EventDialog(event);
    }

    /**
     * create a new instance of this class to create a new CalendarEvent.
     *
     * @return the EventDialog object which {@link java.util.Optional could} return
     * the new event when called via the blocking {@link super#showAndWait()} method.
     */
    public static EventDialog newEvent() {
        return new EventDialog(null);
    }

    /**
     * create a new instance of this class to create a new CalendarEvent.
     *
     * @param dateTime the date and time from which the initial date and time values
     *                 of the EventDialog will be derived.
     * @return the EventDialog object which {@link java.util.Optional could} return
     * the new event when called via the blocking {@link super#showAndWait()} method.
     */
    public static EventDialog newEventAt(Date dateTime) {
        return new EventDialog(dateTime);
    }

    /**
     * intercepts changes to the yearField TextField.
     * If the change contains any non-digit characters, those are filtered out.
     *
     * @param chg represents the change
     * @return the possibly altered
     */
    private static TextFormatter.Change yearFormatter(TextFormatter.Change chg) {
        if ((chg.isAdded() || chg.isReplaced())) {
            String chgText = chg.getText();
            StringBuilder onlyDigits = new StringBuilder();
            // filter out all non-digit chars,
            chgText.chars().filter(Character::isDigit)
                    // pushing the remaining chars into the string builder
                    .forEach(i -> onlyDigits.append((char) i));
            if (onlyDigits.length() != chgText.length()) {
                // only update the text if anything was filtered out
                chg.setText(onlyDigits.toString());
            }
        }
        String newText = chg.getControlNewText();
        if (newText.isEmpty()) {
            chg.setText("0");
        } else if (newText.length() > MAX_YEAR_LEN) {
            // reject changes that would put the text length over the maximum
            return null;
        }
        return chg;
    }

    /**
     * key event handler for yearField.
     * Increments the year when up arrow is pressed.
     * Decrements to a lower bound of zero if down arrow is pressed.
     *
     * @param e the key event
     */
    private void yearKeyHandler(KeyEvent e) {
        int increment;
        switch (e.getCode()) {
            case KP_UP:
            case UP:
                increment = 1;
                break;
            case KP_DOWN:
            case DOWN:
                increment = -1;
                break;
            default:
                return;
        }
        int newYear = Integer.parseInt(yearField.getText()) + increment;
        if (newYear >= 0) {
            yearField.setText(String.valueOf(newYear));
        }
    }

    /**
     * put together the scene graph for this object's DialogPane
     */
    private void constructGUI() {
        final BorderPane titleBP, dateBP, startBP, endBP, locationBP;
        final HBox timeHB;

        titleBP = new BorderPane();
        titleBP.setLeft(new Label("Title: "));
        titleBP.setCenter(titleEntryField);

        dateBP = new BorderPane();
        dateBP.setLeft(new Label("Date: "));
        HBox dateHB = new HBox();
        dateHB.setAlignment(Pos.CENTER);
        dateHB.getChildren().addAll(
                monthSelector, daySelector, yearField
        );
        dateBP.setCenter(dateHB);
        yearField.setPromptText("Year");

        // start and end times
        final String timeSeparator = ":";
        startBP = new BorderPane();
        startBP.setLeft(new Label("Start Time: "));
        startBP.setLeft(new Label("Start Time: "));
        startBP.setCenter(new HBox(startHourSelector, new Label(timeSeparator), startMinuteSelector));
        ((HBox)startBP.getCenter()).setAlignment(Pos.CENTER);
        endBP = new BorderPane();
        endBP.setLeft(new Label("End Time: "));
        endBP.setCenter(new HBox(endHourSelector, new Label(timeSeparator), endMinuteSelector));
        ((HBox)endBP.getCenter()).setAlignment(Pos.CENTER);
        timeHB = new HBox(startBP, endBP);

        locationBP = new BorderPane();
        locationBP.setLeft(new Label("Location: "));
        locationBP.setCenter(locationEntryField);

        notesEntryArea.setPromptText("Notes");
        notesEntryArea.setMaxSize(MAX_NOTE_AREA_WID, MAX_NOTE_AREA_HEI);

        VBox mainColumn = new VBox(titleBP, dateBP, timeHB, locationBP, notesEntryArea);
        mainColumn.setAlignment(Pos.TOP_CENTER);
        this.setTitle("Event Editor");
        this.getDialogPane().setContent(mainColumn);
    }

    /**
     * set starting values for the contents of non-time-related graphical elements.
     * If the CalendarEvent is not null, its title, location, and notes will be loaded.
     */
    private void fillNonTimeElements() {
        if (event == null) {
            return;
        }
        titleEntryField.setText(event.getTitle());

        String location = event.getLocation();
        if (location != null) {
            locationEntryField.setText(location);
        }
        String notes = event.getNotes();
        if (notes != null) {
            notesEntryArea.setText(notes);
        }
    }

    /**
     * initialize values for time-related graphical elements
     */
    private void setupTimeElements() {
        final int numDays = date.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < 60; i++) {
            String formatted = String.format("%02d", i);
            String unformatted = String.valueOf(i);
            if (i < 24) {  // hour
                startHourSelector.getItems().add(formatted);
                endHourSelector.getItems().add(formatted);
            }
            if (1 <= i) {
                if (i <= 12) // month
                    monthSelector.getItems().add(unformatted);
                if (i <= numDays) // day
                    daySelector.getItems().add(unformatted);
            }
            // minute
            startMinuteSelector.getItems().add(formatted);
            endMinuteSelector.getItems().add(formatted);
        }

        yearField.setText(String.valueOf(date.get(Calendar.YEAR)));
        monthSelector.getSelectionModel().select(date.get(Calendar.MONTH));
        daySelector.getSelectionModel().select(date.get(Calendar.DAY_OF_MONTH) - 1);
        yearField.textProperty().addListener((a, b, newVal) -> {
            updateNumDaysInMonth();
            date.set(Calendar.YEAR, Integer.parseInt(newVal));
        });
        monthSelector.getSelectionModel().selectedIndexProperty()
                .addListener((a, b, newVal) -> {
                    updateNumDaysInMonth();
                    date.set(Calendar.MONTH, newVal.intValue());
                });
        daySelector.getSelectionModel().selectedIndexProperty().addListener(
                (a, b, newVal) -> date.set(Calendar.DAY_OF_MONTH, newVal.intValue() + 1)
        );

        startHourSelector.getSelectionModel().select(start.get(Calendar.HOUR_OF_DAY));
        startMinuteSelector.getSelectionModel().select(start.get(Calendar.MINUTE));
        startHourSelector.getSelectionModel().selectedIndexProperty()
                .addListener((a, b, newVal) -> start.set(Calendar.HOUR_OF_DAY, newVal.intValue()));
        startMinuteSelector.getSelectionModel().selectedIndexProperty()
                .addListener((a, b, newVal) -> start.set(Calendar.MINUTE, newVal.intValue()));

        endHourSelector.getSelectionModel().select(end.get(Calendar.HOUR_OF_DAY));
        endMinuteSelector.getSelectionModel().select(end.get(Calendar.MINUTE));
        endHourSelector.getSelectionModel().selectedIndexProperty()
                .addListener((a, b, newVal) -> end.set(Calendar.HOUR_OF_DAY, newVal.intValue()));
        endMinuteSelector.getSelectionModel().selectedIndexProperty()
                .addListener((a, b, newVal) -> end.set(Calendar.MINUTE, newVal.intValue()));
    }

    /**
     * called when the month or year fields are updated. updates the number of days
     * available for selection in the monthSelector ChoiceBox.
     */
    private void updateNumDaysInMonth() {
        // change range of available day choices based on selected month
        Calendar tempCal = Calendar.getInstance();
        tempCal.set(Calendar.MONTH, monthSelector.getSelectionModel().getSelectedIndex());
        int year = yearField.getText().isEmpty() ?
                // if yearField is empty, fall back onto the date Calendar object
                date.get(Calendar.YEAR) : Integer.parseInt(yearField.getText());
        tempCal.set(Calendar.YEAR, year);

        final int numDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        final int selected = daySelector.getSelectionModel().getSelectedIndex() + 1; // account for zero-index
        List<String> selectorList = daySelector.getItems();
        final int diff = selectorList.size() - numDays;
        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                // remove the extra days
                selectorList.remove(selectorList.size() - 1);
            }
        } else if (diff < 0) {
            for (int i = selectorList.size() + 1; i <= numDays; i++) {
                // append the missing days
                selectorList.add(String.valueOf(i));
            }
        }
        if (selected > numDays) {
            daySelector.getSelectionModel().selectFirst();
        }
    }

    /**
     * maps empty or whitespace-only strings to null
     *
     * @param s any string
     * @return the given string unchanged, or null if that
     * string is empty or contains only whitespace
     */
    private String nullIfBlank(String s) {
        return s.trim().isEmpty() ? null : s;
    }

    /**
     * the "result converter" for this Dialog object.
     * Returns an event if changes were committed. Used to produce
     * the value returned by this.showAndWait()
     *
     * @param bt the type of button that this is in response to.
     * @return a CalendarEvent object if changes were committed. Otherwise, null.
     */
    private CalendarEvent getResult(ButtonType bt) {
        if (bt == ButtonType.OK) {
            if (event == null) {
                return new CalendarEvent(
                        titleEntryField.getText(),
                        date.getTime(),
                        start.getTime(),
                        end.getTime(),
                        nullIfBlank(locationEntryField.getText()),
                        nullIfBlank(notesEntryArea.getText())
                );
            } else {
                event.setTitle(titleEntryField.getText());
                event.setDate(date.getTime());
                event.setStartTime(start.getTime());
                event.setEndTime(end.getTime());
                event.setLocation(nullIfBlank(locationEntryField.getText()));
                event.setNotes(nullIfBlank(notesEntryArea.getText()));
                return event;
            }
        }
        return null;
    }
}
