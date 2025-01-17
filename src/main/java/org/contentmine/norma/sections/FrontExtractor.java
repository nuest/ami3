package org.contentmine.norma.sections;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.contentmine.cproject.files.CTree;
import org.contentmine.graphics.html.HtmlDiv;

public class FrontExtractor extends SectionExtractor /*implements SingleDivExtractor*/ {
	private static final Logger LOG = Logger.getLogger(FrontExtractor.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private FrontExtractor() {
		super();
	}
	
	public FrontExtractor(CTree cTree) {
		this();
		this.cTree = cTree;
	}

	public HtmlDiv getSingleDiv(JATSSectionTagger tagger) { 
		return tagger.getFront();
	}

	@Override
	public String getSection() {return CTree.FRONT;}

	@Override
	public String getDirectoryName() {return CTree.FRONT_DIR;}
	

}
