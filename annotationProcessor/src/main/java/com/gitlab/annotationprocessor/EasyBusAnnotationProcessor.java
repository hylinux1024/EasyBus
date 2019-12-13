package com.gitlab.annotationprocessor;

import com.gitlab.annotation.EasySubscribe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

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
    private Map<TypeElement, List<ExecutableElement>> methodsByClass = new LinkedHashMap<>();
    private boolean writeDone = false;

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
                        List<ExecutableElement> list = methodsByClass.get(classElement);
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        list.add(method);
                        methodsByClass.put(classElement, list);
                    }
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "@EasySubscribe is only valid for methods", element);
                }
            }
        }

        if (!writeDone && !methodsByClass.isEmpty()) {
            createInfoIndexFile("com.github.easybus.MyEventBusIndex");
            writeDone = true;
        } else {
            messager.printMessage(Diagnostic.Kind.WARNING, "No @EasySubscribe annotations found");
        }

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

    private void createInfoIndexFile(String index) {
        BufferedWriter writer = null;
        try {
            JavaFileObject sourceFile = filer.createSourceFile(index);
            int period = index.lastIndexOf('.');
            String myPackage = period > 0 ? index.substring(0, period) : null;
            String clazz = index.substring(period + 1);
            writer = new BufferedWriter(sourceFile.openWriter());
            if (myPackage != null) {
                writer.write("package " + myPackage + ";\n\n");
            }
            writer.write("import com.gitlab.annotation.meta.SubscriberInfoIndex;\n");
            writer.write("import com.gitlab.annotation.meta.SubscriberInfo;\n");
            writer.write("import com.gitlab.annotation.meta.SubscriberMethodInfo;\n");
            writer.write("import java.util.HashMap;\n");
            writer.write("import java.util.Map;\n\n");
            writer.write("/** This class is generated by EasyBus, do not edit. */\n");
            writer.write("public class " + clazz + " implements SubscriberInfoIndex {\n");
            writer.write("    private static final Map<Class<?>, SubscriberInfo> SUBSCRIBER_INDEX;\n\n");
            writer.write("    static {\n");
            writer.write("        SUBSCRIBER_INDEX = new HashMap<Class<?>, SubscriberInfo>();\n\n");
            writeIndexLines(writer, myPackage);
            writer.write("    }\n\n");
            writer.write("    private static void putIndex(SubscriberInfo info) {\n");
            writer.write("        SUBSCRIBER_INDEX.put(info.getSubscriberClass(), info);\n");
            writer.write("    }\n\n");
            writer.write("    @Override\n");
            writer.write("    public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {\n");
            writer.write("        SubscriberInfo info = SUBSCRIBER_INDEX.get(subscriberClass);\n");
            writer.write("        if (info != null) {\n");
            writer.write("            return info;\n");
            writer.write("        } else {\n");
            writer.write("            return null;\n");
            writer.write("        }\n");
            writer.write("    }\n");
            writer.write("}\n");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not write source for " + index, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //Silent
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeIndexLines(BufferedWriter writer, String myPackage) throws IOException {
        for (TypeElement subscriberTypeElement : methodsByClass.keySet()) {


            String subscriberClass = getClassString(subscriberTypeElement, myPackage);
            if (isVisible(myPackage, subscriberTypeElement)) {
                writeLine(writer, 2,
                        "putIndex(new SubscriberInfo(" + subscriberTypeElement + ".class,",
                        "new SubscriberMethodInfo[] {");
                List<ExecutableElement> methods = methodsByClass.get(subscriberTypeElement);

                writeCreateSubscriberMethods(writer, methods, "new SubscriberMethodInfo", myPackage);
                writer.write("        }));\n\n");
            } else {
                writer.write("        // Subscriber not visible to index: " + subscriberClass + "\n");
            }
        }
    }

    private String getClassString(TypeElement typeElement, String myPackage) {
        PackageElement packageElement = getPackageElement(typeElement);
        String packageString = packageElement.getQualifiedName().toString();
        String className = typeElement.getQualifiedName().toString();
        if (packageString != null && !packageString.isEmpty()) {
            if (packageString.equals(myPackage)) {
                className = cutPackage(myPackage, className);
            } else if (packageString.equals("java.lang")) {
                className = typeElement.getSimpleName().toString();
            }
        }
        return className;
    }

    private String cutPackage(String paket, String className) {
        if (className.startsWith(paket + '.')) {
            // Don't use TypeElement.getSimpleName, it doesn't work for us with inner classes
            return className.substring(paket.length() + 1);
        } else {
            // Paranoia
            throw new IllegalStateException("Mismatching " + paket + " vs. " + className);
        }
    }

    private void writeCreateSubscriberMethods(BufferedWriter writer, List<ExecutableElement> methods,
                                              String callPrefix, String myPackage) throws IOException {
        for (ExecutableElement method : methods) {
            List<? extends VariableElement> parameters = method.getParameters();
            TypeMirror paramType = getParamTypeMirror(parameters.get(0), null);
            TypeElement paramElement = (TypeElement) processingEnv.getTypeUtils().asElement(paramType);
            String methodName = method.getSimpleName().toString();
            String eventClass = getClassString(paramElement, myPackage) + ".class";

//            EasySubscribe subscribe = method.getAnnotation(EasySubscribe.class);
            List<String> parts = new ArrayList<>();
            parts.add(callPrefix + "(\"" + methodName + "\",");
            String lineEnd = "),";
            parts.add(eventClass + lineEnd);

            writeLine(writer, 3, parts.toArray(new String[parts.size()]));

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Indexed @Subscribe at " +
                    method.getEnclosingElement().getSimpleName() + "." + methodName +
                    "(" + paramElement.getSimpleName() + ")");

        }
    }

    private TypeMirror getParamTypeMirror(VariableElement param, Messager messager) {
        TypeMirror typeMirror = param.asType();
        // Check for generic type
        if (typeMirror instanceof TypeVariable) {
            TypeMirror upperBound = ((TypeVariable) typeMirror).getUpperBound();
            if (upperBound instanceof DeclaredType) {
                if (messager != null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Using upper bound type " + upperBound +
                            " for generic parameter", param);
                }
                typeMirror = upperBound;
            }
        }
        return typeMirror;
    }

    private PackageElement getPackageElement(TypeElement subscriberClass) {
        Element candidate = subscriberClass.getEnclosingElement();
        while (!(candidate instanceof PackageElement)) {
            candidate = candidate.getEnclosingElement();
        }
        return (PackageElement) candidate;
    }

    private void writeLine(BufferedWriter writer, int indentLevel, String... parts) throws IOException {
        writeLine(writer, indentLevel, 2, parts);
    }

    private void writeLine(BufferedWriter writer, int indentLevel, int indentLevelIncrease, String... parts)
            throws IOException {
        writeIndent(writer, indentLevel);
        int len = indentLevel * 4;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i != 0) {
                if (len + part.length() > 118) {
                    writer.write("\n");
                    if (indentLevel < 12) {
                        indentLevel += indentLevelIncrease;
                    }
                    writeIndent(writer, indentLevel);
                    len = indentLevel * 4;
                } else {
                    writer.write(" ");
                }
            }
            writer.write(part);
            len += part.length();
        }
        writer.write("\n");
    }

    private void writeIndent(BufferedWriter writer, int indentLevel) throws IOException {
        for (int i = 0; i < indentLevel; i++) {
            writer.write("    ");
        }
    }

    private boolean isVisible(String myPackage, TypeElement typeElement) {
        Set<Modifier> modifiers = typeElement.getModifiers();
        boolean visible;
        if (modifiers.contains(Modifier.PUBLIC)) {
            visible = true;
        } else if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.PROTECTED)) {
            visible = false;
        } else {
            String subscriberPackage = getPackageElement(typeElement).getQualifiedName().toString();
            if (myPackage == null) {
                visible = subscriberPackage.length() == 0;
            } else {
                visible = myPackage.equals(subscriberPackage);
            }
        }
        return visible;
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
