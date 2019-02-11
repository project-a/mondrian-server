package bi.meteorite.license;

/**
 * Dummy implementation of bi.meteorite.license.LicenseException
 *
 * Please consider sponsoring Saiku: https://www.meteorite.bi/products/saiku/sponsorship
 */
public class LicenseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LicenseException(String msg) {
        super(msg);
    }

}
