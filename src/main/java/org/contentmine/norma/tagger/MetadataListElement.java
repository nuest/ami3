package org.contentmine.norma.tagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.contentmine.eucl.xml.XMLUtil;

import nu.xom.Element;

public class MetadataListElement extends AbstractTElement {

	private static final Logger LOG = Logger.getLogger(TagListElement.class);
	
	public static final String TAG_DEFINITION_NAME = "name";
	public static final String TAG = "metadataList";

	private List<MetadataElement> metadataList;
	
	public MetadataListElement() {
		super(TAG);
	}

	public String getName() {
		return this.getAttributeValue(TAG_DEFINITION_NAME);
	}
	
	public List<MetadataElement> getMetadataElements() {
		List<Element> taggerList0 = XMLUtil.getQueryElements(this, MetadataElement.TAG);
		metadataList = new ArrayList<MetadataElement>();
		for (Element tag : taggerList0) {
			metadataList.add((MetadataElement) tag);
		}
		return metadataList;
	}
	

}
