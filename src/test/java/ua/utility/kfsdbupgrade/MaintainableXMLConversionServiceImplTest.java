package ua.utility.kfsdbupgrade;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

public final class MaintainableXMLConversionServiceImplTest {

	@Test
	public void convert() throws Exception {
		File f = new File("src/main/resources/MaintainableXMLUpgradeRules.xml");
		MaintainableXMLConversionServiceImpl maintainableXMLConversionServiceImpl = new MaintainableXMLConversionServiceImpl(
				f);
		String oldXml = FileUtils.readFileToString(new File("/home/quikkian/maintainableWithPerson.xml"));
		String newXml = maintainableXMLConversionServiceImpl.transformMaintainableXML(oldXml);
		Logger.getLogger(getClass().getName()).info(newXml);
	}

}