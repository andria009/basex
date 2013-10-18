package org.basex.io.parse.csv;

import static org.basex.util.Token.*;

import java.io.*;

import org.basex.build.*;
import org.basex.build.CsvOptions.CsvFormat;
import org.basex.io.*;
import org.basex.io.in.*;
import org.basex.query.value.node.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * <p>This class converts CSV input to XML.</p>
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class CsvConverter {
  /** CSV token. */
  private static final byte[] CSV = token("csv");
  /** CSV token. */
  private static final byte[] RECORD = token("record");
  /** CSV token. */
  private static final byte[] ENTRY = token("entry");
  /** CSV token. */
  private static final byte[] NAME = token("name");

  /** CSV options. */
  protected final CsvParserOptions copts;
  /** Column separator (see {@link CsvOptions#SEPARATOR}). */
  private final int separator;

  /** Root node. */
  private final FElem root = new FElem(CSV);
  /** Record. */
  private FElem record;

  /** Headers. */
  private final TokenList headers = new TokenList();
  /** Attributes format. */
  private final boolean atts;
  /** Lax QName conversion. */
  private final boolean lax;

  /** Header flag. */
  private boolean header;
  /** Current column. */
  private int col;

  /**
   * Constructor.
   * @param opts CSV options
   */
  private CsvConverter(final CsvParserOptions opts) {
    copts = opts;
    separator = opts.separator();
    header = opts.get(CsvOptions.HEADER);
    lax = opts.get(CsvOptions.LAX);
    atts = opts.get(CsvOptions.FORMAT) == CsvFormat.ATTRIBUTES;
  }

  /**
   * Converts the specified input to an XQuery item.
   * @param input input
   * @param copts options
   * @return item
   * @throws IOException I/O exception
   */
  public static FDoc convert(final IO input, final CsvParserOptions copts) throws IOException {
    final String encoding = copts.get(CsvParserOptions.ENCODING);
    return convert(new NewlineInput(input).encoding(encoding), copts);
  }

  /**
   * Converts the specified input to an XQuery item.
   * @param input input
   * @param copts options
   * @return item
   * @throws IOException I/O exception
   */
  public static FDoc convert(final byte[] input, final CsvParserOptions copts) throws IOException {
    return convert(new NewlineInput(new IOContent(input)), copts);
  }

  /**
   * Converts the specified input to an XQuery item.
   * @param input input stream
   * @param copts options
   * @return item
   * @throws IOException I/O exception
   */
  private static FDoc convert(final NewlineInput input, final CsvParserOptions copts)
      throws IOException {
    return new CsvConverter(copts).convert(input);
  }

  /**
   * Converts the specified input stream to an XML element.
   * @param input CSV input
   * @return node
   * @throws IOException I/O exception
   */
  private FDoc convert(final NewlineInput input) throws IOException {
    final TokenBuilder data = new TokenBuilder();
    boolean quoted = false, open = true;
    int ch = -1;

    try {
      while(true) {
        if(ch == -1) ch = input.read();
        if(ch == -1) break;
        if(quoted) {
          if(ch == '"') {
            ch = input.read();
            if(ch != '"') {
              quoted = false;
              continue;
            }
          }
          data.add(ch);
        } else if(ch == separator) {
          if(open) {
            newRecord();
            open = false;
          }
          newEntry(data);
        } else if(ch == '\n') {
          finish(data, open);
          open = true;
        } else if(ch == '"') {
          quoted = true;
        } else {
          data.add(XMLToken.valid(ch) ? ch : '?');
        }
        ch = -1;
      }
    } finally {
      input.close();
    }

    finish(data, open);
    return new FDoc().add(root);
  }

  /**
   * Creates a new record.
   */
  private void newRecord() {
    if(header) return;
    record = new FElem(RECORD);
    root.add(record);
  }

  /**
   * Finishes the current record.
   * @param entry current entry
   * @param open open flag
   */
  private void finish(final TokenBuilder entry, final boolean open) {
    if(open && !entry.isEmpty()) newRecord();
    newEntry(entry);
    header = false;
    col = 0;
  }

  /**
   * Adds an entry.
   * @param entry current entry
   */
  private void newEntry(final TokenBuilder entry) {
    if(header) {
      // add header
      headers.add(atts ? entry.finish() : XMLToken.encode(entry.finish(), lax));
    } else {
      final byte[] name = headers.get(col);
      final FElem e;
      if(atts) {
        e = new FElem(ENTRY);
        if(name != null) e.add(NAME, name);
      } else {
        e = new FElem(name == null ? ENTRY : name);
      }
      if(record != null) record.add(e.add(entry.finish()));
      ++col;
    }
    entry.reset();
  }
}