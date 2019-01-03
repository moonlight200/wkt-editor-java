package wkteditor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wkteditor.io.WKTReader;
import wkteditor.ui.DisplayOptions;
import wkteditor.ui.WKTFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WKTEditor {
    public static void main(String[] args) {
        WKTEditor editor = new WKTEditor();
        WKTFrame frame = new WKTFrame(editor);
        editor.setElementChangeListener(frame);
    }

    public static final String DEFAULT_FILE_NAME = "Unnamed Geometry.wkt";

    private DisplayOptions displayOpt;
    private CursorMode cursorMode;
    private ElementChangeListener listener;

    @NotNull
    private WeakReference<WKTElement> curElement;
    private List<WKTElement> elements;
    @Nullable
    private File openFile;
    private boolean unsavedChanges;

    public WKTEditor() {
        displayOpt = new DisplayOptions();
        cursorMode = CursorMode.SELECT;
        elements = new ArrayList<>();
        openFile = null;
        unsavedChanges = false;
        curElement = new WeakReference<>(null);
    }

    /**
     * Unloads the currently open file.<br>
     * <b>Note</b>: This will delete all wkt data from ram. Unsaved changes will be gone!
     */
    private void unload() {
        elements = new ArrayList<>();
        openFile = null;
        unsavedChanges = false;
    }

    /**
     * Sets a listener that is notified whenever a wkt element changes.
     *
     * @param listener The listener to set.
     */
    public void setElementChangeListener(ElementChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Gets all elements in the current wkt file.
     *
     * @return A collection of elements in the current wkt file.
     */
    public Collection<WKTElement> getElements() {
        return elements;
    }

    /**
     * Gets the current display options. These options specify how the wkt
     * elements are displayed in the editor.
     *
     * @return The display options.
     */
    public DisplayOptions getDisplayOptions() {
        return displayOpt;
    }

    /**
     * Gets the current cursor mode. This mode specifies the actions take when
     * the user clicks within the editor.
     *
     * @return The cursor mode.
     */
    public CursorMode getCursorMode() {
        return cursorMode;
    }

    /**
     * Updates the cursor mode. This mode specifies the actions takes when the
     * user clicks within the editor.
     *
     * @param cursorMode The new cursor mode.
     */
    public void setCursorMode(CursorMode cursorMode) {
        this.cursorMode = cursorMode;
    }

    /**
     * Gets the wkt element that is currently being edited.
     *
     * @return The wkt element selected for editing.
     */
    @Nullable
    public WKTElement getCurrentElement() {
        return curElement.get();
    }

    /**
     * Ends the current to allow creation / selection of a new element. The
     * element being ended will be added back to the list of all elements.
     *
     * @see #endCurrentSubElement()
     */
    public void endCurrentElement() {
        WKTElement elem = curElement.get();
        if (elem == null) {
            return;
        }
        curElement = new WeakReference<>(null);
        onElementChanged();
    }

    /**
     * If the current element supports sub elements, this will end the current
     * sub element and start a new one.
     *
     * @see #endCurrentElement()
     */
    public void endCurrentSubElement() {
        if (!cursorMode.hasSubElements()) {
            return;
        }

        WKTElement elem = curElement.get();
        if (elem == null) {
            return;
        }

        elem.endSubElement();
        onElementChanged();
    }

    /**
     * Opens the specified file.
     *
     * @param file The file to open.
     */
    public void open(File file) {
        unload();
        notifyElementChanged();

        try {
            WKTReader wktReader = new WKTReader(file);
            elements = wktReader.readElements();
            wktReader.close();

            openFile = file;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        notifyElementChanged();
    }

    /**
     * Save the edited wkt elements to the specified file.
     *
     * @param file The file to save the wkt elements to.
     */
    public void save(File file) {
        endCurrentElement();

        openFile = file;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (WKTElement element : elements) {
                String wkt = element.toWKT();
                wkt += "\n";
                fos.write(wkt.getBytes(Charset.forName("UTF-8")));
            }
            unsavedChanges = false;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Gets the file that is currently opened.
     *
     * @return The file that is currently opened, or <code>null</code> if no file is currently opened.
     */
    @Nullable
    public File getOpenFile() {
        return openFile;
    }

    /**
     * Checks if there are changes to the currently open file, that have not yet been saved.
     *
     * @return <code>true</code> if there are unsaved changes.
     */
    public boolean areThereUnsavedChanges() {
        return unsavedChanges;
    }

    /**
     * Selects an element based on the given coordinates.
     *
     * @param x The x-coordinate of the selection.
     * @param y The y-coordinate of the selection.
     * @return The element that was selected, or <code>null</code> if no element
     * was selected.
     */
    public WKTElement getSelectedElement(int x, int y) {
        for (WKTElement element : elements) {
            if (element.isOnElement(x, y, 3.0)) {
                return element;
            }
        }
        return null;
    }

    /**
     * Adds the specified point to the currently edited element. If no element
     * is being edited, a new one will be created.
     *
     * @param x The x-coordinate of the point to add.
     * @param y The y-coordinate of the point to add.
     */
    public void addPoint(int x, int y) {
        if (!cursorMode.isElement()) {
            // Select element
            WKTElement selected = getSelectedElement(x, y);
            curElement = new WeakReference<>(selected);
            onElementChanged();

            return;
        }

        WKTElement element = curElement.get();
        if (element != null && element.getClass() != cursorMode.getWktClass()) {
            System.err.println("Cursor mode changed without ending previous element!");
            endCurrentElement();
        }

        element = curElement.get();
        if (element != null && !element.canAdd()) {
            endCurrentElement();
        }

        element = curElement.get();
        if (element == null) {
            try {
                element = cursorMode.getWktClass().getConstructor().newInstance();
                elements.add(element);
                curElement = new WeakReference<>(element);
            } catch (InstantiationException | IllegalAccessException |
                    NoSuchMethodException | InvocationTargetException exception) {
                exception.printStackTrace();
                return;
            }
        }

        // Add point to current element
        element.add(x, y);
        onElementChanged();
    }

    /**
     * Called when an element has changed. If a listener is set, it will be
     * notified about the change.
     */
    private void onElementChanged() {
        unsavedChanges = true;
        notifyElementChanged();
    }

    /**
     * Notifies the listener that an element has changed.
     */
    private void notifyElementChanged() {
        if (listener != null) {
            listener.onElementChanged();
        }
    }

    /**
     * Called when the program exits.
     */
    public void shutdown() {
        System.exit(0);
    }

    /**
     * A listener for element changes.
     */
    public interface ElementChangeListener {
        /**
         * Called when an element has changed.
         */
        void onElementChanged();
    }
}
