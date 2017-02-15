package spimedb.server.webdav;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spimedb.server.webdav.exceptions.UnauthenticatedException;
import spimedb.server.webdav.exceptions.WebdavException;
import spimedb.server.webdav.fromcatalina.MD5Encoder;
import spimedb.server.webdav.locking.ResourceLocks;
import spimedb.server.webdav.methods.DoCopy;
import spimedb.server.webdav.methods.DoDelete;
import spimedb.server.webdav.methods.DoGet;
import spimedb.server.webdav.methods.DoHead;
import spimedb.server.webdav.methods.DoLock;
import spimedb.server.webdav.methods.DoMkcol;
import spimedb.server.webdav.methods.DoMove;
import spimedb.server.webdav.methods.DoNotImplemented;
import spimedb.server.webdav.methods.DoOptions;
import spimedb.server.webdav.methods.DoPropfind;
import spimedb.server.webdav.methods.DoProppatch;
import spimedb.server.webdav.methods.DoPut;
import spimedb.server.webdav.methods.DoUnlock;

public class WebDavServletBean extends HttpServlet {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(WebDavServletBean.class);

    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest MD5_HELPER;

    /**
     * The MD5 helper object for this class.
     */
    protected static final MD5Encoder MD5_ENCODER = new MD5Encoder();

    private static final boolean READ_ONLY = false;
    private final ResourceLocks _resLocks;
    private IWebdavStore _store;
    private final HashMap<String, DAVMethod> _methodMap = new HashMap<>();

    public WebDavServletBean() {
        _resLocks = new ResourceLocks();

        try {
            MD5_HELPER = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }

    public void init(IWebdavStore store, String dftIndexFile,
            String insteadOf404, int nocontentLenghHeaders,
            boolean lazyFolderCreationOnPut) {

        _store = store;

        IMimeTyper mimeTyper = path -> getServletContext().getMimeType(path);

        register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks,
                mimeTyper, nocontentLenghHeaders));
        register("HEAD", new DoHead(store, dftIndexFile, insteadOf404,
                _resLocks, mimeTyper, nocontentLenghHeaders));
        DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store,
                _resLocks, READ_ONLY));
        DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks,
                doDelete, READ_ONLY));
        register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
        register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
        register("MOVE", new DoMove(_resLocks, doDelete, doCopy, READ_ONLY));
        register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
        register("OPTIONS", new DoOptions(store, _resLocks));
        register("PUT", new DoPut(store, _resLocks, READ_ONLY,
                lazyFolderCreationOnPut));
        register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
        register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
        register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
    }

    private DAVMethod register(String methodName, DAVMethod method) {
        _methodMap.put(methodName, method);
        return method;
    }

    /**
     * Handles the special WebDAV methods.
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String methodName = req.getMethod();
        ITransaction transaction = null;
        boolean needRollback = false;

        if (LOG.isTraceEnabled())
            debugRequest(methodName, req);

        try {
            Principal userPrincipal = req.getUserPrincipal();
            transaction = _store.start(userPrincipal);
            needRollback = true;
            _store.authenticate(transaction);
            resp.setStatus(WebdavStatus.SC_OK);

            try {
                DAVMethod methodExecutor = _methodMap
                        .get(methodName);
                if (methodExecutor == null) {
                    methodExecutor = _methodMap
                            .get("*NO*IMPL*");
                }

                methodExecutor.run(transaction, req, resp);

                _store.commit(transaction);
                needRollback = false;
            } catch (IOException e) {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                LOG.error("IOException: {}", sw.toString());
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                _store.rollback(transaction);
                throw new ServletException(e);
            }

        } catch (UnauthenticatedException e) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } catch (WebdavException e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("WebdavException: {}", sw.toString());
            throw new ServletException(e);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.error("Exception: {}", sw.toString());
        } finally {
            if (needRollback)
                _store.rollback(transaction);
        }

    }

    private static void debugRequest(String methodName, HttpServletRequest req) {
        LOG.trace("-----------");
        LOG.trace("WebdavServlet\n request: methodName = {}", methodName);
        LOG.trace("time: {}", System.currentTimeMillis());
        LOG.trace("path: {}", req.getRequestURI());
        LOG.trace("-----------");
        Enumeration<?> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("header: {} {}", s, req.getHeader(s));
        }
        e = req.getAttributeNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("attribute: {} {}", s, req.getAttribute(s));
        }
        e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.trace("parameter: {} {}", s, req.getParameter(s));
        }
    }

}
