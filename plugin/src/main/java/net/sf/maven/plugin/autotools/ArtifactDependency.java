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

import java.io.Serializable;


public abstract class ArtifactDependency
implements Serializable {

    /** */
    private static final long serialVersionUID = 7600103827104057795L;

    private String groupId;

    private String artifactId;

    private String version;


    public String getGroupId() {
        return groupId;
    }


    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    public String getArtifactId() {
        return artifactId;
    }


    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }


    public String getVersion() {
        return version;
    }


    public void setVersion(String version) {
        this.version = version;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        ArtifactDependency other = (ArtifactDependency) obj;
        return other.getGroupId().equals(getGroupId())
               && other.getArtifactId().equals(getArtifactId())
               && other.getVersion().equals(getVersion());
    }


    @Override
    public int hashCode() {
        return getGroupId().hashCode()
               + 8683253 * getArtifactId().hashCode()
               + 50652617 * getVersion().hashCode();
    }


    @Override
    public String toString() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

}

