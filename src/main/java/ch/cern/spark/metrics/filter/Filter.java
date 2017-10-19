package ch.cern.spark.metrics.filter;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import ch.cern.Properties;
import ch.cern.spark.metrics.Metric;

public class Filter implements Predicate<Metric>, Serializable{
    
    private static final long serialVersionUID = 9170996730102744051L;

    private Predicate<Metric> predicate = (m -> true);
    
    public Filter(){
    }
    
    @Override
	public boolean test(Metric metric) {
		return predicate.test(metric);
	}
    
    public void addPredicate(String key, String value){
    		if(value.charAt(0) == '!') {
    			Pattern pattern = Pattern.compile(value.substring(1));
    			
    			Predicate<Metric> notExist = metric -> !metric.getIDs().containsKey(key);
    			Predicate<Metric> notMatch = metric -> !pattern.matcher(metric.getIDs().get(key)).matches();
        		
        		predicate = predicate.and(notExist.or(notMatch));
    		}else{
    			Pattern pattern = Pattern.compile(value);
    			
    			Predicate<Metric> exist = metric -> metric.getIDs().containsKey(key);
    			Predicate<Metric> match = metric -> pattern.matcher(metric.getIDs().get(key)).matches();
        		
        		predicate = predicate.and(exist).and(match);
    		}
    }

    public static Filter build(Properties props) {
        Filter filter = new Filter();
        
        Properties filterProperties = props.getSubset("attribute");
        Set<String> attributesNames = filterProperties.getUniqueKeyFields();
        
        for (String attributeName : attributesNames) {
            String key = "attribute." + attributeName;
            
            filter.addPredicate(attributeName, props.getProperty(key));
        }
        
        return filter;
    }

    @Override
    public String toString() {
        return "Filter [predicate=" + predicate + "]";
    }
    
}