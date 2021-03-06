package ch.cern.exdemon.monitor.analysis.types;

import static org.numenta.nupic.algorithms.Anomaly.KEY_ESTIMATION_SAMPLES;
import static org.numenta.nupic.algorithms.Anomaly.KEY_LEARNING_PERIOD;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.cern.exdemon.components.ConfigurationResult;
import ch.cern.exdemon.metrics.Metric;
import ch.cern.exdemon.monitor.analysis.results.AnalysisResult;
import ch.cern.exdemon.monitor.analysis.types.htm.HTMParameters;
import ch.cern.exdemon.monitor.analysis.types.htm.MetricsFromFileReader;
import ch.cern.exdemon.monitor.analysis.types.htm.ResultsToFileWriter;
import ch.cern.properties.ConfigurationException;
import ch.cern.properties.Properties;
import ch.cern.spark.status.StatusValue;
import ch.cern.spark.status.storage.JSONStatusSerializer;
import ch.cern.spark.status.storage.StatusSerializer;
import scala.util.Random;


public class HTMAnalysisTest {
	
	private static final String DATA_FILE = "src/test/resources/write_mounts.csv";
	private static final String OUTPUT_DATA_FILE = "src/test/resources/write_mounts_outputs.csv";
	private static final int EXPECTED_ERRORS = 16;
	private static final int EXPECTED_WARNINGS = 8;
	private static final String CSV_SPLIT_BY = ",";
	private static final String TIME_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
	
	StatusSerializer serializer = new JSONStatusSerializer();
	
	//@Test
	public void serializeNetworkTest() throws ConfigurationException, IOException {

		
		HTMAnalysis htm = new HTMAnalysis();
		Properties prop = new Properties();
		prop.setProperty(HTMAnalysis.MAX_VALUE_PARAMS, Float.toString(200));
		prop.setProperty(HTMAnalysis.MIN_VALUE_PARAMS, Float.toString(0));
		htm.config(prop);
		
		MetricsFromFileReader reader = new MetricsFromFileReader(DATA_FILE, CSV_SPLIT_BY, TIME_FORMAT);
		ResultsToFileWriter writer = new ResultsToFileWriter(OUTPUT_DATA_FILE);
		int nErrors = 0;
		int nWarnings = 0;
		int i = 0;
		
		writer.writeHeader();
		reader.skipHeader();
		Metric metric = reader.next();
		
		htm.load(null);
		htm.process(metric.getTimestamp(), metric.getValue().getAsFloat().get());
		StatusValue status = htm.save();
		byte[] barray = serializer.fromValue(status);
		
		while(reader.hasNext()) {
			status = serializer.toValue(barray);
			htm.load(status);
			
			metric = reader.next();
			AnalysisResult results = htm.process(metric.getTimestamp(), metric.getValue().getAsFloat().get());
			if(results.getStatus() == AnalysisResult.Status.WARNING)
				nWarnings++;
			
			if(results.getStatus() == AnalysisResult.Status.ERROR)
				nErrors++;
			
			writer.write(results);
			status = htm.save();
			barray = serializer.fromValue(status);
			System.out.println(i);
			i++;
		}
		
		System.out.println(i);
		Assert.assertEquals(EXPECTED_WARNINGS, nWarnings);
		Assert.assertEquals(EXPECTED_ERRORS, nErrors);
		writer.close();
	}
	
	@Test
	public void processTest() throws ConfigurationException{
		HTMAnalysis htm = new HTMAnalysis();
		Properties prop = new Properties();
		prop.setProperty(HTMAnalysis.MAX_VALUE_PARAMS, Float.toString(200));
		prop.setProperty(HTMAnalysis.MIN_VALUE_PARAMS, Float.toString(0));
		htm.config(prop);
		
		MetricsFromFileReader reader = new MetricsFromFileReader(DATA_FILE, CSV_SPLIT_BY, TIME_FORMAT);
		ResultsToFileWriter writer = new ResultsToFileWriter(OUTPUT_DATA_FILE);
		Metric metric;
		
		int nErrors = 0;
		int nWarnings = 0;
		writer.writeHeader();
		reader.skipHeader();
		while (reader.hasNext()) {
			
			metric = reader.next();
			AnalysisResult results = htm.process(metric.getTimestamp(), metric.getValue().getAsFloat().get());
			
			if(results.getStatus() == AnalysisResult.Status.WARNING)
				nWarnings++;
			
			if(results.getStatus() == AnalysisResult.Status.ERROR)
				nErrors++;
			
			writer.write(results);			
		}
		Assert.assertEquals(EXPECTED_WARNINGS, nWarnings);
		Assert.assertEquals(EXPECTED_ERRORS, nErrors);
		writer.close();
	}
	
	@Test
	public void learningPhaseTest() throws ConfigurationException{
		Random r = new Random();
		HTMAnalysis htm = new HTMAnalysis();
		Map<String, Object> alparams = HTMParameters.getAnomalyLikelihoodParams();
		int learningPoints = (int)alparams.get(KEY_LEARNING_PERIOD) + (int)alparams.get(KEY_ESTIMATION_SAMPLES);
		Metric metric;
		
		Properties prop = new Properties();
		htm.config(prop);
		
		//Testing if the status for the learning phase is an Exception
		AnalysisResult result;
		for(int i = 0; i < learningPoints; i++) {
			metric = new Metric(Instant.now().truncatedTo(ChronoUnit.SECONDS), r.nextFloat(), new HashMap<String, String>());
			result = htm.process(metric.getTimestamp(), metric.getValue().getAsFloat().get());
			Assert.assertEquals(AnalysisResult.Status.EXCEPTION, result.getStatus());
		}
		
		//Testing if after the learning phase algorithm start working properly.
		for(int i = 1; i < 200; i++) {
			metric = new Metric(Instant.now().truncatedTo(ChronoUnit.SECONDS), r.nextFloat(), new HashMap<String, String>());
			result = htm.process(metric.getTimestamp(), metric.getValue().getAsFloat().get());
			Assert.assertNotEquals(AnalysisResult.Status.EXCEPTION, result.getStatus());
		}
	}
	
	@Test
	public void thresholdCheckingTest() throws ConfigurationException {
		HTMAnalysis htm = new HTMAnalysis();
		Properties prop = new Properties();
		prop.setProperty(HTMAnalysis.ERROR_THRESHOLD_PARAMS, Float.toString(0.8f));
		prop.setProperty(HTMAnalysis.WARNING_THRESHOLD_PARAMS, Float.toString(0.9f));
		
		ConfigurationResult confResult = htm.config(prop);
		List<ConfigurationException> errors = confResult.getErrors();
		Assert.assertEquals(HTMAnalysis.ERROR_THRESHOLD_PARAMS, errors.get(0).getParameter());
	}
	
}
