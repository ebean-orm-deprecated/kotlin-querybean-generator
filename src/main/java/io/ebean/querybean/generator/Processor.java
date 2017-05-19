package io.ebean.querybean.generator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Process compiled entity beans and generates 'query beans' for them.
 */
public class Processor extends AbstractProcessor {

  private static final String GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code";
  private static final String KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated";

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
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {

    Set<String> annotations = new LinkedHashSet<>();
    annotations.add(Entity.class.getCanonicalName());
    annotations.add(Embeddable.class.getCanonicalName());
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    int entityCount = 0;
    int embeddableCount = 0;

    //processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "options[" + processingEnv.getOptions() + "]");

    String generatedDir = processingEnv.getOptions().get("kapt.kotlin.generated");
    if (generatedDir == null) {
      generatedDir = "target/generated-sources/kapt/compile";
    }

    ProcessingContext context = new ProcessingContext(processingEnv, generatedDir);

    for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
      generateQueryBeans(context, element);
      entityCount++;
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(Embeddable.class)) {
      generateQueryBeans(context, element);
      embeddableCount++;
    }

    if (entityCount > 0 || embeddableCount > 0) {
      context.logNote("Generated query beans for [" + entityCount + "] entities [" + embeddableCount + "] embeddable");
    }

    return true;
  }

  private void generateQueryBeans(ProcessingContext context, Element element) {
    try {
      SimpleQueryBeanWriter beanWriter = new SimpleQueryBeanWriter((TypeElement) element, context);
      beanWriter.writeRootBean();
      beanWriter.writeAssocBean();
    } catch (Exception e) {
      context.logError(element, "Error generating query beans: " + e);
    }
  }
}
