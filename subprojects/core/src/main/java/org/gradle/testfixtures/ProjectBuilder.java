/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.testfixtures;

import com.google.common.hash.HashCode;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.internal.ProjectBuilderImpl;

import java.io.File;

/**
 * <p>Creates dummy instances of {@link org.gradle.api.Project} which you can use in testing custom task and plugin
 * implementations.</p>
 *
 * <p>To create a project instance:</p>
 *
 * <ol>
 *
 * <li>Create a {@code ProjectBuilder} instance by calling {@link #builder()}.</li>
 *
 * <li>Optionally, configure the builder.</li>
 *
 * <li>Call {@link #build()} to create the {@code Project} instance.</li>
 *
 * </ol>
 *
 * <p>You can reuse a builder to create multiple {@code Project} instances.</p>
 */
public class ProjectBuilder {

    private File projectDir;
    private String name = "test";
    private Project parent;
    private ProjectBuilderImpl impl = new ProjectBuilderImpl();

    /**
     * Creates a project builder.
     *
     * @return The builder
     */
    public static ProjectBuilder builder() {
        return new ProjectBuilder();
    }

    /**
     * Specifies the project directory for the project to build.
     *
     * @param dir The project directory
     * @return The builder
     */
    public ProjectBuilder withProjectDir(File dir) {
        projectDir = dir;
        return this;
    }

    /**
     * Specifies the name for the project
     *
     * @param name project name
     * @return The builder
     */
    public ProjectBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Specifies the parent project. Use it to create multi-module projects.
     *
     * @param parent parent project
     * @return The builder
     */
    public ProjectBuilder withParent(Project parent) {
        this.parent = parent;
        return this;
    }

    /**
     * Creates the project.
     *
     * @return The project
     */
    public Project build() {
        ProjectInternal project;
        if (parent != null) {
            project = (ProjectInternal) impl.createChildProject(name, parent, projectDir);
        } else {
            project = (ProjectInternal) impl.createProject(name, projectDir);
        }
        project.setClasspathHash(HashCode.fromString("1234abcdef"));
        return project;
    }
}
