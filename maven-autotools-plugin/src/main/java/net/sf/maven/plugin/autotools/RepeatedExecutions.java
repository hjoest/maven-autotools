/*
 * Copyright (C) 2006-2009 Holger Joest <hjoest@users.sourceforge.net>
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;


/**
 *
 */
public final class RepeatedExecutions {

    /**
     * Remember if a mojo has already been run with exactly a
     * certain configuration.
     */
    private static Set<String> seenConfigurations = new HashSet<String>();


    /**
     *
     */
    public boolean alreadyRun(Object... elements) {
        StringBuffer sb = new StringBuffer();
        for (Object element : elements) {
            sb.append(':');
            if (element instanceof File) {
                sb.append(((File) element).getAbsolutePath());
            } else if (element != null) {
                sb.append(element.toString());
            }
        }
        String fp = sb.toString();
        if (seenConfigurations.contains(fp)) {
            return true;
        }
        seenConfigurations.add(fp);
        return false;
    }

}
