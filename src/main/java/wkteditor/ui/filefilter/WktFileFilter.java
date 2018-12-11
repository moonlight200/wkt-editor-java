package wkteditor.ui.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ResourceBundle;

public class WktFileFilter extends FileFilter {
    private final ResourceBundle strings;

    public WktFileFilter(ResourceBundle strings) {
        this.strings = strings;
    }

    @Override
    public boolean accept(File file) {
        if (!file.isFile()) {
            return true;
        }

        String[] parts = file.getName().split("\\.");
        if (parts.length < 2) {
            return false;
        }
        String ext = parts[parts.length - 1].toLowerCase();
        return "wkt".equals(ext);
    }

    @Override
    public String getDescription() {
        return strings.getString("fileFilter.wkt");
    }
}
