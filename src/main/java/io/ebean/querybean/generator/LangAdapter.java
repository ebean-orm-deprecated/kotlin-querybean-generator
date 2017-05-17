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

//  void finderConstructors(Writer writer, String shortName) throws IOException;
//
//  void finderWhere(Writer writer, String shortName, String modifier) throws IOException;
//
//  void finderText(Writer writer, String shortName, String modifier) throws IOException;
//
//  void finderClass(Writer writer, String shortName, String idTypeShortName) throws IOException;
//
//  String finderDefn(String shortName);

}
