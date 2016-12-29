/**
 * 
 */
package lib.parser.aptaplex;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import exceptions.InvalidSequenceReadFileException;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 * This class implements the Producer of the parser. 
 * Depending on the specified file format and single/paired end sequencing,
 * it iterates over the file, exacts the relevant information, and add it
 * to a queue for the consumers to process.
 *
 */
public class AptaplexProducer implements Runnable{


	/**
	 * Enable logging for debugging and information
	 */
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	
	/**
	 * The queue to fill
	 */
	BlockingQueue<Object> queue = null;
	
	
	public AptaplexProducer(BlockingQueue<Object> queue){
		
		this.queue = queue;
	
	}
	
	/**
	 * The total number of processed reads
	 */
	public int totalProcessedReads = 0;
	
	@Override
	public void run() {

		// the files we need to process
		String[] forward_files = Configuration.getParameters().getStringArray("AptaplexParser.forwardFiles");
		String[] reverse_files = Configuration.getParameters().getStringArray("AptaplexParser.reverseFiles");

		// Sanity checks
		// make sure the forward read files exist
		if (forward_files.length == 0)
		{
			throw new InvalidSequenceReadFileException("No forward read files where specified. Please check your configuration.");
		}
		
		// if they exist, we need the same number of forward and reverse files, or no reverse files in 
		// case of single end sequencing
		if (reverse_files.length != 0 && forward_files.length!=reverse_files.length){
			throw new InvalidSequenceReadFileException("The number of forward and reverse read files must be identical.");
		}
		
		//iterate over all files and populate the queue
		for (int x=0; x<forward_files.length; x++){
			
			Path current_forward_file_path = Paths.get(forward_files[x]);
			
			Path current_reverse_file_path = null;
			// at this point we know reverse_files.length is either 0 or equal to forward_file.length
			if (reverse_files.length != 0){
				current_reverse_file_path = Paths.get(reverse_files[x]);
			}
			
			// Create a new SelectionCycle instance. Use reflection so we can define the backend in the configuration
			Reader reader = null;
			Class reader_class = null;
			try {
				reader_class = Class.forName("lib.parser.aptaplex." + Configuration.getParameters().getString("AptaplexParser.reader"));
			} catch (ClassNotFoundException e) {

				LOGGER.info("Error, the backend for the Reader could not be found.");
				e.printStackTrace();
			
			}
			
			// Try to instantiate the class
			try {
				reader = (Reader)reader_class.getConstructor(Path.class, Path.class).newInstance(current_forward_file_path, current_reverse_file_path);
			} catch (InstantiationException e) {
				LOGGER.info("Error, could not instantiate the backend for the SelectionCycle");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				LOGGER.info("Error invoking construtor of SelectionCycle backend");
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			
			try {
				// get the first read
				Read read = reader.getNextRead();
				
				// do the same for the remaining reads
				while (read != null){
					
					// are we multiplexing?
					if(Configuration.getParameters().getBoolean("AptaplexParser.isPerFile")){
						
						// add cycle information to read
						read.selection_cycle = Configuration.getExperiment().getAllSelectionCycles().get(x);
						
					}
					
					// put read into to queue
					queue.put(read);
					totalProcessedReads++;
					
					// get the next read
					read = reader.getNextRead();
				}
			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			reader.close();
			reader = null;
			
		}
		
		// at the end we need to add a poison pill to 
		// the queue to let the consumers know when to stop
		LOGGER.info("Added poison pill to parsing queue");
		try {
			queue.put(Configuration.POISON_PILL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}