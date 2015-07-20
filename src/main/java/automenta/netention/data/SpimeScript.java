/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.data;

import automenta.netention.geo.SpimeBase;

/**
 * Provides a javascript context with a DB reference for querying, populating, and/or transforming it
 */
public class SpimeScript extends JSScript {

    private final SpimeBase db;

    public SpimeScript(SpimeBase db) {
        super();
        this.db = db;

        js.put("db", db);
    }


}
