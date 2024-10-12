package net.lycorissystem.modpack_updater.versioning;

import java.util.HashMap;

public class VersionManifest {

	HashMap<String, VersionInfo> versions = new HashMap<>();
	String defaultVersion;
	
	public VersionManifest(String defaultVersion) {
		this.defaultVersion = defaultVersion;
	}
	
	public void addVersion(VersionInfo versionInfo) {
		versions.put(versionInfo.getVersionName(), versionInfo);
	}
	
	@Override
	public String toString() {
		return "VersionManifest{" +
		       "versions=" + versions.toString() +
		       ", defaultVersion='" + defaultVersion + '\'' +
		       '}';
	}
}
