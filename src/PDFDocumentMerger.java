import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFDocumentMerger {

	public static Logger logger = LoggerFactory.getLogger(PDFDocumentMerger.class);

	public static String root = "L:\\iPad Sync\\Books\\Travel\\";
	public static String acceptedExtension = ".pdf";

	public static void main(String[] args) throws IOException, COSVisitorException, CryptographyException, InvalidPasswordException {

		// The root contains multiple src directories - 1 for each book.
		File srcDir = new File(root);
		for (File dir : srcDir.listFiles()) {
			if (dir.isDirectory()) {
				mergePdfsInDir(dir);
			}
		}
	}

	/**
	 * Merges all PDFs with in the directory based on each PDF's page labels
	 * 
	 * @param srcDir
	 * @throws IOException
	 * @throws COSVisitorException
	 * @throws CryptographyException
	 * @throws InvalidPasswordException
	 */
	private static void mergePdfsInDir(File srcDir) throws IOException, COSVisitorException, CryptographyException,
			InvalidPasswordException {

		if (srcDir.isDirectory()) {
			File[] files = srcDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String name) {
					return name.endsWith(acceptedExtension);
				}
			});

			List<PDDocument> docList = new ArrayList<PDDocument>();
			for (File f : files) {
				docList.add(PDDocument.load(f));
			}

			Collections.sort(docList, getPageLabelComparator());
			PDDocument mergedOutputDocument = new PDDocument();
			PDFMergerUtility merge = new PDFMergerUtility();
			merge.setDestinationFileName(srcDir.getAbsolutePath() + "mergedOutput.pdf");
			for (PDDocument doc : docList) {
				if (doc.isEncrypted()) {
					doc.decrypt("");
				}
				doc.setAllSecurityToBeRemoved(true);
				merge.appendDocument(mergedOutputDocument, doc);
			}
			String outputFileName = srcDir.getAbsolutePath() + System.getProperty("file.separator") + "mergedOutput.pdf";
			mergedOutputDocument.save(outputFileName);
			mergedOutputDocument.close();
			logger.info("Merge Complete - " + outputFileName);
		}
	}

	/**
	 * Comparator used for sorting multiple pdf documents based on page label
	 * (i.e. page numbers)
	 * 
	 * @return
	 */
	private static Comparator<PDDocument> getPageLabelComparator() {

		return new Comparator<PDDocument>() {

			Logger logger = LoggerFactory.getLogger("PageLabelComparator");

			private int coercePageLabelToInt(String pageLabel) {
				logger.info("Coercing Label - " + pageLabel);
				int index = pageLabel.indexOf("-");
				int output;
				if (index > 0) {
					output = Integer.parseInt(pageLabel.substring(0, index));
				} else {
					output = Integer.parseInt(pageLabel);
				}
				logger.info("Coerce Result - " + output);
				return output;
			}

			@Override
			public int compare(PDDocument doc1, PDDocument doc2) {
				try {
					String label1 = doc1.getDocumentCatalog().getPageLabels().getLabelsByPageIndices()[0];
					String label2 = doc2.getDocumentCatalog().getPageLabels().getLabelsByPageIndices()[0];
					return coercePageLabelToInt(label1) - coercePageLabelToInt(label2);
				} catch (Exception e) {
					throw new RuntimeException("Failed to sort by page labels");
				}
			}
		};
	}
}
