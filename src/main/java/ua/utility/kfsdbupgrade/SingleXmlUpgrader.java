package ua.utility.kfsdbupgrade;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class SingleXmlUpgrader {

	private static final Logger LOGGER = Logger.getLogger(SingleXmlUpgrader.class.getName());

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			LOGGER.fatal("Usage: Decrypter.jar encryptionKey fileToUpgrade.xml ");
		}

		String docNumber = args[1].split("-")[0];
		// decrypt file, write to disk
		EncryptionService encryptService = new EncryptionService(args[0]);
		String ciphertext = FileUtils.readFileToString(new File(args[1]));
		String decrypted = encryptService.decrypt(ciphertext);
		FileUtils.writeStringToFile(new File(docNumber + "-KFS3.xml"), decrypted);

		// upgrade file
		File f = new File("src/main/resources/MaintainableXMLUpgradeRules.xml");
		MaintainableXMLConversionServiceImpl maintainableXMLConversionServiceImpl = new MaintainableXMLConversionServiceImpl(
				f);
		String newXml = maintainableXMLConversionServiceImpl.transformMaintainableXML(decrypted);
		FileUtils.writeStringToFile(new File(docNumber + "-KFS6.xml"), newXml);

		// encrypt file
		String encrypted = encryptService.encrypt(newXml);
		FileUtils.writeStringToFile(new File(docNumber + "-KFS6-encrypted"), encrypted);

	}

}
