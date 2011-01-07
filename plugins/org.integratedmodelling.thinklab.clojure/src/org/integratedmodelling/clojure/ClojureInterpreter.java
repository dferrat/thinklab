package org.integratedmodelling.clojure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.integratedmodelling.thinklab.exception.ThinklabException;
import org.integratedmodelling.thinklab.exception.ThinklabIOException;
import org.integratedmodelling.thinklab.exception.ThinklabInternalErrorException;
import org.integratedmodelling.thinklab.exception.ThinklabResourceNotFoundException;
import org.integratedmodelling.thinklab.exception.ThinklabScriptException;
import org.integratedmodelling.thinklab.exception.ThinklabValidationException;
import org.integratedmodelling.thinklab.extensions.Interpreter;
import org.integratedmodelling.thinklab.interfaces.annotations.TaskNamespace;
import org.integratedmodelling.thinklab.interfaces.applications.ISession;
import org.integratedmodelling.thinklab.interfaces.literals.IValue;
import org.integratedmodelling.thinklab.literals.Value;
import org.integratedmodelling.utils.CamelCase;
import org.integratedmodelling.utils.Escape;
import org.integratedmodelling.utils.MiscUtilities;

import clojure.lang.Compiler;
import clojure.lang.DynamicClassLoader;
import clojure.lang.LineNumberingPushbackReader;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class ClojureInterpreter implements Interpreter {

	InputStream input = System.in;
	OutputStream output = System.out;
	OutputStream error = System.err;
	private ISession session;
	
	private URL currentSource = null;
	
	public interface FormListener {
		public abstract void onFormEvaluated(Object retval, String formCode);
	}
 	
	private synchronized Symbol newGlobalSymbol(String ns) {
		return Symbol.intern(ns);
	}
	
	public IValue evalInNamespace(Object code, String namespace) throws ThinklabException {
		
		Object ret = evalRaw(code, namespace, null);
		return ret == null ? null : Value.getValueForObject(ret);
	}

	@Override
	public IValue eval(Object code, HashMap<String, Object> args)
			throws ThinklabException {
		
		return Value.getValueForObject(
				evalRaw(code, session == null ? "user" : session.getSessionWorkspace(), args));
	}
	
	private synchronized void addRTClasspath(URL[] urls) throws ThinklabInternalErrorException {
		for (URL url : urls) {
			try {
				RT.addURL(url);
			} catch (Exception e) {
				throw new ThinklabInternalErrorException(e);
			}
		}
	}
	
	@Override
	public void addClasspath(URL[] urls) throws ThinklabException {
		addRTClasspath(urls);
	}
	
	@Override
	public synchronized void loadBindings(URL source, ClassLoader cloader) throws ThinklabException {

		currentSource = source;
		
        try {
        	
        	DynamicClassLoader cl = null;
        	if (cloader != null) {
        		cl = RT.ROOT_CLASSLOADER;
        		RT.ROOT_CLASSLOADER = new DynamicClassLoader(cloader);
        	}
        		
			Compiler.loadFile(Escape.fromURL(source.getFile().toString()));
			
			if (cloader != null) {
				RT.ROOT_CLASSLOADER = cl;
			}
			
		} catch (Exception e) {
			throw new ThinklabValidationException(e);
		} finally {
			currentSource = null;
		}
	}
	

	/**
	 * This one is basically a REPL that loads one form at a time and calls the 
	 * listener with the result of evaluating it. It uses a special subclass of
	 * Reader that stores the bytes read, so that the listener is passed the
	 * unaltered source code as well as the result of evaluating it.
	 * 
	 * @param source
	 * @param cloader
	 * @param listener
	 * @throws ThinklabException
	 */
	public synchronized void 
		loadMonitored(URL source, ClassLoader cloader,
					  FormListener listener) throws ThinklabException {
		
		currentSource = source;
		
		class MonitoringReader extends LineNumberingPushbackReader {
			
			StringBuffer buffer = new StringBuffer();
			
			public MonitoringReader(InputStreamReader i) {
				super(i);
			}

			@Override
			public int read(char[] arg0, int arg1, int arg2) throws IOException {
				int ret = super.read(arg0, arg1, arg2);
				buffer.append(arg0, arg1, arg2);
				return ret;
			}

			@Override
			public void unread(char[] arg0, int arg1, int arg2)
					throws IOException {
				// TODO Auto-generated method stub
				super.unread(arg0, arg1, arg2);
				buffer.delete(buffer.length() - arg1 - 1, buffer.length() - arg2 -1);
			}
			
			public String getBuffer() {
				String ret = buffer.toString();
				buffer = new StringBuffer();
				return ret;
			}
		}
		
		class MonitoringInputStream extends FilterInputStream {

			StringBuffer buffer = new StringBuffer();
			
			public MonitoringInputStream(InputStream in) {
				super(in);
			}

			@Override
			public int read() throws IOException {
				int n = super.read();
				buffer.append((char)n);
				return n;
			}

			@Override
			public int read(byte[] arg0, int arg1, int arg2) throws IOException {
				int ret = super.read(arg0, arg1, arg2);
//				buffer.append(arg0, arg1, arg2);
				return ret;
			}
			
			public String getBuffer() {
				String ret = buffer.toString();
				buffer = new StringBuffer();
				return ret;
			}
			
			
		}
		
		final Symbol USER = Symbol.create("user");
		final Symbol CLOJURE = Symbol.create("clojure.core");
		final Symbol TL = Symbol.create("tl");
		final Symbol EXIT = Symbol.create("exit");
		
		final Var in_ns = RT.var("clojure.core", "in-ns");
		final Var refer = RT.var("clojure.core", "refer");
		final Var ns = RT.var("clojure.core", "*ns*");
		final Var compile_path = RT.var("clojure.core", "*compile-path*");
		final Var warn_on_reflection = RT.var("clojure.core",
				"*warn-on-reflection*");
		final Var print_meta = RT.var("clojure.core", "*print-meta*");
		final Var print_length = RT.var("clojure.core", "*print-length*");
		final Var print_level = RT.var("clojure.core", "*print-level*");
		final Var star1 = RT.var("clojure.core", "*1");
		final Var star2 = RT.var("clojure.core", "*2");
		final Var star3 = RT.var("clojure.core", "*3");
		final Var stare = RT.var("clojure.core", "*e");
		final Var exit = RT.var("clojure.core", "exit");
		final Var sess  = RT.var("tl", "*session*");
		
		Var.pushThreadBindings(RT.map(ns, ns.get(), warn_on_reflection,
				warn_on_reflection.get(), print_meta, print_meta.get(),
				print_length, print_length.get(), print_level, print_level
						.get(), compile_path, "classes", star1, null,
				star2, null, star3, null, stare, null, sess, this.session, exit, EXIT));

		// create and move into the user namespace
		try {
			in_ns.invoke(USER);
			refer.invoke(CLOJURE);
			refer.invoke(TL);
		} catch (Exception e) {
			throw new ThinklabValidationException(e);
		}

		InputStreamReader rd = null;
		MonitoringInputStream input = null;
		try {
			input = new MonitoringInputStream(new FileInputStream(Escape.fromURL(source.getFile().toString())));
			rd = new InputStreamReader(new MonitoringInputStream(input));
		} catch (FileNotFoundException e) {
			throw new ThinklabResourceNotFoundException(e);
		}
		
		LineNumberingPushbackReader rdr = new LineNumberingPushbackReader(rd);
		
        try {
        	
        	DynamicClassLoader cl = null;
        	if (cloader != null) {
        		cl = RT.ROOT_CLASSLOADER;
        		RT.ROOT_CLASSLOADER = new DynamicClassLoader(cloader);
        	}
        		
        	// mah
        	Object EOF = new Object();

        	while (true) {

        		Object r = LispReader.read(rdr, false, EOF, false);
        		Object ret = Compiler.eval(r);
        		
        		if (listener != null) {
        			listener.onFormEvaluated(ret, input.getBuffer());
        		}
        		
        		if (r == EOF) {
					break;
				}
        	}
        	
			if (cloader != null) {
				RT.ROOT_CLASSLOADER = cl;
			}
			
		} catch (Exception e) {
			throw new ThinklabValidationException(e);
		} finally {
			
			try {
				rdr.close();
			} catch (IOException e) {
				throw new ThinklabIOException(e);
			}
			currentSource = null;
		}
	}
	@Override
	public void setError(OutputStream input) {
		this.error = input;
	}

	@Override
	public void setInput(InputStream input) {
		this.input = input;
	}

	@Override
	public void setOutput(OutputStream input) {
		this.output = input;
	}

	@Override
	public void setSession(ISession session) {
		this.session = session;
	}

	/**
	 * Returns whatever URL is being read while executing loadBindings, which
	 * is synchronized. 
	 * 
	 * @return
	 */
	public URL getCurrentSource() {
		return currentSource;
	}
	
	@Override
	public void defineTask(Class<?> taskClass, ClassLoader cloader) throws ThinklabException {
		
		/*
		 * FIXME this should be the ID of the declaring plugin by default
		 */
		String ns = "plugin";
		
		/*
		 * Create Clojure binding for passed task.
		 */
		if (taskClass.isInterface() || Modifier.isAbstract(taskClass.getModifiers()))
			return;
		
		/*
		 * lookup namespace if any
		 */
		for (Annotation annotation : taskClass.getAnnotations()) {
			if (annotation instanceof TaskNamespace) {
				ns = ((TaskNamespace)annotation).ns();
			}
		}
		
		String fname = CamelCase.toLowerCase(MiscUtilities.getFileExtension(taskClass.getName()), '-');
		
		String clj = "(defn " + fname;
		
		ArrayList<String> set = new ArrayList<String>();
		String get = null;
		
		for (Method method : taskClass.getMethods()) {
			
			if (!method.getDeclaringClass().equals(taskClass))
				continue;
			
			if (method.getName().startsWith("set")) {
				set.add(method.getName().substring(3));
			} else if (method.getName().startsWith("get")) {
				get = method.getName().substring(3);
			}
		}
		
		/*
		 * add description: nothing for now, may want to scan annotations later
		 */
		clj += "\n\t\"\"";
		
		/*
		 * FIXME
		 * sort the get() methods for predictability of the parameter order. Eventually
		 * we may want to use a map for an argument, instead of N args.
		 */
		Collections.sort(set);
		
		/*
		 * add parameters
		 */
		clj += "\n\t[";
		for (int i = 0; i < set.size(); i++) {
			clj += 
				(i > 0 ? " " : "") + 
				Character.toLowerCase(set.get(i).charAt(0)) + 
				set.get(i).substring(1);
		}
		clj += "]";
		
		/*
		 * main code: construct initialized instance
		 */
		clj += "\n\t(. (doto (new " + taskClass.getCanonicalName() + ")";
		
		/*
		 * pass parameters
		 */
		for (int i = 0; i < set.size(); i++) {
			clj += 
				"\n\t\t(.set" +
				set.get(i) + 
				" " +
				Character.toLowerCase(set.get(i).charAt(0)) + 
				set.get(i).substring(1) +
				")";
		}
		
		/* 
		 * invoke run() and close doto
		 */
		clj += "\n\t\t(.run (tl/get-session)))";
		
		/*
		 * invoke result getter on constructed object and close
		 */
		clj += "\n\tget" + get + "))";
		
		/*
		 * eval the finished method in given namespace
		 */
    	
    	DynamicClassLoader cl = null;
    	if (cloader != null) {
    		cl = RT.ROOT_CLASSLOADER;
    		RT.ROOT_CLASSLOADER = new DynamicClassLoader(cloader);
    	}
    	
		evalInNamespace(clj, ns);
		
		if (cloader != null) {
			RT.ROOT_CLASSLOADER = cl;
		}
	}

	@Override
	public IValue eval(Object code) throws ThinklabException {   
    	return evalInNamespace(code, session == null ? "user" : session.getSessionWorkspace());    	
	}

	public Object evalRaw(Object code, String namespace, HashMap<String, Object> args) throws ThinklabException {
		
		if (namespace == null)
			namespace = "user";
		
		InputStream inp = null;
		try {
			if (code instanceof URL) {
				inp = ((URL)code).openStream();
			} else if (code instanceof File) {
				inp = new FileInputStream((File)code);
			} else {
				inp = new ByteArrayInputStream(code.toString().getBytes("UTF-8"));
			}
		} catch (Exception e) {
			throw new ThinklabInternalErrorException(e);
		}
		
		final Symbol TL = Symbol.intern("tl");
		final Symbol CLOJURE = Symbol.intern("clojure.core");

		final Var refer = RT.var("clojure.core", "refer");
		final Var ns = RT.var("clojure.core", "*ns*");
		final Var star1 = RT.var("clojure.core", "*1");
		final Var star2 = RT.var("clojure.core", "*2");
		final Var star3 = RT.var("clojure.core", "*3");
		final Var stare = RT.var("clojure.core", "*e");
		final Var sess  = RT.var("tl", "*session*");

		final Namespace CUSTOM_NS = 
				Namespace.findOrCreate(newGlobalSymbol(namespace));		

		Object ret = null;
		
		try {

			Var.pushThreadBindings(
				RT.map(
					//RT.USE_CONTEXT_CLASSLOADER, RT.T, 
					ns, CUSTOM_NS, 
					star1, null,
					star2, null, 
					star3, null, 
					stare, null, 
					sess, this.session));

			if (args != null)
				for (String arg : args.keySet()) {
					final Var vz = RT.var(CUSTOM_NS.toString(), arg);
					Var.pushThreadBindings(RT.map(vz, args.get(arg)));
				}
			
			refer.invoke(CLOJURE);
			refer.invoke(TL);
			
			LineNumberingPushbackReader rdr = new LineNumberingPushbackReader(
					new InputStreamReader(inp, RT.UTF8));
			
			Object EOF = new Object();

			for (;;) {
				
				try {
					Object r = LispReader.read(rdr, false, EOF, false);
					if (r == EOF) {
						break;
					}
					ret = Compiler.eval(r);
					star3.set(star2.get());
					star2.set(star1.get());
					star1.set(ret);
					
				} catch (Exception e) {
					stare.set(e);
					throw e;
				}
			}
		} catch (Exception e) {
			throw new ThinklabScriptException(e);
		} finally {
			Var.popThreadBindings();
		}
		
		/*
		 * FIXME remove
		 */
		// System.out.println("EXECUTED: [" + namespace + "] " + code);
		
		// TODO Auto-generated method stub
		return ret;
	}
	
}
