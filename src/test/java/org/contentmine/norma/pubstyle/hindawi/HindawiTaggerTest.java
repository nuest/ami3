package org.contentmine.norma.pubstyle.hindawi;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.contentmine.eucl.xml.XMLUtil;
import org.contentmine.norma.NormaFixtures;
import org.contentmine.norma.tagger.AbstractTElement;
import org.contentmine.norma.tagger.MetadataElement;
import org.contentmine.norma.tagger.PubstyleTagger;
import org.contentmine.norma.tagger.TagElement;
import org.contentmine.norma.tagger.hindawi.HTMLHindawiTagger;
import org.contentmine.norma.xsl.TransformerWrapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import nu.xom.Element;

public class HindawiTaggerTest {

	private static final Logger LOG = Logger.getLogger(HindawiTaggerTest.class);
	
	@Test
	public void testMetadataDefinitions() {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		List<MetadataElement> metadataElements = hindawiTagger.getMetadataDefinitions();
		Assert.assertEquals("metadata", 37, metadataElements.size());
	}
	
	@Test
	public void testTagDefinitions() {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		List<TagElement> tagElements = hindawiTagger.getTagElements();
		Assert.assertEquals("tag", 14, tagElements.size());
	}
	
	@Test
	public void testXSLStylsheets() {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		List<Element> xslElements = hindawiTagger.getStyleSheetElements();
		Assert.assertEquals("xsl", 1, xslElements.size());
	}
	
	@Test
	// not quite sure what this does
	@Ignore // for the present
	public void testGrouping() throws Exception {
		File outfile = new File("target/hindawi/507405.grouped.xml");
		TransformerWrapper transformerWrapper = new TransformerWrapper();
	    transformerWrapper.transform(NormaFixtures.F507405_XML, NormaFixtures.GROUP_MAJOR_SECTIONS_XSL, outfile);
	    XMLUtil.equalsCanonically(NormaFixtures.F507405_GROUPED_XHTML, outfile, true);
	    
	    Assert.assertTrue("exists: "+outfile, outfile.exists());
	}

	@Test
	public void testVariables() throws Exception {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		Assert.assertEquals("variables",  3, hindawiTagger.getOrCreateVariableElementList().size());
	}
	
	@Test
	@Ignore // till we sort namespaces
	public void testExpandVariables() throws Exception {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		hindawiTagger.expandVariablesInVariables();
		Assert.assertEquals("variables",  3, hindawiTagger.getOrCreateVariableElementList().size());
		Assert.assertEquals("variable0",  "/*[local-name()='html']/*[local-name()='body']", hindawiTagger.getOrCreateVariableElementList().get(0).getExpandedValue());
		Assert.assertEquals("variable1",  
				"/*[local-name()='html']/*[local-name()='body']/*[local-name()='div' and @id='container']/*[local-name()='div' and @id='content']"
				+ "/*[local-name()='div' and @class='middle_content']", 
				hindawiTagger.getOrCreateVariableElementList().get(1).getExpandedValue());
		Assert.assertEquals("variable2",  
				"/*[local-name()='html']/*[local-name()='body']/*[local-name()='div' and @id='container']/*[local-name()='div' and @id='content']"
				+ "/*[local-name()='div' and @class='middle_content']/*[local-name()='div']/*[local-name()='div' and @class='xml-content']", 
				hindawiTagger.getOrCreateVariableElementList().get(2).getExpandedValue());
	}
	
	@Test
	@Ignore // till we sort namespaces
	public void testExpandTags() throws Exception {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		hindawiTagger.expandVariablesInTags();
		Assert.assertEquals("tags",  14, hindawiTagger.getTagElements().size());
		Assert.assertEquals("tag0",  "/*[local-name()='html']/*[local-name()='body']/*[local-name()='div' and @id='container']/*[local-name()='div' and @id='content']/*[local-name()='div' and @class='middle_content']/*[local-name()='div']/*[local-name()='div' and @class='xml-content']/*[local-name()='section' and @title='Abstract']", hindawiTagger.getTagElements().get(0).getExpandedXPath());
		
	}
	
	@Test
	public void testMetadataExtraction() throws Exception {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		AbstractTElement metadataElementList = hindawiTagger.extractMetadataElements(NormaFixtures.F507405_XML);
		new File("target/hindawi/").mkdirs();
		XMLUtil.debug(metadataElementList, new FileOutputStream("target/hindawi/metadata.xml"), 0);
		Assert.assertEquals("metadata", 16, metadataElementList.size());
	}
	
	@Test
	public void testSectionTagging() throws Exception {
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		Element taggedElement = hindawiTagger.addTagsToSections(NormaFixtures.F507405_GROUPED_XHTML);
		new File("target/hindawi/").mkdirs();
		XMLUtil.debug(taggedElement, new FileOutputStream("target/hindawi/tagged507405.xml"), 0);
//		Assert.assertEquals("metadata", 16, taggedElement.size());
	}
	
	@Test
	/** tags most sections of a typical Hindawi paper.
	 * 
	 * SHOWCASE
	 * 
	 * @throws Exception
	 */
	@Ignore // Jenkins fails with NoSuchMethod (no idea why)
	public void test247835() throws Exception {
		ensureGroupedFile(NormaFixtures.F247835_GROUPED_XHTML, new File(NormaFixtures.F247835_DIR, "fulltext.xml"));
		PubstyleTagger hindawiTagger = new HTMLHindawiTagger();
		Element taggedElement = hindawiTagger.addTagsToSections(NormaFixtures.F247835_GROUPED_XHTML);
		String message = XMLUtil.equalsCanonically(NormaFixtures.F247835_TAGGED_XHTML, taggedElement, true);
		Assert.assertNull("message: "+message, message);
		new File("target/hindawi/").mkdirs();
		XMLUtil.debug(taggedElement, new FileOutputStream("target/hindawi/tagged247835.xml"), 0);
		
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='abstract']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='copyright']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='background']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='results']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='discussion']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='conclusion']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='abbreviations']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='disclosure']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='acknowledgments']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='competing']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='references']").size());
		Assert.assertEquals("x", 1, XMLUtil.getQueryElements(taggedElement, "//*[@tag='methods']").size());
		
		Assert.assertEquals("figure",  3, XMLUtil.getQueryElements(taggedElement, "//*[@tag='figure']").size());
		Assert.assertEquals("table",  7, XMLUtil.getQueryElements(taggedElement, "//*[@tag='table']").size());
	}

	private void ensureGroupedFile(File groupedFile, File rawFile) throws Exception {
		if (XMLUtil.isXMLFile(groupedFile) != null || true) {
			LOG.debug("FORCE TRANSFORM");
			TransformerWrapper transformerWrapper = new TransformerWrapper();
		    transformerWrapper.transform(rawFile, NormaFixtures.GROUP_MAJOR_SECTIONS_XSL, groupedFile);
		}
	}
}
