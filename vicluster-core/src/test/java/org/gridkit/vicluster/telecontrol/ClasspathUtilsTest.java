package org.gridkit.vicluster.telecontrol;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Random;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

public class ClasspathUtilsTest {

    @Test
    public void verify_jar_has_dir_entry() throws Exception {
        final File baseDir = new File("target/testJarFiles/" + System.currentTimeMillis());
        new File(baseDir, "1").mkdirs();
        new FileOutputStream(new File(baseDir, "test.txt")).close();
        final byte[] jarAsBytes = ClasspathUtils.jarFiles(baseDir.toString());
        final File testJar = new File(baseDir, "test.jar");
        FileOutputStream fos = new FileOutputStream(testJar);
        fos.write(jarAsBytes);
        fos.close();
        URLClassLoader urlClassLoader = null;
        urlClassLoader = new URLClassLoader(new URL[]{testJar.toURI().toURL()});
        final URL resource = urlClassLoader.getResource("1");
        assertNotNull(resource);
    }

    @Test
    public void verify_spring_can_find_files() throws Exception {

        Random random = new Random();
        final String packageName = "package" + random.nextLong();
        final String classname = packageName + ".Test";

        final File baseDir = new File("target/testClasspathWithSpring/" + System.currentTimeMillis());
        baseDir.mkdirs();

        final ClassPool pool = ClassPool.getDefault();
        final CtClass cc = pool.makeClass(classname);
        final ClassFile classFile = cc.getClassFile();
        ConstPool cp = classFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
        attr.setAnnotation(new Annotation("org.springframework.stereotype.Component", cp));
        classFile.addAttribute(attr);
        cc.writeFile(baseDir.toString());


        final byte[] jarAsBytes = ClasspathUtils.jarFiles(baseDir.toString());
        final File testJar = new File(baseDir, "test.jar");
        FileOutputStream fos = new FileOutputStream(testJar);
        fos.write(jarAsBytes);
        fos.close();

        final URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
        final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        addURL.invoke(classLoader, testJar.toURI().toURL());

        ApplicationContext context = new AnnotationConfigApplicationContext(packageName);
        final Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Component.class);
        assertEquals(1, beansWithAnnotation.size());
        assertTrue(beansWithAnnotation.containsKey("test"));
        assertEquals(classname, beansWithAnnotation.get("test").getClass().getName());
    }
}