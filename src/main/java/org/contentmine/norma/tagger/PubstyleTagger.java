package org.contentmine.norma.tagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.contentmine.eucl.xml.XMLUtil;
import org.contentmine.graphics.html.util.HtmlUtil;
import org.contentmine.norma.NAConstants;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.XPathContext;

/** reads a set of tag definitions and uses them to tag an Element and later search it.
 * 
 * @author pm286
 *
 */
public class PubstyleTagger {

	

	private static final Logger LOG = Logger.getLogger(PubstyleTagger.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	public static final String NAME = "name";

	private static final String OR = " | ";
	private static final String OR1 = " or ";
	private static final String AND = " and ";
	private static final String NOT = "not";

	/** all the sections we might tag.
	 * 
	 */
	public static final String ABSTRACT = "abstract";
	public static final String ACKNOWLEDGEMENTS = "acknowledge";
	public static final String ADDITONALFILES = "additionfiles";
	public static final String AUTHORCONTRIB = "authorcontrib";
	public static final String BACKGROUND = "background";
	public static final String COMPETING = "competing";
	public static final String CONCLUSION = "conclusion";
	public static final String DISCUSSION = "discussion";
	public static final String METHODS = "methods";
	public static final String REFERENCES = "references";
	public static final String RESULTS = "results";

	public static final String[] TAGS = {
		ABSTRACT,
		ACKNOWLEDGEMENTS,
		ADDITONALFILES,
		AUTHORCONTRIB,
		BACKGROUND,
		COMPETING,
		CONCLUSION,
		DISCUSSION,
		METHODS,
		REFERENCES,
		RESULTS,
		
		
		};
	
	public static final String TAG = "tagger";
	private static final String STYLESHEET = "stylesheet";
	private static final String ADDED_TAG = "tag";
	
	private List<String> tagNames;
	private List<TagElement> tagElementList;
	private List<MetadataElement> metadataDefinitions;
	private TaggerElement taggerElement;

	private List<Element> styleSheetList;
	private AbstractTElement metadataListElement;
	private TagListElement tagListElement;
	private Map<String, String> metadataByName;
	private List<VariableElement> variableElementList;
	
	private XPathContext xPathContext;
	
	protected PubstyleTagger() {
	}

	protected PubstyleTagger(File taggerFile) {
		this();
		readAndParse(taggerFile);
	}

	protected PubstyleTagger(String taggerResource) {
		this();
		LOG.trace("resource: "+taggerResource);
		InputStream inputStream = this.getClass().getResourceAsStream(taggerResource);
		if (inputStream == null) {
			throw new RuntimeException("Cannot parse input resource: "+taggerResource);
		}
		readAndParse(inputStream);
	}

	private void readAndParse(InputStream inputStream) {
		taggerElement = (TaggerElement) AbstractTElement.createElement(inputStream);
		createTagListAndMetadata();
	}

	private void readAndParse(File taggerFile) {
		taggerElement = (TaggerElement) AbstractTElement.createElement(taggerFile);
		createTagListAndMetadata();
	}

	private void createTagListAndMetadata() {
		metadataListElement = taggerElement.getMetadataListElement();
		tagListElement = taggerElement.getTagListElement();
		this.expandVariablesInTags();
		setXpathContext(HtmlUtil.XHTML_XPATH);
	}

	private void setXpathContext(XPathContext xPathContext) {
		this.xPathContext = xPathContext;
	}

	protected TaggerElement getTaggerElement() {
		return taggerElement;
	}

	/** tagging through the class attribute.
	 * 
	 * his can have a whitespace separated list of values, so
	 * create it if it doesn't exists and append if it does.
	 * 
	 * @param tagName
	 * @param element
	 */
	static void addTag(String tagName, Element element) {
		Attribute attribute = element.getAttribute(ADDED_TAG);
		if (attribute == null) {
			attribute = new Attribute(ADDED_TAG, tagName);
			element.addAttribute(attribute);
		}
	}
	
	static String getTagName(Element element) {
		String tag = element.getAttributeValue(ADDED_TAG);
		return tag;
	}

	public List<Element> findSectionsFromTagDefinitions(Element elementToSearch, String tagName) {
		String xpath = getXpathForTag(tagName);
		if (xpath != null) {
			return XMLUtil.getQueryElements(elementToSearch, xpath);
		}
		return new ArrayList<Element>();
	}

	public String getName() {
		return taggerElement.getAttributeValue(NAME);
	}
	
	protected String getXpathForTag(String tagName) {
		LOG.trace("tagName "+tagName);
		TagElement tagElement = getTagWithName(tagName);
		String xpath = (tagElement == null) ? null : tagElement.getXPath();
		LOG.trace("xpath "+xpath);
		return xpath;
	}

	protected String getExpandedXpathForTag(String tagName) {
		TagElement tagElement = getTagWithName(tagName);
		String xpath = (tagElement == null) ? null : tagElement.getExpandedXPath();
		return xpath;
	}

	public List<MetadataElement> getMetadataDefinitions() {
		if (metadataDefinitions == null) {
			metadataDefinitions = new ArrayList<MetadataElement>();
			List<Element> tagElements = XMLUtil.getQueryElements(metadataListElement, "*[local-name()='"+MetadataElement.TAG+"']");
			for (Element tagElement : tagElements) {
				metadataDefinitions.add((MetadataElement)tagElement);
			}
		}
		return metadataDefinitions;
	}

	public List<Element> getStyleSheetElements() {
		if (styleSheetList == null) {
			styleSheetList = XMLUtil.getQueryElements(taggerElement, "*[local-name()='"+STYLESHEET+"']");
		}
		return styleSheetList;
	}

	public List<TagElement> getTagElements() {
		if (tagElementList == null) {
			tagElementList = new ArrayList<TagElement>();
			List<Element> tagElements = XMLUtil.getQueryElements(tagListElement, "*[local-name()='"+TagElement.TAG+"']");
			for (Element tagElement : tagElements) {
				tagElementList.add((TagElement)tagElement);
			}
		}
		return tagElementList;
	}

	/** return all tags with given name.
	 * 
	 * should only be one or none.
	 * 
	 * @param tagName
	 * @return
	 */
	public TagElement getTagWithName(String tagName) {
		List<Element> tagElements = XMLUtil.getQueryElements(tagListElement, "*[local-name()='"+TagElement.TAG+"' and @name='"+tagName+"']");
		return tagElements.size() != 1 ? null : (TagElement) tagElements.get(0);
	}
	
	protected List<String> getTagNames() {
		if (tagNames == null) {
			List<TagElement> tagElements = getTagElements();
			tagNames = new ArrayList<String>();
			for (Element tagElement : tagElements) {
				String tagName = ((TagElement)tagElement).getName();
				LOG.trace("tag: "+tagName);
				tagNames.add(tagName);
			}
		}
		return tagNames;
	}

	public Element addTagsToSections(Element elementToTag) {
		getTagNames();
		int count = 0;
		LOG.trace("tag names "+tagNames);
		for (String tagName : tagNames) {
			List<Element> sections = findSectionsFromMatchingTags(elementToTag, tagName);
			for (Element section : sections) {
				PubstyleTagger.addTag(tagName, section);
				count++;
			}
		}
		LOG.trace("tagged "+count+" sections");
		return elementToTag;
	}

	/** query with a single section name.
	 * 
	 * @param element containing sections
	 * @param tagName for xpath
	 * @return list of sections
	 */
	public List<Element> findSectionsFromMatchingTags(Element element, String tagName) {
		String xpath = getExpandedXpathForTag(tagName);
		if (xpath == null) {
			throw new RuntimeException("Cannot find xpath definition for: "+tagName);
		}
		List<Element> elements = (xPathContext == null) ?
			XMLUtil.getQueryElements(element, xpath) :
			XMLUtil.getQueryElements(element, xpath, xPathContext);
		return elements;
	}

	public void debug(String msg) {
		XMLUtil.debug(taggerElement, msg);
	}

	public AbstractTElement extractMetadataElements(File file) throws Exception {
		Document doc = new Builder().build(new FileInputStream(file));
		return doc == null ? new MetadataListElement() : extractMetadataElements(doc.getRootElement());
	}

	private MetadataListElement extractMetadataElements(Element element) {
		getMetadataDefinitions();
		metadataByName = new HashMap<String, String>();
		MetadataListElement metadataListElement = new MetadataListElement();
		for (MetadataElement metadataDefinition : metadataDefinitions) {
			String name = metadataDefinition.getName();
			String oldValue = metadataByName.get(name);
			if (oldValue == null) {
				String xpath = metadataDefinition.getXPath();
				List<String> valueList = HtmlUtil.getQueryHtmlStrings(element, xpath);
				if (valueList.size() > 0) {
					String newValue = valueList.get(0);
					metadataByName.put(name,  newValue);
					MetadataElement metadataElement = new MetadataElement(name, newValue);
					metadataListElement.appendChild(metadataElement);
				}
			}
		}
		
		return metadataListElement;
	}

	public Element addTagsToSections(File file) throws Exception {
		Document doc = new Builder().build(new FileInputStream(file));
		LOG.trace("tagging "+file);
		return doc == null ? new MetadataListElement() : addTagsToSections(doc.getRootElement());
	}

	public List<VariableElement> getOrCreateVariableElementList() {
		if (variableElementList == null) {
			List<Element> variableElements = XMLUtil.getQueryElements(tagListElement, "*[local-name()='"+VariableElement.TAG+"']");
			variableElementList = new ArrayList<VariableElement>();
			for (Element variableElement : variableElements) {
				variableElementList.add((VariableElement) variableElement);
			}
		}
		return variableElementList;
	}

	public void expandVariablesInVariables() {
		getOrCreateVariableElementList();
		for (VariableElement variableElement : variableElementList) {			
			variableElement.expandVariablesInValue(variableElementList);
		}
	}

	public void expandVariablesInTags() {
		getTagElements();
		expandVariablesInVariables();
		for (TagElement tagElement : tagElementList) {
			tagElement.expandVariablesInValue(variableElementList);
		}
	}

}
