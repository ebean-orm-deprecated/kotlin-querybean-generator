package io.ebean.querybean.generator;

import java.io.IOException;
import java.io.Writer;

public interface LangAdapter {

  String NEWLINE = "\n";

  void beginClass(Writer writer, String shortName) throws IOException;

  void beginAssocClass(Writer writer, String shortName, String origShortName) throws IOException;

  void alias(Writer writer, String shortName) throws IOException;

  void rootBeanConstructor(Writer writer, String shortName) throws IOException;

  void assocBeanConstructor(Writer writer, String shortName) throws IOException;

  void fetch(Writer writer, String origShortName) throws IOException;

  void fieldDefn(Writer writer, String propertyName, String typeDefn) throws IOException;

}
