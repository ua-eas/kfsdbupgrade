package ua.utility.kfsdbupgrade.json;

import static com.google.common.base.Ascii.SPACE;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.Instantiatable;
import com.google.common.base.Ascii;

/**
 * Print each field contained in the root level object on a single line. If the root level object is an array, print
 * each element in the array on it's own line.
 */
public final class CompactPrettyPrinter implements PrettyPrinter, Instantiatable<CompactPrettyPrinter> {

  private static final String SP = ((char) SPACE) + "";
  private static final String LF = ((char) Ascii.LF) + "";
  private static final String COMMA = ",";

  // stateless
  private final int indentAmount = 2;
  private final String valueSeparator = ":";
  private final String endArray = "]";
  private final String startArray = "[";
  private final String endObject = "}";
  private final String startObject = "{";

  // stateful
  private int indentation = 0;
  private int arrayLevel = 0;
  private int objectLevel = 0;
  private Boolean rootIsArray = null;

  @Override
  public CompactPrettyPrinter createInstance() {
    return new CompactPrettyPrinter();
  }

  @Override
  public void writeStartObject(JsonGenerator jg) throws IOException, JsonGenerationException {
    // detect if this is the root level object and set rootIsArray to false if it is
    this.rootIsArray = (rootIsArray == null) ? false : rootIsArray;
    // bump the object level
    this.objectLevel++;
    // bump the indentation level
    this.indentation += indentAmount;
    // print the opening bracket
    jg.writeRaw(startObject);
  }

  @Override
  public void writeStartArray(JsonGenerator jg) throws IOException, JsonGenerationException {
    // detect if this is the root level object and set rootIsArray to true if it is
    this.rootIsArray = (rootIsArray == null) ? true : rootIsArray;
    if (isRootLevelArray()) {
      // print LF and indentation if the root object is an array and we are on the root array
      jg.writeRaw(startArray + LF + repeat(SP, indentAmount));
    } else {
      // otherwise just print the start of the array and a space
      jg.writeRaw(startArray + SP);
    }
    // bump the array level
    this.arrayLevel++;
  }

  @Override
  public void beforeObjectEntries(JsonGenerator jg) throws IOException, JsonGenerationException {
    if (isRootLevelObject()) {
      // print a linefeed and indentation if we are on the root object
      jg.writeRaw(LF + repeat(SP, indentation));
    } else {
      // otherwise just print a space
      jg.writeRaw(SP);
    }
  }

  @Override
  public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
    jg.writeRaw(SP + valueSeparator + SP);
  }

  @Override
  public void beforeArrayValues(JsonGenerator jg) throws IOException, JsonGenerationException {}

  @Override
  public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
    if (isRootLevel()) {
      // print linefeed + indentation if we are on the root level
      jg.writeRaw(COMMA + LF + repeat(SP, indentation));
    } else {
      // otherwise just print a comma and a space
      jg.writeRaw(COMMA + SP);
    }
  }

  @Override
  public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException, JsonGenerationException {
    this.indentation -= indentAmount;
    if (isRootLevel()) {
      // print linefeed + indentation if we are on the root level
      jg.writeRaw(LF + repeat(SP, indentation) + endObject);
    } else {
      // otherwise just print a space and a closing curly brace
      jg.writeRaw(SP + endObject);
    }
    this.objectLevel--;
  }

  @Override
  public void writeArrayValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
    if (rootIsArray && arrayLevel == 1) {
      // print linefeed + indentation if we have finished an element in the root level array
      jg.writeRaw(COMMA + LF + repeat(SP, indentAmount));
    } else {
      // otherwise just print a comma and a space
      jg.writeRaw(COMMA + SP);
    }
  }

  @Override
  public void writeRootValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {}

  @Override
  public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException, JsonGenerationException {
    this.arrayLevel--;
    if (isRootLevelArray()) {
      // print a linefeed if we are finishing the root level array
      jg.writeRaw(LF + endArray);
    } else {
      // otherwise just print a space plus the end of the array
      jg.writeRaw(SP + endArray);
    }
  }

  /**
   * Returns true if we are on the root level array or the root level object
   */
  private boolean isRootLevel() {
    return isRootLevelArray() || isRootLevelObject();
  }

  private boolean isRootLevelArray() {
    return (rootIsArray && arrayLevel == 0);
  }

  private boolean isRootLevelObject() {
    return (!rootIsArray && objectLevel == 1);
  }

}