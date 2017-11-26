package ch.cern.spark.metrics.notifications.sink.types;

import ch.cern.components.RegisterComponent;
import ch.cern.properties.ConfigurationException;
import ch.cern.properties.Properties;
import ch.cern.spark.Stream;
import ch.cern.spark.http.HTTPSink;
import ch.cern.spark.metrics.notifications.Notification;
import ch.cern.spark.metrics.notifications.sink.NotificationsSink;

@RegisterComponent("http")
public class HTTPNotificationsSink extends NotificationsSink {

	private static final long serialVersionUID = 6368509840922047167L;

	private HTTPSink sink = new HTTPSink();

	@Override
	public void config(Properties properties) throws ConfigurationException {
		super.config(properties);
		
		sink.config(properties);
	}

	@Override
	protected void notify(Stream<Notification> notifications) {
		sink.sink(notifications);	
	}

}