package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CheckServerStatus extends Procedure {

    static CheckServerStatus create(Plugin plugin) {
        return new CheckServerStatus(procedureName: 'CheckServerStatus', plugin: plugin, )
    }


    CheckServerStatus flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CheckServerStatus withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CheckServerStatus criteria(String criteria) {
        this.addParam('criteria', criteria)
        return this
    }
    
    CheckServerStatus criteria(CriteriaOptions criteria) {
        this.addParam('criteria', criteria.toString())
        return this
    }
    
    
    CheckServerStatus host(String host) {
        this.addParam('host', host)
        return this
    }
    
    
    CheckServerStatus scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    CheckServerStatus server(String server) {
        this.addParam('server', server)
        return this
    }
    
    
    CheckServerStatus serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    CheckServerStatus urlcheck(boolean urlcheck) {
        this.addParam('url_check', urlcheck)
        return this
    }
    
    
    CheckServerStatus waittime(String waittime) {
        this.addParam('wait_time', waittime)
        return this
    }
    
    
    
    
    enum CriteriaOptions {
    
    RUNNING("RUNNING"),
    
    NOT_RUNNING("NOT_RUNNING")
    
    private String value
    CriteriaOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}