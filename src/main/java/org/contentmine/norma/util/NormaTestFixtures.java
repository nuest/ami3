package org.contentmine.norma.util;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.contentmine.cproject.args.DefaultArgProcessor;
import org.contentmine.cproject.files.CProject;
import org.contentmine.cproject.files.CTree;
import org.contentmine.cproject.files.CTreeList;
import org.contentmine.eucl.xml.XMLUtil;
import org.contentmine.norma.NormaArgProcessor;

import nu.xom.Document;

public class NormaTestFixtures {
	private static final Logger LOG = Logger.getLogger(NormaTestFixtures.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	


	public static void checkScholarlyHtml(File target, String start) {
		CTree cTree = new CTree(target);
		File shtml = cTree.getExistingScholarlyHTML();
		if (shtml == null || !shtml.exists()) {
			throw new RuntimeException("No SHTML found "+target);
		}
		Document doc = XMLUtil.parseQuietlyToDocument(shtml);
		if (doc == null) {
			throw new RuntimeException("Cannot parse as ScholarlyHtml "+shtml);
		}
		String text = doc.toXML();
		text = text.replaceAll("\\n\\s*", "");
		String tesc = text.replaceAll("\"", "\\\\\\\"");
		if (!text.startsWith(start)) {
			String t300 = tesc.substring(0,  Math.min(300, text.length()));
			LOG.debug("\n"+t300);
			throw new RuntimeException("ScholarlyHtml does not start correctly: "+shtml+":: "+ text);
		}
	}

	/**
	 * 
	 * @param dir
	 * @param type "project" or "ctree"
	 * @param transform transform (nlm2html, etc.)
	 */
	public static void runNorma(File dir, String type, String transform) {
		// norma-lise
		// check if already normalized
		String args = null;
		if (type.equals("project")) {
			CProject project = new CProject(dir);
			CTreeList cTreeList = project.getOrCreateCTreeList();
			for (CTree cTree : cTreeList) {
				if (!cTree.hasScholarlyHTML()) {
					// mising SHTML, normalize all
					args = "-i fulltext.xml  --transform "+transform+" -o scholarly.html --"+type+" "+dir;
					break;
				}
			}
		} else if (type.equals("ctree")) {
			CTree cTree = new CTree(dir);
			if (!cTree.hasScholarlyHTML()) {
				// mising SHTML, normalize all
				args = "-i fulltext.xml  --transform "+transform+" -o scholarly.html --"+type+" "+dir;
			}
		}
		if (args != null) {
			DefaultArgProcessor argProcessor = new NormaArgProcessor(args);
			argProcessor.runAndOutput();
		}
	}

}
