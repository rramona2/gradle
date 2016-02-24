/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.performance

import spock.lang.Unroll

/**
 * Performance tests aimed at comparing the performance of Gradle for compiling and executing test suites, making
 * sure we are always faster than Maven.
 */
class JavaTestGradleVsMavenPerformanceTest extends AbstractGradleVsMavenPerformanceTest {
    @Unroll("Gradle vs Maven #description build for #template")
    def "cleanTest test performance test"() {
        given:
        runner.testGroup = "Gradle vs Maven test build using Java plugin"
        runner.testId = "$size $description with Java plugin"
        runner.baseline {
            projectName(template).displayName("Gradle $description for project $template").invocation {
                tasksToRun(gradleTasks).useDaemon().gradleOpts('-Xms1G', '-Xmx1G')
            }.warmUpCount(1).invocationCount(5)
        }
        runner.mavenBuildSpec {
            projectName(template).displayName("Maven $description for project $template").invocation {
                tasksToRun(equivalentMavenTasks).mavenOpts('-Xms1G', '-Xmx1G')
                    .args('-q', '-Dsurefire.printSummary=false')
            }.warmUpCount(1).invocationCount(5)
        }


        when:
        def results = runner.run()

        then:
        noExceptionThrown()
        results.assertComparesWithMaven(maxDiffMillis, maxDiffMB)

        where:
        template          | size     | description                 | gradleTasks           | equivalentMavenTasks | maxDiffMillis | maxDiffMB
        'mediumWithJUnit' | 'medium' | 'runs tests only'           | ['cleanTest', 'test'] | ['test']             | 10000         | 60
        'mediumWithJUnit' | 'medium' | 'clean build and run tests' | ['clean', 'test']     | ['clean', 'test']    | 5000          | 60
    }
}