import Models.JBoss.Domain.ServerGroupHelper
import Models.JBoss.Domain.ServerHelper
import Services.CliCommandsGeneratorHelper
import Utils.EnvPropertiesHelper
import spock.lang.*

@Ignore
@IgnoreIf({ env.JBOSS_MODE == 'standalone' })
class DeployApplicationDomain extends PluginTestHelper {

    @Shared
    String procName = 'DeployApplication'
    @Shared
    String projectName = "EC-JBoss Specs $procName Project"
    @Shared
    String defaultConfigName = 'specConfig'
    @Shared
    String defaultCliPath = ''
    @Shared
    String linkToSampleWarFile = "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/versions/hello-world-war-version-1.war"
    @Shared
    String linkToSampleWarFile2 = "https://github.com/electric-cloud/hello-world-war/raw/system_tests/dist/versions/hello-world-war-version-2.war"


    // 2 default server groups
    @Shared
    String serverGroup1 = "main-server-group"
    @Shared
    String serverGroup2 = "other-server-group"
    @Shared
    String serverGroup1Server1 = "server-one"
    @Shared
    String serverGroup1Server2 = "server-two"
    @Shared
    String serverGroup2Server1 = "server-three"

    @Shared
    ServerGroupHelper serverGroup1Model
    @Shared
    ServerGroupHelper serverGroup2Model

    @Shared
    String hostNameMaster = EnvPropertiesHelper.getJbossDomainMasterHostname()

    static String getPathApp() {
        String applicationContentSourcePath = "/tmp/"
        EnvPropertiesHelper.getOS() == "WINDOWS" ? applicationContentSourcePath = "C:\\\\tmp\\\\" : applicationContentSourcePath
        return  applicationContentSourcePath
    }

    def doSetupSpec() {
        logger.info("Hello World! doSetupSpec")
        redirectLogs()
        createDefaultConfiguration(defaultConfigName)
        def resName = createJBossResource()

        dslFile 'dsl/RunProcedure.dsl', [
                projName: projectName,
                resName : resName,
                procName: procName,
                params  : [
                        additionalOptions              : '',
                        applicationContentSourcePath   : '',
                        deploymentName                 : '',
                        disabledServerGroups           : '',
                        enabledServerGroups            : '',
                        runtimeName                    : '',
                        serverconfig                   : '',
                ]
        ]

        createHelperProject(resName, defaultConfigName)
        createServerGroupModels()
        runCliCommand(CliCommandsGeneratorHelper.startServerCmd(serverGroup2Server1, hostNameMaster))
    }

    def doCleanupSpec() {
        logger.info("Hello World! doCleanupSpec")
        deleteProject(projectName)
        deleteConfiguration("EC-JBoss", defaultConfigName)
    }

    RunProcedureJob runProcedureUnderTest(def parameters) {
        return runProcedureDsl(projectName, procName, parameters)
    }

   @Unroll
    def "DeployApplication, 1st time, file, enabled server group: 1 server group, minimum params (C278234)"() {
        String testCaseId = "C278234"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                deploymentName                 : '',
                disabledServerGroups           : '',
                enabledServerGroups            : "$serverGroup1",
                runtimeName                    : '',
                serverconfig                   : defaultConfigName,
        ]

        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup1 server groups."

        String[] expectedServerGroupsWithApp = [serverGroup1]
        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithApp)

        cleanup:
        undeployFromAllRelevantServerGroups(expectedAppName)
    }

        @Unroll
       def "DeployApplication, 1st time, file, disabled server group: 1 server group, minimum params (C278235)"() {
           String testCaseId = "C278235"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                   deploymentName                 : '',
                   disabledServerGroups           : "$serverGroup1",
                   enabledServerGroups            : '',
                   runtimeName                    : '',
                   serverconfig                   : defaultConfigName,
           ]

           setup:
           downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app.war"
           String expectedRuntimeName = "$testCaseId-app.war"
           String expectedContextRoot = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nDisabled on: $serverGroup1 server groups."

           String[] expectedServerGroupsWithApp = [serverGroup1]
           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithApp)
           checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

           cleanup:
           undeployFromAllRelevantServerGroups("$testCaseId-app.war")
       }


       @Unroll
       def "DeployApplication, 1st time, file, enabled server group: 1 server group, disabled server group: 1 server group, minimum params (C278236)"() {
           String testCaseId = "C278236"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                   deploymentName                 : '',
                   disabledServerGroups           : "$serverGroup1",
                   enabledServerGroups            : "$serverGroup2",
                   runtimeName                    : '',
                   serverconfig                   : defaultConfigName,
           ]

           setup:
           downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app.war"
           String expectedRuntimeName = "$testCaseId-app.war"
           String expectedContextRoot = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

           String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
           String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
           checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
           checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)


           cleanup:
           undeployFromAllRelevantServerGroups("$testCaseId-app.war")
       }

          @Unroll
          def "DeployApplication, 1st time, file, with custom name and runtime name (C278237)"() {
              String testCaseId = "C278237"

              def runParams = [
                      additionalOptions              : '',
                      applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                      deploymentName                 : "app-custom-$testCaseId-appname.war",
                      disabledServerGroups           : "$serverGroup1",
                      enabledServerGroups            : "$serverGroup2",
                      runtimeName                    : "app-custom-$testCaseId-runtimename.war",
                      serverconfig                   : defaultConfigName,
              ]

              setup:
              downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

              when:
              RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

              then:
              String expectedAppName = "app-custom-$testCaseId-appname.war"
              String expectedRuntimeName = "app-custom-$testCaseId-runtimename.war"
              String expectedContextRoot = "app-custom-$testCaseId-runtimename"

              assert runProcedureJob.getStatus() == "success"
              assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."


              String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
              String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

              checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
              checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

              checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
              checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)


              cleanup:
              undeployFromAllRelevantServerGroups("app-custom-$testCaseId-appname.war")
          }

           @Unroll
           def "DeployApplication, app already deployed, file, enabled and disabled server groups, update app (C278242)"() {
               String testCaseId = "C278242"

               def runParams = [
                       additionalOptions              : '',
                       applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                       deploymentName                 : "$testCaseId-app.war",
                       disabledServerGroups           : "$serverGroup1",
                       enabledServerGroups            : "$serverGroup2",
                       runtimeName                    : "$testCaseId-app.war",
                       serverconfig                   : defaultConfigName,
               ]

               setup:
               downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

               String expectedAppName = "$testCaseId-app.war"
               String runtimeName = "$testCaseId-app.war"
               String expectedContextRoot = "$testCaseId-app"
               String[] oldServerGroupsWithApp = [serverGroup1, serverGroup2]
               deployToServerGroups(oldServerGroupsWithApp, runParams.applicationContentSourcePath, expectedAppName, runtimeName)
               downloadArtifact(linkToSampleWarFile2, runParams.applicationContentSourcePath)

               when:
               RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

               then:
               assert runProcedureJob.getStatus() == "success"
               assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

               String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
               String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

               checkAppDeployedToServerGroupsCli(expectedAppName, runtimeName, expectedServerGroupsWithAppEnabled)
               checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled, "2")

               checkAppDeployedToServerGroupsCli(expectedAppName, runtimeName, expectedServerGroupsWithAppDisabled)
               checkAppUploadedToContentRepo(expectedAppName, runtimeName)

               cleanup:
               undeployFromAllRelevantServerGroups("$testCaseId-app.war")
           }

             @Unroll
             @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
             def "DeployApplication, app already deployed, url (for EAP 7 and later), enabled server group and disabled server group, update app (C278258)"() {
                 String testCaseId = "C278258"

                 def runParams = [
                         additionalOptions              : '',
                         applicationContentSourcePath   : "--url=$linkToSampleWarFile2",
                         deploymentName                 : "$testCaseId-app.war",
                         disabledServerGroups           : "$serverGroup1",
                         enabledServerGroups            : "$serverGroup2",
                         runtimeName                    : "$testCaseId-app.war",
                         serverconfig                   : defaultConfigName,
                 ]

                 String existingAppName = "$testCaseId-app.war"
                 String runtimeName = "$testCaseId-app.war"
                 String expectedContextRoot = "$testCaseId-app"
                 String[] oldServerGroupsWithApp = [serverGroup1, serverGroup2]
                 deployToServerGroups(oldServerGroupsWithApp, "--url=$linkToSampleWarFile", existingAppName, runtimeName)

                 when:
                 RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                 then:
                 assert runProcedureJob.getStatus() == "success"
                 assert runProcedureJob.getUpperStepSummary() =~ "Application '$existingAppName' has been successfully deployed from '$linkToSampleWarFile2'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

                 String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
                 String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

                 checkAppDeployedToServerGroupsCli(existingAppName, runtimeName, expectedServerGroupsWithAppEnabled)
                 checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled, "2")

                 checkAppDeployedToServerGroupsCli(existingAppName, runtimeName, expectedServerGroupsWithAppDisabled)
                 checkAppUploadedToContentRepo(existingAppName, runtimeName)


                 cleanup:
                 existingAppName = "$testCaseId-app.war"
                 undeployFromAllRelevantServerGroups(existingAppName)
             }

             @Unroll
             def "DeployApplication,  1st time, file, custom app name (C278245)"() {
                 String testCaseId = "C278245"

                 def runParams = [
                         additionalOptions              : '',
                         applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                         deploymentName                 : "$testCaseId-custom-appname.war",
                         disabledServerGroups           : "$serverGroup1",
                         enabledServerGroups            : "$serverGroup2",
                         runtimeName                    : '',
                         serverconfig                   : defaultConfigName,
                 ]

                 setup:
                 downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

                 when:
                 RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                 then:
                 String expectedAppName = "$testCaseId-custom-appname.war"
                 String expectedRuntimeName = "$testCaseId-custom-appname.war"
                 String expectedContextRoot = "$testCaseId-custom-appname"

                 assert runProcedureJob.getStatus() == "success"
                 assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

                 String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
                 String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

                 checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
                 checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

                 checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
                 checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

                 cleanup:
                 undeployFromAllRelevantServerGroups("$testCaseId-custom-appname.war")
             }

             @Unroll
             def "DeployApplication, 1st time, file, custom runtime name (C278246)"() {
                 String testCaseId = "C278246"

                 def runParams = [
                         additionalOptions              : '',
                         applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                         deploymentName                 : "$testCaseId-app.war",
                         disabledServerGroups           : "$serverGroup1",
                         enabledServerGroups            : "$serverGroup2",
                         runtimeName                    : "app-custom-$testCaseId-runtimename.war",
                         serverconfig                   : defaultConfigName,
                 ]

                 setup:
                 downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

                 when:
                 RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                 then:
                 String expectedAppName = "$testCaseId-app.war"
                 String expectedRuntimeName = "app-custom-$testCaseId-runtimename.war"
                 String expectedContextRoot = "app-custom-$testCaseId-runtimename"

                 assert runProcedureJob.getStatus() == "success"
                 assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

                 String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
                 String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

                 checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
                 checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

                 checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
                 checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)


                 cleanup:
                 undeployFromAllRelevantServerGroups(expectedAppName)
             }

             @Unroll
             def "DeployApplication, 1st time, file, whitespace in path (C278248)"() {
                 String testCaseId = "C278248"

                 def runParams = [
                         additionalOptions              : '',
                         applicationContentSourcePath   : getPathApp()+"$testCaseId-app with whitespace.war",
                         deploymentName                 : "$testCaseId-app.war",
                         disabledServerGroups           : "$serverGroup1",
                         enabledServerGroups            : "$serverGroup2",
                         runtimeName                    : "app-custom-$testCaseId-runtimename.war",
                         serverconfig                   : defaultConfigName,
                 ]

                 setup:
                 downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

                 when:
                 RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                 then:
                 String expectedAppName = "$testCaseId-app.war"
                 String expectedRuntimeName = "app-custom-$testCaseId-runtimename.war"
                 String expectedContextRoot = "app-custom-$testCaseId-runtimename"

                 assert runProcedureJob.getStatus() == "success"
                 assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

                 String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
                 String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

                 checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
                 checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

                 checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
                 checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)


                 cleanup:
                 undeployFromAllRelevantServerGroups(expectedAppName)
             }

               @Unroll
               def "DeployApplication, 1st time, file, custom app name without extension, custom runtime name (C278249)"() {
                   String testCaseId = "C278249"

                   def runParams = [
                           additionalOptions              : '',
                           applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                           deploymentName                 : "$testCaseId-app",
                           disabledServerGroups           : "$serverGroup1",
                           enabledServerGroups            : "$serverGroup2",
                           runtimeName                    : "app-custom-$testCaseId-runtimename.war",
                           serverconfig                   : defaultConfigName,
                   ]

                   setup:
                   downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

                   when:
                   RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                   then:
                   String expectedAppName = "$testCaseId-app"
                   String expectedRuntimeName = "app-custom-$testCaseId-runtimename.war"
                   String expectedContextRoot = "app-custom-$testCaseId-runtimename"

                   assert runProcedureJob.getStatus() == "success"
                   assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."


                   String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
                   String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

                   checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
                   checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

                   checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
                   checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)


                   cleanup:
                   undeployFromAllRelevantServerGroups(expectedAppName)
               }


   @Unroll
   @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "DeployApplication, 1st time, url (for EAP 7 and later), enabled server groups (C278256)"() {
        String testCaseId = "C278256"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : "$serverGroup2",
                runtimeName                    : "$testCaseId-app.war",
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app.war"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'.\nEnabled on: $serverGroup2 server groups."

        String[] expectedServerGroupsWithAppDisabled = [serverGroup2]

        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
        checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

        cleanup:
        undeployFromAllRelevantServerGroups(expectedAppName)
    }


    @Unroll
    @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
    def "DeployApplication, 1st time, url (for EAP 7 and later), disabled server groups (C278265)"() {
        String testCaseId = "C278265"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : "$serverGroup2",
                enabledServerGroups            : '',
                runtimeName                    : "$testCaseId-app.war",
                serverconfig                   : defaultConfigName,
        ]

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'.\nDisabled on: $serverGroup2 server groups."

        String[] expectedServerGroupsWithAppEnabled = [serverGroup2]

        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
        checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

        cleanup:
        undeployFromAllRelevantServerGroups(expectedAppName)
    }


       @Unroll
       @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
       def "DeployApplication, 1st time, url (for EAP 7 and later), disabled server groups and enabled server groups (C278266)"() {
           String testCaseId = "C278266"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : "--url=$linkToSampleWarFile",
                   deploymentName                 : "$testCaseId-app.war",
                   disabledServerGroups           : "$serverGroup2",
                   enabledServerGroups            : "$serverGroup1",
                   runtimeName                    : "$testCaseId-app.war",
                   serverconfig                   : defaultConfigName,
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app.war"
           String expectedRuntimeName = "$testCaseId-app.war"
           String expectedContextRoot = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$linkToSampleWarFile'.\nEnabled on: $serverGroup1 server groups.\nDisabled on: $serverGroup2 server groups."

           String[] expectedServerGroupsWithAppEnabled = [serverGroup1]
           String[] expectedServerGroupsWithAppDisabled = [serverGroup2]

           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
           checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
           checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

           cleanup:
           undeployFromAllRelevantServerGroups(expectedAppName)
       }


       @Unroll
       def "Negative. DeployApplication,1st time, file, non existing server group (C278221)"() {
           String testCaseId = "C278221"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                   deploymentName                 : "$testCaseId-app.war",
                   disabledServerGroups           : "disabled-non-existing-server-group",
                   enabledServerGroups            : "enabled-non-existing-server-group",
                   runtimeName                    : "$testCaseId-app",
                   serverconfig                   : defaultConfigName,
           ]

           setup:
           downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "error"
           assert runProcedureJob.getUpperStepSummary() =~ "Specified non existing server group\\(s\\): enabled-non-existing-server-group disabled-non-existing-server-group. Please add server groups before deploying to them."

       }

          @Unroll
          @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
          def "Negative. DeployApplication, app already deployed, url (for EAP 7 and later) is empty, update app (C278276)"() {
              String testCaseId = "C278276"

              def runParams = [
                      additionalOptions              : '',
                      applicationContentSourcePath   : "--url=",
                      deploymentName                 : "$testCaseId-app",
                      disabledServerGroups           : "$serverGroup1",
                      enabledServerGroups            : "$serverGroup2",
                      runtimeName                    : "$testCaseId-app.war",
                      serverconfig                   : defaultConfigName,
              ]


              setup:
              downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")

              String existingAppName = "$testCaseId-app.war"
              String oldRuntimeName = "$testCaseId-app.war"
              String[] oldServerGroupsWithApp = [serverGroup2]
              deployToServerGroups(oldServerGroupsWithApp, getPathApp()+"$testCaseId-app.war", existingAppName, oldRuntimeName)

              when:
              RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

              then:
              assert runProcedureJob.getStatus() == "error"
              assert runProcedureJob.getUpperStepSummary() =~ "Filesystem path or --url pointing to the deployment is required."

              cleanup:
              existingAppName = "$testCaseId-app.war"
              undeployFromAllRelevantServerGroups(existingAppName)
          }


          @Unroll
          def "Negative. DeployApplication, 1st time, file, not existing path in the 'Path to the application to deploy' (C278218)"() {
              String testCaseId = "C278218"

              def runParams = [
                      additionalOptions              : '',
                      applicationContentSourcePath   : getPathApp()+"wrongpath/$testCaseId-app.war",
                      deploymentName                 : "$testCaseId-app",
                      disabledServerGroups           : "$serverGroup1",
                      enabledServerGroups            : "$serverGroup2",
                      runtimeName                    : "$testCaseId-app.war",
                      serverconfig                   : defaultConfigName,
              ]

              setup:
              downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")

              when:
              RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

              then:
              String expectedAppName = "$testCaseId-app.war"

              assert runProcedureJob.getStatus() == "error"
              assert runProcedureJob.getUpperStepSummary() =~ "File '$runParams.applicationContentSourcePath' doesn't exists"

          }

             @Unroll
             def "DeployApplication, 1st time, file, with not choose server group (C278224)"() {
                 String testCaseId = "C278224"

                 def runParams = [
                         additionalOptions              : '',
                         applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                         deploymentName                 : "$testCaseId-app.war",
                         disabledServerGroups           : '',
                         enabledServerGroups            : '',
                         runtimeName                    : "$testCaseId-app.war",
                         serverconfig                   : defaultConfigName,
                 ]

                 setup:
                 downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")

                 when:
                 RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                 then:
                 String expectedAppName = "$testCaseId-app.war"
                 String expectedRuntimeName = "$testCaseId-app.war"

                 assert runProcedureJob.getStatus() == "success"
                 assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$runParams.applicationContentSourcePath'."

                 checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)
             }


             @Unroll
             def "Negative. DeployApplication, 1st time, file, with specified file without extension (C278226)"() {
                 String testCaseId = "C278226"

                 def runParams = [
                         additionalOptions              : '',
                         applicationContentSourcePath   : getPathApp()+"$testCaseId-app",
                         deploymentName                 : '',
                         disabledServerGroups           : '',
                         enabledServerGroups            : '',
                         runtimeName                    : '',
                         serverconfig                   : defaultConfigName,
                 ]

                 setup:
                 downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app")

                 when:
                 RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

                 then:
                 String expectedAppName = "$testCaseId-app"
                 String expectedRuntimeName = "$testCaseId-app"

                 assert runProcedureJob.getStatus() == "success"
                 assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$runParams.applicationContentSourcePath'."

                 checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)
             }


       @Unroll
       def "DeployApplication, 1st time, file, with wrong additional options (C278227)"() {
           String testCaseId = "C278227"

           def runParams = [
                   additionalOptions              : '--some-wrong-param',
                   applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                   deploymentName                 : '',
                   disabledServerGroups           : "$serverGroup1",
                   enabledServerGroups            : "$serverGroup2",
                   runtimeName                    : '',
                   serverconfig                   : defaultConfigName,
           ]

           setup:
           downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app.war"
           String expectedRuntimeName = "$testCaseId-app.war"
           String expectedContextRoot = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "success"
           assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '$runParams.applicationContentSourcePath'.\nEnabled on: $serverGroup2 server groups.\nDisabled on: $serverGroup1 server groups."

           String[] expectedServerGroupsWithAppEnabled = [serverGroup2]
           String[] expectedServerGroupsWithAppDisabled = [serverGroup1]

           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
           checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

           checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppDisabled)
           checkAppUploadedToContentRepo(expectedAppName, expectedRuntimeName)

           cleanup:
           undeployFromAllRelevantServerGroups(expectedAppName)
       }


       @Unroll
       def "Negative. DeployApplication,  incorrect param, undef required param, path to app (C278229)"() {
           String testCaseId = "C278229"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : '',
                   deploymentName                 : '',
                   disabledServerGroups           : "$serverGroup1",
                   enabledServerGroups            : "$serverGroup2",
                   runtimeName                    : '',
                   serverconfig                   : defaultConfigName,
           ]

           setup:
           downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "error"
           assert runProcedureJob.getUpperStepSummary() =~ "Required parameter 'applicationContentSourcePath' is not provided"

       }


       @Unroll
       @IgnoreIf({ env.JBOSS_VERSION =~ '6.*' })
       def "Negative. DeployApplication,1st time, url incorrect value (for EAP 7 and later) (C278230)"() {
           String testCaseId = "C278230"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : '--url=https://github.com/electric-cloud/incorrect-path/hello-world.war',
                   deploymentName                 : '',
                   disabledServerGroups           : "$serverGroup1",
                   enabledServerGroups            : "$serverGroup2",
                   runtimeName                    : '',
                   serverconfig                   : defaultConfigName,
           ]

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "error"
           assert runProcedureJob.getUpperStepSummary() =~ "Invalid url stream."

       }




       @Unroll
       def "Negative. DeployApplication, 1st time, file, same server group in enabled and disabled server group (C278273)"() {
           String testCaseId = "C278273"

           def runParams = [
                   additionalOptions              : '',
                   applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                   deploymentName                 : '',
                   disabledServerGroups           : "$serverGroup2",
                   enabledServerGroups            : "$serverGroup2",
                   runtimeName                    : '',
                   serverconfig                   : defaultConfigName,
           ]
           setup:
           downloadArtifact(linkToSampleWarFile, getPathApp()+"$testCaseId-app.war")

           when:
           RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

           then:
           String expectedAppName = "$testCaseId-app"

           assert runProcedureJob.getStatus() == "error"
           assert runProcedureJob.getUpperStepSummary() =~ "Duplicated server group\\(s\\) in enabled and disabled lists \\(please check provided parameters\\): $serverGroup2"

       }


    @Unroll
    def "DeployApplication, 1st time, file, enabled --all-server-group (C278546)"() {
        String testCaseId = "C278546"

        def runParams = [
                additionalOptions              : '',
                applicationContentSourcePath   : getPathApp()+"$testCaseId-app.war",
                deploymentName                 : "$testCaseId-app.war",
                disabledServerGroups           : '',
                enabledServerGroups            : '--all-server-groups',
                runtimeName                    : "$testCaseId-app.war",
                serverconfig                   : defaultConfigName,
        ]
        setup:
        downloadArtifact(linkToSampleWarFile, runParams.applicationContentSourcePath)

        when:
        RunProcedureJob runProcedureJob = runProcedureUnderTest(runParams)

        then:
        String expectedAppName = "$testCaseId-app.war"
        String expectedRuntimeName = "$testCaseId-app.war"
        String expectedContextRoot = "$testCaseId-app"

        assert runProcedureJob.getStatus() == "success"
        assert runProcedureJob.getUpperStepSummary() =~ "Application '$expectedAppName' has been successfully deployed from '${runParams.applicationContentSourcePath}'.\nEnabled on: $serverGroup1,$serverGroup2 server groups."

        String[] expectedServerGroupsWithAppEnabled = [serverGroup2,serverGroup1]

        checkAppDeployedToServerGroupsCli(expectedAppName, expectedRuntimeName, expectedServerGroupsWithAppEnabled)
        checkAppDeployedToServerGroupsUrl(expectedContextRoot, expectedServerGroupsWithAppEnabled)

        cleanup:
        undeployFromAllRelevantServerGroups(expectedAppName)
    }

    void checkAppDeployedToServerGroupsCli(String appName, String runtimeName, def serverGroups) { //not working for JBoss 6.4
        for (String serverGroup : serverGroups) {
            checkAppDeployedToServerGroupCli(appName, runtimeName, serverGroup)
        }
    }

    void checkAppDeployedToServerGroupCli(String appName, String runtimeName, String serverGroup) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnServerGroup(serverGroup, appName)).result
        assert result.'name' == appName
        assert result.'runtime-name' == runtimeName
    }

    void checkAppNotDeployedToServerGroups(String appName, String[] serverGroups) {
        for (String serverGroup : serverGroups) {
            checkAppNotDeployedToServerGroup(appName, serverGroup)
        }
    }

    void checkAppNotDeployedToServerGroup(String appName, String serverGroup) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getServerGroupInfo(serverGroup)).result
        assert result.containsKey('deployment') && (!result.'deployment' || !result.'deployment'.keySet().contains(appName))
    }

    void checkAppUploadedToContentRepo(String appName, String runtimeName) {
        def result = runCliCommandAndGetJBossReply(CliCommandsGeneratorHelper.getDeploymentInfoOnContentRepo(appName)).result
        assert result.'name' == appName
        assert result.'runtime-name' == runtimeName
    }

    void checkAppDeployedToServerGroupsUrl(String contextRoot, def serverGroups) {
        checkAppDeployedToServerGroupsUrl(contextRoot, serverGroups, "1")
    }

    void checkAppDeployedToServerGroupsUrl(String contextRoot, def serverGroups, String version) {
        for (String rootUrls : getExpectedRootUrls(serverGroups)) {
            String url = "$rootUrls/$contextRoot/version$version"
            assert isUrlAvailable(url)
        }
    }

    void undeployFromAllRelevantServerGroups(String appName) {
        runCliCommand(CliCommandsGeneratorHelper.undeployFromAllRelevantServerGroups(appName))
    }

    void deployToServerGroups(String[] serverGroups, String filePath, String appName, String runtimeName) {
        runCliCommand(CliCommandsGeneratorHelper.deployToServerGroups(serverGroups, filePath, appName, runtimeName))
    }

    Set<String> getExpectedRootUrls(def serverGroupsWithApp) {
        Set<String> expectedRootUrls = new HashSet<String>()
        for (String serverGroup : serverGroupsWithApp) {
            for (ServerHelper server : getServerGroupModel(serverGroup).getServers()) {
                String hostname = EnvPropertiesHelper.getResourceHostname()
                String port = 8080 + server.getSocketBindingPortOffset()
                String expectedRootUrl = "http://$hostname:$port"
                expectedRootUrls.add(expectedRootUrl)
            }
        }
        return expectedRootUrls
    }

    void createServerGroupModels() {
        serverGroup1Model = new ServerGroupHelper(serverGroup1)
        serverGroup2Model = new ServerGroupHelper(serverGroup2)
        ServerHelper serverGroup1Server1Model = new ServerHelper(serverGroup1Server1, serverGroup1, hostNameMaster)
        serverGroup1Server1Model.setSocketBindingPortOffset(0)
        ServerHelper serverGroup1Server2Model = new ServerHelper(serverGroup1Server2, serverGroup1, hostNameMaster)
        serverGroup1Server2Model.setSocketBindingPortOffset(150)
        ServerHelper serverGroup2Server1Model = new ServerHelper(serverGroup2Server1, serverGroup2, hostNameMaster)
        serverGroup2Server1Model.setSocketBindingPortOffset(250)
        serverGroup1Model.addServer(serverGroup1Server1Model)
        serverGroup1Model.addServer(serverGroup1Server2Model)
        serverGroup2Model.addServer(serverGroup2Server1Model)
    }

    ServerGroupHelper getServerGroupModel(String serverGroupName) {
        switch (serverGroupName) {
            case serverGroup1: return serverGroup1Model
            case serverGroup2: return serverGroup2Model
            default: throw new Exception("Unknown server group")
        }
    }
}