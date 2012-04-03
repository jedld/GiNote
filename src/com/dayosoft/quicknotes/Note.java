package com.dayosoft.quicknotes;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class Note implements Serializable{
	int id;
	String title, content, uid;
	long sync_ts;
	public long getSync_ts() {
		return sync_ts;
	}

	public void setSync_ts(long sync_ts) {
		this.sync_ts = sync_ts;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	double longitude, latitude;
	Vector<NoteMeta> meta = new Vector<NoteMeta>();

	public List<NoteMeta> getMeta() {
		return meta;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getDate_created() {
		return date_created;
	}

	public void setDate_created(Date date_created) {
		this.date_created = date_created;
	}

	public Date getDate_updated() {
		return date_updated;
	}

	public void setDate_updated(Date date_updated) {
		this.date_updated = date_updated;
	}

	Date date_created, date_updated;

	public void addMeta(NoteMeta noteMeta) {
		meta.add(noteMeta);
	}

	public void clearMeta(int type) {
		Vector<NoteMeta> list = new Vector<NoteMeta>();
		for (NoteMeta notemeta : meta) {
			if (notemeta.type != type) {
				list.add(notemeta);
			}
		}
		meta = list;
	}

	public List<NoteMeta> getMeta(int type) {
		Vector<NoteMeta> list = new Vector<NoteMeta>();
		for (NoteMeta notemeta : meta) {
			if (notemeta.type == type) {
				list.add(notemeta);
			}
		}
		return list;
	}

}
