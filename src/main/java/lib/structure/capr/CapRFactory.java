/**
 * 
 */
package lib.structure.capr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

import lib.parser.Parser;
import utilities.Configuration;

/**
 * @author Jan Hoinka Java implementation of AptaPlex as described in Hoinka et
 *         al. 2016, Methods This class controls the producer-consumer pattern
 *         implemented in lib.parser.aptaplex
 */
public class CapRFactory implements Runnable{

	/**
	 * The progress of the parser instance. Writable to the consumers and thread-safe
	 */
	private static final AtomicInteger progress = new AtomicInteger();
	
	private Iterable<Entry<byte[], Integer>> items = null;
	
	public CapRFactory(Iterable<Entry<byte[], Integer>> items){
		
		this.items = items;
		
	}
	
	public void predict() {

		// Creating shared object
		BlockingQueue<Object> sharedQueue = new ArrayBlockingQueue<>(500); //TODO: MAKE THIS A PARAMETER

		// We need to know how many threads we can use on the system
		int num_threads = Math.min( Runtime.getRuntime().availableProcessors(), Configuration.getParameters().getInt("Performance.maxNumberOfCores"));
		
		// Creating Producer and Consumer Thread
		Thread prodThread  = new Thread(new CapRFactoryProducer(items, sharedQueue), "CapR Producer");
		
		ArrayList<Thread> consumers = new ArrayList<Thread>();
		
		// We need at least one consumer
		consumers.add(new Thread(new CapRFactoryConsumer(sharedQueue, progress), "CapR Consumer 1"));
		
		// Add remaining consumers
		for (int x=1; x<num_threads-1; x++){
			consumers.add(new Thread(new CapRFactoryConsumer(sharedQueue, progress), ("CapR Consumer " + (x+1)) ));
		}

		// Start the producer and consumer threads
		for (int x=0; x<consumers.size(); x++){
			consumers.get(x).start();
		}
		prodThread.start();

		// Make sure the threads wait until completion
		try {
			for (int x=0; x<consumers.size(); x++){
				consumers.get(x).join();
			}
			prodThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Clear resources used by the threads
		for (int x=0; x<consumers.size(); x++){
			consumers.set(x, null);
		}
		prodThread  = null;
		
	}


	public AtomicInteger getProgress() {
		return progress;
	}

	@Override
	public void run() {
		
		predict();
		
	}

}
