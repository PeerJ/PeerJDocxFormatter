package peerjDocxFormatter;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.docx4j.TraversalUtil;
import org.docx4j.finders.SectPrFinder;
import org.docx4j.model.structure.PageDimensions;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.Spacing;
import org.docx4j.wml.STPageOrientation;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.SectPr.PgMar;
import org.docx4j.wml.SectPr.PgSz;
import org.docx4j.wml.CTLineNumber;
import org.docx4j.wml.STLineNumberRestart;

public class PeerJDocxFormatter {
	public static void main (String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		Options options = new Options();

		options.addOption("h", false, "help");
		options.addOption("m", true, "margins left,top,right,bottom");
		options.addOption("l",  true, "line numbering distance");
		options.addOption("r",  false, "remove headers and footers");
		options.addOption("i",  true, "input docx file");
		options.addOption("o",  true, "output docx file");

		boolean removeHeaderFooters = true;
		double lineNumberingDistance = 0;
		PgMar pageMargins = null;
		String inputFilename = null;
    	String outputFilename = null;
    	File inputFile = null; 
    	
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			
			if (cmd.hasOption("h")) {
				formatter.printHelp( "DocxJ", options );
				return;
			}

			// required options
			inputFilename = cmd.getOptionValue("i");
			outputFilename = cmd.getOptionValue("o");
			
			try {
				inputFile = new File(inputFilename);
				if (!inputFile.isFile()) {
					throw new Exception("File doesn't exist");
				}	
			} catch (Exception e) {
				System.out.println(String.format("%s file does not exist", inputFilename));
				System.exit(1);
			}

			// optional
			removeHeaderFooters = cmd.hasOption("r");
			
			if (cmd.hasOption("l")) {
				lineNumberingDistance = Double.parseDouble(cmd.getOptionValue("l"));
			}

			if (cmd.hasOption("m")) {
				String marginOption = cmd.getOptionValue("m");
				String[] margins = marginOption.split(","); 
				
				pageMargins = new PgMar();
	    		pageMargins.setLeft(cmToDxa(Double.parseDouble(margins[0])));
	    		pageMargins.setTop(cmToDxa(Double.parseDouble(margins[1])));
	    		pageMargins.setRight(cmToDxa(Double.parseDouble(margins[2])));
	    		pageMargins.setBottom(cmToDxa(Double.parseDouble(margins[3])));
			}
		} catch (Exception e) {
			formatter.printHelp( "DocxJ", options );
			e.printStackTrace();
			return;
		}

    	try {
    		WordprocessingMLPackage wordMLPackage;
    		wordMLPackage = WordprocessingMLPackage.load(inputFile);

    		MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();

    		SectPrFinder finder = new SectPrFinder(mdp);
    		new TraversalUtil(mdp.getContent(), finder);
    		for (SectPr sectPr : finder.getOrderedSectPrList()) {
    			// always force us letter, but keep page orientation
    			if (sectPr.getPgSz().getOrient() == STPageOrientation.LANDSCAPE) {
    				sectPr.setPgSz(getUSLetterLandscapePageSize());
    			} else {
    				sectPr.setPgSz(getUSLetterPortraitPageSize());
    			}

    			if (lineNumberingDistance > 0) {
    				CTLineNumber lineNumbering = getLineNumbering(cmToDxa(lineNumberingDistance)); 
    				sectPr.setLnNumType(lineNumbering);
    			} else if (lineNumberingDistance < 0) {    				
    				CTLineNumber lineNumbering = removeLineNumbering(); 
    				sectPr.setLnNumType(lineNumbering);
    			}
    			
    			// TODO: Double spacing?
    			
    			if (pageMargins != null) {
    				sectPr.setPgMar(pageMargins);
    			}
    			
    			if (removeHeaderFooters) {
    				// remove header/footer references
    				sectPr.getEGHdrFtrReferences().clear();
    			}
    			
    			
    		}
    		
    		if (removeHeaderFooters) {
    			removeHeaderFooters(mdp);
    		}
    		
    		/* Needs further investigation
    		ClassFinder pFinder = new ClassFinder(P.class);    		
    		new TraversalUtil(mdp.getContent(), pFinder);
    		for (Object p: pFinder.results) {
    			((P)p).setPPr(setDoubleSpacing());
    		}
    		*/

    		File outputFile = new File(outputFilename);
    		wordMLPackage.save(outputFile);
    		
    		if (!outputFile.isFile()) {
    			throw new Exception("Failed to generate output file");
    		}

    		System.out.println(String.format("Finished converting %s", outputFilename));
    	} catch (Exception e) {
    		System.out.println(String.format("Error!! Failed converting %s", outputFilename));
    		e.printStackTrace();
    		System.exit(1);
    	}
    }

	private static BigInteger cmToDxa(double cm) {
		// http://www.asknumbers.com/CentimetersToPointsConversion.aspx
		// https://startbigthinksmall.wordpress.com/2010/01/04/points-inches-and-emus-measuring-units-in-office-open-xml/
		double points = cm * 28.3464567;
		double dxa = points * 20;
		return BigInteger.valueOf((long)dxa);
	}

	@SuppressWarnings("unused")
	private static PPr setDoubleSpacing()
	{
		// not exactly, changes to fixed 1.0cm
		// leaving out for now
		Spacing s = new Spacing();
		s.setLine(cmToDxa(1));
		PPr ppr = new PPr();
		ppr.setSpacing(s);
		//System.out.println(String.format("Spacing %s", ppr.getSpacing().g));

		return ppr;
	}
	
	private static CTLineNumber getLineNumbering(BigInteger distance) {
		CTLineNumber n = new CTLineNumber();
		n.setCountBy(BigInteger.valueOf(1));
		n.setDistance(distance);
		n.setStart(BigInteger.valueOf(1));
		n.setRestart(STLineNumberRestart.CONTINUOUS);
		
		return n;
	}
	
	private static CTLineNumber removeLineNumbering() {
		CTLineNumber n = new CTLineNumber();

		return n;		
	}
	
	private static PgSz getUSLetterLandscapePageSize() {
		PageDimensions page = new PageDimensions();
		page.setPgSize(PageSizePaper.LETTER, true);

		return page.getPgSz();
	}

	private static PgSz getUSLetterPortraitPageSize() {
		PageDimensions page = new PageDimensions();
		page.setPgSize(PageSizePaper.LETTER, false);

		return page.getPgSz();
	}
	
	private static void removeHeaderFooters(MainDocumentPart mdp) {
		 // Remove rels
		List<Relationship> hfRels = new ArrayList<Relationship>();
		for (Relationship rel : mdp.getRelationshipsPart().getRelationships().getRelationship() ) {
			if (rel.getType().equals(Namespaces.HEADER) || rel.getType().equals(Namespaces.FOOTER)) {
				hfRels.add(rel);
			}
		}
		for (Relationship rel : hfRels ) {
			mdp.getRelationshipsPart().removeRelationship(rel);
		}    		
	}
}

