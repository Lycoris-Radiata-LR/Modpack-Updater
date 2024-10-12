package net.lycorissystem.modpack_updater.updating;

public class UpdatePack {
	String version;
	SidedUpdatePack common;
	SidedUpdatePack client;
	SidedUpdatePack server;
	
	public UpdatePack(String version, SidedUpdatePack common, SidedUpdatePack client, SidedUpdatePack server) {
		this.version = version;
		this.common = common;
		this.client = client;
		this.server = server;
	}
	
	public String getVersion() {
		return version;
	}
	
	public SidedUpdatePack getCommon() {
		return common;
	}
	
	public SidedUpdatePack getClient() {
		return client;
	}
	
	public SidedUpdatePack getServer() {
		return server;
	}
	
	@Override
	public String toString() {
		return "UpdatePack{" +
		       "version='" + version + '\'' +
		       ", common=" + common +
		       ", client=" + client +
		       ", server=" + server +
		       '}';
	}
}
