package com.cloudbees.pdk.hen.procedures

import groovy.transform.AutoClone
import com.cloudbees.pdk.hen.*
import com.cloudbees.pdk.hen.*

@AutoClone
//generated
class DeleteDatasource extends Procedure {

    static DeleteDatasource create(Plugin plugin) {
        return new DeleteDatasource(procedureName: 'DeleteDatasource', plugin: plugin, )
    }


    DeleteDatasource flush() {
        this.flushParams()
        this.contextUser = null
        return this
    }

    DeleteDatasource withUser(User user) {
        this.contextUser = user
        return this
    }

    //Generated
    
    DeleteDatasource datasourcename(String datasourcename) {
        this.addParam('datasource_name', datasourcename)
        return this
    }
    
    
    DeleteDatasource profile(String profile) {
        this.addParam('profile', profile)
        return this
    }
    
    
    DeleteDatasource scriptphysicalpath(String scriptphysicalpath) {
        this.addParam('scriptphysicalpath', scriptphysicalpath)
        return this
    }
    
    
    DeleteDatasource serverconfig(String serverconfig) {
        this.addParam('serverconfig', serverconfig)
        return this
    }
    
    
    
    
}