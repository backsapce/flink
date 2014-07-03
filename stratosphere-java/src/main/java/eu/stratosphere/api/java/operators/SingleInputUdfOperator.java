/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2013 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package eu.stratosphere.api.java.operators;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.stratosphere.api.common.operators.SingleInputSemanticProperties;
import eu.stratosphere.api.java.DataSet;
import eu.stratosphere.api.java.functions.FunctionAnnotation;
import eu.stratosphere.api.java.functions.SemanticPropUtil;
import eu.stratosphere.types.TypeInformation;
import eu.stratosphere.configuration.Configuration;

/**
 * The <tt>SingleInputUdfOperator</tt> is the base class of all unary operators that execute
 * user-defined functions (UDFs). The UDFs encapsulated by this operator are naturally UDFs that
 * have one input (such as {@link eu.stratosphere.api.java.functions.MapFunction} or
 * {@link eu.stratosphere.api.java.functions.ReduceFunction}).
 * <p>
 * This class encapsulates utilities for the UDFs, such as broadcast variables, parameterization
 * through configuration objects, and semantic properties.
 * @param <IN> The data type of the input data set.
 * @param <OUT> The data type of the returned data set.
 */
public abstract class SingleInputUdfOperator<IN, OUT, O extends SingleInputUdfOperator<IN, OUT, O>>
	extends SingleInputOperator<IN, OUT, O> implements UdfOperator<O>
{
	private Configuration parameters;

	private Map<String, DataSet<?>> broadcastVariables;

	private SingleInputSemanticProperties udfSemantics;

	// --------------------------------------------------------------------------------------------

	/**
	 * Creates a new operators with the given data set as input. The given result type
	 * describes the data type of the elements in the data set produced by the operator.
	 *
	 * @param input The data set that is the input to the operator.
	 * @param resultType The type of the elements in the resulting data set.
	 */
	protected SingleInputUdfOperator(DataSet<IN> input, TypeInformation<OUT> resultType) {
		super(input, resultType);
	}
	
	
	protected void extractSemanticAnnotationsFromUdf(Class<?> udfClass) {
		Set<Annotation> annotations = FunctionAnnotation.readSingleConstantAnnotations(udfClass);
		SingleInputSemanticProperties sp = SemanticPropUtil.getSemanticPropsSingle(annotations, getInputType(), getResultType());
		setSemanticProperties(sp);
	}

	// --------------------------------------------------------------------------------------------
	// Fluent API methods
	// --------------------------------------------------------------------------------------------

	@Override
	public O withParameters(Configuration parameters) {
		this.parameters = parameters;

		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}

	@Override
	public O withBroadcastSet(DataSet<?> data, String name) {
		if (this.broadcastVariables == null) {
			this.broadcastVariables = new HashMap<String, DataSet<?>>();
		}

		this.broadcastVariables.put(name, data);

		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}

	/**
	 * Adds a constant-set annotation for the UDF.
	 * 
	 * <p>
	 * Constant set annotations are used by the optimizer to infer the existence of data properties (sorted, partitioned, grouped).
	 * In certain cases, these annotations allow the optimizer to generate a more efficient execution plan which can lead to improved performance.
	 * Constant set annotations can only be specified if the second input and the output type of the UDF are of
	 * {@link eu.stratosphere.api.java.tuple.Tuple} data types.
	 * 
	 * <p>
	 * A constant-set annotation is a set of constant field specifications. The constant field specification String "4->3" specifies, that this UDF copies the fourth field of 
	 * an input tuple to the third field of the output tuple. Field references are zero-indexed.
	 * 
	 * <p>
	 * <b>NOTICE: Constant set annotations are optional, but if given need to be correct. Otherwise, the program might produce wrong results!</b>
	 * 
	 * @param constantSet A list of constant field specification Strings.
	 * @return This operator with an annotated constant field set.
	 */
	public O withConstantSet(String... constantSet) {
		SingleInputSemanticProperties props = SemanticPropUtil.getSemanticPropsSingleFromString(constantSet, null, null, this.getInputType(), this.getResultType());
		this.setSemanticProperties(props);
		@SuppressWarnings("unchecked")
		O returnType = (O) this;
		return returnType;
	}

	// --------------------------------------------------------------------------------------------
	// Accessors
	// --------------------------------------------------------------------------------------------

	@Override
	public Map<String, DataSet<?>> getBroadcastSets() {
		return this.broadcastVariables == null ?
				Collections.<String, DataSet<?>>emptyMap() :
				Collections.unmodifiableMap(this.broadcastVariables);
	}

	@Override
	public Configuration getParameters() {
		return this.parameters;
	}

	@Override
	public SingleInputSemanticProperties getSematicProperties() {
		return this.udfSemantics;
	}

	/**
	 * Sets the semantic properties for the user-defined function (UDF). The semantic properties
	 * define how fields of tuples and other objects are modified or preserved through this UDF.
	 * The configured properties can be retrieved via {@link UdfOperator#getSematicProperties()}.
	 *
	 * @param properties The semantic properties for the UDF.
	 * @see UdfOperator#getSematicProperties()
	 */
	public void setSemanticProperties(SingleInputSemanticProperties properties) {
		this.udfSemantics = properties;
	}
}
