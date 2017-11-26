package ch.cern.spark.metrics.defined;

import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function4;
import org.apache.spark.streaming.State;
import org.apache.spark.streaming.Time;

import ch.cern.properties.Properties;
import ch.cern.spark.metrics.Metric;
import ch.cern.spark.metrics.defined.equation.var.VariableStores;

public class UpdateDefinedMetricStatusesF 
	implements Function4<Time, DefinedMetricID, Optional<Metric>, State<VariableStores>, Optional<Metric>> {

	private static final long serialVersionUID = 2965182980222300453L;

	private Properties propertiesSourceProps;

	public UpdateDefinedMetricStatusesF(Properties propertiesSourceProps) {
		this.propertiesSourceProps = propertiesSourceProps;
	}

	@Override
	public Optional<Metric> call(Time time, DefinedMetricID id, Optional<Metric> metricOpt, State<VariableStores> status)
			throws Exception {

		if(status.isTimingOut() || !metricOpt.isPresent())
			return Optional.empty();
		
		DefinedMetrics.initCache(propertiesSourceProps);
		
		Optional<DefinedMetric> definedMetricOpt = Optional.fromNullable(DefinedMetrics.getCache().get().get(id.getDefinedMetricName()));
		if(!definedMetricOpt.isPresent()) {
			status.remove();
			return Optional.empty();
		}
		DefinedMetric definedMetric = definedMetricOpt.get();
			
		VariableStores store = getStore(status);
		
		Metric metric = metricOpt.get();
		
		definedMetric.updateStore(store, metric, id.getGroupByMetricIDs().keySet());
		
		Optional<Metric> newMetric = toOptional(definedMetric.generateByUpdate(store, metric, id.getGroupByMetricIDs()));
		
		status.update(store);
		
		return newMetric;
	}

	private Optional<Metric> toOptional(java.util.Optional<Metric> javaOptional) {
		return javaOptional.isPresent() ? Optional.of(javaOptional.get()) : Optional.empty();
	}

	private VariableStores getStore(State<VariableStores> status) {
		return status.exists() ? status.get() : new VariableStores();
	}

}