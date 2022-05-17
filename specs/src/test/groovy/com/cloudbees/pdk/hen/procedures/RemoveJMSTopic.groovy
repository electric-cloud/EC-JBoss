package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class RemoveJMSTopic extends Procedure {

    static RemoveJMSTopic create(Plugin plugin) {
        return new RemoveJMSTopic(procedureName: 'RemoveJMSTopic', plugin: plugin, )
    }


    RemoveJMSTopic flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    RemoveJMSTopic withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    RemoveJMSTopic profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    RemoveJMSTopic serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    RemoveJMSTopic topicName(String topicName) {
        this.addParam('topicName', topicName)
        return this
    }
    
    
    
    
}