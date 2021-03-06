package ch.cern.exdemon.metrics.defined.equation.functions.bool;

import java.text.ParseException;

import ch.cern.exdemon.metrics.defined.equation.ValueComputable;
import ch.cern.exdemon.metrics.defined.equation.functions.Function;
import ch.cern.exdemon.metrics.value.BooleanValue;
import ch.cern.exdemon.metrics.value.Value;

public class NotFunc extends Function{
	
	public static String REPRESENTATION = "!";

	public static Class<? extends Value>[] argumentTypes = types(BooleanValue.class);

	public NotFunc(ValueComputable... arguments) throws ParseException {
		super(REPRESENTATION, argumentTypes, arguments);

		operationInTheMiddleForToString();
	}
	
	@Override
	public Class<BooleanValue> returnType() {
		return BooleanValue.class;
	}

	@Override
	protected Value compute(Value... values) {
		return new BooleanValue(!values[0].getAsBoolean().get());
	}
	
}
