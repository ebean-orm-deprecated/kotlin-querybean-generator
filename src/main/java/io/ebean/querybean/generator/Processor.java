package io.ebean.querybean.generator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Process compiled entity beans and generates 'query beans' for them.
 */
public class Processor extends AbstractProcessor implements Constants {

  private static final String GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code";
  private static final String KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated";

  private ProcessingContext processingContext;

  public Processor() {
  }

  @Override
  public Set<String> getSupportedOptions() {

    Set<String> options =  new LinkedHashSet<>();
    options.add(KAPT_KOTLIN_GENERATED_OPTION);
    options.add(GENERATE_KOTLIN_CODE_OPTION);
    return options;
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.processingContext = new ProcessingContext(processingEnv);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {

    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(ENTITY);
    annotations.add(EMBEDDABLE);
    annotations.add(MODULEINFO);
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    processingContext.readModuleInfo();
    int count = 0;
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        generateQueryBeans(element);
        count++;
      }
    }
    final int loaded = processingContext.complete();
    if (roundEnv.processingOver()) {
      writeModuleInfoBean();
    }

    if (count > 0) {
      String msg = "Ebean APT generated %s query beans, loaded %s others - META-INF/ebean-generated-info.mf entity-packages: %s";
      processingContext.logNote(msg, count, loaded, processingContext.getAllEntityPackages());
    }

    return true;
  }

  private void writeModuleInfoBean() {
    try {
      SimpleModuleInfoWriter writer = new SimpleModuleInfoWriter(processingContext);
      writer.write();
    } catch (Throwable e) {
      processingContext.logError(null, "Failed to write ModuleInfoLoader " + e.getMessage());
    }
  }

  private void generateQueryBeans(Element element) {
    try {
      SimpleQueryBeanWriter beanWriter = new SimpleQueryBeanWriter((TypeElement) element, processingContext);
      beanWriter.writeRootBean();
      beanWriter.writeAssocBean();
    } catch (Throwable e) {
      processingContext.logError(element, "Error generating query beans: " + e);
    }
  }
}
