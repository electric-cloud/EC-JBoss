package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CreateOrUpdateJMSTopic extends Procedure {

    static CreateOrUpdateJMSTopic create(Plugin plugin) {
        return new CreateOrUpdateJMSTopic(procedureName: 'CreateOrUpdateJMSTopic', plugin: plugin, )
    }


    CreateOrUpdateJMSTopic flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CreateOrUpdateJMSTopic withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CreateOrUpdateJMSTopic additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    CreateOrUpdateJMSTopic jndiNames(String jndiNames) {
        this.addParam('jndiNames', jndiNames)
        return this
    }
    
    
    CreateOrUpdateJMSTopic profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    CreateOrUpdateJMSTopic serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    CreateOrUpdateJMSTopic topicName(String topicName) {
        this.addParam('topicName', topicName)
        return this
    }
    
    
    
    
}