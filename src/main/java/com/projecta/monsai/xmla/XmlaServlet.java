package com.projecta.monsai.xmla;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import mondrian.xmla.SaxWriter;
import mondrian.xmla.XmlaException;
import mondrian.xmla.XmlaHandler;
import mondrian.xmla.impl.DefaultSaxWriter;
import mondrian.xmla.impl.DefaultXmlaServlet;

/**
 * Extension of XmlaServlet that serves /xmla requests and adds a bit of error handling
 *
 */
public class XmlaServlet extends DefaultXmlaServlet {

    private static final long serialVersionUID = -1344224742858747588L;


    @Override
    protected XmlaHandler.ConnectionFactory createConnectionFactory(ServletConfig servletConfig) throws ServletException {
        return new XmlaConnectionFactory();
    }


    /**
     * Patched version of DefaultXmlaServlet that provides some better error messages.
     */
    @Override
    protected void handleFault( HttpServletResponse response, byte[][] responseSoapParts, Phase phase, Throwable t) {

        // Regardless of whats been put into the response so far, clear
        // it out.
        response.reset();

        // NOTE: if you can think of better/other status codes to use
        // for the various phases, please make changes.
        // I think that XMLA faults always returns OK.
        switch (phase) {
        case VALIDATE_HTTP_HEAD:
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            break;
        case INITIAL_PARSE:
        case CALLBACK_PRE_ACTION:
        case PROCESS_HEADER:
        case PROCESS_BODY:
        case CALLBACK_POST_ACTION:
        case SEND_RESPONSE:
            response.setStatus(HttpServletResponse.SC_OK);
            break;
        default:
            break;
        }

        String code;
        String faultCode;
        String faultString;
        String detail;
        if (t instanceof XmlaException) {
            XmlaException xex = (XmlaException) t;
            code = xex.getCode();
            faultString = xex.getFaultString();
            faultCode = XmlaException.formatFaultCode(xex);
            detail = formatDetail(XmlaException.getRootCause(t)); // PATCHED

        } else {
            // some unexpected Throwable
            t = XmlaException.getRootCause(t);
            code = UNKNOWN_ERROR_CODE;
            faultString = UNKNOWN_ERROR_FAULT_FS;
            faultCode = XmlaException.formatFaultCode(SERVER_FAULT_FC, code);
            detail = formatDetail(t);  // PATCHED
        }

        String encoding = response.getCharacterEncoding();

        ByteArrayOutputStream osBuf = new ByteArrayOutputStream();
        try {
            SaxWriter writer = new DefaultSaxWriter(osBuf, encoding);
            writer.startDocument();
            writer.startElement(SOAP_PREFIX + ":Fault");

            // The faultcode element is intended for use by software to provide
            // an algorithmic mechanism for identifying the fault. The faultcode
            // MUST be present in a SOAP Fault element and the faultcode value
            // MUST be a qualified name
            writer.startElement("faultcode");
            writer.characters(faultCode);
            writer.endElement();

            // The faultstring element is intended to provide a human readable
            // explanation of the fault and is not intended for algorithmic
            // processing.
            writer.startElement("faultstring");
            writer.characters(faultString);
            writer.endElement();

            // The faultactor element is intended to provide information about
            // who caused the fault to happen within the message path
            writer.startElement("faultactor");
            writer.characters(FAULT_ACTOR);
            writer.endElement();

            // The detail element is intended for carrying application specific
            // error information related to the Body element. It MUST be present
            // if the contents of the Body element could not be successfully
            // processed. It MUST NOT be used to carry information about error
            // information belonging to header entries. Detailed error
            // information belonging to header entries MUST be carried within
            // header entries.
            if (phase != Phase.PROCESS_HEADER) {
                writer.startElement("detail");
                writer.startElement(
                    FAULT_NS_PREFIX + ":error",
                    "xmlns:" + FAULT_NS_PREFIX, MONDRIAN_NAMESPACE);
                writer.startElement("code");
                writer.characters(code);
                writer.endElement(); // code
                writer.startElement("desc");
                writer.characters(detail);
                writer.endElement(); // desc
                writer.endElement(); // error
                writer.endElement(); // detail
            }

            writer.endElement();   // </Fault>
            writer.endDocument();
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn(
                "This should be handled at begin of processing request",
                uee);
        } catch (Exception e) {
            LOGGER.error(
                "Unexcepted runimt exception when handing SOAP fault :(");
        }

        responseSoapParts[1] = osBuf.toByteArray();
    }


    /**
     * Details formatter for exceptions that shows the class name and the first
     * stack trace element.
     */
    private String formatDetail( Throwable t ) {

        String detail = t.getClass().getName();
        if ( StringUtils.isNotEmpty( t.getMessage() ) ) {
            detail = detail + ": " + t.getMessage();
        }

        if ( t.getStackTrace() != null && t.getStackTrace().length > 0 ) {
            detail = detail + " at " +  t.getStackTrace()[0];
        }

        return detail;
    }

}
