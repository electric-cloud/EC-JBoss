package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class StartDomainServer extends Procedure {

    static StartDomainServer create(Plugin plugin) {
        return new StartDomainServer(procedureName: 'StartDomainServer', plugin: plugin, )
    }


    StartDomainServer flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    StartDomainServer withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    StartDomainServer alternatejbossconfig(String alternatejbossconfig) {
        this.addParam('alternatejbossconfig', alternatejbossconfig)
        return this
    }
    
    
    StartDomainServer alternateJBossConfigHost(String alternateJBossConfigHost) {
        this.addParam('alternateJBossConfigHost', alternateJBossConfigHost)
        return this
    }
    
    
    StartDomainServer scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    StartDomainServer serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}