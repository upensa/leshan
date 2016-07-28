/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.cluster.serialization;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.server.client.ClientUpdate;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Functions for serialize and deserialize a ClientUpdate in JSON.
 */
public class ClientUpdateSerDes {

    public static JsonObject jSerialize(ClientUpdate c) {
        JsonObject o = Json.object();

        // mandatory fields
        o.add("regId", c.getRegistrationId());
        o.add("address", c.getAddress().getHostAddress());
        o.add("port", c.getPort());

        // optional fields
        if (c.getLifeTimeInSec() != null)
            o.add("lt", c.getLifeTimeInSec());
        if (c.getSmsNumber() != null)
            o.add("sms", c.getSmsNumber());
        if (c.getBindingMode() != null)
            o.add("bnd", c.getBindingMode().name());
        if (c.getObjectLinks() != null) {
            JsonArray links = new JsonArray();
            for (LinkObject l : c.getObjectLinks()) {
                JsonObject ol = Json.object();
                ol.add("url", l.getUrl());
                JsonObject at = Json.object();
                for (Map.Entry<String, Object> e : l.getAttributes().entrySet()) {
                    if (e.getValue() instanceof Integer) {
                        at.add(e.getKey(), (int) e.getValue());
                    } else {
                        at.add(e.getKey(), e.getValue().toString());
                    }
                }
                ol.add("at", at);
                links.add(ol);
            }
            o.add("objLink", links);
        }

        return o;
    }

    public static String sSerialize(ClientUpdate c) {
        return jSerialize(c).toString();
    }

    public static byte[] bSerialize(ClientUpdate c) {
        return jSerialize(c).toString().getBytes();
    }

    public static ClientUpdate deserialize(byte[] data) throws UnknownHostException {
        JsonObject v = (JsonObject) Json.parse(new String(data));

        // mandatory fields
        String regId = v.getString("regId", null);
        InetAddress addr = InetAddress.getByName(v.getString("address", null));
        int port = v.getInt("port", -1);

        // optional fields
        BindingMode b = null;
        if (v.get("bnd") != null) {
            b = BindingMode.valueOf(v.getString("bnd", null));
        }
        Long lifetime = null;
        if (v.get("lt") != null) {
            lifetime = v.getLong("lt", 0);
        }
        String sms = null;
        if (v.get("sms") != null) {
            sms = v.getString("sms", "");
        }

        // parse object link
        JsonArray links = (JsonArray) v.get("objLink");
        LinkObject[] linkObjs = null;
        if (links != null) {
            linkObjs = new LinkObject[links.size()];
            for (int i = 0; i < links.size(); i++) {
                JsonObject ol = (JsonObject) links.get(i);

                Map<String, Object> attMap = new HashMap<>();
                JsonObject att = (JsonObject) ol.get("at");
                for (String k : att.names()) {
                    JsonValue jsonValue = att.get(k);
                    if (jsonValue.isNumber()) {
                        attMap.put(k, jsonValue.asInt());
                    } else {
                        attMap.put(k, jsonValue.asString());
                    }
                }
                LinkObject o = new LinkObject(ol.getString("url", null), attMap);
                linkObjs[i] = o;
            }
        }

        return new ClientUpdate(regId, addr, port, lifetime, sms, b, linkObjs);
    }
}