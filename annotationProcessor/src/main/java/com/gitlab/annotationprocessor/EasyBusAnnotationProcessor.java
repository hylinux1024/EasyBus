package com.gitlab.annotationprocessor;

import com.gitlab.annotation.EasySubscribe;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

// 可以使用注解指定要解析的自定义注解以及Java版本号
// 也可以重写 AbstractProcessor 中的方法达到类似的目的
//@SupportedAnnotationTypes({"com.gitlab.annotation.EasySubscribe"})
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EasyBusAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elements;
    // key 为自定义注解标记的类，value 为自定义注解标记的方法
    // 即 EventBus 中的onEvent*方法
    private Map<TypeElement, ExecutableElement> methodsByClass = new LinkedHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elements = processingEnvironment.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        collectSubscribers(set, roundEnvironment, messager);
        return true;
    }

    private void collectSubscribers(Set<? extends TypeElement> annotations, RoundEnvironment env, Messager messager) {
        // 遍历要解析的注解
        for (TypeElement annotation : annotations) {
            messager.printMessage(Diagnostic.Kind.NOTE, "annotation:" + annotation.getSimpleName());
            // 获取自定义的注解对象
            Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
            // Element 是抽象的注解对象包括：包名、类、接口、方法、构造方法等
            for (Element element : elements) {
                // 自定义注解 EasySubscribe 是作用在方法上的
                // 所以检查一下是否是 ExecutableElement 对象
                // 它可以表示方法以及构造方法
                if (element instanceof ExecutableElement) {
                    ExecutableElement method = (ExecutableElement) element;
                    if (checkHasNoErrors(method, messager)) {
                        // 获取到这个被自定义注解的标记的方法所在类
                        TypeElement classElement = (TypeElement) method.getEnclosingElement();
                        methodsByClass.put(classElement, method);
                    }
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "@Subscribe is only valid for methods", element);
                }
            }
        }

        generateJavaFile();
    }

    private boolean checkHasNoErrors(ExecutableElement element, Messager messager) {
        if (element.getModifiers().contains(Modifier.STATIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must not be static", element);
            return false;
        }

        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be public", element);
            return false;
        }

        List<? extends VariableElement> parameters = ((ExecutableElement) element).getParameters();
        if (parameters.size() != 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must have exactly 1 parameter", element);
            return false;
        }
        return true;
    }

    private void generateJavaFile() {

    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
//        annotations.add("com.gitlab.annotation.EasySubscribe");
        //指定要解析的注解
        annotations.add(EasySubscribe.class.getCanonicalName());
        return annotations;
    }
}
