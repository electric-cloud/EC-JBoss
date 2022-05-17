package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class GetEnvInfo extends Procedure {

    static GetEnvInfo create(Plugin plugin) {
        return new GetEnvInfo(procedureName: 'GetEnvInfo', plugin: plugin, )
    }


    GetEnvInfo flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    GetEnvInfo withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    GetEnvInfo additionalOptions(String additionalOptions) {
        this.addParam('additionalOptions', additionalOptions)
        return this
    }
    
    
    GetEnvInfo informationType(String informationType) {
        this.addParam('informationType', informationType)
        return this
    }
    
    GetEnvInfo informationType(InformationTypeOptions informationType) {
        this.addParam('informationType', informationType.toString())
        return this
    }
    
    
    GetEnvInfo informationTypeContext(String informationTypeContext) {
        this.addParam('informationTypeContext', informationTypeContext)
        return this
    }
    
    
    GetEnvInfo serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
    enum InformationTypeOptions {
    
    SYSTEM_DUMP("systemDump"),
    
    PROFILES("profiles"),
    
    DATA_SOURCES("dataSources"),
    
    XA_DATA_SOURCES("xaDataSources")
    
    private String value
    InformationTypeOptions(String value) {
        this.value = value
    }

    String toString() {
        return this.value
    }
}
    
}