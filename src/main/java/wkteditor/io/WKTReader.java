package wkteditor.io;

import wkteditor.WKTElement;
import wkteditor.WKTLineString;
import wkteditor.WKTPoint;
import wkteditor.WKTPolygon;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class WKTReader implements Closeable {
    private BufferedReader reader;
    private boolean eof;

    public WKTReader(Reader reader) {
        this.reader = new BufferedReader(reader);
        eof = false;
    }

    public WKTReader(File file) throws FileNotFoundException {
        this(new FileReader(file));
    }

    /**
     * Reads wkt data from the input.
     *
     * @return All wkt data that have been read.
     */
    public List<WKTElement> readElements() throws IOException {
        List<WKTElement> elements = new ArrayList<>();

        WKTElement element;

        while (!eof) {
            element = readElement();
            if (element != null) {
                elements.add(element);
            }
        }

        return elements;
    }

    private WKTElement readElement() throws IOException {
        String type = readNextType();
        if (type == null) {
            return null;
        }

        switch (type) {
            case "POINT":
                return parsePoint(readNestedContent());
            case "LINESTRING":
                return parseLineString(readNestedContent());
            case "POLYGON":
                return parsePolygon(readNestedContent());
            default:
                return null;
        }
    }

    /**
     * Parses a wkt point form the given data.
     *
     * @param content The wkt content of the point type.
     * @return The parsed point.
     * @throws IOException If the content is not a polygon.
     */
    private WKTPoint parsePoint(String content) throws IOException {
        Scanner scanner = new Scanner(content);
        scanner.useDelimiter("\\s+");
        int x, y;

        try {
            x = scanner.nextInt();
            y = scanner.nextInt();
        } catch (NoSuchElementException | IllegalStateException exception) {
            throw new IOException("Bad coordinate values: " + content, exception);
        }


        return new WKTPoint(x, y);
    }

    /**
     * Parses a wkt line string from the given data.
     *
     * @param content The wkt content of the line string type.
     * @return The parsed line.
     * @throws IOException If the content is not a line string.
     */
    private WKTLineString parseLineString(String content) throws IOException {
        Scanner scanner = new Scanner(content);
        scanner.useDelimiter(",");

        WKTLineString line = new WKTLineString();
        while (scanner.hasNext()) {
            line.add(parsePoint(scanner.next()));
        }

        return line;
    }

    /**
     * Parses a wkt polygon from the given data.
     *
     * @param content The wkt content of the polygon type.
     * @return The parsed polygon.
     * @throws IOException If the content is not a polygon.
     */
    private WKTPolygon parsePolygon(String content) throws IOException {
        Scanner polyScanner = new Scanner(content);
        polyScanner.useDelimiter(",(?![^(]*\\))");

        WKTPolygon polygon = new WKTPolygon();
        while (polyScanner.hasNext()) {
            String subPoly = polyScanner.next();
            // Remove parenthesis
            subPoly = subPoly.substring(subPoly.indexOf('(') + 1).substring(0, subPoly.lastIndexOf(')') - 1);
            Scanner subPolyScanner = new Scanner(subPoly);
            subPolyScanner.useDelimiter("\\s*,\\s*");

            while (subPolyScanner.hasNext()) {
                polygon.add(parsePoint(subPolyScanner.next()));
            }

            if (polyScanner.hasNext()) {
                polygon.endSubElement();
            }
        }

        return polygon;
    }

    private String readNestedContent() throws IOException {
        StringBuilder content = new StringBuilder();
        char c = '\0';

        skipUntil('(');
        int openParenthesis = 1;
        while (c != (char) -1 && openParenthesis > 0) {
            c = (char) reader.read();

            if (c == '(') {
                openParenthesis++;
            }
            if (c == ')') {
                openParenthesis--;
            }

            if (c != (char) -1) {
                content.append(c);
            }
        }

        if (c == (char) -1) {
            eof = true;
        }

        // Remove last closing parenthesis
        content.deleteCharAt(content.length() - 1);
        return content.toString();
    }

    /**
     * Reads the next wkt type from the reader.
     *
     * @return The next type, or <code>null</code> if the end of the file was reached.
     * @throws IOException If there is an error while reading the next type.
     */
    private String readNextType() throws IOException {
        String type = null;

        while (!eof && type == null) {
            type = readWord();

            if (type.isEmpty()) {
                // Skip empty lines
                type = null;
            }
        }

        return type;
    }

    /**
     * Reads a single word from the reade. A word is defined by an arbitrary sequence of characters separated by
     * whitespaces.
     *
     * @return The read word.
     * @throws IOException If there is an error while reading the word.
     */
    private String readWord() throws IOException {
        StringBuilder builder = new StringBuilder();
        char c = skipWhitespace();

        while (!Character.isWhitespace(c) && c != (char) -1) {
            builder.append(c);
            c = (char) reader.read();
        }

        if (c == (char) -1) {
            eof = true;
        }
        return builder.toString();
    }

    /**
     * Skips all whitespace characters.
     *
     * @return First non-whitespace character.
     * @throws IOException If there is an error while skipping the whitespace.
     */
    private char skipWhitespace() throws IOException {
        char c;

        do {
            c = (char) reader.read();
        } while (Character.isWhitespace(c) && c != (char) -1);

        return c;
    }

    /**
     * Reads characters and skips to the first occurrence of the specified character. The next character read after
     * calling this function will be the first character after the specified character.
     *
     * @param until The character to skip to.
     * @throws IOException If there is an error while skipping until the character.
     */
    private void skipUntil(char until) throws IOException {
        char c;
        do {
            c = (char) reader.read();
        } while (c != (char) -1 && c != until);

        if (c == (char) -1) {
            eof = true;
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
