package net.thebub.privacyproxy.util;

import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataTypes;

public class DataTypeIDs {

	public static int getID(PersonalDataTypes type) {		
		int returnValue = 0;
		
		switch (type) {
		case creditcard:
			returnValue = net.thebub.privacyproxy.R.string.datatype_creditcard;
			break;
		case date:
			returnValue = net.thebub.privacyproxy.R.string.datatype_creditcard;
			break;
		case email:
			returnValue = net.thebub.privacyproxy.R.string.datatype_creditcard;
			break;
		default:
			break;
		}
		
		return returnValue;
	}
	
}
