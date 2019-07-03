package io.ebean.querybean.generator;


import io.ebean.annotation.DbName;

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
import static io.ebean.querybean.generator.Constants.DATABASE;
import static io.ebean.querybean.generator.Constants.DB;
import static io.ebean.querybean.generator.Constants.TQASSOCBEAN;
import static io.ebean.querybean.generator.Constants.TQPROPERTY;
import static io.ebean.querybean.generator.Constants.TQROOTBEAN;
import static io.ebean.querybean.generator.Constants.TYPEQUERYBEAN;

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

  private final Set<String> importTypes = new TreeSet<>();

  private final List<PropertyMeta> properties = new ArrayList<>();

  private final TypeElement element;

  private final ProcessingContext processingContext;

  private final String dbName;
  private final String beanFullName;
  private final LangAdapter langAdapter;
  private boolean writingAssocBean;
  private final String generatedSourcesDir;

  private String destPackage;
  private String origDestPackage;

  private String shortName;
  private String origShortName;

  private Append writer;

  SimpleQueryBeanWriter(TypeElement element, ProcessingContext processingContext) {
    this.langAdapter = new KotlinLangAdapter();
    this.generatedSourcesDir = processingContext.generatedSourcesDir();
    this.element = element;
    this.processingContext = processingContext;

    DbName name = processingContext.findAnnotation(element, DbName.class);
    this.dbName = (name == null) ? null : name.value();
    this.beanFullName = element.getQualifiedName().toString();
    this.destPackage = derivePackage(beanFullName) + ".query";
    this.shortName = deriveShortName(beanFullName);
  }

  private LangAdapter lang() {
    return langAdapter;
  }

  private void gatherPropertyDetails() {

    final String generated = processingContext.getGeneratedAnnotation();
    if (generated != null) {
      importTypes.add(generated);
    }
    importTypes.add(beanFullName);
    importTypes.add(TQROOTBEAN);
    importTypes.add(TYPEQUERYBEAN);
    importTypes.add(DATABASE);
    if (dbName != null) {
      importTypes.add(DB);
    }
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
      writer = new Append(createFileWriter());

      translateKotlinImportTypes();

      writePackage();
      writeImports();
      writeClass();
      writeAlias();
      writeFields();
      writeConstructors();
      //writeStaticAliasClass();
      writeClassEnd();

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

    writer = new Append(createFileWriter());

    writePackage();
    writeImports();
    writeClass();
    writeFields();
    writeConstructors();
    writeClassEnd();

    writer.close();
  }

  /**
   * Prepare the imports for writing assoc bean.
   */
  private void prepareAssocBeanImports() {

    importTypes.remove(DB);
    importTypes.remove(TQROOTBEAN);
    importTypes.remove(DATABASE);
    importTypes.add(TQASSOCBEAN);
    if (isEntity()) {
      importTypes.add(TQPROPERTY);
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
  private void writeConstructors() {

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
  private void writeRootBeanConstructor() {
    lang().rootBeanConstructor(writer, shortName, dbName);
  }

  private void writeAssocBeanFetch() {
    if (isEntity()) {
      lang().fetch(writer, origShortName);
    }
  }

  /**
   * Write constructor for 'assoc' type query bean.
   */
  private void writeAssocBeanConstructor() {

    lang().assocBeanConstructor(writer, shortName);
  }

  /**
   * Write all the fields.
   */
  private void writeFields() {

    for (PropertyMeta property : properties) {
      String typeDefn = property.getTypeDefn(shortName, writingAssocBean);
      lang().fieldDefn(writer, property.getName(), typeDefn);
      writer.eol();
    }
    writer.eol();
  }

  /**
   * Write the class definition.
   */
  private void writeClass() {

    if (writingAssocBean) {
      writer.append("/**").eol();
      writer.append(" * Association query bean for %s.", shortName).eol();
      writer.append(" * ").eol();
      writer.append(" * THIS IS A GENERATED OBJECT, DO NOT MODIFY THIS CLASS.").eol();
      writer.append(" */").eol();
      if (processingContext.isGeneratedAvailable()) {
        writer.append(AT_GENERATED).eol();
      }
      writer.append(AT_TYPEQUERYBEAN).eol();
      lang().beginAssocClass(writer, shortName, origShortName);

    } else {
      writer.append("/**").eol();
      writer.append(" * Query bean for %s.", shortName).eol();
      writer.append(" * ").eol();
      writer.append(" * THIS IS A GENERATED OBJECT, DO NOT MODIFY THIS CLASS.").eol();
      writer.append(" */").eol();
      if (processingContext.isGeneratedAvailable()) {
        writer.append(AT_GENERATED).eol();
      }
      writer.append(AT_TYPEQUERYBEAN).eol();
      lang().beginClass(writer, shortName);
    }

    writer.eol();
  }

  private void writeAlias() {
    if (!writingAssocBean) {
      lang().alias(writer, shortName);
    }
  }

  private void writeClassEnd() {
    writer.append("}").eol();
  }

  /**
   * Write all the imports.
   */
  private void writeImports() {

    for (String importType : importTypes) {
      writer.append("import %s;", importType).eol();
    }
    writer.eol();
  }

  private void writePackage(){
    writer.append("package %s;", destPackage).eol().eol();
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
