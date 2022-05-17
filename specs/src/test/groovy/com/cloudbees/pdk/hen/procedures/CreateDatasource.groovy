package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class CreateDatasource extends Procedure {

    static CreateDatasource create(Plugin plugin) {
        return new CreateDatasource(procedureName: 'CreateDatasource', plugin: plugin, credentials: [
            
            'ds_credential': null,
            
        ])
    }


    CreateDatasource flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    CreateDatasource withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    CreateDatasource applicationname(String applicationname) {
        this.addParam('application_name', applicationname)
        return this
    }
    
    
    CreateDatasource connectionURL(String connectionURL) {
        this.addParam('connectionURL', connectionURL)
        return this
    }
    
    
    CreateDatasource driverClass(String driverClass) {
        this.addParam('driverClass', driverClass)
        return this
    }
    
    
    CreateDatasource driverName(String driverName) {
        this.addParam('driverName', driverName)
        return this
    }
    
    
    CreateDatasource enabled(boolean enabled) {
        this.addParam('enabled', enabled)
        return this
    }
    
    
    CreateDatasource jndiName(String jndiName) {
        this.addParam('jndiName', jndiName)
        return this
    }
    
    
    CreateDatasource profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    CreateDatasource scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    CreateDatasource serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    CreateDatasource ds_credential(String user, String password) {
        this.addCredential('ds_credential', user, password)
        return this
    }

    CreateDatasource ds_credentialReference(String path) {
        this.addCredentialReference('ds_credential', path)
        return this
    }
    
    
}