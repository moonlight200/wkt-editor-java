package wkteditor.ui;

import org.jetbrains.annotations.Nullable;
import wkteditor.CursorMode;
import wkteditor.WKTEditor;
import wkteditor.ui.filefilter.ImageFileFilter;
import wkteditor.ui.filefilter.WktFileFilter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.*;

/**
 * This frame displays the currently edited wkt file and provides UI elements
 * to edit the wkt elements.
 */
public class WKTFrame extends JFrame implements ActionListener, WKTEditor.ElementChangeListener {
    private static final String AC_OPEN = "actionCommand:open";
    private static final String AC_SAVE = "actionCommand:save";
    private static final String AC_SAVE_AS = "actionCommand:saveAs";
    public static final String AC_CURSOR_SELECT = "actionCommand:cursorSelect";
    public static final String AC_CURSOR_POINT = "actionCommand:cursorPoint";
    public static final String AC_CURSOR_LINE = "actionCommand:cursorLine";
    public static final String AC_CURSOR_POLYGON = "actionCommand:cursorPolygon";
    private static final String AC_END_ELEMENT = "actionCommand:endElement";
    private static final String AC_END_SUB_ELEMENT = "actionCommand:endSubElement";
    private static final String AC_SET_BG_IMAGE = "actionCommand:setBgImage";
    private static final String AC_REMOVE_BG_IMAGE = "actionCommand:removeBgImage";
    private static final String AC_ZOOM_IN = "actionCommand:zoom.in";
    private static final String AC_ZOOM_OUT = "actionCommand:zoom.out";
    private static final String AC_ZOOM_RESET = "actionCommand:zoom.reset";
    private static final String AC_VIEW_RESET = "actionCommand:view.reset";

    private ResourceBundle strings;
    private WKTEditor editor;

    private WKTPane wktPane;
    private ButtonGroup cursorMenuGroup;
    private ButtonGroup cursorToolbarGroup;
    private Map<CursorMode, ButtonModel> menuButtonMap;
    private Map<CursorMode, ButtonModel> toolbarButtonMap;
    private List<ButtonModel> endElementModels;
    private List<ButtonModel> endSubElementModels;

    public WKTFrame(WKTEditor editor) {
        this.editor = editor;
        strings = ResourceBundle.getBundle("lang/strings");
        endElementModels = new ArrayList<>(2);
        endSubElementModels = new ArrayList<>(2);

        setLayout(new BorderLayout());

        wktPane = new WKTPane(editor);
        add(wktPane, BorderLayout.CENTER);
        add(buildToolBar(), BorderLayout.NORTH);

        setJMenuBar(buildMenuBar());

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 600);
        setMinimumSize(new Dimension(200, 200));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (editor.areThereUnsavedChanges()) {
                    final int result = showUnsavedChangesDialog("saveBeforeClose");

                    switch (result) {
                        case 0:
                            // Save
                            if (saveWkt(editor.getOpenFile())) {
                                // Save succeeded
                                dispose();
                                editor.shutdown();
                            }
                            break;
                        case 2:
                            // Don't save
                            dispose();
                            editor.shutdown();
                            break;
                        default:
                            // Cancel, or close dialog
                            // Nothing to do
                            break;
                    }
                } else {
                    dispose();
                    editor.shutdown();
                }
            }
        });

        onModeChanged(editor.getCursorMode());
        updateTitle();

        setVisible(true);
    }

    /**
     * Builds the complete menu.
     *
     * @return The created menu.
     */
    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu(strings.getString("menu.file"));
        menuBar.add(menuFile);

        JMenuItem menuFileOpen = new JMenuItem(strings.getString("menu.file.open"));
        menuFileOpen.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        menuFileOpen.setActionCommand(AC_OPEN);
        menuFileOpen.addActionListener(this);
        menuFile.add(menuFileOpen);

        JMenuItem menuFileSave = new JMenuItem(strings.getString("menu.file.save"));
        menuFileSave.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        menuFileSave.setActionCommand(AC_SAVE);
        menuFileSave.addActionListener(this);
        menuFile.add(menuFileSave);

        JMenuItem menuFileSaveAs = new JMenuItem(strings.getString("menu.file.saveAs"));
        menuFileSaveAs.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        menuFileSaveAs.setActionCommand(AC_SAVE_AS);
        menuFileSaveAs.addActionListener(this);
        menuFile.add(menuFileSaveAs);

        JMenu menuEdit = new JMenu(strings.getString("menu.edit"));
        menuBar.add(menuEdit);

        cursorMenuGroup = new ButtonGroup();
        menuButtonMap = new EnumMap<>(CursorMode.class);


        for (CursorMode mode : CursorMode.values()) {
            JRadioButtonMenuItem menuEditCursor = new JRadioButtonMenuItem(
                    strings.getString(mode.getNameRes()));
            menuEditCursor.setAccelerator(mode.getKeyStroke());
            menuEditCursor.setActionCommand(mode.getActionCommand());
            menuEditCursor.addActionListener(this);
            cursorMenuGroup.add(menuEditCursor);
            menuButtonMap.put(mode, menuEditCursor.getModel());
            menuEdit.add(menuEditCursor);
        }

        menuEdit.addSeparator();

        JMenuItem menuEditEndElement = new JMenuItem(strings.getString("menu.edit.endElement"));
        menuEditEndElement.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        menuEditEndElement.setActionCommand(AC_END_ELEMENT);
        menuEditEndElement.addActionListener(this);
        endElementModels.add(menuEditEndElement.getModel());
        menuEdit.add(menuEditEndElement);

        JMenuItem menuEditEndSubElement = new JMenuItem(strings.getString("menu.edit.endElement.sub"));
        menuEditEndSubElement.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK));
        menuEditEndSubElement.setActionCommand(AC_END_SUB_ELEMENT);
        menuEditEndSubElement.addActionListener(this);
        endSubElementModels.add(menuEditEndSubElement.getModel());
        menuEdit.add(menuEditEndSubElement);

        JMenu menuView = new JMenu(strings.getString("menu.view"));
        menuBar.add(menuView);

        JMenuItem menuViewImage = new JMenuItem(strings.getString("menu.view.bgImage"));
        menuViewImage.setActionCommand(AC_SET_BG_IMAGE);
        menuViewImage.addActionListener(this);
        menuView.add(menuViewImage);

        JMenuItem menuViewImageRemove = new JMenuItem(strings.getString("menu.view.bgImage.remove"));
        menuViewImageRemove.setActionCommand(AC_REMOVE_BG_IMAGE);
        menuViewImageRemove.addActionListener(this);
        menuView.add(menuViewImageRemove);

        menuView.addSeparator();

        JMenu menuZoom = new JMenu(strings.getString("menu.view.zoom"));
        menuView.add(menuZoom);

        JMenuItem menuZoomIn = new JMenuItem(strings.getString("menu.view.zoom.in"));
        menuZoomIn.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK));
        menuZoomIn.setActionCommand(AC_ZOOM_IN);
        menuZoomIn.addActionListener(this);
        menuZoom.add(menuZoomIn);

        JMenuItem menuZoomOut = new JMenuItem(strings.getString("menu.view.zoom.out"));
        menuZoomOut.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
        menuZoomOut.setActionCommand(AC_ZOOM_OUT);
        menuZoomOut.addActionListener(this);
        menuZoom.add(menuZoomOut);

        JMenuItem menuZoomReset = new JMenuItem(strings.getString("menu.view.zoom.reset"));
        menuZoomReset.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK));
        menuZoomReset.setActionCommand(AC_ZOOM_RESET);
        menuZoomReset.addActionListener(this);
        menuZoom.add(menuZoomReset);

        JMenuItem menuViewReset = new JMenuItem(strings.getString("menu.view.reset"));
        menuViewReset.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
        menuViewReset.setActionCommand(AC_VIEW_RESET);
        menuViewReset.addActionListener(this);
        menuView.add(menuViewReset);

        return menuBar;
    }

    /**
     * Builds the complete toolbar.
     *
     * @return The created toolbar.
     */
    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);

        JButton buttonSave = buildToolbarButton("save",
                "toolbar.save", AC_SAVE);
        toolBar.add(buttonSave);

        toolBar.addSeparator();

        cursorToolbarGroup = new ButtonGroup();
        toolbarButtonMap = new EnumMap<>(CursorMode.class);

        for (CursorMode mode : CursorMode.values()) {
            JButton buttonCursor = buildToolbarButton(mode.getIconName(),
                    mode.getNameRes(), mode.getActionCommand());
            cursorToolbarGroup.add(buttonCursor);
            toolbarButtonMap.put(mode, buttonCursor.getModel());
            toolBar.add(buttonCursor);
        }

        toolBar.addSeparator();

        JButton buttonEndElement = buildToolbarButton("end_shape",
                "toolbar.endElement", AC_END_ELEMENT);
        endElementModels.add(buttonEndElement.getModel());
        toolBar.add(buttonEndElement);

        JButton buttonEndSubElement = buildToolbarButton("end_sub_shape",
                "toolbar.endElement.sub", AC_END_SUB_ELEMENT);
        endSubElementModels.add(buttonEndSubElement.getModel());
        toolBar.add(buttonEndSubElement);

        return toolBar;
    }

    /**
     * Builds a single button for the toolbar.
     *
     * @param iconName      The name of the button icon in the buttons resources
     *                      folder.
     * @param nameRes       The id of the name resource in the resource bundle.
     * @param actionCommand The action command that is executed when the button
     *                      is clicked.
     * @return A new button with the above specified settings.
     */
    private JButton buildToolbarButton(String iconName, String nameRes,
                                       String actionCommand) {
        String name = strings.getString(nameRes);
        URL iconUrl = WKTFrame.class.getResource("/buttons/" + iconName + ".png");

        JButton button = new JButton();
        button.setToolTipText(name);
        button.setOpaque(false);
        button.setActionCommand(actionCommand);
        button.addActionListener(this);

        if (iconUrl != null) {
            button.setIcon(new ImageIcon(iconUrl, name));
        } else {
            button.setText(name);
        }

        return button;
    }

    /**
     * Updates the window title.
     */
    private void updateTitle() {
        StringBuilder sb = new StringBuilder();

        if (editor.areThereUnsavedChanges()) {
            sb.append("*");
            if (editor.getOpenFile() == null) {
                sb.append(WKTEditor.DEFAULT_FILE_NAME);
            } else {
                sb.append(editor.getOpenFile().getName());
            }
            sb.append(" - ");
        } else if (editor.getOpenFile() != null) {
            sb.append(editor.getOpenFile().getName());
            sb.append(" - ");
        }

        sb.append(strings.getString("name"));

        setTitle(sb.toString());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String ac = event.getActionCommand();
        if (ac == null) {
            return;
        }

        switch (ac) {
            case AC_SET_BG_IMAGE:
                setBgImage();
                break;
            case AC_REMOVE_BG_IMAGE:
                wktPane.setBackgroundImage(null);
                break;
            case AC_OPEN:
                openFile();
                break;
            case AC_SAVE:
                saveWkt(editor.getOpenFile());
                break;
            case AC_SAVE_AS:
                saveWkt(null);
                break;
            case AC_CURSOR_SELECT:
                editor.endCurrentElement();
                editor.setCursorMode(CursorMode.SELECT);
                onModeChanged(CursorMode.SELECT);
                break;
            case AC_CURSOR_POINT:
                editor.endCurrentElement();
                editor.setCursorMode(CursorMode.POINT);
                onModeChanged(CursorMode.POINT);
                break;
            case AC_CURSOR_LINE:
                editor.endCurrentElement();
                editor.setCursorMode(CursorMode.LINE);
                onModeChanged(CursorMode.LINE);
                break;
            case AC_CURSOR_POLYGON:
                editor.endCurrentElement();
                editor.setCursorMode(CursorMode.POLYGON);
                onModeChanged(CursorMode.POLYGON);
                break;
            case AC_END_ELEMENT:
                editor.endCurrentElement();
                break;
            case AC_END_SUB_ELEMENT:
                editor.endCurrentSubElement();
                break;
            case AC_ZOOM_IN:
                wktPane.zoom(1.0);
                break;
            case AC_ZOOM_OUT:
                wktPane.zoom(-1.0);
                break;
            case AC_ZOOM_RESET:
                wktPane.resetZoom();
                break;
            case AC_VIEW_RESET:
                wktPane.resetView();
                break;
        }
    }

    /**
     * Shows a file dialog to let the user select the file to open. Then opens the selected file.
     * If there are unsaved changes, shows a dialog to let the user choose what to do with the changes.
     */
    private void openFile() {
        if (editor.areThereUnsavedChanges()) {
            switch (showUnsavedChangesDialog("saveBeforeOpen")) {
                case 0:
                    // Save
                    if (!saveWkt(editor.getOpenFile())) {
                        // Save was canceled
                        return;
                    }
                    break;
                case 2:
                    // Don't save
                    // Nothing to do
                    break;
                default:
                    // Cancel, or close dialog
                    return;
            }
        }

        final JFileChooser fc = new JFileChooser(getCurrentDirectory());
        fc.setFileFilter(new WktFileFilter(strings));
        final int result = fc.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        editor.open(fc.getSelectedFile());
        updateTitle();
    }

    /**
     * Shows a file dialog to let the user select the destination file. If a
     * file was selected, forwards the save operation to the {@link WKTEditor}.
     *
     * @param file The file to save the wkt data to. Set to <code>null</code> to show file chooser dialog.
     * @return Whether or not the save was performed (It might not have been if the user canceled the file chooser
     * dialog).
     */
    private boolean saveWkt(File file) {
        if (file == null) {
            final JFileChooser fc = new JFileChooser(getCurrentDirectory());
            fc.setFileFilter(new WktFileFilter(strings));
            final int result = fc.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
            }
        }

        if (file != null) {
            editor.save(file);
            updateTitle();
            return true;
        }
        return false;
    }

    /**
     * Shows a file dialog to let the user select a background image. If an
     * image was selected, updates the {@link WKTPane} accordingly.
     */
    private void setBgImage() {
        final JFileChooser fc = new JFileChooser(getCurrentDirectory());
        fc.setFileFilter(new ImageFileFilter(strings));
        final int result = fc.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                wktPane.setBackgroundImage(ImageIO.read(fc.getSelectedFile()));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Called when the cursor mode changed. Updates the related UI elements to
     * represent the new cursor mode.
     *
     * @param mode The new cursor mode.
     */
    private void onModeChanged(CursorMode mode) {
        cursorMenuGroup.setSelected(menuButtonMap.get(mode), true);
        cursorToolbarGroup.setSelected(toolbarButtonMap.get(mode), true);

        for (ButtonModel model : endElementModels) {
            model.setEnabled(mode.isElement());
        }
        for (ButtonModel model : endSubElementModels) {
            model.setEnabled(mode.isElement() && mode.hasSubElements());
        }
    }

    /**
     * Gets the current directly that is used as a starting directory for file dialogs.
     *
     * @return The starting directory for file dialogs.
     */
    @Nullable
    private File getCurrentDirectory() {
        if (editor.getOpenFile() == null) {
            return null;
        }

        return editor.getOpenFile().getParentFile();
    }

    @Override
    public void onElementChanged() {
        updateTitle();
        wktPane.repaint();
    }

    /**
     * Show a dialog to ask the user what to do with unsaved changes to the currently opened file.
     *
     * @param dialogName The name of the dialog to display (used for sting resources).
     * @return The selected option.<br>
     * <ul>
     * <li><code>0</code>: save</li>
     * <li><code>1</code>: cancel</li>
     * <li><code>2</code>: don't save</li>
     * <li>{@link JOptionPane#CLOSED_OPTION}: dialog was closed without selecting an answer</li>
     * </ul>
     */
    private int showUnsavedChangesDialog(final String dialogName) {
        final String[] options = {
                strings.getString("dialog.options.save"),
                strings.getString("dialog.options.cancel"),
                strings.getString("dialog.options.dontSave")
        };
        String fileName = WKTEditor.DEFAULT_FILE_NAME;
        if (editor.getOpenFile() != null) {
            fileName = editor.getOpenFile().getName();
        }
        final String message = String.format(strings.getString("dialog." + dialogName + ".message"), fileName);

        return JOptionPane.showOptionDialog(this, message, strings.getString("dialog." + dialogName + ".title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
    }
}
