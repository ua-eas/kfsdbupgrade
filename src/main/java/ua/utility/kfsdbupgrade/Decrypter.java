package ua.utility.kfsdbupgrade;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class Decrypter {

	private static final Logger LOGGER = Logger.getLogger(Decrypter.class.getName());

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			LOGGER.fatal("Usage: Decrypter.jar encryptionKey fileToDecrypt.xml");
		}
		EncryptionService encryptService = new EncryptionService(args[0]);
		String ciphertext = FileUtils.readFileToString(new File(args[1]));
		String decrypted = encryptService.decrypt(ciphertext);
		LOGGER.info(decrypted);
	}

}
