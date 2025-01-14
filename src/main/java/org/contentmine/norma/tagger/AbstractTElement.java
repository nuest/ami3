package org.contentmine.norma.tagger;

import java.io.File;
import java.io.InputStream;

import org.contentmine.eucl.xml.XMLUtil;

import nu.xom.Comment;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;

public abstract class AbstractTElement extends Element {

	public AbstractTElement(String tag) {
		super(tag);
	}
	
	public static AbstractTElement createElement(InputStream inputStream) {
		Element element = XMLUtil.parseQuietlyToDocument(inputStream).getRootElement();
		return createTagDefinitionsElement(element);
	}

	public static AbstractTElement createElement(File file) {
		Element element = XMLUtil.parseQuietlyToDocument(file).getRootElement();
		return createTagDefinitionsElement(element);
	}

	private static AbstractTElement createTagDefinitionsElement(Element element) {
		AbstractTElement tagDefinitionsElement = AbstractTElement.createElement(element);
		return tagDefinitionsElement;
	}

	/** 
	 * Copy constructor from non-subclassed elements
	 */
	public static AbstractTElement createElement(Element element) {
		AbstractTElement newElement = null;
		String tag = element.getLocalName();
		if (tag == null || tag.equals("")) {
			throw new RuntimeException("no tag");
		} else if (tag.equals(TaggerElement.TAG)) {
			newElement = new TaggerElement();
		} else if (tag.equals(TagElement.TAG)) {
			newElement = new TagElement();
		} else if (tag.equals(TagListElement.TAG)) {
			newElement = new TagListElement();
		} else if (tag.equals(MetadataElement.TAG)) {
			newElement = new MetadataElement();
		} else if (tag.equals(MetadataListElement.TAG)) {
			newElement = new MetadataListElement();
		} else if (tag.equals(StylesheetElement.TAG)) {
			newElement = new StylesheetElement();
		} else if (tag.equals(VariableElement.TAG)) {
			newElement = new VariableElement();
		} else {
			throw new RuntimeException("unsupported tag element: "+tag);
		}
		if (newElement != null && !(tag.equals(StylesheetElement.TAG))) {
			XMLUtil.copyAttributes(element, newElement);
	        createSubclassedChildren(element, newElement);
		}
        return newElement;
	}

	protected static void createSubclassedChildren(Element oldElement, Element newElement) {
		if (oldElement != null) {
			for (int i = 0; i < oldElement.getChildCount(); i++) {
				Node node = oldElement.getChild(i);
				Node newNode = null;
				if (node instanceof Text) {
					String value = node.getValue();
					newNode = new Text(value);
				} else if (node instanceof Comment) {
					newNode = new Comment(node.getValue());
				} else if (node instanceof ProcessingInstruction) {
					newNode = new ProcessingInstruction((ProcessingInstruction) node);
				} else if (node instanceof Element) {
					newNode = createElement((Element) node);
				} else {
					throw new RuntimeException("Cannot create new node: "+node.getClass());
				}
				newElement.appendChild(newNode);
			}
		}
	}

	public int size() {
		return this.getChildElements().size();
	}
	
	

}
