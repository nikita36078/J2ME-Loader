package javax.microedition.io;

import java.io.IOException;

public interface HttpsConnection extends HttpConnection {
	
	public SecurityInfo getSecurityInfo() throws IOException;
	
	public int getPort();

}
