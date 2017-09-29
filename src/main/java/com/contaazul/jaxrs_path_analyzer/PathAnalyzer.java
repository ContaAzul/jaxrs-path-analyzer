package com.contaazul.jaxrs_path_analyzer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;

@Mojo(name = "analyze-paths", defaultPhase = LifecyclePhase.INSTALL)
public class PathAnalyzer extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	private static final DotName PATH_ANNOTATION = DotName.createSimple("javax.ws.rs.Path");
	private static final DotName GET_ANNOTATION = DotName.createSimple("javax.ws.rs.GET");
	private static final DotName POST_ANNOTATION = DotName.createSimple("javax.ws.rs.POST");
	private static final DotName PUT_ANNOTATION = DotName.createSimple("javax.ws.rs.PUT");
	private static final DotName DELETE_ANNOTATION = DotName.createSimple("javax.ws.rs.DELETE");
	private static final DotName OPTIONS_ANNOTATION = DotName.createSimple("javax.ws.rs.OPTIONS");
	private static final DotName HEAD_ANNOTATION = DotName.createSimple("javax.ws.rs.HEAD");
	private static final DotName PRODUCES_ANNOTATION = DotName.createSimple("javax.ws.rs.Produces");
	private static final DotName CONSUMES_ANNOTATION = DotName.createSimple("javax.ws.rs.Consumes");

	public void execute() throws MojoExecutionException {
		try {
			File classesDirectory = new File(outputDirectory.getAbsolutePath() + "/classes");
			URLClassLoader classLoader = getClassLoader(classesDirectory);
			List<String> classNames = getClassNames(classesDirectory, classesDirectory.getAbsolutePath());
			analyzeClasses(classLoader, classNames);
		} catch (MojoExecutionException e) {
			getLog().debug(e);
			throw e;
		} catch (IOException e) {
			getLog().debug(e);
			throw new MojoExecutionException("Better debug, I don't know what is goin on :(", e);
		}
	}

	private URLClassLoader getClassLoader(File classesDirectory) throws MalformedURLException {
		URL classesUrl = classesDirectory.toURI().toURL();
		return URLClassLoader.newInstance(new URL[] { classesUrl }, getClass().getClassLoader());
	}

	private List<String> getClassNames(File classesDirectory, String absolutePath) {
		List<String> classNames = new ArrayList<String>();
		addClassNames(classNames, classesDirectory, classesDirectory.getAbsolutePath());
		return classNames;
	}

	private void analyzeClasses(URLClassLoader classLoader, List<String> classNames)
			throws IOException, MojoExecutionException {
		Map<String, String> paths = new LinkedHashMap<String, String>();
		for (String className : classNames) {
			getLog().debug("- Scanning class: " + className);
			Index index = getIndexForClass(classLoader, className);
			List<AnnotationInstance> annotations = index.getAnnotations(PATH_ANNOTATION);
			process(paths, className, annotations);

		}
		for (String key : paths.keySet())
			getLog().debug(key);
	}

	private void process(Map<String, String> paths, String className, List<AnnotationInstance> annotations)
			throws MojoExecutionException {
		AnnotationInstance rootAnnotation = getRootAnnotation(annotations);

		processRootAnnotation(paths, className, rootAnnotation);

		String pathInClass = getRootPath(rootAnnotation);

		for (AnnotationInstance annotation : annotations) {
			AnnotationValue value = annotation.value();
			if (value != null) {
				if (annotation.target().kind() != (AnnotationTarget.Kind.CLASS)) {
					MethodInfo annotatedMethod = annotation.target().asMethod();
					String path = new String();
					path = addHttpVerb(path, annotatedMethod);
					path = addProducer(path, annotatedMethod);
					path = addConsumer(path, annotatedMethod);
					String fullPath = path.concat(
							pathInClass != null ? pathInClass.concat("/").concat(value.asString()) : value.asString());
					addInPaths(paths, className, fullPath);
				}
			}
		}
	}

	private String getRootPath(AnnotationInstance rootAnnotation) {
		if (rootAnnotation == null)
			return null;
		String pathInClass = rootAnnotation.value().asString();
		return pathInClass;
	}

	private void processRootAnnotation(Map<String, String> paths, String className, AnnotationInstance rootAnnotation)
			throws MojoExecutionException {
		if (rootAnnotation == null)
			return;
		List<MethodInfo> methods = rootAnnotation.target().asClass().methods();
		for (MethodInfo method : methods)
			if (!containsPathAnnotation(method)) {
				String httpVerb = getHttpVerb(method);
				if (httpVerb != null) {
					String path = httpVerb;
					path = addProducer(path, method);
					path = addConsumer(path, method);
					path = path.concat("/").concat(getRootPath(rootAnnotation));
					addInPaths(paths, className, path);
				}
			}
	}

	private String addHttpVerb(String path, MethodInfo method) {
		String httpVerb = getHttpVerb(method);
		if (httpVerb != null)
			return httpVerb.concat(path);
		return path;
	}

	private String addConsumer(String path, MethodInfo method) {
		String consumer = getConsumer(method);
		if (consumer != null)
			return path.concat(consumer);
		return path;
	}

	private String addProducer(String path, MethodInfo method) {
		String producer = getProducer(method);
		if (producer != null)
			return path.concat(producer);
		return path;
	}

	private boolean containsPathAnnotation(MethodInfo method) {
		List<AnnotationInstance> annotations = method.annotations();
		for (AnnotationInstance annotation : annotations) {
			DotName annotationName = annotation.name();
			if (annotationName.equals(PATH_ANNOTATION)) {
				return true;
			}
		}
		return false;
	}

	private String getHttpVerb(MethodInfo method) {
		getLog().debug("Getting http verb from method:");
		getLog().debug(method.name());

		List<AnnotationInstance> methodAnnotations = method.annotations();
		for (AnnotationInstance annotation : methodAnnotations) {
			DotName annotationName = annotation.name();
			if (!annotationName.equals(PATH_ANNOTATION)) {
				if (annotationName.equals(GET_ANNOTATION))
					return "GET ";
				if (annotationName.equals(POST_ANNOTATION))
					return "POST ";
				if (annotationName.equals(PUT_ANNOTATION))
					return "PUT ";
				if (annotationName.equals(DELETE_ANNOTATION))
					return "DELETE ";
				if (annotationName.equals(OPTIONS_ANNOTATION))
					return "OPTIONS ";
				if (annotationName.equals(HEAD_ANNOTATION))
					return "HEAD ";
			}
		}
		return null;
	}

	private String getProducer(MethodInfo method) {
		getLog().debug("Getting producer from method:");
		getLog().debug(method.name());

		List<AnnotationInstance> methodAnnotations = method.annotations();
		for (AnnotationInstance annotation : methodAnnotations) {
			DotName annotationName = annotation.name();
			if (annotationName.equals(PRODUCES_ANNOTATION)) {
				return "PRODUCES(".concat(getValueFromAnnotation(annotation)).concat(") ");
			}

		}
		return null;
	}

	private String getValueFromAnnotation(AnnotationInstance annotation) {
		return ((AnnotationValue[]) annotation.value().value())[0].asString();
	}

	private String getConsumer(MethodInfo method) {
		getLog().debug("Getting producer from method:");
		getLog().debug(method.name());

		List<AnnotationInstance> methodAnnotations = method.annotations();
		for (AnnotationInstance annotation : methodAnnotations) {
			DotName annotationName = annotation.name();
			if (annotationName.equals(CONSUMES_ANNOTATION))
				return "CONSUMES(".concat(getValueFromAnnotation(annotation)).concat(") ");
		}
		return null;
	}

	private AnnotationInstance getRootAnnotation(List<AnnotationInstance> annotations) {
		for (AnnotationInstance annotation : annotations)
			if (annotation.target().kind() == (AnnotationTarget.Kind.CLASS))
				return annotation;
		return null;
	}

	private Index getIndexForClass(URLClassLoader classLoader, String className) throws IOException {
		Indexer indexer = new Indexer();
		indexer.index(classLoader.getResourceAsStream(className));
		return indexer.complete();
	}

	private void addInPaths(Map<String, String> paths, String className, String path) throws MojoExecutionException {
		if (paths.containsKey(path))
			throw new MojoExecutionException("Double mapping " + path + " from class " + className
					+ " already found at class " + paths.get(path));
		paths.put(path, className);
	}

	private void addClassNames(List<String> classNames, File file, String absolutePath) {
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			for (File childFile : childFiles)
				addClassNames(classNames, childFile, absolutePath);
		}
		if (file.isFile() && file.getAbsolutePath().endsWith(".class"))
			classNames.add(file.getAbsolutePath().replace(absolutePath + "/", ""));
	}
}
