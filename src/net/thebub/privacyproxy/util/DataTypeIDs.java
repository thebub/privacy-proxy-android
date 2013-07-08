package net.thebub.privacyproxy.util;

import net.thebub.privacyproxy.PrivacyProxyAPI.PersonalDataTypes;

/**
 * This class provides a mapping of the PersonalDataTaypes to the string identifiers
 * @author dbub
 *
 */
public class DataTypeIDs {

	/**
	 * Return the string identifier for the given PersonalDataTypes
	 * @param type 
	 * @return The string identifier
	 */
	public static int getID(PersonalDataTypes type) {		
		int returnValue = 0;
		
		switch (type) {
		case creditcard:
			returnValue = net.thebub.privacyproxy.R.string.datatype_creditcard;
			break;
		case date:
			returnValue = net.thebub.privacyproxy.R.string.datatype_date;
			break;
		case email:
			returnValue = net.thebub.privacyproxy.R.string.datatype_email;
			break;
		default:
			break;
		}
		
		return returnValue;
	}
	
}
