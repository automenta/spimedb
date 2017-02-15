package spimedb.server.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spimedb.server.webdav.DAVMethod;
import spimedb.server.webdav.ITransaction;
import spimedb.server.webdav.WebdavStatus;

public class DoNotImplemented implements DAVMethod {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoNotImplemented.class);
    private final boolean _readOnly;

    public DoNotImplemented(boolean readOnly) {
        _readOnly = readOnly;
    }

    public void run(ITransaction transaction, HttpServletRequest req,
                    HttpServletResponse resp) throws IOException {
        LOG.trace("-- {}", req.getMethod());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
}
