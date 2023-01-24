package netty.v2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jctools.queues.MpscUnboundedArrayQueue;

/**
 * worker线程池中的线程
 * 功能类似于Netty中的同名类
 */
public class NioEventLoop extends Thread {
	
    private static final long AWAKE = -1L;
    private static final long NONE = Long.MAX_VALUE;

	/**
	 * 下一个唤醒时间点（毫秒）
	 * 
	 * 可能的值有：
	 * AWAKE	当EL处于非阻塞状态
	 * NONE		当EL处于阻塞状态，且没有已安排的唤醒时间点
	 * 其他值	T	当EL处于阻塞状态，且已安排好唤醒时间点T
	 */
    private final AtomicLong nextWakeupMillis = new AtomicLong(AWAKE);
	
	/**
	 * 任务队列
	 */
	private Queue<Runnable> taskQueue = new MpscUnboundedArrayQueue<>(1024);
	
	/**
	 * 定时任务队列
	 */
	private Queue<FutureTask> futureTaskQueue = new PriorityQueue<>();
	
	public NioEventLoop() {
		try {
			//构造方法中初始化自己的selector
			this.selector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Selector selector;
	
	public Selector getSelector() {
		return this.selector;
	}
	
	/**
	 * 添加任务（默认立即执行）
	 */
	public void addTask(Runnable task) {
		this.addTask(task, true);
	}
	
	/**
	 * 添加任务
	 * 
	 * 可指定是否立即执行，若立即执行，则尝试唤醒selector
	 */
	private void addTask(Runnable task, boolean immediate) {
		taskQueue.offer(task);
		if (immediate) {
			wakeup(inEventLoop());
		}
	}
	
	/**
	 * 唤醒selector
	 */
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && nextWakeupMillis.getAndSet(AWAKE) != AWAKE) {
            selector.wakeup();
        }
    }
	
	/**
	 * 添加定时任务
	 */
	public void scheduleFutureTask(FutureTask task) {
		if (inEventLoop()) {
			futureTaskQueue.offer(task);
		} else {
			final long execTime = task.getExecTime();
			if (execTime < nextWakeupMillis.get()) {
				this.addTask(() -> {
					scheduleFutureTask(task);
				});
			} else {
				this.addTask(() -> {
					scheduleFutureTask(task);
				}, false);
			}
		}
	} 
	
	/**
	 * 延时添加任务
	 */
    public void schedule(Runnable command, long delay, TimeUnit unit) {
    	long aftMillis = unit.toMillis(delay);
    	long execTime = System.currentTimeMillis() + aftMillis;
    	FutureTask task = new FutureTask(execTime, command);
    	this.scheduleFutureTask(task);
    }
    
	/**
	 * 判断当前线程是否就是本event loop
	 */
    public boolean inEventLoop() {
        return Thread.currentThread() == this;
    }
	
	@Override
	public void run() {
	       while (true) {
	    	   int selectNum = 0;
	    	   try {
	    		    //根据无定时任务、定时任务已过期、未过期三种情况做不同处理
		    	   	FutureTask ft = futureTaskQueue.peek();
		    	   	if (ft != null) {
		    	   		nextWakeupMillis.set(ft.getExecTime());
		    	   		long leftTime = ft.getExecTime() - System.currentTimeMillis();
		    	   		if (leftTime > 0) {
		    	   			selectNum = selector.select(leftTime);
		    	   		} else {
		    	   			selectNum = selector.selectNow();
		    	   		}
		    	   	} else {
		    	   		nextWakeupMillis.set(NONE);
		    	   		selectNum = selector.select();
		    	   	}
				} catch (IOException e) {
					e.printStackTrace();
				}  
	    	    if (selectNum > 0) {
		            //监听所有通道
		            //遍历selectionKeys
		            Set<SelectionKey> selectionKeys = selector.selectedKeys();
		            Iterator<SelectionKey> iterator = selectionKeys.iterator();
		            while (iterator.hasNext()) {
		                SelectionKey key = iterator.next();
		                if (key.isReadable()) {  //处理读取事件
		                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
		                    SocketChannel channel = (SocketChannel) key.channel();
		                    try {
								int len = channel.read(byteBuffer);
								if (len >= 0) {
									byte[] array = byteBuffer.array();
									String msg = new String(array);
									System.out.println("client:" + channel + " send " + msg);
								}
								// <0说明断开
								else {
									System.out.println("client:" + channel + " disconnect");
									channel.close();
									this.schedule(() -> {
										System.out.println("100 millis after client:" + channel + " disconnect");
									}, 100, TimeUnit.MILLISECONDS);
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
		                }
		                iterator.remove();  //事件处理完毕，要记得清除
		            }
	    	    }
	            
	            // 将过期的定时任务加入任务队列
	            while (true) {
	            	FutureTask task = futureTaskQueue.peek();
	            	if (task == null) {
	            		break;
	            	}
	            	if (task.isExpired()) {
	            		taskQueue.offer(task.getCommand());
	            		futureTaskQueue.poll();
	            	} else {
	            		break;
	            	}
	            }
	            
	            // 处理任务队列中的任务
	            while (true) {
	            	Runnable task = taskQueue.poll();
	            	if (task == null) {
	            		break;
	            	}
	            	task.run();
	            }
	        }
	}

}
