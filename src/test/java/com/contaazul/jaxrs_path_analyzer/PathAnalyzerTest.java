package com.contaazul.jaxrs_path_analyzer;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class PathAnalyzerTest extends AbstractMojoTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testPluginIsDeclared() throws Exception {
		File pom = getTestFile("src/test/resources/unit/project-to-test/pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());
		
		PathAnalyzer myMojo = (PathAnalyzer) lookupMojo("analyze-paths", pom);
		assertNotNull(myMojo);
		myMojo.execute();
	}

}
