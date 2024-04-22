package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BServiceInfo extends Bean implements Comparable<BServiceInfo> {
	/**
	 * 服务名，比如"GameServer"
	 */
	public @NotNull String serviceName;
	public long version;

	/**
	 * 服务id，对于 Zeze.Application，一般就是 Config.ServerId.
	 * 这里使用类型 string 是为了更好的支持扩展。
	 */
	public @NotNull String serviceIdentity;

	/**
	 * 服务ip-port，如果没有，保持空和0.
	 */
	private @NotNull String passiveIp = "";
	private int passivePort;

	// 服务扩展信息，可选。
	private @NotNull Binary extraInfo = Binary.Empty;

	// ServiceManager用来存储服务器的SessionId。算是一个优化吧。
	public @Nullable Long sessionId;

	public @NotNull String getServiceName() {
		return serviceName;
	}

	public @NotNull String getServiceIdentity() {
		return serviceIdentity;
	}

	public @NotNull String getPassiveIp() {
		return passiveIp;
	}

	public long getVersion() {
		return version;
	}

	public void setPassiveIp(@NotNull String value) {
		passiveIp = value;
	}

	public int getPassivePort() {
		return passivePort;
	}

	public void setPassivePort(int value) {
		passivePort = value;
	}

	public @NotNull Binary getExtraInfo() {
		return extraInfo;
	}

	public void setExtraInfo(@Nullable Binary value) {
		extraInfo = value != null ? value : Binary.Empty;
	}

	public BServiceInfo() {
		this("", "", 0,null, 0, null);
	}

	public BServiceInfo(@NotNull String name, @NotNull String identity, long version, @Nullable String ip, int port) {
		this(name, identity, version, ip, port, null);
	}

	public BServiceInfo(@NotNull String name, @NotNull String identity, long version, @Nullable String ip) {
		this(name, identity, version, ip, 0, null);
	}

	public BServiceInfo(@NotNull String name, @NotNull String identity, long version) {
		this(name, identity, version, null, 0, null);
	}

	public BServiceInfo(@NotNull String name, @NotNull String identity, long version,
						@Nullable String ip, int port,
						@Nullable Binary extraInfo) {
		serviceName = name;
		serviceIdentity = identity;
		this.version = version;
		if (ip != null)
			passiveIp = ip;
		passivePort = port;
		if (extraInfo != null)
			this.extraInfo = extraInfo;
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		serviceName = bb.ReadString();
		serviceIdentity = bb.ReadString();
		passiveIp = bb.ReadString();
		passivePort = bb.ReadInt();
		extraInfo = bb.ReadBinary();
		this.version = bb.ReadLong();
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteString(getServiceName());
		bb.WriteString(getServiceIdentity());
		bb.WriteString(getPassiveIp());
		bb.WriteInt(getPassivePort());
		bb.WriteBinary(getExtraInfo());
		bb.WriteLong(version);
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 17;
		result = prime * result + getServiceName().hashCode();
		result = prime * result + getServiceIdentity().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj instanceof BServiceInfo) {
			var other = (BServiceInfo)obj;
			// 版本分类存储，同一个集合的版本肯定相等，这里不需要判断版本。
			return getServiceName().equals(other.getServiceName())
					&& getServiceIdentity().equals(other.getServiceIdentity());
		}
		return false;
	}

	@Override
	public int compareTo(BServiceInfo o) {
		int c = serviceName.compareTo(o.serviceName);
		if (c != 0)
			return c;

		return serviceIdentity.compareTo(o.serviceIdentity);
	}

	@Override
	public @NotNull String toString() {
		return "BServiceInfo{" + "ServiceName='" + serviceName + '\''
				+ ", ServiceIdentity='" + serviceIdentity + '\'' +
				", Version=" + version +
				", PassiveIp='" + passiveIp + '\'' + ", PassivePort=" + passivePort
				+ ", ExtraInfo=" + extraInfo + '}';
	}
}
