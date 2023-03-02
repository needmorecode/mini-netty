package netty.v2;

/**
 * 定时任务
 */
public class FutureTask implements Comparable<FutureTask>{
	
	/**
	 * 执行时间（毫秒）
	 */
	private long execTime;
	
	/**
	 * 具体任务
	 */
	private Runnable command;
	
	public FutureTask(long execTime, Runnable command) {
		this.execTime = execTime;
		this.command = command;
	}
	
	/**
	 * 是否过期
	 */
	public boolean isExpired() {
		return System.currentTimeMillis() >= execTime;
	}

	@Override
	public int compareTo(FutureTask task) {
		return Long.compare(this.execTime, task.execTime);
	}
	
	public Runnable getCommand() {
		return command;
	}
	
	/**
	 * 定时任务执行
	 */
	public void run() {
		command.run();
	}
	
	public long getExecTime() {
		return this.execTime;
	}
	

}
