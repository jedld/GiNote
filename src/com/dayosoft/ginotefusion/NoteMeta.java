package com.dayosoft.ginotefusion;

import java.io.Serializable;

public class NoteMeta implements Serializable {

	public static final int GOOGLEMAPSURL = 1;
	public static final int LOCATIONNAME = 2;
	public static final int IMAGE = 3;

	int type;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getResource_url() {
		return resource_url;
	}

	public void setResource_url(String resource_url) {
		this.resource_url = resource_url;
	}

	int id;
	int note_id;
	String resource_url;

}
