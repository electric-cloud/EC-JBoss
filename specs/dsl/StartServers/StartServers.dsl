def projName = args.projName
def resName = args.resName

project projName, {
    procedure 'Start Servers Procedure', {
        description = ''
        jobNameTemplate = ''
        projectName = projName
        resourceName = resName
        timeLimit = ''
        timeLimitUnits = 'minutes'
        workspaceName = ''

        step 'Start Servers Step', {
            subprocedure = 'StartServers'
            subproject = '/plugins/EC-JBoss/project'
            actualParameter 'serverconfig', '$[configName]'
            actualParameter 'scriptphysicalpath', '$[cliPath]'
            actualParameter 'serversgroup', '$[serverGroup]'
            actualParameter 'wait_time', '$[waitTime]'
        }

        formalParameter 'configName', defaultValue: 'specConfig', {
            type = 'entry'
        }
        formalParameter 'cliPath', defaultValue: '', {
            type = 'entry'
        }
        formalParameter 'serverGroup', defaultValue: '', {
            type = 'entry'
        }
        formalParameter 'waitTime', defaultValue: '300', {
            type = 'entry'
        }
    }
}