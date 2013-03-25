/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.testing.jacoco.tasks

import org.gradle.api.Incubating
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.testing.jacoco.plugin.JacocoTaskExtension
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskCollection

/**
 * Task to generate HTML reports of Jacoco coverage data.
 */
@Incubating
class JacocoReport extends JacocoBase {
	/**
	 * Collection of execution data files to analyze.
	 */
	@InputFiles
	FileCollection executionData

	/**
	 * Source sets that coverage should be reported for.
	 */
	Set<SourceSet> sourceSets

	/**
	 * Additional class dirs that coverage data should be reported for.
	 */
    @Optional @InputFiles
	FileCollection additionalClassDirs

	/**
	 * Additional source dirs for the classes coverage data is being reported for.
	 */
    @Optional @InputFiles
    FileCollection additionalSourceDirs

	/**
	 * Path to write report to. Defaults to {@code build/reports/jacoco/<task name>}.
	 */
	Object destPath

	JacocoReport() {
		onlyIf { getExecutionData().every { it.exists() } }
	}

	@TaskAction
	void generate() {
		getAnt().taskdef(name:'report', classname:'org.jacoco.ant.ReportTask', classpath:getJacocoClasspath().asPath)
		getAnt().report {
			executiondata {
				getExecutionData().addToAntBuilder(getAnt(), 'resources')
			}
			structure(name:getProject().getName()) {
				classfiles {
					getClassDirs().filter { it.exists() }.addToAntBuilder(getAnt(), 'resources')
				}
				sourcefiles {
					getSourceDirs().filter { it.exists() }.addToAntBuilder(getAnt(), 'resources')
				}
			}
			html(destdir:getDestDir())
		}
	}

	/**
	 * Gets the directory to write the report to.
	 */
	@OutputDirectory
	File getDestDir() {
		return getProject().file(destPath)
	}

	/**
	 * Adds execution data files to be used during coverage
	 * analysis.
	 * @param files one or more files to add
	 */
	void executionData(Object... files) {
		if (this.executionData == null) {
			this.executionData = getProject().files(files)
		} else {
			this.executionData += getProject().files(files)
		}
	}

	/**
	 * Adds execution data generated by a task to the list
	 * of those used during coverage analysis. Only tasks
	 * with a {@link JacocoTaskExtension} will be included;
	 * all others will be ignored.
	 * @param tasks one or more tasks to add
	 */
	void executionData(Task... tasks) {
		tasks.each { task ->
			JacocoTaskExtension extension = task.extensions.findByType(JacocoTaskExtension)
			if (extension != null) {
				executionData({ extension.destFile })
				this.executionData.builtBy task
			}
		}
	}

	/**
	 * Adds execution data generated by the given tasks to
	 * the list of those used during coverage analysis.
	 * Only tasks with a {@link JacocoTaskExtension} will
	 * be included; all others will be ignored.
	 * @param tasks one or more tasks to add
	 */
	void executionData(TaskCollection tasks) {
		tasks.all {	executionData(it) }
	}

	/**
	 * Gets the class directories that coverage will
	 * be reported for. All classes in these directories
	 * will be included in the report.
	 * @return class dirs to report coverage of
	 */
	@InputFiles
	FileCollection getClassDirs() {
		return processDirs(getAdditionalClassDirs()) { it.output }
	}

	/**
	 * Gets the source directories for the classes that will
	 * be reported on. Source will be obtained from these
	 * directories only for the classes included in the report.
	 * @return source directories for the classes reported on
	 * @see #getClassDirs()
	 */
	@InputFiles
	FileCollection getSourceDirs() {
		return processDirs(getAdditionalSourceDirs()) { getProject().files(it.allSource.srcDirs as File[]) }
	}

	/**
	 * Helper method to process class and source directories and combine
	 * the source sets and additional directories into one {@code FileCollection}.
	 * @param additionalDirs a collection of additional directories to include
	 * @param processSourceSet a closure that transforms a {@code SourceSet} into a
	 * {@code FileCollection} with the relevant contents
	 * @return a {@code FileCollection} including the contents of the additional dirs and
	 * the source sets
	 */
	private FileCollection processDirs(FileCollection additionalDirs, Closure processSourceSet) {
		FileCollection allSetDirs = getProject().files([])
		allSetDirs = getSourceSets().inject(allSetDirs) { dirs, sourceSet ->
			FileCollection setDirs = processSourceSet.call(sourceSet)
			dirs == null ? setDirs : dirs + setDirs
		}
		if (additionalDirs != null) {
			allSetDirs += additionalDirs
		}
		return allSetDirs
	}

	/**
	 * Adds a source set to the list to be reported on. The
	 * output of this source set will be used as classes to
	 * include in the report. The source for this source set
	 * will be used for any classes included in the report.
	 * @param sourceSets one or more source sets to report on
	 */
	void sourceSets(SourceSet... sourceSets) {
		if (this.sourceSets == null) {
			this.sourceSets = [] as Set
		}
		this.sourceSets.addAll(sourceSets)
	}

	/**
	 * Adds additional class directories to those
	 * that will be included in the report.
	 * @param dirs one or more directories containing
	 * classes to report coverage of
	 */
	void additionalClassDirs(File... dirs) {
		additionalClassDirs(getProject().files(dirs))
	}

	/**
	 * Adds additional class directories to those
	 * that will be included in the report.
	 * @param dirs a {@code FileCollection} of directories
	 * containing classes to report coverage of
	 */
	void additionalClassDirs(FileCollection dirs) {
		if (this.additionalClassDirs == null) {
			this.additionalClassDirs = dirs
		} else {
			this.additionalClassDirs += dirs
		}
	}

	/**
	 * Adds additional source directories to be used
	 * for any classes included in the report.
	 * @param dirs one or more directories containing
	 * source files for the classes included in the report
	 */
	void additionalSourceDirs(File... dirs) {
		additionalSourceDirs(getProject().files(dirs))
	}

	/**
	 * Adds additional source directories to be used
	 * for any classes included in the report.
	 * @param dirs a {@code FileCollection} of directories
	 * containing source files for the classes included in
	 * the report
	 */
	void additionalSourceDirs(FileCollection dirs) {
		if (this.additionalSourceDirs == null) {
			this.additionalSourceDirs = dirs
		} else {
			this.additionalSourceDirs += dirs
		}
	}
}
