package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class DeployApplication extends Procedure {

    static DeployApplication create(Plugin plugin) {
        return new DeployApplication(procedureName: 'DeployApplication', plugin: plugin, )
    }


    DeployApplication flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    DeployApplication withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    DeployApplication additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    DeployApplication applicationContentSourcePath(String applicationContentSourcePath) {
        this.addParam('applicationContentSourcePath', applicationContentSourcePath)
        return this
    }
    
    
    DeployApplication deploymentName(String deploymentName) {
        this.addParam('deploymentName', deploymentName)
        return this
    }
    
    
    DeployApplication disabledServerGroups(String disabledServerGroups) {
        this.addParam('disabledServerGroups', disabledServerGroups)
        return this
    }
    
    
    DeployApplication enabledServerGroups(String enabledServerGroups) {
        this.addParam('enabledServerGroups', enabledServerGroups)
        return this
    }
    
    
    DeployApplication runtimeName(String runtimeName) {
        this.addParam('runtimeName', runtimeName)
        return this
    }
    
    
    DeployApplication serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}