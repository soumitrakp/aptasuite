/**
 * 
 */
package lib.parser.aptaplex;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import utilities.AptaLogger;

/**
 * @author Jan Hoinka Implements the parsing logic for fastq files
 */
public class FastqReader implements Reader {

	/**
	 * The buffered reader for the forward file
	 */
	BufferedReader forward_reader = null;

	/**
	 * The buffered reader for the reverse file
	 */
	BufferedReader reverse_reader = null;
	
	/**
	 * Buffers for forward and reverse lines
	 */
	String buffer;


	/**
	 * Constructor
	 * 
	 * @param forward_file
	 *            forward reads in fastq format, optionally gzip compressed
	 * @param reverse_file
	 *            reverse reads in fastq format, optionally gzip compressed.
	 *            Null if single end sequencing was performed
	 */
	public FastqReader(Path forward_file, Path reverse_file) {

		// Initialize the file reader depending on whether the files are gzip
		// compressed or not
		
		// Forward file
		try { // This fill fail if the file is not gzip compressed
			
			forward_reader = new BufferedReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(forward_file.toFile())))));
			AptaLogger.log(Level.CONFIG, this.getClass(), "Opened gzip compressed forward file in fastq format" + forward_file.toString());
		
		} catch (IOException e) {
			// Not in GZip Format
			try {
				forward_reader = new BufferedReader(new InputStreamReader(new FileInputStream(forward_file.toFile())));
				AptaLogger.log(Level.CONFIG, this.getClass(), "Opened forward file in fastq format" + forward_file.toString());	
			} catch (FileNotFoundException e1) {
				AptaLogger.log(Level.SEVERE, this.getClass(), "Error opening forward file " + forward_file.toString());
				e1.printStackTrace();
				System.exit(0);
			}
		}

		// Reverse file
		if (reverse_file != null){
			try {
				
				reverse_reader = new BufferedReader(new InputStreamReader(new GZIPInputStream((new FileInputStream(reverse_file.toFile())))));
				AptaLogger.log(Level.CONFIG, this.getClass(), "Opened gzip compressed reverse file in fastq format" + reverse_file.toString());
				
			} catch (IOException e) {
				// Not in GZip Format
				try {
	
					reverse_reader = new BufferedReader(new InputStreamReader(new FileInputStream(reverse_file.toFile())));
					AptaLogger.log(Level.CONFIG, this.getClass(), "Opened forward reverse in fastq format" + reverse_file.toString());	
					
				} catch (FileNotFoundException e1) {
					AptaLogger.log(Level.SEVERE, this.getClass(), "Error opening reverse file " + reverse_file.toString());
					e1.printStackTrace();
					System.exit(0);
				}
			}	
		}

	}

	@Override
	public Read getNextRead() {
		
		Read r = new Read();
		
		try {
			
			// Forward reads
			// read 4 lines at a time
			buffer = forward_reader.readLine();
			
			//return null if we are at the end of the file
			if (buffer == null){
				return null;
			}
			
			//read the next three lines of which we need lines 1 and 3
			r.forward_read = forward_reader.readLine().getBytes();
			forward_reader.readLine();
			r.forward_quality = forward_reader.readLine().getBytes();
			
			
			// Reverse reads, if applicable
			if (reverse_reader != null){
				
				// read 4 lines at a time
				buffer = reverse_reader.readLine();
				
				//return null if we are at the end of the file
				if (buffer == null){
					return null;
				}
				
				//read the next three lines of which we need lines 1 and 3
				r.reverse_read = reverse_reader.readLine().getBytes();
				reverse_reader.readLine();
				r.reverse_quality = reverse_reader.readLine().getBytes();
				
			}
			
		} catch (IOException e) {
			AptaLogger.log(Level.SEVERE, this.getClass(), "Error while parsing files.");
			e.printStackTrace();
			System.exit(0);
		}
		
		return r;
	}
	
	@Override
	public void close(){
		
		try {
			this.forward_reader.close();
			this.reverse_reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
