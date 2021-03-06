package org.integratedmodelling.thinklab.rest;

import java.util.ArrayList;
import java.util.HashMap;

import org.integratedmodelling.thinklab.exception.ThinklabRuntimeException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class that can be set with a result and translate it into a JSON object.
 * 
 * @author ferdinando.villa
 *
 */
public class ResultHolder {

	HashMap<String, Object> _parameters = new HashMap<String, Object>();
	int resultType  = DefaultRESTHandler.VOID;

	// result holders. Could be more efficient and a lot messier.
	ArrayList<Integer> _intResult = null;
	ArrayList<Double> _dblResult = null;
	ArrayList<String> _txtResult = null;
	ArrayList<String> _urnResult = null;
	ArrayList<String> _urnTypes  = null;

	/**
	 * If this is used, "return wrap()" should be the last call in your handler function. Any 
	 * data set through this one or setResult will be automatically returned in a JSON object.
	 * 
	 * @param key
	 * @param o
	 */
	protected void put(String key, Object... o) {
		if (o == null || o.length == 0)
			_parameters.put(key, "nil");
		else if (o.length == 1)
			_parameters.put(key, o[0]);
		else {
			
			/*
			 * if a tree node, do our tree thing
			 * TODO when we know what it is, of course.
			 */
			
			/*
			 * else make a JSONArray
			 */
			try {
				JSONArray ja = new JSONArray(o);
				_parameters.put(key, ja);
			} catch (JSONException e) {
				throw new ThinklabRuntimeException(e);
			}
		}
	}
	
	protected void setResult(int... iResult) {
		_intResult = new ArrayList<Integer>();
		for (int i : iResult)
			_intResult.add(i);
		resultType = _intResult.size() > 1 ? DefaultRESTHandler.INTS : DefaultRESTHandler.INT;
	}

	protected void setResult(double... dResult) {
		_dblResult = new ArrayList<Double>();
		for (double i : dResult)
			_dblResult.add(i);		
		resultType = _dblResult.size() > 1 ? DefaultRESTHandler.DOUBLES : DefaultRESTHandler.DOUBLE;
	}

	protected void setResult(String... tResult) {
		_txtResult = new ArrayList<String>();
		for (String i : tResult)
			_txtResult.add(i);
		resultType = _txtResult.size() > 1 ? DefaultRESTHandler.TEXTS : DefaultRESTHandler.TEXT;
	}
	
	protected void addResult(String rURN, String rMIME) {
		
		if (_urnResult == null) {
			_urnResult = new ArrayList<String>();
			_urnTypes  = new ArrayList<String>();
		}
		
		_urnResult.add(rURN);
		_urnTypes.add(rMIME);
		
		resultType = _urnResult.size() > 1 ? DefaultRESTHandler.URNS : DefaultRESTHandler.URN;
	}
	
	public void toJSON(JSONObject jsonObject) {

		/*
		 * TODO
		 * put any result; add type if indirect (URN)
		 */
		
		/*
		 * put any fields
		 */
		for (String s : _parameters.keySet()) {
			try {
				jsonObject.put(s, _parameters.get(s));
			} catch (JSONException e) {
				throw new ThinklabRuntimeException(e);
			}
		}

	}
}
