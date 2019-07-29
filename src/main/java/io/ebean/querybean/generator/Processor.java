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
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    int count = 0;

    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        generateQueryBeans(element);
        count++;
      }
    }

    if (count > 0) {
      processingContext.logNote("Generated " + count + " query beans");
    }

    return true;
  }

  private void generateQueryBeans(Element element) {
    try {
      SimpleQueryBeanWriter beanWriter = new SimpleQueryBeanWriter((TypeElement) element, processingContext);
      beanWriter.writeRootBean();
      beanWriter.writeAssocBean();
    } catch (Exception e) {
      processingContext.logError(element, "Error generating query beans: " + e);
    }
  }
}
