package sernet.gs.reveng;

// Generated Jun 5, 2015 1:28:30 PM by Hibernate Tools 3.4.0.CR1

/**
 * CmMasState generated by hbm2java
 */
public class CmMasState implements java.io.Serializable {

	private CmMasStateId id;
	private MsCmState msCmState;
	private byte cmVerId2;
	private String cmUsername;
	private String guid;

	public CmMasState() {
	}

	public CmMasState(CmMasStateId id, MsCmState msCmState, byte cmVerId2,
			String cmUsername, String guid) {
		this.id = id;
		this.msCmState = msCmState;
		this.cmVerId2 = cmVerId2;
		this.cmUsername = cmUsername;
		this.guid = guid;
	}

	public CmMasStateId getId() {
		return this.id;
	}

	public void setId(CmMasStateId id) {
		this.id = id;
	}

	public MsCmState getMsCmState() {
		return this.msCmState;
	}

	public void setMsCmState(MsCmState msCmState) {
		this.msCmState = msCmState;
	}

	public byte getCmVerId2() {
		return this.cmVerId2;
	}

	public void setCmVerId2(byte cmVerId2) {
		this.cmVerId2 = cmVerId2;
	}

	public String getCmUsername() {
		return this.cmUsername;
	}

	public void setCmUsername(String cmUsername) {
		this.cmUsername = cmUsername;
	}

	public String getGuid() {
		return this.guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

}
