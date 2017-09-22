package ua.utility.kfsdbupgrade;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

public final class MaintainableXMLConversionServiceImplTest {

	@Test
	@Ignore
	// test to manually run a conversion on a developer's workstation
	public void convert() throws Exception {
		File f = new File("src/main/resources/MaintainableXMLUpgradeRules.xml");
		MaintainableXMLConversionServiceImpl maintainableXMLConversionServiceImpl = new MaintainableXMLConversionServiceImpl(
				f);
		String oldXml = FileUtils.readFileToString(new File("/home/quikkian/maintainableXml/323807-KFS3.xml"));
		String newXml = maintainableXMLConversionServiceImpl.transformMaintainableXML(oldXml, "0");
		Logger.getLogger(getClass().getName()).info(newXml);
	}

}
