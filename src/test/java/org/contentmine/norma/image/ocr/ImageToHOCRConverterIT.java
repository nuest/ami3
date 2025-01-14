package org.contentmine.norma.image.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.contentmine.cproject.args.DefaultArgProcessor;
import org.contentmine.graphics.svg.SVGElement;
import org.contentmine.graphics.svg.SVGSVG;
import org.contentmine.image.ImageUtil;
import org.contentmine.norma.NormaFixtures;
import org.contentmine.norma.input.pdf.PDF2ImagesConverter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("long")
public class ImageToHOCRConverterIT {

	private final static Logger LOG = Logger.getLogger(ImageToHOCRConverterIT.class);
	static {LOG.setLevel(Level.DEBUG);}
	
	@Test
	public void testConvert() throws Exception {
		HOCRConverter converter = new HOCRConverter();
		File infile = new File(NormaFixtures.TEST_PUBSTYLE_DIR, "neuro/image.2.1.Im0.png.png");
		File outfile = new File("target/neuro/image.2.1.hocr");
		converter.convertImageToHOCR(infile, outfile);
	}
	
	@Test
	public void testConvertToSVG() throws Exception {
		HOCRConverter converter = new HOCRConverter();
		File infile = new File(NormaFixtures.TEST_PUBSTYLE_DIR, "neuro/image.2.1.Im0.png.png");
		File outfileRoot = new File("target/neuro/image.2.1.hocr");
		File outfile = converter.convertImageToHOCR(infile, outfileRoot);
		if (outfile == null) {
			LOG.debug("cannot run tesseract");
		} else {
			Assert.assertTrue("outfile exists", outfile.exists());
			Assert.assertEquals("filename", "image.2.1.hocr.html", outfile.getName());
			HOCRReaderOLD hocrReader = new HOCRReaderOLD();
			hocrReader.readHOCR(new FileInputStream(outfile));
			SVGElement svgg = hocrReader.getOrCreateSVG();
			Assert.assertNotNull("svgg not null", svgg);
			SVGSVG.wrapAndWriteAsSVG(svgg, new File("target/neuro/image.2.1.hocr.svg"));
		}
	}
	
	@Test
	@Ignore // not fully tested for overlap
	public void testConvertPDFToSVG() throws Exception {
		File infile = new File(NormaFixtures.TEST_PUBSTYLE_DIR, "neuro/Chen2005.pdf");
//		File infile = new File(Fixtures.TEST_PUBSTYLE_DIR, "neuro/Maggio2009.pdf");
		analyzePDF(infile);
	}

	@Test
	@Ignore("closed access")
	public void testConvertPNGsToSVG() throws Exception {
		File ERIN_PNG = new File(NormaFixtures.TEST_PUBSTYLE_DIR, "neuro/erinPngs/");
		File[] pngs = new File[] { 
				
				new File(new File(ERIN_PNG, "chen2006"), "raw.png"),
				new File(new File(ERIN_PNG, "Gant"), "raw.png"),
				new File(new File(ERIN_PNG, "MaggioSegal_PC1_DH"), "raw.png"),
				new File(new File(ERIN_PNG, "pedarzani"), "raw.png"),
				new File(new File(ERIN_PNG, "pedarzani2001"), "raw.png"),
				new File(new File(ERIN_PNG, "podlogar"), "raw.png"),
				new File(new File(ERIN_PNG, "Staff1"), "raw.png"),
				new File(new File(ERIN_PNG, "White"), "raw.png"),
		};
		analyzePNG(new File("target/neuro/erinpngs"), pngs);
	}

	private void analyzePNG(File outputDir, File[] infiles) throws Exception {
		outputDir.mkdirs();
		String imageSuffix = "png";
		for (File file : infiles) {
			DefaultArgProcessor.CM_LOG.debug(file.toString());
			String root = FilenameUtils.getBaseName(file.getParentFile().getName());
			DefaultArgProcessor.CM_LOG.debug(root);
			BufferedImage image = ImageUtil.readImage(file);
			HOCRReaderOLD hocrReader = new HOCRReaderOLD();
			hocrReader.setMaxFontSize(50.);
			hocrReader.labelSubImages("[A-Za-z]");
			File outputDir1 = new File(outputDir, root);
			outputDir1.mkdirs();
			hocrReader.createHTMLandSVG(outputDir1, imageSuffix, image, root);
		}
	}

	private void analyzePDF(File infile) throws Exception {
		PDF2ImagesConverter pdf2ImagesConverter = new PDF2ImagesConverter();
		List<NamedImage> namedImageList = pdf2ImagesConverter.readPDF(new FileInputStream(infile));
		File outputDir = new File("target/neuro/images");
		outputDir.mkdirs();
		String imageSuffix = "png";
		HOCRReaderOLD hocrReader = new HOCRReaderOLD();
		hocrReader.setMaxFontSize(50.);
		hocrReader.labelSubImages("[A-Za-z]");
//		hocrReader.setImageMarginX(50);
//		hocrReader.setImageMarginY(50);
		for (NamedImage namedImage : namedImageList) {
			System.out.println("-------"+namedImage.getKey()+"--------");
			hocrReader.createHTMLandSVG(outputDir, imageSuffix, namedImage);
		}
	}


}
