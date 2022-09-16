package Zeze.Arch;

public class LoadConfig {
	private int maxOnlineNew = 30;
	private int approximatelyLinkdCount = 4; // 大致的Linkd数量。在Provider报告期间，用来估算负载均衡。

	private int reportDelaySeconds = 10;
	private int proposeMaxOnline = 15000;
	private int digestionDelayExSeconds = 2;

	public final int getMaxOnlineNew() {
		return maxOnlineNew;
	}

	public final void setMaxOnlineNew(int value) {
		maxOnlineNew = value;
	}

	public final int getApproximatelyLinkdCount() {
		return approximatelyLinkdCount;
	}

	public final void setApproximatelyLinkdCount(int value) {
		approximatelyLinkdCount = value;
	}

	public final int getReportDelaySeconds() {
		return reportDelaySeconds;
	}

	public final void setReportDelaySeconds(int value) {
		reportDelaySeconds = value;
	}

	public final int getProposeMaxOnline() {
		return proposeMaxOnline;
	}

	public final void setProposeMaxOnline(int value) {
		proposeMaxOnline = value;
	}

	public final int getDigestionDelayExSeconds() {
		return digestionDelayExSeconds;
	}

	public final void setDigestionDelayExSeconds(int value) {
		digestionDelayExSeconds = value;
	}
}
