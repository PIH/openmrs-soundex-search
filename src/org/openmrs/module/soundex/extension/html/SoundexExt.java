package org.openmrs.module.soundex.extension.html;

import org.openmrs.module.web.extension.LinkExt;

public class SoundexExt extends LinkExt {
	
	@Override
	public String getLabel() {
		return "Soundex";
	}
	
	@Override
	public String getRequiredPrivilege() {
		//TODO check privileges
		return null;
	}
	
	@Override
	public String getUrl() {
		return "module/soundex/soundex.htm";
	}
	
}
