package Zeze.Net;

public final class SocketOptions {
	// 系统选项
	private Boolean noDelay; // 不指定的话由系统提供默认值
	private Integer sendBuffer; // 不指定的话由系统提供默认值
	private Integer receiveBuffer; // 不指定的话由系统提供默认值
	private int backlog = 128; // 只有 ServerSocket 使用

	// 应用选项
	private int inputBufferMaxProtocolSize = 2 * 1024 * 1024; // 最大协议包的大小。协议需要完整收到才解析和处理，所以需要缓存。这是个安全选项。防止出现攻击占用大量内存。
	private long outputBufferMaxSize = 2 * 1024 * 1024; // 最大发送协议堆积大小. 用于Service.checkOverflow

	private String timeThrottle;
	private Integer timeThrottleSeconds;
	private Integer timeThrottleLimit;
	private Integer timeThrottleBandwidth;
	private Long overBandwidth;
	private double overBandwidthFusingRate = 1.0;
	private double overBandwidthNormalRate = 0.7;

	public double getOverBandwidthFusingRate() {
		return overBandwidthFusingRate;
	}

	public double getOverBandwidthNormalRate() {
		return overBandwidthNormalRate;
	}

	public void setOverBandwidthFusingRate(double value) {
		overBandwidthFusingRate = value;
	}

	public void setOverBandwidthNormalRate(double value) {
		overBandwidthNormalRate = value;
	}

	/**
	 * Service最大熔断输出带宽（字节）。当达到时会熔断（拒绝所有的请求）
	 * @return 熔断输出带宽。
	 */
	public Long getOverBandwidth() {
		return overBandwidth;
	}

	public void setOverBandwidth(long value) {
		overBandwidth = value;
	}

	public String getTimeThrottle() {
		return timeThrottle;
	}

	public Integer getTimeThrottleBandwidth() {
		return timeThrottleBandwidth;
	}

	public void setTimeThrottleBandwidth(int band) {
		timeThrottleBandwidth = band;
	}

	public void setTimeThrottle(String name) {
		timeThrottle = name;
	}

	public Integer getTimeThrottleSeconds() {
		return timeThrottleSeconds;
	}

	public Integer getTimeThrottleLimit() {
		return timeThrottleLimit;
	}

	public void setTimeThrottleSeconds(int seconds) {
		timeThrottleSeconds = seconds;
	}

	public void setTimeThrottleLimit(int limit) {
		timeThrottleLimit = limit;
	}

	public Boolean getNoDelay() {
		return noDelay;
	}

	public void setNoDelay(Boolean value) {
		noDelay = value;
	}

	public Integer getSendBuffer() {
		return sendBuffer;
	}

	public void setSendBuffer(Integer value) {
		sendBuffer = value;
	}

	public Integer getReceiveBuffer() {
		return receiveBuffer;
	}

	public void setReceiveBuffer(Integer value) {
		receiveBuffer = value;
	}

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int value) {
		backlog = value;
	}

	public int getInputBufferMaxProtocolSize() {
		return inputBufferMaxProtocolSize;
	}

	public void setInputBufferMaxProtocolSize(int value) {
		inputBufferMaxProtocolSize = value;
	}

	public long getOutputBufferMaxSize() {
		return outputBufferMaxSize;
	}

	public void setOutputBufferMaxSize(long value) {
		outputBufferMaxSize = value;
	}
}
