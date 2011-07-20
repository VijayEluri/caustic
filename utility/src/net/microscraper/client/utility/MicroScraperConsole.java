package net.microscraper.client.utility;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import au.com.bytecode.opencsv.CSVReader;

import net.microscraper.client.Client;
import net.microscraper.client.ClientException;
import net.microscraper.client.Log;
import net.microscraper.client.NameValuePair;
import net.microscraper.client.Utils;
import net.microscraper.client.impl.FileLogInterface;
import net.microscraper.client.impl.JDBCSQLite;
import net.microscraper.client.impl.JSONME;
import net.microscraper.client.impl.JakartaRegexpCompiler;
import net.microscraper.client.impl.JavaNetBrowser;
import net.microscraper.client.impl.JavaNetInterface;
import net.microscraper.client.impl.SQLInterface.SQLInterfaceException;
import net.microscraper.client.impl.SQLPublisher;
import net.microscraper.client.impl.SystemLogInterface;
import net.microscraper.client.interfaces.Browser;
import net.microscraper.client.interfaces.JSONInterface;
import net.microscraper.client.interfaces.Logger;
import net.microscraper.client.interfaces.NetInterface;
import net.microscraper.client.interfaces.RegexpCompiler;
import net.microscraper.client.interfaces.URIInterface;
import net.microscraper.client.interfaces.URILoader;

public class MicroScraperConsole {
	private static final String newline = System.getProperty("line.separator");
	private static final String usage = 
"usage: microscraper <uri> [<options>]" + newline +
"" + newline +
"uri" + newline +
"	A URI that points to microscraper instructions." + newline +
"options:" + newline +
"	--defaults=<defaults>" + newline +
"		A form-encoded string of name value pairs to use as" + newline +
"		defaults during execution." + newline +
"	--input=<path> [--column-delimiter=<delimiter>]" + newline +
"		Path to a file with any number of additional default" + newline +
"		values.  Each row is executed separately.  The first" + newline +
"		row contains column names." + newline +
"		The default column delimiter is ',' ." + newline +
"	--log-file[=<path>]" + newline +
"		Pipe the log to a file." + newline +
"		Path is optional, defaults to 'yyyyMMddkkmmss.log' in the" + newline +
"		current directory." + newline +
"	--log-stdout" + newline +
"		Pipe the log to stdout." + newline +
"	--output-format=(csv|formencoded|tab|sqlite)" + newline +
"		How to format output.  Defaults to sqlite." + newline +
"	--output-file[=<path>], --no-output-file" + newline +
"		Whether to save output to a file.  Enabled by default." + newline +
"		Path is optional, defaults to 'yyyyMMddkkmmss.<format>' in" + newline +
"		the current directory." + newline +
"	--output-stdout" + newline +
"		Pipe output to stdout.  This is not recommended for a" + newline +
"		binary format like sqlite.";
	
	private NameValuePair[] defaults = new NameValuePair[0];
	private CSVReader input = null;
	private File logFile = null;
	private boolean logStdout = false;
	private String outputFormat = "sqlite";
	private File outputFile = null;
	private boolean outputStdout = false;
	
	private static final SimpleDateFormat DATETIME_FORMAT =
		new SimpleDateFormat("yyyyMMddkkmmss");
	private static final String ENCODING = "UTF-8";
	private static final int sqlBatchSize = 1;
	
	private final Log log = new Log();
	private final Browser browser = new JavaNetBrowser(log, Browser.MAX_KBPS_FROM_HOST);
	private final NetInterface netInterface = new JavaNetInterface(browser);
	private final URILoader uriLoader = new UtilityURILoader(netInterface);
	private final JSONInterface jsonInterface = new JSONME(uriLoader);
	private final RegexpCompiler regexpCompiler = new JakartaRegexpCompiler();
	private SQLPublisher publisher;
	private Client client;
	
	public static void main (String[] args) {
		new MicroScraperConsole(args);
	}
	
	public MicroScraperConsole(String[] args) {
		Logger sysLogger = new SystemLogInterface();
		FileLogInterface fileLogger;
		String fileTimestamp = DATETIME_FORMAT.format(new Date());
		log.register(sysLogger);
		try {
			fileLogger = new FileLogInterface("./" + fileTimestamp + ".log");
			log.register(fileLogger);

			if(args.length > 2 || args.length < 1) {
				System.out.print(usage);
				return;
			}
			
			fileLogger.open();
			publisher = new SQLPublisher(
					new JDBCSQLite("./" + fileTimestamp + ".sqlite", log), sqlBatchSize);
			client = new Client(regexpCompiler,	
					log,
					netInterface,
					jsonInterface, ENCODING);
					
			URIInterface uri = netInterface.makeURI(args[0]);
			
			NameValuePair[] extraVariables;
			if(args.length == 2) {
				extraVariables = Utils.formEncodedDataToNameValuePairs(args[1], ENCODING);
			} else {
				extraVariables = new NameValuePair[] {};
			}
			
			// scrape!
			client.scrape(uri, extraVariables, publisher);
			
			// tie things up
			publisher.forceCommit();
			
			try {
				fileLogger.close();
			} catch (IOException e) {
				sysLogger.e(e);
			}
		} catch (UnsupportedEncodingException e) {
			log.e(e);
		} catch (SQLInterfaceException e ) {
			log.e(e);
		} catch (IOException e) {
			log.e(e);
		} catch (ClientException e) {
			log.e(e);
		}
	}
}