package net.lycorissystem.modpack_updater.versioning;

import java.time.ZonedDateTime;

public class VersionInfo {
	String versionName;         //版本名称
	String nextVersionName;     //下一个版本名称
	boolean isLatest;           //是否为最新版本
	boolean isPrevious;         //是否为最新版本的前一个版本
	boolean isEndOfLife;        //是否为终止支持版本
	String packageURL;          //包地址
	String checksumURL;         //校验和地址
	long versionDeployTime;     //版本部署时间（换算为系统时间，单位为毫秒）
	
	public VersionInfo(String versionName, String nextVersionName, boolean isLatest, boolean isPrevious, boolean isEndOfLife, String packageURL, String checksumURL, long versionDeployTime) {
		this.versionName = versionName;
		this.nextVersionName = nextVersionName;
		this.isLatest = isLatest;
		this.isPrevious = isPrevious;
		this.isEndOfLife = isEndOfLife;
		this.packageURL = packageURL;
		this.checksumURL = checksumURL;
		this.versionDeployTime = versionDeployTime;
	}
	
	public VersionInfo(String versionName, String nextVersionName, boolean isLatest, boolean isPrevious, boolean isEndOfLife, String packageURL, String checksumURL, ZonedDateTime versionDeployTime) {
		this.versionName = versionName;
		this.nextVersionName = nextVersionName;
		this.isLatest = isLatest;
		this.isPrevious = isPrevious;
		this.isEndOfLife = isEndOfLife;
		this.packageURL = packageURL;
		this.checksumURL = checksumURL;
		this.versionDeployTime = versionDeployTime.toInstant().toEpochMilli();
	}
	
	public String getVersionName() {
		return versionName;
	}
	
	public String getNextVersionName() {
		return nextVersionName;
	}
	
	public boolean isLatest() {
		return isLatest;
	}
	
	public boolean isEndOfLife() {
		return isEndOfLife;
	}
	
	public String getPackageURL() {
		return packageURL;
	}
	
	public String getChecksumURL() {
		return checksumURL;
	}
	
	public long getVersionDeployTime() {
		return versionDeployTime;
	}
	
	public boolean isPrevious() {
		return isPrevious;
	}
	
	@Override
	public String toString() {
		return "VersionInfo{" +
		       "versionName='" + versionName + '\'' +
		       ", nextVersionName='" + nextVersionName + '\'' +
		       ", isLatest=" + isLatest +
		       ", isPrevious=" + isPrevious +
		       ", isEndOfLife=" + isEndOfLife +
		       ", packageURL='" + packageURL + '\'' +
		       ", checksumURL='" + checksumURL + '\'' +
		       ", versionDeployTime=" + versionDeployTime +
		       '}';
	}
}
