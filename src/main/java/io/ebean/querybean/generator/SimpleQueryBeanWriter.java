package io.ebean.querybean.generator;


import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.persistence.Entity;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static io.ebean.querybean.generator.Constants.AT_GENERATED;
import static io.ebean.querybean.generator.Constants.AT_TYPEQUERYBEAN;

/**
 * A simple implementation that generates and writes query beans.
 */
class SimpleQueryBeanWriter {

  private static final String[] javaTypes = {
    "java.lang.String",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Double",
    "java.lang.Float",
    "java.lang.Short",
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Char"
  };

  private static final String[] kotlinTypes = {
    "kotlin.String",
    "kotlin.Int",
    "kotlin.Long",
    "kotlin.Double",
    "kotlin.Float",
    "kotlin.Short",
    "kotlin.Boolean",
    "kotlin.Byte",
    "kotlin.Char"
  };

  static final String NEWLINE = "\n";

  private final Set<String> importTypes = new TreeSet<>();

  private final List<PropertyMeta> properties = new ArrayList<>();

  private final TypeElement element;

  private final ProcessingContext processingContext;

  private final String beanFullName;
  private final LangAdapter langAdapter;
  private boolean writingAssocBean;
  private final String generatedSourcesDir;

  private String destPackage;
  private String origDestPackage;

  private String shortName;
  private String origShortName;

  private Writer writer;

  SimpleQueryBeanWriter(TypeElement element, ProcessingContext processingContext) {
    this.langAdapter = new KotlinLangAdapter();
    this.generatedSourcesDir = processingContext.generatedSourcesDir();
    this.element = element;
    this.processingContext = processingContext;

    this.beanFullName = element.getQualifiedName().toString();
    this.destPackage = derivePackage(beanFullName) + ".query";
    this.shortName = deriveShortName(beanFullName);

    processingContext.addPackage(destPackage);
  }

  private LangAdapter lang() {
    return langAdapter;
  }

  private void gatherPropertyDetails() {

    importTypes.add(beanFullName);
    importTypes.add("javax.annotation.Generated");
    importTypes.add("io.ebean.typequery.TQRootBean");
    importTypes.add("io.ebean.typequery.TypeQueryBean");
    importTypes.add("io.ebean.EbeanServer");

    addClassProperties();
  }

  /**
   * Recursively add properties from the inheritance hierarchy.
   * <p>
   * Includes properties from mapped super classes and usual inheritance.
   * </p>
   */
  private void addClassProperties() {

    List<VariableElement> fields = processingContext.allFields(element);

    for (VariableElement field : fields) {
      PropertyType type = processingContext.getPropertyType(field);
      if (type != null) {
        type.addImports(importTypes);
        properties.add(new PropertyMeta(field.getSimpleName().toString(), type));
      }
    }
  }

  /**
   * Write the type query bean (root bean).
   */
  void writeRootBean() throws IOException {

    gatherPropertyDetails();

    if (isEntity()) {
      writer = createFileWriter();

      translateKotlinImportTypes();

      writePackage();
      writeImports();
      writeClass();
      writeAlias();
      writeFields();
      writeConstructors();
      //writeStaticAliasClass();
      writeClassEnd();

      writer.flush();
      writer.close();
    }
  }

  /**
   * Translate the base types (String, Integer etc) to Kotlin types.
   */
  private void translateKotlinImportTypes() {
    for (int i = 0; i < javaTypes.length; i++) {
      if (importTypes.remove(javaTypes[i])) {
        importTypes.add(kotlinTypes[i]);
      }
    }
  }

  private boolean isEntity() {
    return element.getAnnotation(Entity.class) != null;
  }

  /**
   * Write the type query assoc bean.
   */
  void writeAssocBean() throws IOException {

    writingAssocBean = true;
    origDestPackage = destPackage;
    destPackage = destPackage + ".assoc";
    origShortName = shortName;
    shortName = "Assoc" + shortName;

    prepareAssocBeanImports();

    writer = createFileWriter();

    writePackage();
    writeImports();
    writeClass();
    writeFields();
    writeConstructors();
    writeClassEnd();

    writer.flush();
    writer.close();
  }

  /**
   * Prepare the imports for writing assoc bean.
   */
  private void prepareAssocBeanImports() {

    importTypes.remove("io.ebean.typequery.TQRootBean");
    importTypes.remove("io.ebean.EbeanServer");
    importTypes.add("io.ebean.typequery.TQAssocBean");
    if (isEntity()) {
      importTypes.add("io.ebean.typequery.TQProperty");
      importTypes.add(origDestPackage + ".Q" + origShortName);
    }

    // remove imports for the same package
    Iterator<String> importsIterator = importTypes.iterator();
    String checkImportStart = destPackage + ".QAssoc";
    while (importsIterator.hasNext()) {
      String importType = importsIterator.next();
      if (importType.startsWith(checkImportStart)) {
        importsIterator.remove();
      }
    }
  }

  /**
   * Write constructors.
   */
  private void writeConstructors() throws IOException {

    if (writingAssocBean) {
      writeAssocBeanFetch();
      writeAssocBeanConstructor();
    } else {
      writeRootBeanConstructor();
    }
  }

  /**
   * Write the constructors for 'root' type query bean.
   */
  private void writeRootBeanConstructor() throws IOException {

    lang().rootBeanConstructor(writer, shortName);
  }

  private void writeAssocBeanFetch() throws IOException {

    if (isEntity()) {
      lang().fetch(writer, origShortName);
    }
  }

  /**
   * Write constructor for 'assoc' type query bean.
   */
  private void writeAssocBeanConstructor() throws IOException {

    lang().assocBeanConstructor(writer, shortName);
  }

  /**
   * Write all the fields.
   */
  private void writeFields() throws IOException {

    for (PropertyMeta property : properties) {
      String typeDefn = property.getTypeDefn(shortName, writingAssocBean);
      lang().fieldDefn(writer, property.getName(), typeDefn);
      writer.append(NEWLINE);
    }
    writer.append(NEWLINE);
  }

  /**
   * Write the class definition.
   */
  private void writeClass() throws IOException {

    if (writingAssocBean) {
      writer.append("/**").append(NEWLINE);
      writer.append(" * Association query bean for ").append(shortName).append(".").append(NEWLINE);
      writer.append(" * ").append(NEWLINE);
      writer.append(" * THIS IS A GENERATED OBJECT, DO NOT MODIFY THIS CLASS.").append(NEWLINE);
      writer.append(" */").append(NEWLINE);
      //public class QAssocContact<R>
      writer.append(AT_GENERATED).append(NEWLINE);
      writer.append(AT_TYPEQUERYBEAN).append(NEWLINE);
      lang().beginAssocClass(writer, shortName, origShortName);

    } else {
      writer.append("/**").append(NEWLINE);
      writer.append(" * Query bean for ").append(shortName).append(".").append(NEWLINE);
      writer.append(" * ").append(NEWLINE);
      writer.append(" * THIS IS A GENERATED OBJECT, DO NOT MODIFY THIS CLASS.").append(NEWLINE);
      writer.append(" */").append(NEWLINE);
      //  public class QContact extends TQRootBean<Contact,QContact> {
      writer.append(AT_GENERATED).append(NEWLINE);
      writer.append(AT_TYPEQUERYBEAN).append(NEWLINE);
      lang().beginClass(writer, shortName);
    }

    writer.append(NEWLINE);
  }

  private void writeAlias() throws IOException {
    if (!writingAssocBean) {
      lang().alias(writer, shortName);
    }
  }

//  private void writeStaticAliasClass() throws IOException {
//
//    writer.append(NEWLINE);
//    writer.append("  /**").append(NEWLINE);
//    writer.append("   * Provides static properties to use in <em> select() and fetch() </em>").append(NEWLINE);
//    writer.append("   * clauses of a query. Typically referenced via static imports. ").append(NEWLINE);
//    writer.append("   */").append(NEWLINE);
//    writer.append("  public static class Alias {").append(NEWLINE);
//    for (PropertyMeta property : properties) {
//      property.writeFieldAliasDefn(writer, shortName);
//      writer.append(NEWLINE);
//    }
//    writer.append("  }").append(NEWLINE);
//  }

  private void writeClassEnd() throws IOException {
    writer.append("}").append(NEWLINE);
  }

  /**
   * Write all the imports.
   */
  private void writeImports() throws IOException {

    for (String importType : importTypes) {
      writer.append("import ").append(importType).append(";").append(NEWLINE);
    }
    writer.append(NEWLINE);
  }

  private void writePackage() throws IOException {
    writer.append("package ").append(destPackage).append(";").append(NEWLINE).append(NEWLINE);
  }

  private Writer createFileWriter() throws IOException {

    String relPath = destPackage.replace('.', '/');

    File absDir = new File(generatedSourcesDir, relPath);
    if (!absDir.exists() && !absDir.mkdirs()) {
      processingContext.logNote("failed to create directories for:" + absDir.getAbsolutePath());
    }

    String fullPath = relPath + "/Q" + shortName + ".kt";
    File absFile = new File(generatedSourcesDir, fullPath);
    return new FileWriter(absFile);
  }

  private String derivePackage(String name) {
    int pos = name.lastIndexOf('.');
    if (pos == -1) {
      return "";
    }
    return name.substring(0, pos);
  }

  private String deriveShortName(String name) {
    int pos = name.lastIndexOf('.');
    if (pos == -1) {
      return name;
    }
    return name.substring(pos + 1);
  }
}
