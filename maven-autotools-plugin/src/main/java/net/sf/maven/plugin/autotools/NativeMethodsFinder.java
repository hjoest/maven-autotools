package net.sf.maven.plugin.autotools;

import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class NativeMethodsFinder
extends EmptyVisitor {

    private boolean nativeMethods;


    /**
     * Visits a method of the class.
     */
    public MethodVisitor visitMethod(int access, String name,
                                     String desc, String signature,
                                     String[] exceptions) {
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            nativeMethods = true;
        }
        return null;
    }


    public boolean hasNativeMethods() {
        return nativeMethods;
    }

}

