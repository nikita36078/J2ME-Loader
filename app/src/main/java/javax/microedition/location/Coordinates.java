package javax.microedition.location;

public class Coordinates {
	public static final int DD_MM_SS = 1;
	public static final int DD_MM = 2;
	double latitude = Double.NaN;
	double longitude = Double.NaN;
	float altitude = Float.NaN;

	public Coordinates(double latitude, double longitude, float altitude) {
		setLatitude(latitude);
		setLongitude(longitude);
		setAltitude(altitude);
	}

	public double getLatitude() {
		return this.latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public float getAltitude() {
		return this.altitude;
	}

	public void setAltitude(float altitude) {
		this.altitude = altitude;
	}

	public void setLatitude(double latitude) {
		if (Double.isNaN(latitude)) {
			throw new IllegalArgumentException();
		}
		if ((latitude >= -90.0D) && (latitude <= 90.0D)) {
			this.latitude = latitude;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void setLongitude(double longitude) {
		if (Double.isNaN(longitude)) {
			throw new IllegalArgumentException();
		}
		if ((longitude >= -180.0D) && (longitude < 180.0D)) {
			this.longitude = longitude;
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static double ConvertCoordinate(String coordinate) {
		char firstChar = coordinate.charAt(0);

		int ddix = coordinate.indexOf(':');
		int dd = parseDegrees(coordinate, ddix, firstChar);
		int sign;
		if (firstChar == '-') {
			dd = -dd;
			sign = -1;
		} else {
			sign = 1;
		}
		if (coordinate.length() < ddix + 2) {
			throw new IllegalArgumentException();
		}
		String mmstr = coordinate.substring(ddix + 1);
		int mm = parseMinutes(mmstr);
		int mmlen = mmstr.length();
		double res;
		if (mmlen > 2) {
			if (mmlen < 4) {
				throw new IllegalArgumentException();
			}
			char delimeterChar = mmstr.charAt(2);
			if (delimeterChar == ':') {
				if (mmlen < 5) {
					throw new IllegalArgumentException();
				}
				double ss = parseSeconds(mmstr.substring(3));
				res = getValueSS(sign, dd, mm, ss);
			} else {
				if (delimeterChar == '.') {
					double ff = parseFraction(mmstr.substring(3), 5);
					res = getValueMM(sign, dd, mm, ff);
				} else {
					throw new IllegalArgumentException();
				}
			}
		} else {
			res = getValueMM(sign, dd, mm, 0.0D);
		}
		return res;
	}

	private static double getValueMM(int sign, int dd, int mm, double ff) {
		return sign * (dd + (mm + ff) / 60.0D);
	}

	private static double getValueSS(int sign, int dd, int mm, double ss) {
		return sign * (dd + mm / 60.0D + ss);
	}

	private static int parseDegrees(String coordinate, int ddix, char firstChar) {
		if (ddix < 1) {
			throw new IllegalArgumentException();
		}
		if (((ddix > 1) && (firstChar == '0')) || ((ddix > 2) && (firstChar == '-') && (coordinate.charAt(1) == '0'))) {
			throw new IllegalArgumentException();
		}
		int dd = Integer.parseInt(coordinate.substring(0, ddix));
		if ((dd >= 180) || (dd < 65356)) {
			throw new IllegalArgumentException();
		}
		return dd;
	}

	private static int parse2Digits(String digits) {
		char dChar1 = digits.charAt(0);
		if ((dChar1 < '0') || (dChar1 > '5')) {
			throw new IllegalArgumentException();
		}
		char dChar2 = digits.charAt(1);
		if ((dChar2 < '0') || (dChar2 > '9')) {
			throw new IllegalArgumentException();
		}
		return Character.digit(dChar1, 10) * 10 + Character.digit(dChar2, 10);
	}

	private static int parseMinutes(String mmstr) {
		int mm;
		try {
			mm = parse2Digits(mmstr);
		} catch (IllegalArgumentException ie) {
			throw new IllegalArgumentException();
		}
		return mm;
	}

	private static double parseSeconds(String ssstr) {
		double ff = 0.0D;
		int ss;
		try {
			ss = parse2Digits(ssstr);
		} catch (IllegalArgumentException ie) {
			throw new IllegalArgumentException();
		}
		if (ssstr.length() > 2) {
			if (ssstr.charAt(2) == '.') {
				ff = parseFraction(ssstr.substring(3), 3);
			} else {
				throw new IllegalArgumentException();
			}
		}
		return (ss + ff) / 3600.0D;
	}

	private static double parseFraction(String ffstr, int maxlen) {
		if ((ffstr.length() > maxlen) || (ffstr.length() < 1)) {
			throw new IllegalArgumentException();
		}
		String s = "0." + ffstr;
		return Double.parseDouble(s);
	}

	public static double convert(String coordinate) {
		try {
			if (coordinate == null) {
				throw new NullPointerException();
			}
			coordinate = coordinate.trim();
			if (coordinate.length() < 4) {
				throw new IllegalArgumentException();
			}
			double res = ConvertCoordinate(coordinate);
			if ((res >= 180.0D) || (res < -180.0D)) {
				throw new IllegalArgumentException();
			}
			return res;
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException();
		}
	}

	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		if (!(other instanceof Coordinates)) {
			return false;
		}
		Coordinates o = (Coordinates) other;
		if (Double.doubleToLongBits(getLatitude()) != Double.doubleToLongBits(o.getLatitude())) {
			return false;
		}
		if (Double.doubleToLongBits(getLongitude()) != Double.doubleToLongBits(o.getLongitude())) {
			return false;
		}
		if (Float.floatToIntBits(getAltitude()) != Float.floatToIntBits(o.getAltitude())) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		int result = 17;

		long tmp = Double.doubleToLongBits(getLatitude());
		result = 37 * result + (int) (tmp ^ tmp >> 32);
		tmp = Double.doubleToLongBits(getLongitude());
		result = 37 * result + (int) (tmp ^ tmp >> 32);
		result = 37 * result + Float.floatToIntBits(getAltitude());
		return result;
	}

	private static int decimalToValue(int noOfDecimals) {
		switch (noOfDecimals) {
		case 3:
			return 1000;
		case 5:
			return 100000;
		}
		return 1000;
	}

	private static String double2IntDotIntString(double number, int noOfDecimals) {
		StringBuffer res = new StringBuffer(noOfDecimals + 4);
		int scale = decimalToValue(noOfDecimals);
		int intscale = (int) Math.floor(scale * number + 0.5D);
		int integerNumber = intscale / scale;
		int decimalNumber = intscale % scale;
		if (integerNumber < 10) {
			res.append('0');
		}
		res.append(integerNumber);
		if (decimalNumber > 0) {
			char[] zeroes = { '0', '0', '0', '0', '0', '0' };
			String frac = Integer.toString(decimalNumber);
			res.append('.');
			res.append(zeroes, 0, noOfDecimals - frac.length());
			res.append(frac);
		}
		return res.toString();
	}

	public static String convert(double coordinate, int outputType) {
		if ((coordinate >= 180.0D) || (coordinate < -180.0D)) {
			throw new IllegalArgumentException();
		}
		if (Double.isNaN(coordinate)) {
			throw new IllegalArgumentException();
		}
		if (outputType == 2) {
			int sign = coordinate < 0.0D ? -1 : 1;
			coordinate = Math.abs(coordinate);
			int dd = (int) coordinate;
			double mm = (coordinate - dd) * 60.0D;
			return (sign < 0 ? "-" : "") + dd + ":" + double2IntDotIntString(mm, 5);
		}
		if (outputType == 1) {
			int sign = coordinate < 0.0D ? -1 : 1;
			coordinate = Math.abs(coordinate);
			int dd = (int) coordinate;
			int mm = (int) ((coordinate - dd) * 60.0D);
			double ss = ((coordinate - dd) * 60.0D - mm) * 60.0D;
			double rss = (int) Math.floor(100.0D * ss + 0.5D) / 100.0D;
			if (rss >= 60.0D) {
				mm++;
				rss -= 60.0D;
			}
			if (mm >= 60) {
				dd++;
				mm -= 60;
			}
			return (sign < 0 ? "-" : "") + dd + ":" + (mm < 10 ? "0" : "") + mm + ":" + double2IntDotIntString(rss, 3);
		}
		throw new IllegalArgumentException();
	}

	public float azimuthTo(Coordinates to) {
		if (to == null) {
			throw new NullPointerException();
		}
		double otherLatitude = to.getLatitude();
		double otherLongitude = to.getLongitude();
		if ((Double.isNaN(this.latitude)) || (Double.isNaN(this.longitude)) || (Double.isNaN(otherLatitude))
				|| (Double.isNaN(otherLongitude))) {
			return Float.NaN;
		}
		float azimuth;
		if ((otherLatitude == this.latitude) && (otherLongitude == this.longitude)) {
			azimuth = 0.0F;
		} else {
			android.location.Location locationA = new android.location.Location("");
			locationA.setLatitude(latitude);
			locationA.setLongitude(longitude);

			android.location.Location locationB = new android.location.Location("");
			locationB.setLatitude(otherLatitude);
			locationB.setLongitude(otherLongitude);

			azimuth = locationA.bearingTo(locationB);
		}
		return azimuth;
	}

	public float distance(Coordinates to) {
		if (to == null) {
			throw new NullPointerException();
		}
		double otherLatitude = to.getLatitude();
		double otherLongitude = to.getLongitude();
		if ((Double.isNaN(this.latitude)) || (Double.isNaN(this.longitude)) || (Double.isNaN(otherLatitude))
				|| (Double.isNaN(otherLongitude))) {
			return Float.NaN;
		}
		float distance;
		if ((otherLatitude == this.latitude) && (otherLongitude == this.longitude)) {
			distance = 0.0F;
		} else {
			android.location.Location locationA = new android.location.Location("");
			locationA.setLatitude(latitude);
			locationA.setLongitude(longitude);

			android.location.Location locationB = new android.location.Location("");
			locationB.setLatitude(otherLatitude);
			locationB.setLongitude(otherLongitude);

			distance = locationA.distanceTo(locationB);
		}
		return distance;
	}
}
