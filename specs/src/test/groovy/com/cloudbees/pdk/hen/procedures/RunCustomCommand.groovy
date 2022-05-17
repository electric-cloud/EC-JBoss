package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class RunCustomCommand extends Procedure {

    static RunCustomCommand create(Plugin plugin) {
        return new RunCustomCommand(procedureName: 'RunCustomCommand', plugin: plugin, )
    }


    RunCustomCommand flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RunCustomCommand withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    RunCustomCommand customCommand(String customCommand) {
        this.addParam('customCommand', customCommand)
        return this
    }
    
    
    RunCustomCommand scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    RunCustomCommand serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}