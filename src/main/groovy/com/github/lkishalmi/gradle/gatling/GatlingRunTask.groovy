package com.github.lkishalmi.gradle.gatling

import org.gradle.api.tasks.JavaExec
import org.gradle.process.ExecResult
import java.util.regex.Pattern

class GatlingRunTask extends JavaExec {

    private final String GATLING_MAIN_CLASS = 'io.gatling.app.Gatling'

    def simulations
    def failedSimulations = []
    ExecResult execResult = null
    List<String> jvmArgs

    public GatlingRunTask() {

        main = GATLING_MAIN_CLASS
        classpath = project.configurations.gatlingRuntime

        args "-m"
        args "-bf", "${project.sourceSets.gatling.output.classesDir}"
        args "-df", "${project.sourceSets.gatling.output.resourcesDir}"
        args "-bdf", "${project.sourceSets.gatling.output.resourcesDir}"
        args "-rf", "${project.reportsDir}/gatling"

        systemProperties = System.properties as Map
        standardInput = System.in
    }

    @Override
    void exec() {
        def self = this

        Iterable<String> actualSimulations
        if (getSimulations() instanceof Closure<Iterable<String>>) {
            actualSimulations = project.extensions.getByType(GatlingPluginExtension).resolveSimulations(getSimulations())
        } else if (getSimulations() instanceof Iterable<String>) {
            actualSimulations = getSimulations()
        } else {
            throw new IllegalArgumentException("`simulations` property neither Closure nor Iterable<String>")
        }

        actualSimulations.each { simu ->
            execResult = project.javaexec {
                main = self.getMain()
                classpath = self.getClasspath()

                jvmArgs = self.getJvmArgs()

                ignoreExitValue = Boolean.valueOf(System.getProperty('gatling.continueOnFailure', project.gatling.continueOnFailure as String))

                args self.getArgs()
                args "-s", simu

                systemProperties = self.getSystemProperties()
                standardInput = self.getStandardInput()
            }
            if (execResult.exitValue != 0) {
                println ">>\n>>\n>>\n>> FAILURE ---- Simulation ${simu} has FAILED ---- \n>>\n>>\n>>\n>>"
                failedSimulations.add(simu)

            } else {
                println ">>\n>>\n>>\n>> SUCCESS ---- Simulation ${simu} has PASSED ---- \n>>\n>>\n>>\n>>"
            }
        }
        if (failedSimulations.size() > 0) {
            throw new GroovyRuntimeException("THE FOLLOWING TESTS HAVE FAILED: ${failedSimulations}".toString())
        }
    }
}
