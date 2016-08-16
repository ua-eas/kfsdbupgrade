package ua.utility.kfsdbupgrade;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class Encrypter {

	private static final Logger LOGGER = Logger.getLogger(Encrypter.class.getName());

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			LOGGER.fatal("Usage: Encrypter.jar encryptionKey fileToEncrypt.xml");
		}
		File file = new File(args[1]);
		EncryptionService encryptService = new EncryptionService(args[0]);
		String plaintext = FileUtils.readFileToString(file);
		String ciphertext = encryptService.encrypt(plaintext);
		FileUtils.writeStringToFile(file, ciphertext);
	}
}
