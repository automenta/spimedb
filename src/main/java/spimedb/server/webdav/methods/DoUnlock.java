package spimedb.server.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import spimedb.server.webdav.StoredObject;
import spimedb.server.webdav.ITransaction;
import spimedb.server.webdav.WebdavStatus;
import spimedb.server.webdav.IWebdavStore;
import spimedb.server.webdav.exceptions.LockFailedException;
import spimedb.server.webdav.locking.IResourceLocks;
import spimedb.server.webdav.locking.LockedObject;

public class DoUnlock extends DeterminableMethod {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoUnlock.class);

    private final IWebdavStore _store;
    private final IResourceLocks _resourceLocks;
    private final boolean _readOnly;

    public DoUnlock(IWebdavStore store, IResourceLocks resourceLocks,
            boolean readOnly) {
        _store = store;
        _resourceLocks = resourceLocks;
        _readOnly = readOnly;
    }

    public void run(ITransaction transaction, HttpServletRequest req,
                    HttpServletResponse resp) throws IOException, LockFailedException {
        LOG.trace("-- {}", this.getClass().getName());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else {

            String path = getRelativePath(req);
            String tempLockOwner = "doUnlock" + System.currentTimeMillis()
                    + req.toString();
            try {
                if (_resourceLocks.lock(transaction, path, tempLockOwner,
                        false, 0, TEMP_TIMEOUT, TEMPORARY)) {

                    String lockId = getLockIdFromLockTokenHeader(req);
                    LockedObject lo;
                    if (lockId != null
                            && ((lo = _resourceLocks.getLockedObjectByID(
                                    transaction, lockId)) != null)) {

                        String[] owners = lo.getOwner();
                        String owner = null;
                        if (lo.isShared()) {
                            // more than one owner is possible
                            if (owners != null) {
                                for (String owner1 : owners) {
                                    // remove owner from LockedObject
                                    lo.removeLockedObjectOwner(owner1);
                                }
                            }
                        } else {
                            // exclusive, only one lock owner
                            if (owners != null)
                                owner = owners[0];
                            else
                                owner = null;
                        }

                        if (_resourceLocks.unlock(transaction, lockId, owner)) {
                            StoredObject so = _store.get(
                                    transaction, path);
                            if (so.isNullResource()) {
                                _store.remove(transaction, path);
                            }

                            resp.setStatus(WebdavStatus.SC_NO_CONTENT);
                        } else {
                            LOG.trace("DoUnlock failure at {}", lo.getPath());
                            resp.sendError(WebdavStatus.SC_METHOD_FAILURE);
                        }

                    } else {
                        resp.sendError(WebdavStatus.SC_BAD_REQUEST);
                    }
                }
            } catch (LockFailedException e) {
                e.printStackTrace();
            } finally {
                _resourceLocks.unlockTemporaryLockedObjects(transaction, path,
                        tempLockOwner);
            }
        }
    }

}
