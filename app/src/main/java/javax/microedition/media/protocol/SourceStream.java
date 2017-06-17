/**
 *  MicroEmulator
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
package javax.microedition.media.protocol;

import java.io.IOException;
import javax.microedition.media.Controllable;

public interface SourceStream extends Controllable {

	public static final int NOT_SEEKABLE = 0;

	public static final int SEEKABLE_TO_START = 1;

	public static final int RANDOM_ACCESSIBLE = 2;
	
	public abstract ContentDescriptor getContentDescriptor();

	public abstract long getContentLength();

	public abstract int read(byte abyte0[], int i, int j) throws IOException;

	public abstract int getTransferSize();

	public abstract long seek(long l) throws IOException;

	public abstract long tell();

	public abstract int getSeekType();

}
