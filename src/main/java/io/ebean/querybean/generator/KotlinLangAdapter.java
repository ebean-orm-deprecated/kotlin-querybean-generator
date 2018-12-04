package io.ebean.querybean.generator;

class KotlinLangAdapter implements LangAdapter {

  @Override
  public void beginClass(Append writer, String shortName) {
    writer.append("class Q%s : TQRootBean<%1$s, Q%1$s> {", shortName).eol();
  }

  @Override
  public void beginAssocClass(Append writer, String shortName, String origShortName) {
    writer.append("class Q%s<R>(name: String, root: R) : TQAssocBean<%s,R>(name, root) {", shortName, origShortName).eol();
  }

  @Override
  public void alias(Append writer, String shortName) {

    writer.append("  companion object {").eol();
    writer.append("    /**").eol();
    writer.append("     * shared 'Alias' instance used to provide").eol();
    writer.append("     * properties to select and fetch clauses").eol();
    writer.append("     */").eol();
    writer.append("    val _alias = Q").append(shortName).append("(true)").eol();
    writer.append("  }").eol().eol();
  }

  @Override
  public void assocBeanConstructor(Append writer, String shortName) {
    // not required for kotlin as part of type definition
  }

  @Override
  public void fetch(Append writer, String origShortName) {

    writeAssocBeanFetch(writer, origShortName, "", "Eagerly fetch this association loading the specified properties.");
    writeAssocBeanFetch(writer, origShortName, "Query", "Eagerly fetch this association using a 'query join' loading the specified properties.");
    writeAssocBeanFetch(writer, origShortName, "Lazy", "Use lazy loading for this association loading the specified properties.");
  }

  private void writeAssocBeanFetch(Append writer, String origShortName, String fetchType, String comment) {

//    fun fetch(vararg properties: TQProperty<QContact>): R {
//      return fetchProperties(*properties)
//    }

    writer.append("  /**").eol();
    writer.append("   * ").append(comment).eol();
    writer.append("   */").eol();
    writer.append("  fun fetch%s(vararg properties: TQProperty<Q%s>) : R {", fetchType, origShortName).eol();
    writer.append("    return fetch%sProperties(*properties)", fetchType).eol();
    writer.append("  }").eol();
    writer.eol();
  }


  @Override
  public void rootBeanConstructor(Append writer, String shortName) {

    writer.eol();
    writer.append("  /**").eol();
    writer.append("   * Construct with a given EbeanServer.").eol();
    writer.append("   */").eol();
    writer.append("  constructor(server: EbeanServer) : super(%s::class.java, server)", shortName).eol().eol();

    writer.append("  /**").eol();
    writer.append("   * Construct using the default EbeanServer.").eol();
    writer.append("   */").eol();
    writer.append("  constructor() : super(%s::class.java)", shortName).eol().eol();

    writer.append("  /**").eol();
    writer.append("   * Construct for Alias.").eol();
    writer.append("   */").eol();
    writer.append("  private constructor(dummy: Boolean) : super(dummy)").eol();
  }

  @Override
  public void fieldDefn(Append writer, String propertyName, String typeDefn)  {

    writer.append("  lateinit var %s: ", propertyName);
    if (typeDefn.endsWith(",Integer>")) {
      typeDefn = typeDefn.replace(",Integer>", ",Int>");
    }
    writer.append(typeDefn);
  }

}
