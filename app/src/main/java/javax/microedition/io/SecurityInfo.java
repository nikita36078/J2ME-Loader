package javax.microedition.io;

import javax.microedition.pki.Certificate;

public interface SecurityInfo {

	public Certificate getServerCertificate();
	
	public String getProtocolVersion();
	
	public String getProtocolName();
	
	public String getCipherSuite();
	
}
