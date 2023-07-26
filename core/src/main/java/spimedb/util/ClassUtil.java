package spimedb.util;


import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * Copyright 2003 Sapient
 * @since carbon 2.0
 * @author Greg Hinkle, March 2003
 * @version $Revision: 1.5 $($Author: dvoet $ / $Date: 2003/05/05 21:21:23 $)
 */
public class ClassUtil {

    /**
     * Retrieves all interfaces implemented by a specified interface
     * including all recursively extended interfaces and the classes supplied
     * int the parameter.
     * @param childInterfaces a set of interfaces
     * @return Class[] an array of interfaces that includes those specifed
     * in childInterfaces plus all of those interfaces' super interfaces
     */
    public static Set<Class> getSuperInterfaces(Class... childInterfaces) {

        Set<Class> allInterfaces = new HashSet();

        for (int i = 0; i < childInterfaces.length; i++) {
            Class c = childInterfaces[i];
            allInterfaces.add(c);
            allInterfaces.addAll(
                            getSuperInterfaces(c.getInterfaces()));
        }

        return allInterfaces;
    }

    public static Set<Class> getSuperInterfacesOf(Class c) {
        Set<Class> cs = getSuperInterfaces(new Class[] { c });
        cs.remove(c);
        return cs;
    }

    /**
     * Builds an <b>unordered</b> set of all interface and object classes that
     * are generalizations of the provided class.
     * @param classObject the class to find generalization of.
     * @return a Set of class objects.
     */
    public static Set getGeneralizations(Class classObject) {
        Set generalizations = new HashSet();

        generalizations.add(classObject);

        Class superClass = classObject.getSuperclass();
        if (superClass != null) {
            generalizations.addAll(getGeneralizations(superClass));
        }

        Class[] superInterfaces = classObject.getInterfaces();
        for (int i = 0; i < superInterfaces.length; i++) {
            Class superInterface = superInterfaces[i];
            generalizations.addAll(getGeneralizations(superInterface));
        }

        return generalizations;
    }

}