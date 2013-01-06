/*
 * Copyright (C) 2006-2013 Holger Joest <holger@joest.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

