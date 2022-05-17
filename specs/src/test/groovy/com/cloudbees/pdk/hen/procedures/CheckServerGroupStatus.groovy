package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CheckServerGroupStatus extends Procedure {

    static CheckServerGroupStatus create(Plugin plugin) {
        return new CheckServerGroupStatus(procedureName: 'CheckServerGroupStatus', plugin: plugin, )
    }


    CheckServerGroupStatus flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CheckServerGroupStatus withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CheckServerGroupStatus criteria(String criteria) {
        this.addParam('criteria', criteria)
        return this
    }
    
    CheckServerGroupStatus criteria(CriteriaOptions criteria) {
        this.addParam('criteria', criteria.toString())
        return this
    }
    
    
    CheckServerGroupStatus scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    CheckServerGroupStatus serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    CheckServerGroupStatus serversgroup(String serversgroup) {
        this.addParam('serversgroup', serversgroup)
        return this
    }
    
    
    CheckServerGroupStatus waittime(String waittime) {
        this.addParam('wait_time', waittime)
        return this
    }
    
    
    
    
    enum CriteriaOptions {
    
    STARTED("STARTED"),
    
    STOPPED("STOPPED"),
    
    DISABLED("DISABLED"),
    
    STOPPED_OR_DISABLED("STOPPED_OR_DISABLED")
    
    private String value
    CriteriaOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}