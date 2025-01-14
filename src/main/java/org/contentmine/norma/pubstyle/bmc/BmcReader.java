package org.contentmine.norma.pubstyle.bmc;

import java.util.ArrayList;
import java.util.List;

import org.contentmine.norma.InputFormat;
import org.contentmine.norma.pubstyle.PubstyleReader;
import org.contentmine.norma.tagger.bmc.HTMLBmcTagger;

public class BmcReader extends PubstyleReader {

	public BmcReader() {
		super();
	}

	public BmcReader(InputFormat type) {
		super(type);
	}

	@Override
	protected void addTaggers() {
		this.addTagger(InputFormat.HTML, new HTMLBmcTagger());
//		this.addTagger(InputFormat.XML, new XMLBmcTagger());
	}

	@Override
	protected List<String> getExtraneousXPaths() {
		return new ArrayList<String>();
	}

}
