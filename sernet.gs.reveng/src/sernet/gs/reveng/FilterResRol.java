package sernet.gs.reveng;

// Generated Jun 5, 2015 1:28:30 PM by Hibernate Tools 3.4.0.CR1

/**
 * FilterResRol generated by hbm2java
 */
public class FilterResRol implements java.io.Serializable {

	private FilterResRolId id;
	private short spid;

	public FilterResRol() {
	}

	public FilterResRol(FilterResRolId id, short spid) {
		this.id = id;
		this.spid = spid;
	}

	public FilterResRolId getId() {
		return this.id;
	}

	public void setId(FilterResRolId id) {
		this.id = id;
	}

	public short getSpid() {
		return this.spid;
	}

	public void setSpid(short spid) {
		this.spid = spid;
	}

}